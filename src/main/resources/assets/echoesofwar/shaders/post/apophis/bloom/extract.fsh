#version 330

// Isolates the scene's warm emissives into a small target for the blur passes to spread.

#moj_import <echoesofwar:common_math.glsl>
#moj_import <echoesofwar:apophis/glow.glsl>

uniform sampler2D DiffuseSampler;
uniform sampler2D MainDepthSampler;
uniform sampler2D GlowSampler;

layout(std140) uniform SamplerInfo {
	vec2 OutSize;   // this (quarter-size) target
	vec2 InSize;    // DiffuseSampler, i.e. the main render target
	vec2 DepthSize; // MainDepthSampler
	vec2 GlowSize;  // GlowSampler, also main-target sized
};

in vec2 texCoord;
out vec4 fragColor;

void main() {
	// Four bilinear taps, one source pixel out from the centre in each diagonal,
	// each itself a 2x2 average, covering the block of source pixels this output
	// texel stands for. Apophis's glow is thin cracks a couple of pixels wide,
	// which a single tap would miss and cause to flicker as the camera moves.
	vec2 srcTexel = 1.0 / InSize;
	vec3 sum = vec3(0.0);
	for (int i = 0; i < 4; i++) {
		vec2 uv = texCoord + srcTexel * vec2(i % 2 == 0 ? -1.0 : 1.0, i < 2 ? -1.0 : 1.0);
		// Apophis's glow needs no depth test of its own: ApophisGlowTarget is handed
		// the world's depth buffer before the layer draws into it.
		// The bloom curve, not the core one; a blur needs boosting to survive being
		// spread. See GLOW_BLOOM_GAIN.
		sum += apophisGlowBloom(GlowSampler, uv).rgb;

		// The world's own emissives, guessed from scene colour. Sky excluded: a
		// sunset band would pass the mask and bloom the whole horizon.
		if (getDepth(MainDepthSampler, uv) >= 0.9999) continue;
		vec3 c = texture(DiffuseSampler, uv).rgb;
		sum += c * emissiveMask(c);
	}

	fragColor = vec4(sum * 0.25, 1.0);
}
