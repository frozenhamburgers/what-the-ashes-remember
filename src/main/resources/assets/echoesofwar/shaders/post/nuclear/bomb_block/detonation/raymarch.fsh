#version 330

// The volumetric pass: marches the view ray through the detonation and writes a
// PREMULTIPLIED radiance/coverage pair for the composite to lay over the scene.
//
// Structurally the same march as post/apophis/smog/raymarch.fsh - same dither,
// same distance and procedural LOD, same two-lobe lighting - over a five-field
// model instead of a two-field one. The bounding volume is a vertical cylinder
// rather than a sphere, because a 200-block column inside a bounding sphere
// spends most of every ray on empty air.

#moj_import <echoesofwar:nuclear/bomb_block/instance.glsl>
#moj_import <echoesofwar:nuclear/detonation.glsl>

uniform sampler2D MainDepthSampler;

// OutSize is THIS pass's output target, the low-res march buffer.
// InSize is its one input.
layout(std140) uniform SamplerInfo {
	vec2 OutSize;
	vec2 InSize;
};

in vec2 texCoord;
out vec4 fragColor;

// How opaque the volume is allowed to get with the camera inside it.
const float INSIDE_ALPHA_MAX = 0.92;

void main() {
	vec3 rayOrigin = cameraPos;
	vec3 rayDir = normalize(getFarWorldPos(texCoord) - cameraPos);

	// Clip the march at whatever solid geometry the ray hits, if any.
	float tMaxScene = getDepth(MainDepthSampler, texCoord) < 0.9999
			? length(getWorldPos(MainDepthSampler, texCoord, invProjMat, invViewMat, cameraPos) - cameraPos)
			: RENDER_DISTANCE_CAP;

	// Interleaved gradient noise. Offsetting each ray start inside its own step
	// trades banding for a fine stipple, which the low-res upscale then blurs
	// away. Deliberately not animated - at this resolution a temporal jitter
	// shimmers instead of resolving.
	float dither = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));

	vec3 acc = vec3(0.0);      // premultiplied radiance
	float trans = 1.0;         // transmittance along the view ray
	float inside = 0.0;        // how buried in smoke the camera itself is

	for (int instance = 0; instance < count; instance++) {
		if (trans < 0.015) break;

		int idx = instance * BOMB_DATA_SIZE;
		vec3 base = vec3(data[idx], data[idx + 1], data[idx + 2]);
		Detonation d = setupDetonation(
			base,
			data[idx + 3],   // seed
			data[idx + 4],   // age, seconds
			data[idx + 5],   // lifetime, seconds
			data[idx + 6],   // H  - nominal cap height
			data[idx + 7],   // R1 - nominal cap radius
			data[idx + 8],   // R0 - vent radius
			data[idx + 9]);  // intensity
		if (d.lf >= 1.0 || d.intensity <= 0.001) continue;

		// One density tap at the eye. The only thing an opacity ceiling is
		// actually needed for is standing in the smoke - a fully opaque near
		// field is a grey screen, not a cloud - and that case is worth
		// identifying properly rather than taxing every ray for it.
		inside = max(inside, clamp(densityAt(d, cameraPos, 0.0) * 2.2, 0.0, 1.0));

		float tEnter, tExit;
		if (!intersectColumnBounds(rayOrigin, rayDir, base,
				detonationBoundRadius(d), -3.5, detonationBoundTop(d), tEnter, tExit)) continue;
		tExit = min(tExit, tMaxScene);
		if (tEnter >= tExit) continue;

		// Distance LOD. The ramp starts further out than the Apophis one because
		// this volume is far larger - at 64 blocks you are still inside the base
		// surge, not looking at it from across the map.
		int steps = int(mix(float(MAX_STEPS), float(MIN_STEPS),
				clamp((tEnter - 64.0) / 300.0, 0.0, 1.0)));
		float dt = (tExit - tEnter) / float(steps);

		// Procedural LOD: detail finer than the march step or one output pixel
		// cannot be resolved, and sampling it anyway only converts it into noise.
		// invProjMat[1][1] is tan(fovY/2), so this is the world height one pixel
		// of the low-res target covers at the cloud.
		float pixW = (tEnter + tExit) * abs(invProjMat[1][1]) / max(OutSize.y, 1.0);
		float lodSize = max(dt, pixW);
		float detail = clamp(1.35 - lodSize * F_FINE / d.grain, 0.0, 1.0);
		float t = tEnter + dt * dither;

		for (int i = 0; i < MAX_STEPS; i++) {
			if (i >= steps || trans < 0.015) break;
			vec3 p = rayOrigin + rayDir * t;
			t += dt;

			float dens = densityAt(d, p, detail);
			if (dens <= 0.004) continue;

			vec3 gsrc;
			float g = glowField(d, p, gsrc);

			// Deep inside the volume, estimate optical depth from local density
			// instead of paying for shadow rays. Still tracks density, unlike a
			// flat fallback that would make interiors read as uniform.
			bool cheapLight = trans < 0.12 || dens < 0.03;
			bool litByGlow = g > 0.004;

			float sunOD = cheapLight
					? sunDepthCheap(d, dens)
					: sunDepth(d, p, dens);
			float glowOD = (litByGlow && !cheapLight)
					? glowDepth(d, p, dens, gsrc)
					: glowDepthCheap(d, dens);

			// Two-lobe transmittance: a sharp direct lobe throws hard shadows, and
			// a multiple-scattering lobe at a fifth of the extinction fills deep
			// pockets with dim coloured light instead of crushing to black. This
			// is what carries the fireball's light through the smoke, rather than
			// the core being drawn as a glowing surface.
			float sT  = exp(-sunOD);
			// The multiple-scattering lobe is what fills a crevice back in, so how
			// far it penetrates sets how deep a crevice is allowed to get. At a
			// fifth of the extinction it barely darkens at all across the depth
			// range a crevice spans, which put a bright directionless floor under
			// every shaded face. A quarter costs the lit faces almost nothing -
			// their optical depth is low either way - and lets the shaded ones
			// actually fall away.
			float sMs = exp(-sunOD * 0.26);
			float gT  = exp(-glowOD);
			float gMs = exp(-glowOD * 0.26);

			vec3 toSrc = gsrc - p;
			float srcDist = length(toSrc);
			float cosT = srcDist > 1e-4 ? dot(toSrc / srcDist, rayDir) : 0.0;
			float phase = mix(1.0, hg(cosT, 0.42), 0.62);
			// Mild forward scattering on the sun too, separating lit/shaded faces
			// further when backlit.
			float sPhase = mix(1.0, hg(dot(SUN_DIR, rayDir), 0.25), 0.35);
			vec3 glowCol = mix(GLOW_COLOR, GLOW_HOT, clamp(g * 0.42, 0.0, 1.0));

			// Emission gated on local density, so the core can only ever be seen
			// where there is smoke to scatter it - it cannot paint a halo in clear
			// air. Thin smoke still reveals it: the ray keeps most of its
			// transmittance through a thin patch and goes on to pick up the much
			// brighter samples deeper inside.
			acc += trans * glowCol * (g * dens * 0.42 * dt);

			// Powder: too little medium at a thin edge to scatter much, so edges
			// facing the light stay mildly dark.
			float powder = 1.0 - exp(-dens * 3.4);
			float pw = mix(0.52, 1.0, powder);

			// Fine-scale self-occlusion, below what a shadow ray can afford to
			// resolve; gives the interior detail beyond the silhouette.
			float localOcc = mix(0.24, 1.0, exp(-dens * 1.55));

			// Ambient has to work its way in through the same medium, so occlude
			// it with the soft lobe rather than letting a flat floor sit under
			// every shadow in the volume. Weighted well down from the first pass:
			// at 0.68 the sky term alone matched a fully lit face, so it flooded
			// every crevice back up to roughly the brightness of the flank beside
			// it and the terminator disappeared. The volume gets its exposure from
			// the key now, not from an omnidirectional fill.
			float ao = (0.05 + 0.95 * sMs) * localOcc;

			vec3 L = (SMOKE_SHADOW + SKY_COLOR * 0.40) * ao
			       + SUN_COLOR * ((sT * sPhase * 1.15 + sMs * 0.09) * pw)
			       + glowCol * (g * (gT * phase * 1.24 + gMs * 0.18) * pw);

			float a = 1.0 - exp(-dens * SIGMA_T * dt);
			acc += trans * L * a;
			trans *= 1.0 - a;
		}
	}


	// The early-out is its own quiet ceiling: breaking at trans < 0.015 leaves
	// 1.5% of the background showing through a volume the march has already
	// decided is opaque. Since that is exactly the condition for breaking,
	// spend it here rather than carrying it to the composite.
	if (trans < 0.015) trans = 0.0;

	// Opacity is whatever the march accumulated - EXCEPT with the camera in the
	// smoke, where it is held under a ceiling so the world stays faintly
	// readable through it. Scaled by how buried the eye is, so walking into the
	// cloud fades the ceiling in rather than snapping it on at the boundary.
	fragColor = vec4(acc, min(1.0 - trans, mix(1.0, INSIDE_ALPHA_MAX, inside)));
}
