// ---------------------------------------------------------------------------
// apophisGlowCore() and apophisGlowBloom() are exact, reading the mask that
// ApophisGlowTarget fills by drawing Apophis's emissive layer into a buffer of
// its own.
//
// emissiveMask() is the guess, kept only for world light sources: lava, fire,
// the core of a torch flame.
// ---------------------------------------------------------------------------

// Brightness is measured on the max channel rather than on luminance.
const float EMISSIVE_VALUE_LOW  = 0.55;
const float EMISSIVE_VALUE_HIGH = 0.88;
// Applied to saturation * warmth, so a colour has to be both vivid and red-leaning.
// Torchlit stone sits near 0.13 on this measure and is correctly ignored; lava sits
// above 0.8.
const float EMISSIVE_WARMTH_LOW  = 0.42;
const float EMISSIVE_WARMTH_HIGH = 0.72;

float emissiveMask(vec3 c) {
	float mx = max(c.r, max(c.g, c.b));
	if (mx < 1e-4) return 0.0;
	float mn = min(c.r, min(c.g, c.b));

	float saturation = (mx - mn) / mx;
	// How far the colour leans red-over-blue, normalised so it is a property of the
	// hue and not of how bright the pixel happens to be.
	float warmth = clamp((c.r - c.b) / mx, 0.0, 1.0);

	return smoothstep(EMISSIVE_VALUE_LOW, EMISSIVE_VALUE_HIGH, mx)
	     * smoothstep(EMISSIVE_WARMTH_LOW, EMISSIVE_WARMTH_HIGH, saturation * warmth);
}

// ---------------------------------------------------------------------------
// ApophisGlowTarget clears its buffer to transparent black and the glow render
// type blends onto it with the usual src-alpha function, so what arrives here is
// PREMULTIPLIED: rgb is the emissive colour already scaled by its own alpha, and
// alpha is the texture's authored intensity.
// ---------------------------------------------------------------------------

// CORE: the sharp, per-pixel term added in composite
const float GLOW_CORE_GAMMA = 1.0;
const float GLOW_CORE_GAIN  = 1.0;

// HALO: the blurred term the bloom passes spread
const float GLOW_BLOOM_GAMMA = 0.45;
const float GLOW_BLOOM_GAIN  = 2.6;

// Returns the emissive radiance in rgb, and in alpha how strongly this pixel should be
// treated as self-lit
vec4 apophisGlowCurve(sampler2D glowMask, vec2 uv, float gamma, float gain) {
	vec4 m = texture(glowMask, uv);
	if (m.a < 1e-4) return vec4(0.0);

	// Undo the premultiply to recover the hue, then re-apply intensity through the
	// curve. Doing it in this order is what lets the curve change the brightness of
	// a crack without also washing out its colour.
	vec3 hue = m.rgb / m.a;
	float intensity = pow(m.a, gamma) * gain;
	return vec4(hue * intensity, clamp(intensity, 0.0, 1.0));
}

/** The sharp term drawn to screen. */
vec4 apophisGlowCore(sampler2D glowMask, vec2 uv) {
	return apophisGlowCurve(glowMask, uv, GLOW_CORE_GAMMA, GLOW_CORE_GAIN);
}

/** The term fed to the blur. */
vec4 apophisGlowBloom(sampler2D glowMask, vec2 uv) {
	return apophisGlowCurve(glowMask, uv, GLOW_BLOOM_GAMMA, GLOW_BLOOM_GAIN);
}
