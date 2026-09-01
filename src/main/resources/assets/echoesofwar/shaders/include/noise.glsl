// NOISE

// Four independent 1D hashes at once.
vec4 hash41(vec4 p) {
	p = fract(p * 0.1031);
	p *= p + 33.33;
	p *= p + p;
	return fract(p);
}

// Trilinear value noise built on a single linear lattice key.
float vnoise(vec3 x) {
	vec3 i = floor(x);
	vec3 f = fract(x);
	f = f * f * (3.0 - 2.0 * f);

	float n = i.x + i.y * 157.0 + i.z * 113.0;
	vec4 k = vec4(0.0, 1.0, 157.0, 158.0) + n; // (0,0) (1,0) (0,1) (1,1) in xy
	vec4 h0 = hash41(k);
	vec4 h1 = hash41(k + 113.0);

	vec4 a = mix(h0, h1, f.z);
	vec2 b = mix(a.xz, a.yw, f.x);
	return mix(b.x, b.y, f.y);
}

// Per-octave offsets, used instead of a rotation matrix.
const vec3 OCT_SHIFT = vec3(37.13, 11.71, 23.57);

float fbm2(vec3 p) {
	float s = vnoise(p) * 0.62;
	p = p * 2.07 + OCT_SHIFT;
	s += vnoise(p) * 0.38;
	return s;
}

// Billow / "turbulence": abs() folds the noise into sharp creases with smooth,
// rounded maxima, giving cauliflower lobes rather than dents.
float billow2(vec3 p) {
	float s = abs(vnoise(p) * 2.0 - 1.0) * 0.64;
	p = p * 2.09 + OCT_SHIFT;
	s += abs(vnoise(p) * 2.0 - 1.0) * 0.36;
	return s;
}
