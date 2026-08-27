#version 330

#moj_import <echoesofwar:common_math.glsl>
#moj_import <echoesofwar:post_defaults.glsl>
#moj_import <echoesofwar:apophis/glow.glsl>
#moj_import <echoesofwar:apophis/atmosphere.glsl>

uniform sampler2D DiffuseSampler;
uniform sampler2D CloudSampler;
uniform sampler2D ShadowSampler;
uniform sampler2D BloomSampler;
uniform sampler2D GlowSampler;
uniform sampler2D MainDepthSampler;

in vec2 texCoord;
out vec4 fragColor;

// How much of the finished bloom buffer reaches the screen.
// This scales both sources in it (Apophis's exact mask and the world's guessed emissives via hue)
const float BLOOM_STRENGTH = 1.0;

void main() {
	vec3 bg = texture(DiffuseSampler, texCoord).rgb;

	// The cloud's shade over the world, from post/apophis/smog/shadow.fsh.
	float shade = texture(ShadowSampler, texCoord).r;

	// Apophis's emissive GlowSampler holds the authored alpha from its own
	// emissive layer. emissiveMask covers world light sources instead.
	vec4 glow = apophisGlowCore(GlowSampler, texCoord);
	shade = mix(shade, 1.0, max(emissiveMask(bg), glow.a));

	vec3 lit = bg * shade;

	// The fight's weather, applied to the lit background. After the cloud's
	// shadow (fog is aerial perspective in front of a surface), before the
	// additive terms below (they are radiance in front of the fog and must not
	// be attenuated by it). emissiveMask above ran on the raw background so
	// fogged-out lava/fire don't get shadowed under the cloud again.
	float atmosphere = fightIntensity;
	if (getDepth(MainDepthSampler, texCoord) >= 0.9999) {
		// Sky and clouds. Smog first, then fog over it.
		vec3 ray = normalize(getFarWorldPos(texCoord) - cameraPos);
		lit = applySkySmog(lit, ray, atmosphere);
		lit = applySkyFog(lit, ray, atmosphere);
	} else {
		lit = applyDepthFog(lit,
				getWorldPos(MainDepthSampler, texCoord, invProjMat, invViewMat, cameraPos),
				atmosphere);
	}

	// CloudSampler is PREMULTIPLIED: post/apophis/smog/raymarch.fsh already
	// composited front-to-back, so rgb must be added, not lerped.
	vec4 cloud = texture(CloudSampler, texCoord);

	// Shade attenuates the background only, so smoke in front of a glowing
	// crack still hides it.
	float cloudA = clamp(cloud.a, 0.0, 1.0);
	vec3 color = lit * (1.0 - cloudA) + cloud.rgb;

	// amplified emissive on top, since exempting Apophis from the shade alone
	// leaves it barely visible at its authored alpha. Attenuated by the volume
	// in front of it.
	color += glow.rgb * (1.0 - cloudA);

	// bloom goes on last, over the smoke: a glow spilling into the air lights
	// the smoke in front of it too.
	color += texture(BloomSampler, texCoord).rgb * BLOOM_STRENGTH;

	fragColor = vec4(color, 1.0);
}
