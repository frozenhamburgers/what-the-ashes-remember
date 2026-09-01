#version 330

// Trinity, entire, in one march.
//
// Body, attacks, meltdown detonation and cloud reformation are all integrated
// along the SAME ray in the SAME loop. That is the architectural requirement
// the whole design turns on: overlapping volumetric geometry split across
// independent post chains does not depth-sort against itself, so there is one
// chain and one march, and phases switch components on and off inside it.
//
// Structurally this is post/nuclear/bomb_block/detonation/raymarch.fsh with
// more fields: same interleaved-gradient dither, same distance and procedural
// LOD, same two-lobe transmittance, phase function, powder and local occlusion,
// same premultiplied output. Sharing the lighting is what stops the body, the
// attacks and the detonation reading as three different effects stacked up.
//
// Two things make the extra geometry affordable:
//
//   * PHASE EXCLUSIVITY. The detonation and the attacks are never both up, so
//     the five-field detonation model is not evaluated at all during normal
//     play. Worst case is roughly the standalone chain's cost, not the sum.
//     Phase is a uniform, identical for every fragment, so these branches are
//     perfectly coherent and cost nothing.
//   * PER-RAY CULLING. Testing 32 attack slots at each of up to 72 steps is not
//     viable. Instead each slot's capsule is tested against the ray ONCE, and
//     the march walks only the survivors.

#moj_import <echoesofwar:common_math.glsl>
#moj_import <echoesofwar:post_defaults.glsl>
#moj_import <echoesofwar:noise.glsl>
#moj_import <echoesofwar:nuclear/trinity/instance.glsl>
#moj_import <echoesofwar:nuclear/palette.glsl>
#moj_import <echoesofwar:nuclear/sdf.glsl>
#moj_import <echoesofwar:nuclear/detonation.glsl>
#moj_import <echoesofwar:nuclear/trinity/body.glsl>
#moj_import <echoesofwar:nuclear/trinity/attacks.glsl>
#moj_import <echoesofwar:nuclear/trinity/bullets.glsl>
#moj_import <echoesofwar:nuclear/trinity/reform.glsl>

uniform sampler2D MainDepthSampler;

layout(std140) uniform SamplerInfo {
	vec2 OutSize;
	vec2 InSize;
};

in vec2 texCoord;
out vec4 fragColor;

// How opaque the volume may get with the camera inside it. Same reasoning as
// the standalone chain: a fully opaque near field is a grey screen, not smoke.
const float INSIDE_ALPHA_MAX = 0.92;

// Most a single ray will ever march. A ray grazing the containment lattice can
// cross a lot of beams; past this the furthest are simply dropped, which is
// invisible because they are behind everything else that survived.
//
// Raised with concurrent scheduling: two or three patterns overlap now, so a
// ray through the lattice can also be crossing a bullet field.
const int MAX_MARCHED_ATTACKS = 16;

// Step budget when no detonation is up. Far above the detonation's 72 because
// beams are thin and the march has to resolve them, and affordable because
// almost every sample rejects on a cheap bounds test long before it reaches any
// noise - only samples genuinely inside a volume pay for anything.
const int TRINITY_MAX_STEPS = 160;

