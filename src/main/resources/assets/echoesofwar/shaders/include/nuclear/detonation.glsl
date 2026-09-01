// ---------------------------------------------------------------------------
// The nuclear detonation: the volume model and the lighting that
// resolves it. SHARED - marched by both bomb block and trinity
//
// Five fields share one march, one lattice family and one lighting model:
// FIREBALL, PLUME, HEAD, GROUND, RINGS
//
// Everything except the fireball is scaled on lf = age / lifetime, so the whole
// sequence retunes proportionally from one constant if that ever ends up being useful in the future
// ---------------------------------------------------------------------------

#moj_import <echoesofwar:common_math.glsl>
#moj_import <echoesofwar:post_defaults.glsl>
#moj_import <echoesofwar:noise.glsl>
#moj_import <echoesofwar:nuclear/palette.glsl>

// ---------------- MARCH BUDGET

const int   MAX_STEPS           = 72;   // primary march budget (per instance)
const int   MIN_STEPS           = 28;   // floor after distance LOD
const float RENDER_DISTANCE_CAP = 512.0;

// extinction
const float SIGMA_T = 2.30;

// Noise frequencies, in lattice units
const float F_MACRO = 0.26; // individual sub-plumes / large masses
const float F_LOBE  = 0.70; // billowing cauliflower lobes
const float F_FINE  = 1.50; // turbulent detail, faded out by the LOD

const float HEAD_LOBE_SCALE = 0.80;

// SELF SHADOWING
//
const float SHADOW_REACH_LOBES = 3.25;
const float GLOW_REACH_LOBES   = 4.30;

const float SUN_SHADOW_DEPTH  = 26.0;
const float GLOW_SHADOW_DEPTH = 32.0;

const float SUN_CHEAP_FRAC  = 0.48;
const float GLOW_CHEAP_FRAC = 0.15;

// ----------- TIMING, FRACTIONS OF THEIR RESPECTIVE LIFETIME

const float PLUME_DELAY     = 1.10; // seconds

const float PLUME_TAU_FRAC  = 0.19;
const float PLUME_COLLAPSE  = 0.80; // stalk starts losing coherence here

const float HEAD_START      = 0.05; // cap can begin accumulating
const float HEAD_FULL       = 0.34; // caps own growth curve saturated
const float LATERAL_START   = 0.20; // cap keeps widening after the plume tops out
const float LATERAL_END     = 0.92;
const float HEAD_SAG_START  = 0.84; // cap ceiling starts sinking and spreading

const float GROUND_FADE_START = 0.30; // base surge starts breaking up
const float GROUND_FADE_END   = 0.76; // and has fully dispersed by here
const float GLOW_FADE_START = 0.26; // internal emission already on its way down
const float GLOW_FADE_END   = 0.64; // and essentially gone by the mature stage

// fireball in seconds.
const float BLAST_TAU         = 1.40;
const float BLAST_R_SCALE     = 0.55; // as a fraction of R1
const float BLAST_ERODE_START = 2.50;
const float BLAST_ERODE_END   = 9.00;

// ---------------------------------------- FLOW

const float EXPAND_R = 3.0; // Radial spread of the plume's flow between vent and ceiling
const float EXPAND_Y = 1.8; // vertical cell growth

const float SCROLL_UP = 1.4; // Lattice scroll rate, in lattice units/second

const float PLUME_TOP_R = 0.30; // Plume radius at the ceiling, as a fraction of the cap radius

// How the cap's lattice is biased where the plume enters it
// This is to make the cap and stalk appear to be connected with the stalk feeding the cap
// near injection point, lattice scrolls faster and rolls outward
const float FEED_REACH = 0.50; // how far up the cap the bias reaches, in u

// Cap silhouette
const float CAP_ROUND     = 0.58; // half-height as a fraction of the radius

// RIM DROOP
const float CAP_DROOP_MIN = 0.14; // young: a nearly symmetric ball
const float CAP_DROOP_MAX = 0.70; // mature: rim hangs just under the underside
const float CAP_DIP       = 1.35; // depth of the depression the stalk rises into
const float CAP_DIP_R     = 0.44; // its radius, as a fraction of the cap's
const float CAP_RIM_THIN  = 0.25; // extra thinning toward the drooping rim
const float FEED_LIFT  = 0.10; // extra vertical scroll near the stalk
const float FEED_PUSH  = 0.06; // outward roll near the stalk

