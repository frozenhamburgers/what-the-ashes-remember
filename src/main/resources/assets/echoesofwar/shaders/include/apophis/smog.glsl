// ---------------------------------------------------------------------------
// The volumetric smog model: the cloud, the jet, and the lighting that resolves
// them. Used only by the two marching passes, post/apophis/smog/raymarch.fsh and
// post/apophis/smog/shadow.fsh.
// ---------------------------------------------------------------------------

#moj_import <echoesofwar:common_math.glsl>
#moj_import <echoesofwar:post_defaults.glsl>
#moj_import <echoesofwar:noise.glsl>
#moj_import <echoesofwar:apophis/palette.glsl>

// Packed by ShaderDataBuffer: 2 instances * 13 floats/instance.
layout(std140) uniform InstanceData {
    int count;
    float data[26];
};

// ---------------------------------------------------------------------------
// Apophis's smog. Two volumes sharing one march and one lighting model:
// the CLOUD, a ball of settled smoke centred on wherever Apophis climbed to,
// which swells as it is fed, the JET, a cone from Apophis's mouth to that ball,
// present only while it is actively spewing, whose noise lattice slides along
// the cone's axis so thesmoke visibly streams from the head into the cloud.
//
// Both are smooth envelopes displaced by billow noise, giving packed cauliflower
// lobes with creases between them.
// ---------------------------------------------------------------------------

const int   MAX_STEPS           = 64;   // primary march budget (per instance)
const int   MIN_STEPS           = 26;   // floor after distance LOD
const float RENDER_DISTANCE_CAP = 512.0;

// Extinction, slightly below the eruption's: this is a hanging bank of smoke
// meant to be flown through, not a wall.
const float SIGMA_T = 2.05;

// Noise frequencies, in lattice units (grain sets the unit's size in blocks).
const float F_MACRO = 0.26; // large masses
const float F_LOBE  = 0.70; // billowing lobes
const float F_FINE  = 1.50; // turbulent detail; faded out by the distance LOD

// How fast the jet's lattice slides along its own axis, in lattice units/second.
const float JET_FLOW = 5.5;
// Cone radius where it leaves the mouth, in blocks.
const float MOUTH_RADIUS = 3.0;
// How fast the jet widens per block travelled.
const float JET_SPREAD = 0.20;
// Extra flare toward the far end, as a multiplier on the spread.
const float JET_FLARE = 1.35;
// Size of one lattice unit inside the jet, in blocks. Fixed rather than derived
// from the cloud radius, since the jet exists before the cloud does.
const float JET_GRAIN = 5.5;
// How far past the leading edge the jet's density is feathered out, in blocks.
const float FRONT_SOFTNESS = 6.0;

// --- terrain shadow (see terrainShadow) ---
const int   SHADOW_STEPS    = 10;
// Scales the optical depth the shadow march accumulates.
const float SHADOW_STRENGTH = 0.55;
// How much light still reaches the ground under the thickest part of the cloud.
const float SHADOW_MIN_LIGHT = 0.14;

// Ray/sphere intersection, used to bound the raymarch loop.
bool intersectSphere(vec3 ro, vec3 rd, vec3 c, float r, out float tEnter, out float tExit) {
	vec3 oc = ro - c;
	float b = dot(oc, rd);
	float cc = dot(oc, oc) - r * r;
	float disc = b * b - cc;
	if (disc < 0.0) return false;
	float sq = sqrt(disc);
	tEnter = max(-b - sq, 0.0);
	tExit = -b + sq;
	return tEnter < tExit;
}

// ---------------------------------------------------------------------------
// Smog model
// ---------------------------------------------------------------------------

struct Smog {
	vec3  center;    // centre of the settled cloud
	float R;         // its current radius, grown over the flight
	vec3  mouth;     // Apophis's head, live - the jet's origin
	float emit;      // 1 while spewing, easing to 0 once it stops
	float seed;
	float t;         // age, seconds
	float density;   // 1 while intact, easing to 0 as the cloud dissipates
	float grain;     // size of one noise lattice unit, in blocks
	vec3  axis;      // mouth -> centre, normalised
	float jetLen;    // distance from mouth to centre
	float frontLen;  // how far along that the emitted smoke has actually reached
	float coneMaxR;  // jet radius at frontLen, for the bounding sphere
	float erode;     // how far dissipation has eaten into the field
	float jetT;      // seconds the mouth has been emitting, ungated unlike 't'
	                 // (see ApophisSmogFx#jetAge)
};

// Jet radius at 'along' blocks from the mouth. Parameterised on the full
// mouth-to-cloud distance so extending the jet slides its tip through a cone
// of fixed shape.
float jetRadiusAt(Smog s, float along) {
	float u = clamp(along / max(s.jetLen, 0.001), 0.0, 1.0);
	return MOUTH_RADIUS + JET_SPREAD * along * mix(1.0, JET_FLARE, u);
}

