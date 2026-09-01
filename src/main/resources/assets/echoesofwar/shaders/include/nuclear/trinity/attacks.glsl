// ---------------------------------------------------------------------------
// Trinity's attacks as SDF volumes
// Every attack in the fight is cone or cylinder + lattice scroll
// Requires post_defaults, noise, nuclear/palette, nuclear/sdf and nuclear/trinity/instance to be imported ahead of it.
// ---------------------------------------------------------------------------

// ----------- SHAPE

// lattice unit for attack noise, as a fraction of the attack's radius
const float ATK_GRAIN = 1.15;

// Radial warp
const float ATK_WARP_LOBE  = 0.46;
const float ATK_WARP_MACRO = 0.24;

// Radial falloff exponent
const float ATK_FALLOFF_POW = 2.0;

const float ATK_DENSITY = 1.15;

// How much wider than`radius the volume may actually get once warped
const float ATK_BOUND = 2.2;

// Cone tip sharpness.
const float ATK_CONE_POW = 1.55;

// cylinders taper very slightly
const float ATK_CYL_TAPER = 0.18;

// ----------- MOTION

// outward lattice scroll, in grain units per second
const float ATK_FLOW = 3.1;

// Roll about the axis
const float ATK_ROLL = 0.85;

// --------- ROOT AND TIP

// how far back inside the body the volume starts, as a fraction of the body radius
const float ATK_ROOT_BURY = 0.35;

// Softening at the very tip and at the root, as fractions of the length
const float ATK_TIP_SOFT  = 0.16;
const float ATK_ROOT_SOFT = 0.10;

/**
 * Density of one attack at p. a is slot base index, detail is march's procedural LOD
 */
float attackDensity(int a, vec3 p, float detail) {
	int type = aType(a);
	if (type == ATK_NONE) return 0.0;

	float fade = clamp(1.0 - aFade(a), 0.0, 1.0);
	if (fade <= 0.004) return 0.0;

	float reach = aReach(a);
	if (reach <= 0.01) return 0.0;

	vec3 root = aRoot(a);
	vec3 axis = aDir(a);
	float along, off;
	axialFrame(p, root, axis, along, off);

	float bury = tBodyRadius() * ATK_ROOT_BURY;
	if (along < -bury || along > reach) return 0.0;

	float radius = aRadius(a);
	if (off > radius * ATK_BOUND) return 0.0;

	// Radius profile along the axis. u is 0 at the surface, 1 at the tip.
	float u = clamp(along / max(reach, 0.001), 0.0, 1.0);
	float prof = atkIsCone(type)
			? pow(1.0 - u, ATK_CONE_POW)
			: (1.0 - ATK_CYL_TAPER * u);
	if (prof <= 0.001) return 0.0;

	// lattice scrolled outward along the axis and rolled about it
	float grain = max(radius * ATK_GRAIN, 0.35);
	vec3 rel = p - root;
	float scroll = tTime() * ATK_FLOW * grain;
	rel -= axis * scroll;
	float ang = tTime() * ATK_ROLL + along * 0.035;
	float c = cos(ang), s = sin(ang);
	rel = rel * c + cross(axis, rel) * s + axis * dot(axis, rel) * (1.0 - c);
	vec3 lat = rel / grain + aSeed(a) * 11.7;

	float lobe = billow2(lat * F_LOBE);
	float macro = fbm2(lat * F_MACRO);

	float warped = radius * prof * (1.0
			+ ATK_WARP_LOBE * (lobe - 0.5) * 2.0
			+ ATK_WARP_MACRO * (macro - 0.5) * 2.0);
	if (warped <= 0.001) return 0.0;

	float k = clamp(1.0 - off / warped, 0.0, 1.0);
	float dens = pow(k, ATK_FALLOFF_POW);
	if (dens <= 0.0) return 0.0;

	if (detail > 0.01) {
		float fine = billow2(lat * F_FINE);
		dens *= mix(1.0, 0.60 + 0.80 * fine, detail * 0.60);
	}

	// end tip dissolves and buried part merges back to sphere as densiity lowers
	dens *= smoothstep(1.0, 1.0 - ATK_TIP_SOFT, u);
	dens *= smoothstep(-bury, ATK_ROOT_SOFT * reach, along);

	return dens * ATK_DENSITY * aIntensity(a) * fade;
}

// ------------------ GLOW

// glowing core width as a fraction of the local warped radius, and its gain.
const float ATK_CORE_FRAC = 0.42;
const float ATK_CORE_GAIN = 1.00;

// core shlud be hottest where it leaves the body and cools along the length
const float ATK_CORE_COOL = 0.55;

float attackGlow(int a, vec3 p, out vec3 src) {
	src = tCentre();
	int type = aType(a);
	if (type == ATK_NONE) return 0.0;

	float fade = clamp(1.0 - aFade(a), 0.0, 1.0);
	if (fade <= 0.004) return 0.0;

	float reach = aReach(a);
	if (reach <= 0.01) return 0.0;

	vec3 root = aRoot(a);
	vec3 axis = aDir(a);
	float along, off;
	axialFrame(p, root, axis, along, off);

	float bury = tBodyRadius() * ATK_ROOT_BURY;
	if (along < -bury || along > reach) return 0.0;

	src = root + axis * clamp(along, 0.0, reach);

	float u = clamp(along / max(reach, 0.001), 0.0, 1.0);
	float prof = atkIsCone(type)
			? pow(1.0 - u, ATK_CONE_POW)
			: (1.0 - ATK_CYL_TAPER * u);
	float coreW = max(aRadius(a) * prof * ATK_CORE_FRAC, 0.05);

	float rn = off / coreW;
	float g = exp(-rn * rn);
	g *= mix(1.0, 1.0 - ATK_CORE_COOL, u);
	g *= smoothstep(1.0, 1.0 - ATK_TIP_SOFT, u);

	return g * ATK_CORE_GAIN * aIntensity(a) * fade * tCoreIntensity();
}