// GROUND SURGE
const float GROUND_RMAX   = 1.45; // asymptotic footprint, as a multiple of R1
const float GROUND_CREEP  = 0.18; // Rmax growth across the lifetime
const float GROUND_V0     = 22.0; // initial outward speed
const float GROUND_VTAU   = 5.00; // seconds the scale the speed decays on
const float GROUND_DRIFT     = 0.20;
const float GROUND_DRIFT_CAP = 0.70;
const float GROUND_RIM_START = 0.40;
const float GROUND_RIM_PEAK  = 0.78;
const float GROUND_RIM_LIFT  = 0.42; // how much taller the layer stands there
const float GROUND_RIM_PILE  = 0.26; // how much denser
const float GROUND_RIM_LOBE  = 0.45; // a coarser billow octave, rim only
const float GROUND_RIM_AMP   = 1.22;
const float GROUND_EDGE_SOFT = 0.93; // where the radial falloff starts biting
const float GROUND_GRAIN  = 0.87; // lattice unit, as a fraction of the plumes
const float GROUND_COARSEN = 0.90;
const float GROUND_SQUASH = 1.80; // extra vertical squash on the noise lattice

// SMOKE RINGS
const int   RING_COUNT      = 4;
const float RING_FIRST      = 0.10; // lf at which the first ring forms
const float RING_GAP        = 0.15; // lf between successive rings
const float RING_LIFE       = 0.58; // lf a ring takes to rise and dissolve
const float RING_Y0         = 0.06; // birth height, as a fraction of H
const float RING_CLIMB      = 1.12; // how far up it travels
const float RING_R0         = 0.15; // birth radius, as a fraction of R1
const float RING_EXPAND     = 0.62; // how far it expands
const float RING_TUBE0      = 0.10; // tube radius at birth
const float RING_TUBE_GROW  = 0.05; // and how much it swells as it rises
const float RING_CORE       = 0.15; // wide soft falloff: 0 is be a hard torus
const float RING_WARP       = 0.30; // radial distortion, so it is never a torus
const float RING_WARP_FREQ  = 0.27; // per grain, so halved with the grain
const float RING_DENSITY    = 0.025; // kept low: atmospheric, not the focus

// EMISSION
const float CORE_GLOW = 3.20; // the fireball is far brighter than anything after

// ---------------------------------------------------------------------------

struct Detonation {
	vec3  base;
	float R0;          // vent radius
	float R1;          // nominal mature cap radius
	float H;           // nominal mature cap height
	float seed;
	float t;           // age, seconds
	float life;        // total lifetime, seconds
	float lf;          // t / life
	float grain;       // size of one noise lattice unit, in blocks
	float lobeCell;    // size of one billow, in blocks - what the shadows resolve
	float shadowReach; // sun shadow march span, in blocks
	float glowReach;   // glow shadow march span, in blocks
	float intensity;   // master scale, debug knob

	// plume
	float frontH;      // how high the rising column has actually got
	float plumeTop;    // its top, which sinks back once it loses coherence
	float plumeWiden;  // it broadens as it stops being driven
	float plumeErode;  // dissipation, as erosion of the field
	float scroll;      // integrated lattice scroll phase

	// radial lattice draw-in, in blocks. Zero for an ordinary detonation, See nuclear/trinity/reform.glsl, which is the only thing that writes it.
	float drawIn;

	// head
	float headAmt;     // presence: zero until the plume has climbed
	float headTop;     // ceiling, sinks during the sag
	float headBot;     // underside
	float headY;       // centre, where the late glow migrates to
	float capR;        // width at the crown
	float capDroop;    // how far the rim hangs below the centre
	float headErode;

	// ground surge
	float groundAmt;
	float groundR; // the front: where the outflow has actually reached
	float groundRMax;// the asymptote it is decelerating toward
	float groundPhase; // integrated outward displacement, blocks
	float groundH; // layer thickness across the flat middle
	float groundTop; // including the rim's lift, for the bounding volume
	float groundThin;
	float groundErode;