Smog setupSmog(vec3 center, float R, vec3 mouth, float emit, float seed, float t, float density,
		float frontDistance, float jetT) {
	Smog s;
	s.center = center;
	s.mouth = mouth;
	s.seed = fract(abs(seed) * 0.0143) * 53.0; // wrapped to avoid float precision loss
	s.t = t;
	s.jetT = jetT;
	s.density = clamp(density, 0.0, 1.0);
	s.R = max(R, 0.0);

	// One lattice unit, in blocks. Scaled with the cloud so the number of visible
	// lobes stays roughly constant regardless of cloud size.
	s.grain = max(s.R * 0.145, 1.6);

	vec3 toCloud = center - mouth;
	s.jetLen = length(toCloud);
	s.axis = s.jetLen > 1.0 ? toCloud / s.jetLen : vec3(0.0, 1.0, 0.0);
	// Clamped to the mouth-to-cloud distance so the jet stops growing once it has
	// bridged the gap.
	s.frontLen = clamp(frontDistance, 0.0, s.jetLen);
	s.coneMaxR = jetRadiusAt(s, s.frontLen) + FRONT_SOFTNESS;
	// The jet is absorbed into the cloud as the mouth arrives inside it, faded on
	// the mouth's distance rather than a threshold since both the cloud radius and
	// the mouth position move. Radius floored first to avoid smoothstep dividing
	// by zero.
	float swallowR = max(s.R, 1.0);
	s.emit = clamp(emit, 0.0, 1.0) * smoothstep(swallowR * 0.35, swallowR * 0.85, s.jetLen);

	// Dissipation is applied as erosion of the density field rather than a fade on
	// alpha, so the cloud breaks into thinning wisps instead of a uniform fade.
	s.erode = 1.0 - s.density;
	return s;
}

// The settled cloud. Slightly oblate
float cloudDensity(Smog s, vec3 p, float detail) {
	// nothing at all until the jet has reached the destination - see ApophisSmogWorldEvent
	if (s.R <= 0.001) return 0.0;

	vec3 q = p - s.center;
	float shape = 1.0 - length(vec3(q.x, q.y * 1.22, q.z)) / s.R;
	if (shape < -0.60) return 0.0;

	// Lattice anchored to the cloud, drifting slowly upward.
	vec3 m = (q + vec3(0.0, -s.t * 0.30, 0.0)) / s.grain
	       + vec3(s.seed * 3.7, 0.0, s.seed * 2.3);
	vec3 evo = vec3(s.t * 0.07, s.t * 0.03, s.t * -0.05);

	float macro = fbm2(m * F_MACRO);
	float lobe  = billow2(m * F_LOBE + evo * 0.40);

	// Shape term capped so noise can carve cavities through the middle instead of
	// a solid ball with a billowy rind.
	float field = min(shape, 0.55) * 1.45
	            + (macro - 0.46) * 0.98
	            + (lobe  - 0.40) * 0.80;

	if (detail > 0.01) {
		float fine = billow2(m * F_FINE + evo * 1.05);
		field += (fine - 0.42) * 0.30 * detail;
	}

	field -= s.erode * 1.20;
	// Threshold widens as it erodes so what's left goes soft and ragged rather
	// than keeping a crisp edge until it vanishes.
	return smoothstep(0.0, mix(0.26, 0.54, s.erode), field);
}

// The jet from the mouth. Its own field rather than more of the cloud, since its
// lattice has to translate along the cone's axis to read as streaming.
float jetDensity(Smog s, vec3 p, float detail) {
	if (s.emit <= 0.004 || s.frontLen <= 0.001) return 0.0;

	// Feather on the leading edge capped to half the jet's length, so the jet
	// grows out of nothing instead of popping in several blocks long.
	float frontSoft = min(FRONT_SOFTNESS, s.frontLen * 0.5);

	vec3 q = p - s.mouth;
	float along = dot(q, s.axis);
	if (along < -2.0 || along > s.frontLen + frontSoft) return 0.0;

	float radial = length(q - s.axis * along);
	float coneR = jetRadiusAt(s, along);
	float shape = 1.0 - radial / max(coneR, 0.001);
	// Taper the start so it emerges from the mouth instead of a flat slice.
	shape -= (1.0 - smoothstep(-2.0, 3.0, along)) * 1.30;
	// Taper the leading edge into a rounded head instead of a flat disc.
	shape -= smoothstep(s.frontLen - frontSoft, s.frontLen + frontSoft * 0.35, along) * 1.60;
	if (shape < -0.70) return 0.0;

	// Axis scroll applied in lattice space, after the /JET_GRAIN divide, so
	// JET_FLOW is actually lattice units/second.
	vec3 m = q / JET_GRAIN - s.axis * (s.jetT * JET_FLOW)
	       + vec3(s.seed * 1.9, s.seed * 4.3, 0.0);
	vec3 evo = vec3(s.jetT * 0.30, s.jetT * -0.18, s.jetT * 0.22);

	float macro = fbm2(m * F_MACRO);
	float lobe  = billow2(m * F_LOBE + evo * 0.35);

	float field = min(shape, 0.60) * 1.40
	            + (macro - 0.46) * 0.86
	            + (lobe  - 0.40) * 0.74;

	if (detail > 0.01) {
		float fine = billow2(m * F_FINE + evo);
		field += (fine - 0.42) * 0.28 * detail;
	}

	field -= s.erode * 1.20;
	return smoothstep(0.0, 0.30, field) * s.emit;
}

