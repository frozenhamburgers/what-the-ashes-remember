#version 330

// How much of the overhead sun still reaches each visible surface, as a single
// scalar in the red channel. Split out of post/apophis/smog/raymarch.fsh so the
// composite can apply it separately from the volume's own coverage; see that
// file's header and the emissive carve-out in post/apophis/composite.fsh.
//
// Not gated on the view ray: a surface can sit under the cloud while the pixel
// looking at it never goes near the volume.

#moj_import <echoesofwar:apophis/smog.glsl>
uniform sampler2D MainDepthSampler;

// OutSize is THIS pass's output target - the low-res march buffer, not the window.
// InSize is its one input, MainDepthSampler, i.e. the main render target's size.
layout(std140) uniform SamplerInfo {
	vec2 OutSize;
	vec2 InSize;
};


in vec2 texCoord;
out vec4 fragColor;

void main() {
	// the sky has nothing to cast a shadow onto LOL
	if (getDepth(MainDepthSampler, texCoord) >= 0.9999) {
		fragColor = vec4(1.0);
		return;
	}

	vec3 sceneWorldPos = getWorldPos(MainDepthSampler, texCoord, invProjMat, invViewMat, cameraPos);
	float dither = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));

	float sunlight = 1.0;
	for (int instance = 0; instance < count; instance++) {
		int idx = instance * 13;
		Smog s = setupSmog(
			vec3(data[idx], data[idx + 1], data[idx + 2]),      // cloud centre
			data[idx + 3],                                      // cloud radius
			vec3(data[idx + 4], data[idx + 5], data[idx + 6]),  // mouth
			data[idx + 7],                                      // emit
			data[idx + 8],                                      // seed
			data[idx + 9],                                      // age
			data[idx + 10],                                     // density
			data[idx + 11],                                     // front distance
			data[idx + 12]);                                    // jet age (emission-time, ungated)
		if (s.density <= 0.001) continue;
		sunlight *= terrainShadow(s, sceneWorldPos, dither);
	}

	fragColor = vec4(sunlight, sunlight, sunlight, 1.0);
}