	// rings
	float ringAmt;
	float ringMaxR; // for the bounding volume
	float ringMaxY;

	// fireball
	float blastR;
	float blastRise;
	float blastErode;

	// emission
	float coreI;
	float plumeI;
	float headI;
	float glowSpread;
};

// Antiderivative of smoothstep(a, b, x)
float smoothstepIntegral(float x0, float a, float b) {
	float x = clamp((x0 - a) / (b - a), 0.0, 1.0);
	float x3 = x * x * x;
	return (b - a) * (x3 - 0.5 * x3 * x) + max(x0 - b, 0.0);
}

Detonation setupDetonation(vec3 base, float seed, float t, float life,
		float H, float R1, float R0, float intensity) {
	Detonation d;
	d.base = base;
	d.H = max(H, 1.0);
	d.R1 = max(R1, 1.0);
	d.R0 = max(R0, 0.5);
	d.seed = fract(abs(seed) * 0.0143) * 53.0;
	d.t = max(t, 0.0);
	d.life = max(life, 0.001);
	d.lf = clamp(d.t / d.life, 0.0, 1.0);
	d.intensity = max(intensity, 0.0);

	d.grain = max(d.R1 * 0.070, 1.3);
	d.lobeCell = d.grain / (F_LOBE * HEAD_LOBE_SCALE);
	d.shadowReach = d.lobeCell * SHADOW_REACH_LOBES;
	d.glowReach = d.lobeCell * GLOW_REACH_LOBES;

	// PLUME
	float tp = max(d.t - PLUME_DELAY, 0.0);
	float tau = d.life * PLUME_TAU_FRAC;
	d.frontH = d.H * (1.0 - exp(-tp / tau));

	d.plumeErode = smoothstep(PLUME_COLLAPSE, 1.0, d.lf);
	d.plumeWiden = 1.0 + 0.70 * d.plumeErode;
	d.plumeTop = mix(d.frontH, d.H * 0.30, pow(d.plumeErode, 1.25));

	d.scroll = SCROLL_UP * (tp - d.life * smoothstepIntegral(d.lf, 0.55, 0.95));
	d.drawIn = 0.0;

	// HEAD
	float risen   = clamp(d.frontH / d.H, 0.0, 1.0);
	float grown   = smoothstep(0.12, 0.62, risen);
	float mature  = smoothstep(HEAD_START, HEAD_FULL, d.lf);
	float lateral = smoothstep(LATERAL_START, LATERAL_END, d.lf);
	float sag     = pow(smoothstep(HEAD_SAG_START, 1.02, d.lf), 1.30);

	d.headAmt   = grown * smoothstep(0.02, 0.14, d.lf);
	d.headErode = smoothstep(0.86, 1.02, d.lf);
	d.headTop   = mix(d.frontH * 1.05 + d.H * 0.02, d.H * 0.70, sag);
	d.capR      = d.R1 * (0.28 + mature * 0.60 + lateral * 0.16) * grown * (1.0 + 0.35 * sag);
	d.headY     = d.headTop - max(d.capR, d.H * 0.10) * CAP_ROUND;
	float droopT = smoothstep(0.18, 0.80, d.lf);
	d.capDroop  = d.capR * mix(CAP_DROOP_MIN, CAP_DROOP_MAX, droopT) * (1.0 + 0.25 * sag);
	d.headBot   = d.headY - d.capR * CAP_ROUND * 0.5 - d.capDroop;

	// GROUND SURGE/SMOKE
	float tg = max(d.t - 0.30, 0.0);
	d.groundAmt   = smoothstep(0.15, 1.10, d.t);
	d.groundRMax  = d.R1 * GROUND_RMAX * (1.0 + GROUND_CREEP * d.lf);
	d.groundPhase = GROUND_V0 * GROUND_VTAU * log(1.0 + tg / GROUND_VTAU);
	d.groundR     = d.groundRMax
	              * (1.0 - exp(-d.groundPhase / max(d.groundRMax, 1.0)));
	d.groundH     = max(d.H * 0.022, d.R0 * 0.45) * (1.0 + 0.90 * d.lf);
	// thinner the further it has spread
	d.groundThin  = max(d.groundR / (d.R1 * 1.05) - 1.0, 0.0) * 0.40;
	d.groundTop   = d.groundH * (1.0 + GROUND_RIM_LIFT) * 1.95;
	d.groundErode = smoothstep(GROUND_FADE_START, GROUND_FADE_END, d.lf);

	// RINGS
	d.ringAmt  = smoothstep(RING_FIRST, RING_FIRST + 0.05, d.lf)
	           * (1.0 - smoothstep(0.88, 1.0, d.lf));
	d.ringMaxR = d.R1 * (RING_R0 + RING_EXPAND + RING_TUBE0 + RING_TUBE_GROW)
	           * (1.0 + RING_WARP);
	d.ringMaxY = d.H * (RING_Y0 + RING_CLIMB) + d.R1 * (RING_TUBE0 + RING_TUBE_GROW) * 2.2;

	// FIREBALL
	d.blastR     = d.R1 * BLAST_R_SCALE * (1.0 - exp(-d.t / BLAST_TAU));
	d.blastRise  = smoothstep(0.0, 0.18, d.t);
	d.blastErode = smoothstep(BLAST_ERODE_START, BLAST_ERODE_END, d.t);

	// EMISSION
	d.coreI  = smoothstep(0.0, 0.08, d.t) * (1.0 - smoothstep(1.0, 7.0, d.t));
	d.plumeI = smoothstep(0.25, 1.80, d.t)
	         * (1.0 - smoothstep(GLOW_FADE_START, GLOW_FADE_END, d.lf));
	d.headI  = 0.35 * smoothstep(0.08, 0.22, d.lf) * (1.0 - smoothstep(0.34, 0.66, d.lf));
	d.glowSpread = smoothstep(0.12, 0.44, d.lf);
	return d;
}

