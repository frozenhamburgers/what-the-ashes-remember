// ---------------------------------------------------------------------------
// Apophis fight weather
// ---------------------------------------------------------------------------

#moj_import <echoesofwar:post_defaults.glsl>
#moj_import <echoesofwar:noise.glsl>
#moj_import <echoesofwar:apophis/palette.glsl>

layout(std140) uniform AtmosphereData {
	// How much of the fight's weather is applied, 0..1
	float fightIntensity;
	float atmoPad0;
	float atmoPad1;
	float atmoPad2;
};

// ---------------------------------------------------------------------------
// Horizon smog
// ---------------------------------------------------------------------------

// Height of the notional smog layer above the camera
const float SMOG_SLAB_H = 160.0;
// Lattice units per block on that slab
const float SMOG_SCALE = 0.0014;
const vec2  SMOG_DRIFT = vec2(0.0032, 0.0011); // lattice units/second
// Where the band fades out, as sin(elevation).
const float SMOG_TOP = 0.70;
const float SMOG_MAX = 0.85;
// Distance over which the noise gives up and becomes flat haze, avoid vertical smear at horizon
const float SMOG_DETAIL_FADE = 900.0;
// Coverage the far band settles to once the detail is gone
const float SMOG_FAR_COVER = 0.55;

vec3 applySkySmog(vec3 bg, vec3 ray, float intensity) {
	if (intensity <= 0.001) return bg;

	// Two smoothsteps with disjoint ranges: in from just below the horizon, out by
	// SMOG_TOP.
	float band = smoothstep(-0.22, -0.02, ray.y) * (1.0 - smoothstep(0.02, SMOG_TOP, ray.y));
	band = pow(band, 1.35); // denser at the horizon, softer higher up
	if (band <= 0.002) return bg;

	float t = SMOG_SLAB_H / max(ray.y, 0.02);
	vec2 slab = (cameraPos.xz + ray.xz * t) * SMOG_SCALE + SMOG_DRIFT * time;

	// third axis advances slowly so the bank evolves in place not slide
	vec3 m = vec3(slab.x, time * 0.010, slab.y);
	float cover = smoothstep(0.40, 0.86, fbm2(m) * 0.66 + (1.0 - billow2(m * 2.30 + 11.7)) * 0.34);

	// Angular octave to break up the slab's horizon stretching.
	cover *= mix(0.80, 1.20, fbm2(ray * 6.0 + vec3(0.0, time * 0.02, 0.0)));

	// Far band for flat haze
	cover = mix(SMOG_FAR_COVER, cover, exp(-t / SMOG_DETAIL_FADE));

	// Soot lifted toward the sky colour, with a little smoulder lower down
	vec3 low  = mix(SMOKE_SHADOW, SKY_COLOR, 0.35) + GLOW_COLOR * 0.030;
	vec3 high = mix(SMOKE_SHADOW, SKY_COLOR, 0.75);
	vec3 smogCol = mix(low, high, clamp(ray.y * 2.2, 0.0, 1.0));

	return mix(bg, smogCol, clamp(cover, 0.0, 1.0) * band * intensity * SMOG_MAX);
}

// ---------------------------------------------------------------------------
// Depth fog
// ---------------------------------------------------------------------------

const float FOG_DENSITY = 0.01;
const float FOG_MAX = 0.9;

const float FOG_MAX_DISTANCE = 420.0;
const float FOG_BASE_Y = 100.0;
const float FOG_SCALE_H = 90.0;  // e-folding height above FOG_BASE_Y

vec3 applyDepthFog(vec3 lit, vec3 worldPos, float intensity) {
	if (intensity <= 0.001) return lit;

	float d = min(length(worldPos - cameraPos), FOG_MAX_DISTANCE);

	// World-space so banks stay put as the camera moves.
	vec3 mp = worldPos * 0.0125 + vec3(time * 0.045, time * 0.018, -time * 0.030);
	float wobble = mix(0.72, 1.36, fbm2(mp));

	// Height falloff at the far endpoint only (endpoint approximation).
	float altitude = exp(-max(worldPos.y - FOG_BASE_Y, 0.0) / FOG_SCALE_H);

	float f = 1.0 - exp(-d * FOG_DENSITY * wobble * altitude * intensity);
	return mix(lit, mix(SMOKE_SHADOW, SKY_COLOR, 0.55), min(f, FOG_MAX * intensity));
}

// Distance to stand in for a sky or cloud pixel's, in blocks, measured straight up.
// Sky pixels and Minecraft's clouds seem to carry no depth of their own, so they get a
// distance derived from the view ray instead, saturating toward the horizon to
// match terrain fog, and shortens toward the zenith
const float FOG_SKY_H = 320.0;
// Dial for how much fog reaches the sky, independent of terrain.
const float FOG_SKY_STRENGTH = 0.85;

vec3 applySkyFog(vec3 lit, vec3 ray, float intensity) {
	float t = min(FOG_SKY_H / max(ray.y, 0.05), FOG_MAX_DISTANCE);
	return applyDepthFog(lit, cameraPos + ray * t, intensity * FOG_SKY_STRENGTH);
}
