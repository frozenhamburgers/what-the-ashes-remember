// ---------------------------------------------------------------------------
// The Apophis palette, shared by the volumetric cloud and the atmosphere.
// ---------------------------------------------------------------------------

// Sun pinned straight overhead; the Apophis fight always runs at solar zenith.
const vec3  SUN_DIR      = vec3(0.0, 1.0, 0.0);
const vec3  SUN_COLOR    = vec3(0.62, 0.61, 0.60);
const vec3  SKY_COLOR    = vec3(0.19, 0.20, 0.23);
// Colour of the smoke itself.
const vec3  SMOKE_SHADOW = vec3(0.032, 0.030, 0.029);
// The smoulder inside: orange-red at the edges, pale ember at the core.
const vec3  GLOW_COLOR   = vec3(1.00, 0.29, 0.06);
const vec3  GLOW_HOT     = vec3(1.00, 0.71, 0.32);

// Normalised Henyey-Greenstein phase function. Forward scattering.
float hg(float c, float g) {
	float g2 = g * g;
	float d = 1.0 + g2 - 2.0 * g * c;
	return (1.0 - g2) / pow(max(d, 1e-4), 1.5);
}