// --------------------------------- FIELDS

// Lateral meander of the plume's centreline
vec2 plumeDrift(Detonation d, float h) {
	float hn = clamp(h / d.H, 0.0, 1.0);
	float amp = d.R1 * 0.11 * smoothstep(0.05, 0.90, hn);
	float u = hn * 2.4 - d.t * 0.06 + d.seed;
	vec2 o = vec2(sin(u * 1.61), cos(u * 1.27)) * 0.62
	       + vec2(sin(u * 0.57 + 2.1), cos(u * 0.71 + 4.3)) * 0.38;
	return o * amp;
}

// World point -> noise lattice for the plume AND the cap.
vec3 plumeLattice(Detonation d, vec2 dxz, float hn, float S) {
	float eulY = (d.H / (EXPAND_Y - 1.0)) * log(1.0 + (EXPAND_Y - 1.0) * hn);
	vec2 xz = dxz / S;

	float rr = length(xz);
	xz += (rr > 1e-4 ? xz / rr : vec2(0.0)) * d.drawIn;

	return vec3(xz.x, eulY - d.scroll, xz.y) / d.grain
	     + vec3(d.seed * 3.7, 0.0, d.seed * 2.3);
}

float plumeDensity(Detonation d, vec3 p, float detail) {
	vec3 q = p - d.base;
	float h = q.y;
	if (h < -1.0 || h > d.plumeTop + d.H * 0.06) return 0.0;

	float hn = clamp(h / d.H, 0.0, 1.0);
	vec2 dxz = q.xz - plumeDrift(d, h);
	float r = length(dxz);

	float pr = (d.R0 + (d.R1 * PLUME_TOP_R - d.R0) * pow(hn, 0.80)) * d.plumeWiden;
	// flares where it enters the cap
	pr *= 1.0 + 0.85 * d.headAmt * smoothstep(d.headBot - d.H * 0.10, d.headTop, h);

	float soft = d.H * 0.05 + d.plumeTop * 0.10;
	float fade = smoothstep(d.plumeTop - soft, d.plumeTop + soft * 0.40, h);
	// Cubed falloff
	float rr = r / max(pr, 0.001);
	float shape = 1.0 - rr * rr * rr - fade * 1.70 - d.plumeErode * 0.85;
	if (shape < -0.85) return 0.0;

	float S = 1.0 + (EXPAND_R - 1.0) * hn;
	vec3 m = plumeLattice(d, dxz, hn, S);
	vec3 evo = vec3(d.t * 0.060, d.t * 0.025, d.t * -0.045);

	float macro = fbm2(m * F_MACRO);
	float lobe  = billow2(m * F_LOBE + evo * 0.40);
	// column gets progressively more turbulent as it rises
	float turb = mix(0.82, 1.15, smoothstep(0.05, 0.75, hn));
	// vent pulses
	float puff = sin(m.y * 0.9 + macro * 6.0) * 0.5 + 0.5;

	float field = min(shape, 0.55) * 1.50
	            + (macro - 0.46) * 0.95
	            + (lobe  - 0.40) * 0.78 * turb
	            + (puff  - 0.5)  * 0.24;

	if (detail > 0.01) {
		float fine = billow2(m * F_FINE + evo * 1.05);
		field += (fine - 0.42) * 0.30 * detail * turb;
	}

	// threshold should widens as collapses
	return smoothstep(0.0, mix(0.24, 0.52, d.plumeErode), field);
}