void main() {
	vec3 rayOrigin = cameraPos;
	vec3 rayDir = normalize(getFarWorldPos(texCoord) - cameraPos);

	float tMaxScene = getDepth(MainDepthSampler, texCoord) < 0.9999
			? length(getWorldPos(MainDepthSampler, texCoord, invProjMat, invViewMat, cameraPos) - cameraPos)
			: RENDER_DISTANCE_CAP;

	float dither = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));

	vec3 acc = vec3(0.0);
	float trans = 1.0;
	float inside = 0.0;

	if (count <= 0) {
		fragColor = vec4(0.0);
		return;
	}

	// --- phase gates -------------------------------------------------------
	// Uniform, so these branches are coherent across the whole draw.
	int phase = tPhase();
	float bodyOn = (phase == PHASE_DETONATING || phase == PHASE_DYING) ? 0.0 : 1.0;
	if (tBodyScale() <= 0.001) bodyOn = 0.0;
	// Attacks keep drawing through the meltdown so a pattern caught by one
	// dissolves instead of vanishing between frames.
	float attacksOn = (phase == PHASE_FIGHTING || phase == PHASE_MELTDOWN) ? 1.0 : 0.0;
	// The detonation keeps drawing at FULL strength through the reformation.
	// It used to be faded out by the reform parameter, and that fade was the
	// single biggest reason the transition read wrongly: the cloud lost alpha
	// where it stood while a separate funnel appeared beside it. Nothing is
	// faded now. The cloud leaves because its own geometry contracts into
	// Trinity - see applyReformCollapse in nuclear/trinity/reform.glsl.
	float detOn = tDetActive() > 0.5 ? 1.0 : 0.0;

	vec3 centre = tCentre();
	float bodyR = tBodyR();

	// --- attack culling and bounds -----------------------------------------
	// Both at once, once per pixel. Each volume contributes the interval of the
	// ray actually inside it AND the finest feature that interval contains, so
	// the march can size its step to the geometry present rather than to a
	// sphere enclosing everything.
	float tEnter = 1e30;
	float tExit = -1e30;
	float feature = 1e30;

	int survivors[MAX_MARCHED_ATTACKS];
	int nSurv = 0;
	if (attacksOn > 0.0) {
		int n = tAttackCount();
		for (int s = 0; s < TRINITY_ATTACK_SLOTS; s++) {
			if (s >= n || nSurv >= MAX_MARCHED_ATTACKS) break;
			int a = attackBase(s);
			if (aType(a) == ATK_NONE) continue;
			if (aFade(a) >= 0.996) continue;
			float reach = aReach(a);
			if (reach <= 0.01) continue;

			vec3 pa = aRoot(a);
			vec3 pb = pa + aDir(a) * reach;
			// Grown by ATK_BOUND so the noise warp cannot push the volume
			// outside the bound the cull was based on.
			float rad = aRadius(a) * ATK_BOUND;

			float e, x;
			if (rayCapsuleInterval(rayOrigin, rayDir, pa, pb, rad, e, x)) {
				survivors[nSurv++] = a;
				tEnter = min(tEnter, e);
				tExit = max(tExit, x);
				// Half a radius: enough samples across a beam to resolve it
				// rather than alias it into stripes.
				feature = min(feature, max(aRadius(a) * 0.5, 0.30));
			}
		}
	}

	// The projectile field is bounded by the sphere its furthest pulse has
	// reached, which is generous - but the shell tests inside it reject almost
	// every sample long before any projectile is generated, and there is no
	// tighter bound available for a field that surrounds the camera.
	bool haveField = attacksOn > 0.0 && tBfActive() > 0.5 && tBfCut() < 1.0;
	if (haveField) {
		float outer = tBfLaunch() + tBfTravel() + tBfRadius() * 2.5;
		float e, x;
		if (intersectSphereBounds(rayOrigin, rayDir, centre, outer, e, x)) {
			tEnter = min(tEnter, e);
			tExit = max(tExit, x);
			feature = min(feature, max(tBfRadius() * 0.5, 0.30));
		} else {
			haveField = false;
		}
	}

	if (bodyOn > 0.0) {
		float e, x;
		if (intersectSphereBounds(rayOrigin, rayDir, centre, max(bodyR * BODY_BOUND, 0.5), e, x)) {
			tEnter = min(tEnter, e);
			tExit = max(tExit, x);
			feature = min(feature, max(bodyR * BODY_GRAIN * 0.6, 0.35));
		}
	}

	Detonation det;
	bool haveDet = false;
	if (detOn > 0.001) {
		// Ground zero is the Crucible on the ground, NOT Trinity: the column
		// rises PAST Trinity's position, which is what the model is tuned for
		// and what the reformation then funnels back down from.
		vec3 detBase = tDetBase();
		det = setupDetonation(detBase, tSeed(), tDetAge(), tDetLifetime(),
				tDetH(), tDetR1(), tDetR0(), tDetIntensity());
		det.groundAmt *= tDetSurge();
		// Contracts the plume and the cap back toward Trinity. A no-op outside
		// the reformation, and applied BEFORE the bounds below so the marched
		// volume tightens along with the cloud instead of staying sized for the
		// explosion that is no longer there.
		applyReformCollapse(det);

		if (det.lf < 1.0 && det.intensity > 0.001) {
			haveDet = true;
			float e, x;
			if (intersectColumnBounds(rayOrigin, rayDir, detBase,
					detonationBoundRadius(det), -3.5, detonationBoundTop(det), e, x)) {
				tEnter = min(tEnter, e);
				tExit = max(tExit, x);
				feature = min(feature, max(det.grain * 0.9, 0.5));
			} else {
				haveDet = false;
			}
		}
	}

	tExit = min(tExit, tMaxScene);
	if (tEnter >= tExit || feature > 1e29) {
		fragColor = vec4(0.0);
		return;
	}

	// One density tap at the eye, so standing inside Trinity fades the ceiling
	// in rather than snapping it on at the boundary.
	inside = clamp((bodyDensity(cameraPos, 0.0) * bodyOn
			+ (haveDet ? densityAt(det, cameraPos, 0.0) * detOn : 0.0)) * 2.2, 0.0, 1.0);

	float span = tExit - tEnter;

	// Distance LOD relaxes the FEATURE SIZE rather than cutting a fixed number
	// of steps. Dropping steps on a fixed span is what made distant beams
	// disappear outright: the step grew past their radius and the march simply
	// missed them. Coarsening the feature instead degrades gracefully, because
	// the span shrinks with distance too.
	float lod = mix(1.0, 3.0, clamp((tEnter - 64.0) / 300.0, 0.0, 1.0));
	float pixW = (tEnter + tExit) * abs(invProjMat[1][1]) / max(OutSize.y, 1.0);
	float target = max(feature * lod, pixW * 0.75);

	// The detonation is expensive per sample (five fields plus shadow marches),
	// so it keeps the original budget. Body and attacks are cheap - almost every
	// sample rejects on a bounds test before touching any noise - so they can
	// afford the finer march that thin geometry needs.
	int maxSteps = haveDet ? MAX_STEPS : TRINITY_MAX_STEPS;
	int steps = int(clamp(ceil(span / max(target, 0.05)),
			float(MIN_STEPS), float(maxSteps)));
	float dt = span / float(steps);

	// Procedural LOD: detail finer than a step or an output pixel cannot be
	// resolved, and sampling it anyway only converts it into noise.
	float grain = haveDet ? det.grain : max(bodyR * BODY_GRAIN, 0.5);
	float detail = clamp(1.35 - max(dt, pixW) * F_FINE / max(grain, 0.01), 0.0, 1.0);

	float t = tEnter + dt * dither;

	for (int i = 0; i < TRINITY_MAX_STEPS; i++) {
		if (i >= steps || trans < 0.015) break;
		vec3 p = rayOrigin + rayDir * t;
		t += dt;

		// --- density -------------------------------------------------------
		float dens = 0.0;
		vec3 gsrc = centre;
		float glow = 0.0;

		if (bodyOn > 0.0) {
			dens = bodyDensity(p, detail);
			glow = bodyCoreGlow(p) + telegraphGlow(p) + bulletFieldTelegraph(p);
		}

		if (haveField) {
			vec3 fsrc;
			float fglow;
			float fd = bulletFieldDensity(p, detail, fsrc, fglow);
			if (fd > 0.0) dens = max(dens, fd);
			if (fglow > glow) { glow = fglow; gsrc = fsrc; }
		}

		if (nSurv > 0) {
			for (int k = 0; k < MAX_MARCHED_ATTACKS; k++) {
				if (k >= nSurv) break;
				int a = survivors[k];
				float ad = attackDensity(a, p, detail);
				if (ad <= 0.0) continue;
				// Smooth-max against the body so a spike's root melts into the
				// sphere instead of showing a crease where the two meet; plain
				// max between attacks, which are the same material.
				dens = smaxDensity(dens, ad, 0.22);

				vec3 asrc;
				float ag = attackGlow(a, p, asrc);
				if (ag > glow) { glow = ag; gsrc = asrc; }
			}
		}

		if (haveDet) {
			float dd = densityAt(det, p, detail) * detOn;
			if (dd > 0.0) {
				// Smooth-maxed rather than plain-maxed against the body, which
				// only matters during the reformation - it is what lets the
				// last of the collapsing stalk merge into the sphere gathering
				// underneath it instead of showing a seam between the two.
				dens = smaxDensity(dens, dd, 0.30);
				vec3 dsrc;
				float dg = glowField(det, p, dsrc);
				if (dg > glow) { glow = dg; gsrc = dsrc; }
			}
		}

		if (dens <= 0.004) continue;

		// --- lighting ------------------------------------------------------
		// Identical model to the standalone chain. The one departure is that
		// attacks are NOT sampled by the shadow marches: they are thin, bright
		// and fast, a correct shadow cast through one is imperceptible, and
		// including them would put the whole attack loop inside the five-tap
		// shadow function - which is exactly where the cost would explode.
		bool cheapLight = trans < 0.12 || dens < 0.03;
		bool litByGlow = glow > 0.004;

		float sunOD, glowOD;
		if (haveDet) {
			sunOD = cheapLight ? sunDepthCheap(det, dens) : sunDepth(det, p, dens);
			glowOD = (litByGlow && !cheapLight)
					? glowDepth(det, p, dens, gsrc)
					: glowDepthCheap(det, dens);
		} else {
			// No detonation to march against, so optical depth is estimated
			// from local density alone. The body is a single convex mass and
			// the attacks are excluded anyway, so there is very little a shadow
			// ray would find here that this does not already capture.
			sunOD = dens * SUN_SHADOW_DEPTH * SUN_CHEAP_FRAC * 0.5;
			glowOD = dens * GLOW_SHADOW_DEPTH * GLOW_CHEAP_FRAC * 0.5;
		}

		float sT = exp(-sunOD);
		float sMs = exp(-sunOD * 0.26);
		float gT = exp(-glowOD);
		float gMs = exp(-glowOD * 0.26);

		vec3 toSrc = gsrc - p;
		float srcDist = length(toSrc);
		float cosT = srcDist > 1e-4 ? dot(toSrc / srcDist, rayDir) : 0.0;
		float phaseF = mix(1.0, hg(cosT, 0.42), 0.62);
		float sPhase = mix(1.0, hg(dot(SUN_DIR, rayDir), 0.25), 0.35);
		vec3 glowCol = mix(GLOW_COLOR, GLOW_HOT, clamp(glow * 0.42, 0.0, 1.0));

		// Emission gated on local density, so the core can only be seen where
		// there is smoke to scatter it - it cannot paint a halo in clear air.
		acc += trans * glowCol * (glow * dens * 0.42 * dt);

		float powder = 1.0 - exp(-dens * 3.4);
		float pw = mix(0.52, 1.0, powder);
		float localOcc = mix(0.24, 1.0, exp(-dens * 1.55));
		float ao = (0.05 + 0.95 * sMs) * localOcc;

		vec3 L = (SMOKE_SHADOW + SKY_COLOR * 0.40) * ao
		       + SUN_COLOR * ((sT * sPhase * 1.15 + sMs * 0.09) * pw)
		       + glowCol * (glow * (gT * phaseF * 1.24 + gMs * 0.18) * pw);

		float a = 1.0 - exp(-dens * SIGMA_T * dt);
		acc += trans * L * a;
		trans *= 1.0 - a;
	}

	if (trans < 0.015) trans = 0.0;

	fragColor = vec4(acc, min(1.0 - trans, mix(1.0, INSIDE_ALPHA_MAX, inside)));
}
