#version 330

// Vertical half of the bloom's separable gaussian - see apophis_smog_bloom_blur_h.fsh.

#moj_import <echoesofwar:blur.glsl>

uniform sampler2D BloomSampler;

layout(std140) uniform SamplerInfo {
	vec2 OutSize;
	vec2 InSize;
};

in vec2 texCoord;
out vec4 fragColor;

void main() {
	fragColor = vec4(blur5(BloomSampler, texCoord, vec2(0.0, 1.0 / InSize.y)), 1.0);
}