float headDensity(Detonation d, vec3 p, float detail) {
	if (d.headAmt <= 0.004) return 0.0;
	vec3 q = p - d.base;
	float h = q.y;

	float span = max(d.headTop - d.headBot, 0.001);
	float vSoft = d.capR * 0.22 + d.H * 0.02;
	if (h < d.headBot - vSoft || h > d.headTop + vSoft) return 0.0;

	float hn = clamp(h / d.H, 0.0, 1.0);
	vec2 dxz = q.xz - plumeDrift(d, h);
	float r = length(dxz);
	if (r > d.capR * 1.35) return 0.0;

	float u = clamp((h - d.headBot) / span, 0.0, 1.0); // 0 = underside, 1 = ceiling
	float rn = r / max(d.capR, 0.001);

	// oblate ball, whose centreline sags as it goes out.
	float mid  = d.headY - d.capDroop * rn * rn;
	float dv   = (h - mid) / CAP_ROUND;

	// cubed here too
	float bn = length(vec2(r, dv)) / max(d.capR, 0.001);
	float ball = 1.0 - bn * bn * bn;

	// depression the stalk rises into
	float dip = (1.0 - smoothstep(CAP_DIP_R * 0.35, CAP_DIP_R, rn))
	          * clamp((mid - h) / max(d.capR * CAP_ROUND, 0.001), 0.0, 1.0);

	// material stretched wider than the plume ever was is thinner
	float thin = max(d.capR / (d.R1 * 1.15) - 1.0, 0.0) * 0.60
	           + d.headErode * 1.05
	           + rn * rn * CAP_RIM_THIN;
	float shape = ball - dip * CAP_DIP - thin;
	if (shape < -0.85) return 0.0;

	float S = 1.0 + (EXPAND_R - 1.0) * hn;
	vec3 m = plumeLattice(d, dxz, hn, S);

	// where the plume enters the cap the flow is still driving, so bias the lattice there
	float feed = (1.0 - smoothstep(0.0, FEED_REACH, u))
	           * (1.0 - smoothstep(d.R1 * 0.15, d.R1 * 0.75, r));
	if (feed > 0.001) {
		vec2 outward = r > 1e-3 ? dxz / r : vec2(0.0);
		m.y  -= feed * d.scroll * FEED_LIFT;
		m.xz += outward * (feed * d.scroll * FEED_PUSH);
	}

	vec3 evo = vec3(d.t * 0.050, d.t * 0.020, d.t * -0.035);
	float macro = fbm2(m * F_MACRO);
	float lobe  = billow2(m * F_LOBE * HEAD_LOBE_SCALE + evo * 0.35);

	float field = min(shape, 0.55) * 1.45
	            + (macro - 0.46) * 0.98
	            + (lobe  - 0.40) * 0.86;

	if (detail > 0.01) {
		float fine = billow2(m * F_FINE + evo);
		field += (fine - 0.42) * 0.28 * detail;
	}

	return smoothstep(0.0, mix(0.26, 0.56, d.headErode), field) * d.headAmt;
}

