#version 330

// The volumetric pass: marches the view ray through the cloud and the jet, and
// writes a PREMULTIPLIED radiance/coverage pair for the composite to lay over
// the scene.
//
// Terrain shadow has its own pass, post/apophis/smog/shadow.fsh, so the
// composite can tell "smoke is in front of this pixel" apart from "this pixel
// is in the cloud's shade" and spare emissive surfaces from the shade while
// still letting smoke occlude them normally. See post/apophis/composite.fsh.

#moj_import <echoesofwar:apophis/smog.glsl>
uniform sampler2D MainDepthSampler;

// OutSize is THIS pass's output target, the low-res march buffer
// InSize is its one input
layout(std140) uniform SamplerInfo {
	vec2 OutSize;
	vec2 InSize;
};


in vec2 texCoord;
out vec4 fragColor;

void main() {
	vec3 rayOrigin = cameraPos;
	vec3 rayDir = normalize(getFarWorldPos(texCoord) - cameraPos);

	// Clip the march at whatever solid geometry the ray hits, if any.
	float tMaxScene = getDepth(MainDepthSampler, texCoord) < 0.9999
			? length(getWorldPos(MainDepthSampler, texCoord, invProjMat, invViewMat, cameraPos) - cameraPos)
			: RENDER_DISTANCE_CAP;

	// Interleaved gradient noise. no animation since at this resolution it would probably just be jitter
	float dither = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));

	vec3 acc = vec3(0.0);      // premultiplied radiance
	float trans = 1.0;         // transmittance along the view ray

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
		if (trans < 0.015) break;

		// One bounding sphere over both volumes. While the jet is out it has to span
		// mouth to cloud, centred on the midpoint; otherwise it's just the cloud.
		vec3 boundCenter = s.center;
		float boundR = s.R * 1.35;
		if (s.emit > 0.004) {
			boundCenter = mix(s.mouth, s.center, 0.5);
			// covers the jet's girth too, since early on there is no cloud yet
			boundR = s.jetLen * 0.5 + max(s.R * 1.35, s.coneMaxR * 1.2);
		}

		float tEnter, tExit;
		if (!intersectSphere(rayOrigin, rayDir, boundCenter, boundR, tEnter, tExit)) continue;
		tExit = min(tExit, tMaxScene);
		if (tEnter >= tExit) continue;

		// Distance LOD - a cloud on the horizon does not need the full budget.
		int steps = int(mix(float(MAX_STEPS), float(MIN_STEPS),
				clamp((tEnter - 32.0) / 220.0, 0.0, 1.0)));
		float dt = (tExit - tEnter) / float(steps);

		// Procedural LOD: detail finer than the march step or one output pixel cannot be resolved
		// implemented with the fact that invProjMat[1][1] is tan(fovY/2), giving the world
		// height one low-res pixel covers at the cloud.
		float pixW = (tEnter + tExit) * abs(invProjMat[1][1]) / max(OutSize.y, 1.0);
		float lodSize = max(dt, pixW);
		float detail = clamp(1.35 - lodSize * F_FINE / s.grain, 0.0, 1.0);
		float t = tEnter + dt * dither;

		for (int i = 0; i < MAX_STEPS; i++) {
			if (i >= steps || trans < 0.015) break;
			vec3 p = rayOrigin + rayDir * t;
			t += dt;

			float dens = densityAt(s, p, detail);
			if (dens <= 0.004) continue;

			vec3 gsrc;
			float g = glowField(s, p, gsrc);

			// Deep inside the volume, estimate optical depth from local density
			// instead of paying for shadow rays. Still tracks density, unlike a
			// flat fallback that would make interiors read as uniform.
			bool cheapLight = trans < 0.12 || dens < 0.03;
			bool litByGlow = g > 0.004;

			float sunOD = cheapLight
					? dens * s.grain * 2.20 * SIGMA_T * 0.78
					: sunDepth(s, p, dens);
			float glowOD = (litByGlow && !cheapLight)
					? glowDepth(s, p, dens, gsrc)
					: dens * s.grain * 0.90 * SIGMA_T * 0.80;

			// Two-lobe transmittance: a sharp direct lobe throws hard shadows, and a
			// multiple-scattering lobe at a fifth of the extinction fills deep
			// pockets with dim coloured light instead of crushing to black.
			float sT  = exp(-sunOD);
			float sMs = exp(-sunOD * 0.20);
			float gT  = exp(-glowOD);
			float gMs = exp(-glowOD * 0.26);

			vec3 toSrc = gsrc - p;
			float srcDist = length(toSrc);
			float cosT = srcDist > 1e-4 ? dot(toSrc / srcDist, rayDir) : 0.0;
			float phase = mix(1.0, hg(cosT, 0.42), 0.62);
			// Mild forward scattering on the sun too, separating lit/shaded faces
			// further when backlit.
			float sPhase = mix(1.0, hg(dot(SUN_DIR, rayDir), 0.25), 0.35);
			vec3 glowCol = mix(GLOW_COLOR, GLOW_HOT, clamp(g * 0.5, 0.0, 1.0));

			// Emission gated on local density so the smoulder can't paint a halo in clear air.
			acc += trans * glowCol * (g * dens * 0.42 * dt);

			// Powder: thin edges facing the light stay mildly dark.
			float powder = 1.0 - exp(-dens * 3.4);
			float pw = mix(0.62, 1.0, powder);

			// Fine-scale self-occlusion, below what a shadow ray can resolve;
			// gives the interior detail beyond the silhouette.
			float localOcc = mix(0.42, 1.0, exp(-dens * 1.30));

			// Ambient occluded by the soft lobe so pockets read as pockets
			// instead of sitting under a flat ambient floor.
			float ao = (0.13 + 0.87 * sMs) * localOcc;

			vec3 L = (SMOKE_SHADOW + SKY_COLOR * 0.68) * ao
			       + SUN_COLOR * ((sT * sPhase * 1.05 + sMs * 0.12) * pw)
			       + glowCol * (g * (gT * phase * 1.24 + gMs * 0.18) * pw);

			float a = 1.0 - exp(-dens * SIGMA_T * dt);
			acc += trans * L * a;
			trans *= 1.0 - a;
		}
	}

	// Never a total whiteout when the camera is inside the cloud.
	fragColor = vec4(acc, min(1.0 - trans, 0.98));
}
