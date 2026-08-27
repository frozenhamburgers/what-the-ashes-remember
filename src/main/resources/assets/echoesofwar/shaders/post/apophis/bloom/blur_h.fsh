#version 330

// Horizontal half of the bloom's separable gaussian.

#moj_import <echoesofwar:blur.glsl>

uniform sampler2D BloomSampler;

layout(std140) uniform SamplerInfo {
	vec2 OutSize;
	vec2 InSize;
};

in vec2 texCoord;
out vec4 fragColor;

void main() {
	fragColor = vec4(blur5(BloomSampler, texCoord, vec2(1.0 / InSize.x, 0.0)), 1.0);
}
