const float IMPACT_FLIP1 = 0.10; // black -> white
const float IMPACT_FLIP2 = 0.23; // white -> black
const float IMPACT_OUT   = 0.38; // black -> white flash
const float IMPACT_HOLD  = 0.33; // how long the flash is full white
const float IMPACT_FADE  = 0.85; // and how long it takes to leave
const float IMPACT_LEN   = IMPACT_OUT + IMPACT_HOLD + IMPACT_FADE; // radial blur length

// How far the streaks are stretched 0..1
const float IMPACT_STRETCH_T = 0.10; // seconds to full length
float impactStretch(float t) {
	float k = 1.0 - clamp(t / IMPACT_STRETCH_T, 0.0, 1.0);
	return 1.0 - k * k * k;
}

float impactAlive(float t) {
	return (t >= 0.0 && t < IMPACT_LEN) ? 1.0 : 0.0;
}

float impactFrames(float t) {
	return (t >= 0.0 && t < IMPACT_OUT) ? 1.0 : 0.0;
}

float impactFlash(float t) {
	if (t < IMPACT_OUT || t >= IMPACT_LEN) return 0.0;
	float u = clamp((t - IMPACT_OUT - IMPACT_HOLD) / IMPACT_FADE, 0.0, 1.0);
	float f = 1.0 - u;
	return f * f;
}

float impactPolarity(float t) {
	return t < IMPACT_FLIP1 ? 1.0 : (t < IMPACT_FLIP2 ? 0.0 : 1.0);
}

// projecting the blast onto the screen

vec3 impactRay(vec2 uv, mat4 invProj, mat4 invView) {
	vec4 clip = vec4(uv * 2.0 - 1.0, 1.0, 1.0);
	vec4 view = invProj * clip;
	view /= view.w;
	return (invView * view).xyz; // world space offset from the camera
}

vec2 impactScreenPos(vec3 world, vec3 camPos, mat4 invProj, mat4 invView) {
	vec3 F  = impactRay(vec2(0.5, 0.5), invProj, invView);
	vec3 Rx = (impactRay(vec2(1.0, 0.5), invProj, invView)
	         - impactRay(vec2(0.0, 0.5), invProj, invView)) * 0.5;
	vec3 Ry = (impactRay(vec2(0.5, 1.0), invProj, invView)
	         - impactRay(vec2(0.5, 0.0), invProj, invView)) * 0.5;

	vec3 w = world - camPos;
	float a = dot(w, F) / max(dot(F, F), 1e-6);
	vec2 ndc = vec2(dot(w, Rx) / max(dot(Rx, Rx), 1e-6),
	                dot(w, Ry) / max(dot(Ry, Ry), 1e-6));

	if (a <= 1e-4) return -normalize(ndc + vec2(1e-5, 0.0)) * 3.0 + 0.5;
	return (ndc / a) * 0.5 + 0.5;
}

float impactScreenScale(vec3 world, vec3 camPos, mat4 invProj, mat4 invView) {
	vec3 F  = impactRay(vec2(0.5, 0.5), invProj, invView);
	vec3 Ry = (impactRay(vec2(0.5, 1.0), invProj, invView)
	         - impactRay(vec2(0.5, 0.0), invProj, invView)) * 0.5;
	float a = dot(world - camPos, F) / max(dot(F, F), 1e-6);
	return 0.5 / max(a * length(Ry), 1e-4);
}


const float IMPACT_FALLOFF = 2.50;
const float IMPACT_GATE = 0.30;

const float IMPACT_DISC_R   = 1.20;
const float IMPACT_DISC_MIN = 0.035;
const float IMPACT_DISC_1 = 0.20; // opening black
const float IMPACT_DISC_2 = 0.40; // white
const float IMPACT_DISC_3 = 0.60; // closing black
float impactDiscScale(float t) {
	return t < IMPACT_FLIP1 ? IMPACT_DISC_1
	     : (t < IMPACT_FLIP2 ? IMPACT_DISC_2 : IMPACT_DISC_3);
}

const float IMPACT_DISC_WARP = 0.32; // boundary wander, fraction of the radius
const float IMPACT_DISC_SOFT = 0.26; // dither band, fraction of the radius
float impactDiscField(vec2 off, float radius, float t) {
	vec2 dir = normalize(off + vec2(1e-6, 0.0));
	float n = fbm2(vec3(dir * 2.6, t * 1.5));
	return length(off) / max(radius, 1e-4)
			- (1.0 + (n - 0.5) * 2.0 * IMPACT_DISC_WARP);
}

struct ImpactPick {
	float weight; // 0 = nothing is up: neither frames nor flash
	float age;    // seconds since detonation
	vec2  centre; // ground zero in uv
	float radius; // disc radius
};

// trinity has exactly ONE instance, so there is nothing to choose between
ImpactPick pickImpact() {
	ImpactPick p = ImpactPick(0.0, 0.0, vec2(0.5), 0.0);
	if (count <= 0) return p;

	int phase = tPhase();
	if (phase != PHASE_DETONATING && phase != PHASE_DYING) return p;
	if (tDetActive() < 0.5) return p;

	float t = tDetAge();
	// start from ground zero
	vec3 base = tDetBase();
	float h = max(tDetH(), 1.0);
	float reach = h * IMPACT_FALLOFF;
	float att = reach / (reach + length(base - cameraPos));
	float w = impactAlive(t) * step(IMPACT_GATE, att);
	if (w <= 0.0) return p;

	p.weight = w;
	p.age = t;
	p.centre = impactScreenPos(base, cameraPos, invProjMat, invViewMat);
	p.radius = max(h * IMPACT_DISC_R
			* impactScreenScale(base, cameraPos, invProjMat, invViewMat),
			IMPACT_DISC_MIN) * impactDiscScale(t);
	return p;
}

// offset from ground zero in uv, with x corrected so distances are circular
vec2 impactOffset(vec2 uv, vec2 centre, vec2 outSize) {
	return (uv - centre) * vec2(outSize.x / max(outSize.y, 1.0), 1.0);
}