float densityAt(Smog s, vec3 p, float detail) {
	return min(cloudDensity(s, p, detail) + jetDensity(s, p, detail), 1.40);
}

// Irradiance from the smog's own heat, ignoring occlusion. Two emitters: the body of the cloud
// smoulders throughout, and while Apophis is spewing there is a much hotter, much smaller source
// right at its mouth. `src` returns whichever dominates here, for the shadow march to aim at.
float glowField(Smog s, vec3 p, out vec3 src) {
	// Cloud smoulder. A broad source rather than a point.
	float gr = max(s.R * 0.62, 1.0);
	float dg = length(p - s.center) / gr;
	// Inverse-square core with a gaussian cutoff to kill the tail by ~3 radii.
	float gi = s.density * 0.95 / (1.0 + dg * dg * 2.2) * exp(-dg * dg * 0.40);

	// The ember at the mouth, only while something is actually coming out of it.
	vec3 cp = s.mouth + s.axis * (MOUTH_RADIUS * 0.6);
	float cr = max(MOUTH_RADIUS * 1.7, 1.0);
	float dc = length(p - cp) / cr;
	float ci = s.emit * 1.45 / (1.0 + dc * dc * 2.2) * exp(-dc * dc * 0.40);

	src = ci > gi ? cp : s.center;
	return gi + ci;
}

// Optical depth between p and a light, on a geometric partition. Segment
// bounds are 0, 0.7u, 2.1u, 5.6u, 14u with taps at the midpoints: the near
// pair resolves occlusion at the scale density actually varies at, the last
// still reaches the next mass over.
float shadowDepth(Smog s, vec3 p, float dens, vec3 dir, float reach) {
	float u = reach * (1.0 / 14.0);
	// First segment anchored on the sample's own density rather than a tap.
	return dens                                    * (u * 0.30)
	     + densityAt(s, p + dir * (u * 0.45), 0.0) * (u * 0.40)
	     + densityAt(s, p + dir * (u * 1.40), 0.0) * (u * 1.40)
	     + densityAt(s, p + dir * (u * 3.85), 0.0) * (u * 3.50)
	     + densityAt(s, p + dir * (u * 9.80), 0.0) * (u * 8.40);
}

float sunDepth(Smog s, vec3 p, float dens) {
	// floored on the jet's own scale for when there is no cloud radius yet
	return shadowDepth(s, p, dens, SUN_DIR, max(s.R * 0.85, JET_GRAIN * 3.0)) * SIGMA_T * 0.78;
}

float glowDepth(Smog s, vec3 p, float dens, vec3 src) {
	vec3 dir = src - p;
	float dist = length(dir);
	if (dist < 1e-3) return 0.0;
	return shadowDepth(s, p, dens, dir / dist, min(dist, max(s.R * 1.10, JET_GRAIN * 3.0)))
	     * SIGMA_T * 0.80;
}

/**
 * How much of the overhead sun still reaches a point on solid geometry.
 *
 * Only the cloud casts this
 */
float terrainShadow(Smog s, vec3 origin, float dither) {
	if (s.R <= 0.001) return 1.0;

	float tEnter, tExit;
	if (!intersectSphere(origin, SUN_DIR, s.center, s.R * 1.35, tEnter, tExit)) return 1.0;

	float dt = (tExit - tEnter) / float(SHADOW_STEPS);
	float opticalDepth = 0.0;
	for (int i = 0; i < SHADOW_STEPS; i++) {
		vec3 p = origin + SUN_DIR * (tEnter + dt * (float(i) + dither));
		opticalDepth += cloudDensity(s, p, 0.0) * dt;
		if (opticalDepth * SIGMA_T * SHADOW_STRENGTH > 4.0) break; // already effectively opaque
	}

	return mix(SHADOW_MIN_LIGHT, 1.0, exp(-opticalDepth * SIGMA_T * SHADOW_STRENGTH));
}
