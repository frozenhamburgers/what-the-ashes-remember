// TRINITY'S BODY

// ------------ SHAPE

// Lattice unit, as a fraction of the body radius
const float BODY_GRAIN = 0.42;

// How far noise is allowed to push the surface, as fractions of the radius
const float BODY_WARP_MACRO = 0.20;
const float BODY_WARP_LOBE  = 0.26;

// additional bite on the lobes as the meltdown builds
const float BODY_WARP_MELT = 0.18;

// Peak density at the center
const float BODY_DENSITY = 1.35;

// Radial falloff exponent
const float BODY_FALLOFF_POW = 3.0;

// How far past the nominal radius the marcher must still consider
const float BODY_BOUND = 1.55;

// ---------------- CHURN/STIR ANIMATION

const float BODY_SPIN      = 0.55;  // radians/second at the core
const float BODY_SPIN_FALL = 0.65;  // how much slower the outside turns
const vec3  BODY_SPIN_AXIS = vec3(0.28, 0.92, 0.27);

// slow bodily drift of the whole lattice so a pattern doesn't settle
const float BODY_DRIFT = 0.055;

vec3 bodyLattice(vec3 p, float rNorm, float grain) {
	vec3 rel = p - tCentre();

	// rotation about the tilted axis:  rodrigues, with the angle alling off with radius so angular vleocity slows as it gets
	// farhter from core
	float ang = tTime() * BODY_SPIN * mix(1.0, 1.0 - BODY_SPIN_FALL, clamp(rNorm, 0.0, 1.0));
	vec3 k = normalize(BODY_SPIN_AXIS);
	float c = cos(ang), s = sin(ang);
	rel = rel * c + cross(k, rel) * s + k * dot(k, rel) * (1.0 - c);

	rel += vec3(0.0, -tTime() * BODY_DRIFT, 0.0) * grain;
	return rel / grain + tSeed() * 7.31;
}

// smoke density of body at p, detail is LOD 0..1
float bodyDensity(vec3 p, float detail) {
	float scale = tBodyScale();
	if (scale <= 0.001) return 0.0;

	float R = tBodyR();
	if (R <= 0.001) return 0.0;

	vec3 rel = p - tCentre();
	float r = length(rel);
	if (r > R * BODY_BOUND) return 0.0;

	float rNorm = r / R;
	float grain = max(R * BODY_GRAIN, 0.5);
	vec3 lat = bodyLattice(p, rNorm, grain);

	float macro = fbm2(lat * F_MACRO);
	float lobe = billow2(lat * F_LOBE);

	float melt = tMeltdown();
	float warpLobe = BODY_WARP_LOBE + BODY_WARP_MELT * melt;
	float surf = R * (1.0
			+ BODY_WARP_MACRO * (macro - 0.5) * 2.0
			+ warpLobe * (lobe - 0.5) * 2.0);

	float k = clamp(1.0 - r / max(surf, 0.001), 0.0, 1.0);
	float dens = pow(k, BODY_FALLOFF_POW);
	if (dens <= 0.0) return 0.0;

	// fine octave faded by LOD
	if (detail > 0.01) {
		float fine = billow2(lat * F_FINE);
		dens *= mix(1.0, 0.62 + 0.76 * fine, detail * 0.55);
	}

	// body thins as it grows out of reformation
	return dens * BODY_DENSITY * tTurbulence() * mix(0.35, 1.0, scale);
}

// --------------------- GLOW

// Core radius as a fraction of the body radius
const float CORE_RADIUS = 0.34;
const float CORE_POW    = 2.0; // how sharply it falls.
const float CORE_GAIN   = 0.85;

// Emission strength of the core at p
float bodyCoreGlow(vec3 p) {
	float R = tBodyR();
	if (R <= 0.001) return 0.0;
	float rn = length(p - tCentre()) / (R * CORE_RADIUS);
	return exp(-pow(max(rn, 0.0), CORE_POW)) * CORE_GAIN * tCoreIntensity();
}

// ------------------------------- GLOW TELEGRAPHS
const float TG_SHELL_R  = 0.86; // fraction of the radius the shell sits at
const float TG_SHELL_W  = 0.30; // how thick it is
const float TG_TIGHT    = 26.0; // ~angular tightness of the spot
const float TG_GAIN     = 1.60;

float telegraphGlow(vec3 p) {
	int n = tAttackCount();
	if (n <= 0) return 0.0;

	float R = tBodyR();
	if (R <= 0.001) return 0.0;

	vec3 rel = p - tCentre();
	float r = length(rel);

	float d = (r - R * TG_SHELL_R) / (R * TG_SHELL_W);
	float shell = exp(-d * d);
	if (shell < 0.004) return 0.0;

	vec3 nrm = rel / max(r, 1e-4);
	float g = 0.0;
	for (int s = 0; s < TRINITY_ATTACK_SLOTS; s++) {
		if (s >= n) break;
		int a = attackBase(s);
		if (aType(a) == ATK_NONE) continue;
		float tg = aTelegraph(a);
		if (tg <= 0.004) continue;

		vec3 od = aDir(a);
		float c = dot(nrm, od);
		if (c <= 0.0) continue;

		g += tg * pow(c, TG_TIGHT);
	}
	return g * shell * TG_GAIN * tCoreIntensity();
}
