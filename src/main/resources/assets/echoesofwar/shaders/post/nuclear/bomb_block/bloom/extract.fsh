#version 330

// Isolates the detonation's hot interior into a quarter-size target for the
// blur passes to spread.
//
// Simpler than the Apophis extract: the only source is the raymarch buffer, so
// there is no scene emissive to guess at and no entity glow mask to sample. The
// threshold matters - without it the whole cloud blooms and the contrast between
// the hot core and the dark smoke, which is the entire look, washes out.

layout(std140) uniform SamplerInfo {
	vec2 OutSize; // this (quarter-size) target
	vec2 InSize;  // CloudSampler, i.e. the low-res raymarch buffer
};

uniform sampler2D CloudSampler;

in vec2 texCoord;
out vec4 fragColor;

// Radiance below this contributes nothing; the fireball and the plume core sit
// well above it, lit smoke sits well below.
const float BLOOM_THRESHOLD = 0.55;
// Boost, because a blur needs headroom to survive being spread over the screen.
const float BLOOM_GAIN = 1.35;

void main() {
	// Four bilinear taps, one source pixel out on each diagonal, each itself a
	// 2x2 average - covers the block of source pixels this output texel stands
	// for, so a small bright core cannot flicker as the camera moves.
	vec2 srcTexel = 1.0 / InSize;
	vec3 sum = vec3(0.0);
	for (int i = 0; i < 4; i++) {
		vec2 uv = texCoord + srcTexel * vec2(i % 2 == 0 ? -1.0 : 1.0, i < 2 ? -1.0 : 1.0);
		// The raymarch writes PREMULTIPLIED radiance, so rgb is already what
		// reaches the screen and needs no alpha weighting.
		vec3 c = texture(CloudSampler, uv).rgb;
		float lum = max(max(c.r, c.g), c.b);
		// Soft knee rather than a step, so the bloom's edge does not crawl across
		// the cloud as the core dims.
		sum += c * smoothstep(BLOOM_THRESHOLD, BLOOM_THRESHOLD * 2.0, lum);
	}

	fragColor = vec4(sum * (0.25 * BLOOM_GAIN), 1.0);
}
