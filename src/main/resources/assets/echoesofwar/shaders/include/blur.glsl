// ---------------------------------------------------------------------------
// Separable gaussian for the bloom blur. Five taps standing in for nine, using
// linear sampling to place each off-centre tap between two texels.
// ---------------------------------------------------------------------------
const float BLUR_OFFSETS[3] = float[3](0.0, 1.3846153846, 3.2307692308);
const float BLUR_WEIGHTS[3] = float[3](0.2270270270, 0.3162162162, 0.0702702703);

vec3 blur5(sampler2D tex, vec2 uv, vec2 texelStep) {
	vec3 sum = texture(tex, uv).rgb * BLUR_WEIGHTS[0];
	for (int i = 1; i < 3; i++) {
		vec2 d = texelStep * BLUR_OFFSETS[i];
		sum += texture(tex, uv + d).rgb * BLUR_WEIGHTS[i];
		sum += texture(tex, uv - d).rgb * BLUR_WEIGHTS[i];
	}
	return sum;
}