float groundDensity(Detonation d, vec3 p, float detail) {
	if (d.groundAmt <= 0.004) return 0.0;
	vec3 q = p - d.base;
	float h = q.y;
	if (h < -3.0 || h > d.groundTop) return 0.0;
	float rOut = d.groundR * 1.06;
	if (dot(q.xz, q.xz) > rOut * rOut) return 0.0;

	float r = length(q.xz);
	float rn = r / max(d.groundR, 0.001); // 0 at the vent, 1 at the front

	float rim = smoothstep(GROUND_RIM_START, GROUND_RIM_PEAK, rn);

	float top = d.groundH * (1.0 + GROUND_RIM_LIFT * rim);

	float radial = 1.0 - smoothstep(GROUND_EDGE_SOFT, 1.04, rn);
	float vert   = 1.0 - smoothstep(top * 0.26, top * (1.15 + 0.60 * rim), max(h, 0.0));
	float shape  = radial * vert * 1.55 - 0.40
	             + GROUND_RIM_PILE * rim * radial
	             - d.groundThin
	             - d.groundErode * 1.90;
	if (shape < -0.75) return 0.0;

	vec2 dir = r > 1e-3 ? q.xz / r : vec2(0.0);
	float disp = min(GROUND_DRIFT * d.groundPhase, r * GROUND_DRIFT_CAP);
	float rl = r - disp;

	float cell = d.grain * GROUND_GRAIN * (1.0 + GROUND_COARSEN * d.lf);
	vec3 m = vec3(dir.x * rl, h * GROUND_SQUASH, dir.y * rl) * (1.0 / cell)
	       + vec3(d.seed * 5.1, 71.0, d.seed * 1.9);
	vec3 evo = vec3(d.t * 0.085, d.t * -0.030, d.t * 0.055);

	float macro = fbm2(m * F_MACRO);
	float lobe  = billow2(m * F_LOBE + evo * 0.35);

	float field = min(shape, 0.60) * 1.38
	            + (macro - 0.46) * 0.86
	            + (lobe  - 0.40) * 0.74;

	// ROLLING EDGE
	if (rim > 0.01) {
		float roll = billow2(m * F_LOBE * GROUND_RIM_LOBE + evo * 0.50);
		field += (roll - 0.40) * GROUND_RIM_AMP * rim;
	}

	// 2 fine octaves here
	if (detail > 0.01) {
		float fine = billow2(m * F_FINE + evo);
		float grit = billow2(m * F_FINE * 2.35 + evo * 1.9);
		field += ((fine - 0.42) * 0.24 + (grit - 0.42) * 0.17) * detail;
	}

	return smoothstep(0.0, mix(0.26, 0.66, d.groundErode), field) * d.groundAmt;
}

// RINGS: just one low freq fbm for distortion with no billow fine octave or its own lighting, super cheap
float ringDensity(Detonation d, vec3 p) {
	if (d.ringAmt <= 0.004) return 0.0;
	vec3 q = p - d.base;
	if (q.y < -2.0 || q.y > d.ringMaxY) return 0.0;
	float r = length(q.xz);

	float acc = 0.0;
	for (int i = 0; i < RING_COUNT; i++) {
		float ra = (d.lf - (RING_FIRST + float(i) * RING_GAP)) / RING_LIFE;
		if (ra <= 0.0 || ra >= 1.0) continue;

		// rises out of the blast then coast
		float ease = 1.0 - pow(1.0 - ra, 1.6);
		float ry   = d.H * (RING_Y0 + RING_CLIMB * ease);
		float dy   = q.y - ry;
		float tube = d.R1 * (RING_TUBE0 + RING_TUBE_GROW * ra);
		if (abs(dy) > tube * 2.2) continue;

		float rr  = d.R1 * (RING_R0 + RING_EXPAND * ease);
		float amp = smoothstep(0.0, 0.14, ra) * (1.0 - smoothstep(0.50, 1.0, ra));

		// warp around ring's height to prevent perfect torus
		float warp = fbm2(vec3(q.x, ry, q.z) * (RING_WARP_FREQ / d.grain)
		                + vec3(d.seed * 2.9, float(i) * 17.3, d.seed * 5.1)) - 0.5;

		float tubeU = length(vec2(r - rr * (1.0 + RING_WARP * warp), dy))
		            / max(tube * (1.0 + 0.35 * warp), 0.001);
		acc += (1.0 - smoothstep(RING_CORE, 1.0, tubeU)) * amp;
	}
	return acc * d.ringAmt * RING_DENSITY;
}

