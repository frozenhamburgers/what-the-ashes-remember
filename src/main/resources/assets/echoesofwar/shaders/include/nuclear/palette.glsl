// NUCLEAR PALLETTE
const vec3  SUN_DIR      = vec3(0.3775, 0.7947, 0.4569);
const vec3  SUN_COLOR    = vec3(0.54, 0.52, 0.70);
const vec3  SKY_COLOR    = vec3(0.16, 0.16, 0.28);
const vec3  SMOKE_SHADOW = vec3(0.032, 0.032, 0.042);
// interior emission
const vec3  GLOW_COLOR   = vec3(1.00, 0.26, 0.05);
const vec3  GLOW_HOT     = vec3(1.00, 0.86, 0.62);
// full screen flash
const vec3  FLASH_COLOR  = vec3(1.00, 0.94, 0.84);

// normalised Henyey-Greenstein phase function for forward scattering
float hg(float c, float g) {
	float g2 = g * g;
	float d = 1.0 + g2 - 2.0 * g * c;
	return (1.0 - g2) / pow(max(d, 1e-4), 1.5);
}