float blastDensity(Detonation d, vec3 p, float detail) {
	if (d.blastRise <= 0.003 || d.blastErode >= 0.999) return 0.0;
	vec3 q = p - d.base;
	float R = max(d.blastR, 0.001);

	// Dome sitting on the ground + low wide skirt of smoke
	float dome  = 1.0 - length(vec3(q.x, (q.y - R * 0.26) * 1.30, q.z)) / R;
	float skirt = 1.0 - length(vec3(q.x, (q.y - R * 0.04) * 5.0, q.z)) / (R * 1.55);
	float shape = max(dome, skirt * 0.85);
	if (shape < -0.70) return 0.0;

	// lattice frozen to the expanding ball, so structures inflate with it rather than a scrolling texture appearing
	vec3 m = q * (d.R1 * BLAST_R_SCALE / (R * d.grain))
	       + vec3(d.seed * 5.1, 71.0, d.seed * 1.9);
	vec3 evo = vec3(d.t * 0.55, d.t * -0.22, d.t * 0.33);

	float macro = fbm2(m * F_MACRO);
	float lobe  = billow2(m * F_LOBE + evo * 0.35);

	float field = min(shape, 0.62) * 1.42
	            + (macro - 0.46) * 0.80
	            + (lobe  - 0.40) * 0.72;

	if (detail > 0.01) {
		float fine = billow2(m * F_FINE + evo);
		field += (fine - 0.42) * 0.30 * detail;
	}

	field -= d.blastErode * 1.06;
	return smoothstep(0.0, mix(0.22, 0.50, d.blastErode), field) * d.blastRise;
}

float densityAt(Detonation d, vec3 p, float detail) {
	return min((blastDensity(d, p, detail)
	          + plumeDensity(d, p, detail)
	          + headDensity(d, p, detail)
	          + groundDensity(d, p, detail)
	          + ringDensity(d, p)) * d.intensity, 1.45);
}

// shadow taps
float densityAtShadow(Detonation d, vec3 p) {
	return min((blastDensity(d, p, 0.0)
	          + plumeDensity(d, p, 0.0)
	          + headDensity(d, p, 0.0)
	          + groundDensity(d, p, 0.0)) * d.intensity, 1.45);
}

// ---------------------------------------------Lighting, adapted from apophis and sandworms

vec3 glowSource(Detonation d, vec3 p) {
	float yLo = d.base.y + d.H * 0.02;
	float yHi = d.base.y + max(d.frontH * 0.92, d.H * 0.04);
	float y = clamp(p.y, yLo, yHi);
	y = mix(y, d.base.y + d.headY, d.glowSpread * 0.75);
	vec2 c = plumeDrift(d, y - d.base.y);
	return vec3(d.base.x + c.x, y, d.base.z + c.y);
}

// interior glow
float glowField(Detonation d, vec3 p, out vec3 src) {
	// Column & cap emitter
	vec3 gs = glowSource(d, p);
	float gr = mix(max(d.R0, 1.0) * 1.6, d.R1 * 0.55, d.glowSpread);
	float dg = length(p - gs) / max(gr, 0.001);
	// inverse square core with a gaussian cutoff
	float gi = (d.plumeI + d.headI) / (1.0 + dg * dg * 2.2) * exp(-dg * dg * 0.40);

	// brighter shorter smaller fireball core
	vec3 cp = d.base + vec3(0.0, d.blastR * 0.32, 0.0);
	float cr = max(d.blastR * 0.55, 1.0);
	float dc = length(p - cp) / cr;
	float ci = d.coreI * CORE_GLOW / (1.0 + dc * dc * 2.2) * exp(-dc * dc * 0.40);

	src = ci > gi ? cp : gs;
	return (gi + ci) * d.intensity;
}


float shadowDepth(Detonation d, vec3 p, float dens, vec3 dir, float reach) {
	float u = reach * (1.0 / 14.0);
	return dens                                     * (u * 0.30)
	     + densityAtShadow(d, p + dir * (u * 0.45)) * (u * 0.95)
	     + densityAtShadow(d, p + dir * (u * 1.40)) * (u * 2.45)
	     + densityAtShadow(d, p + dir * (u * 3.85)) * (u * 4.10)
	     + densityAtShadow(d, p + dir * (u * 9.80)) * (u * 6.20);
}

float sunDepth(Detonation d, vec3 p, float dens) {
	return shadowDepth(d, p, dens, SUN_DIR, d.shadowReach)
	     * (SUN_SHADOW_DEPTH / d.shadowReach);
}

float glowDepth(Detonation d, vec3 p, float dens, vec3 src) {
	vec3 dir = src - p;
	float dist = length(dir);
	if (dist < 1e-3) return 0.0;
	return shadowDepth(d, p, dens, dir / dist, min(dist, d.glowReach))
	     * (GLOW_SHADOW_DEPTH / d.glowReach);
}

float sunDepthCheap(Detonation d, float dens) {
	return dens * SUN_SHADOW_DEPTH * SUN_CHEAP_FRAC;
}

float glowDepthCheap(Detonation d, float dens) {
	return dens * GLOW_SHADOW_DEPTH * GLOW_CHEAP_FRAC;
}

// ---------------------------------------BOUNDS

// vertical clyinder clipped to y-slab, better than sphere since a bounding sphere would spend most rays on nothing
bool intersectColumnBounds(vec3 ro, vec3 rd, vec3 base, float boundRadius,
		float yLow, float yHigh, out float tEnter, out float tExit) {
	float ocx = ro.x - base.x;
	float ocz = ro.z - base.z;
	float a = rd.x * rd.x + rd.z * rd.z;
	float cylEnter, cylExit;
	if (a < 1e-6) {
		if (ocx * ocx + ocz * ocz > boundRadius * boundRadius) return false;
		cylEnter = -1e6;
		cylExit = 1e6;
	} else {
		float b = 2.0 * (ocx * rd.x + ocz * rd.z);
		float c = ocx * ocx + ocz * ocz - boundRadius * boundRadius;
		float disc = b * b - 4.0 * a * c;
		if (disc < 0.0) return false;
		float sq = sqrt(disc);
		cylEnter = (-b - sq) / (2.0 * a);
		cylExit = (-b + sq) / (2.0 * a);
	}

	float yEnter, yExit;
	if (abs(rd.y) < 1e-6) {
		if (ro.y < base.y + yLow || ro.y > base.y + yHigh) return false;
		yEnter = -1e6;
		yExit = 1e6;
	} else {
		float t1 = (base.y + yLow - ro.y) / rd.y;
		float t2 = (base.y + yHigh - ro.y) / rd.y;
		yEnter = min(t1, t2);
		yExit = max(t1, t2);
	}

	tEnter = max(max(cylEnter, yEnter), 0.0);
	tExit = min(cylExit, yExit);
	return tEnter < tExit;
}

float detonationBoundRadius(Detonation d) {
	float r = d.capR * 1.35;
	// drop this term from the bound when the ground surge deterioates cuz expensive
	if (d.groundErode < 0.995) r = max(r, d.groundR * 1.06);
	r = max(r, d.ringMaxR);
	r = max(r, d.blastR * 1.65);
	r = max(r, d.R1 * PLUME_TOP_R * d.plumeWiden * 2.0);
	return r + d.R1 * 0.20;
}

float detonationBoundTop(Detonation d) {
	float y = max(d.plumeTop + d.H * 0.10, d.headTop + max(d.H * 0.06, d.capR * 0.25));
	y = max(y, d.ringMaxY);
	y = max(y, d.blastR * 1.60);
	y = max(y, d.groundTop);
	return y + 1.0;
}
