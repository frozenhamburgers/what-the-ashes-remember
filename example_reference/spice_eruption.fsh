#version 150

#moj_import <echoesofwar:common_math.glsl>

uniform sampler2D MainDepthSampler;
uniform samplerBuffer DataBuffer;
uniform int InstanceCount;
uniform mat4 invProjMat;
uniform mat4 invViewMat;
uniform vec3 cameraPos;
uniform vec2 ScreenSize;
// Size of THIS pass's output target (the low-res raymarch buffer), not the
// window. Used to work out how much world space one output pixel covers.
uniform vec2 OutSize;
uniform float time;

in vec2 texCoord;
out vec4 fragColor;

// ---------------------------------------------------------------------------
// The plume is a raymarched volume whose noise lattice is tied to the flow, not
// to world space. A sample point is expressed in a frame that has been divided
// by the local spread of the flow and scrolled upward, so a structure keeps its
// identity while growing as it rises - masses billow, collide and merge instead
// of a fixed texture sliding through a fixed silhouette. See columnLattice().
//
// The silhouette is two fields: blastDensity (the ground-level explosion) and
// columnDensity (the geyser, the stalled mantle it feeds, and the collapse of
// that mantle back to the ground - all one continuous volume). Each is a smooth
// shape displaced by billow noise, which is what turns a smooth envelope into
// packed cauliflower lobes with creases between them.
// ---------------------------------------------------------------------------

const int   MAX_STEPS           = 64;   // primary march budget (per instance)
const int   MIN_STEPS           = 26;   // floor after distance LOD
const float RENDER_DISTANCE_CAP = 512.0;

// Extinction. Deliberately high - the densest smoke is meant to be essentially
// opaque, not a translucent haze.
const float SIGMA_T = 2.35;

// Noise frequencies, in lattice units (Erupt.grain sets the unit's size in
// blocks). Picked against what the march and the low-res target can actually
// resolve: every fbm/billow call runs internal octaves at 2.07x and 2.13x on
// top of these, so the finest content lands well below the stated frequency.
// The old values bottomed out near 0.2 blocks - under one output pixel at any
// distance - which is what turned the column and the blast into speckle while
// the falling smoke, whose finest octave is ~1 block, stayed clean.
const float F_MACRO = 0.26; // individual sub-plumes / large masses
const float F_LOBE  = 0.70; // billowing cauliflower lobes
const float F_FINE  = 1.50; // turbulent detail; faded out by the distance LOD

// ---------------------------------------------------------------------------
// Noise
// ---------------------------------------------------------------------------

// Four independent 1D hashes at once - lets one 3D value-noise lookup cost two
// vec4 hashes instead of eight scalar ones.
vec4 hash41(vec4 p) {
	p = fract(p * 0.1031);
	p *= p + 33.33;
	p *= p + p;
	return fract(p);
}

// Trilinear value noise built on a single linear lattice key.
float vnoise(vec3 x) {
	vec3 i = floor(x);
	vec3 f = fract(x);
	f = f * f * (3.0 - 2.0 * f);

	float n = i.x + i.y * 157.0 + i.z * 113.0;
	vec4 k = vec4(0.0, 1.0, 157.0, 158.0) + n; // (0,0) (1,0) (0,1) (1,1) in xy
	vec4 h0 = hash41(k);
	vec4 h1 = hash41(k + 113.0);

	vec4 a = mix(h0, h1, f.z);
	vec2 b = mix(a.xz, a.yw, f.x);
	return mix(b.x, b.y, f.y);
}

// Per-octave offsets instead of a rotation matrix - same decorrelation, cheaper.
const vec3 OCT_SHIFT = vec3(37.13, 11.71, 23.57);

float fbm2(vec3 p) {
	float s = vnoise(p) * 0.62;
	p = p * 2.07 + OCT_SHIFT;
	s += vnoise(p) * 0.38;
	return s;
}

// Billow / "turbulence": abs() folds the noise so its minima become sharp
// creases and its maxima stay smooth and rounded. That asymmetry is what turns
// a displaced surface into cauliflower lobes rather than dents - it is the
// whole reason the plume reads as packed balls of smoke instead of soft blur.
float billow2(vec3 p) {
	float s = abs(vnoise(p) * 2.0 - 1.0) * 0.64;
	p = p * 2.09 + OCT_SHIFT;
	s += abs(vnoise(p) * 2.0 - 1.0) * 0.36;
	return s;
}

// far plane world position for this texCoord.
vec3 getFarWorldPos(vec2 uv) {
	vec4 clipSpacePosition = vec4(uv * 2.0 - 1.0, 1.0, 1.0);
	vec4 viewSpacePosition = invProjMat * clipSpacePosition;
	viewSpacePosition /= viewSpacePosition.w;
	vec4 localSpacePosition = invViewMat * viewSpacePosition;
	return cameraPos + localSpacePosition.xyz;
}

// ray intersection against a vertical cylinder (radius `boundRadius`, axis through
// base.xz) clipped to the Y-slab [base.y+yLow, base.y+yHigh]. O(1) - lets pixels that never
// touch the column skip the raymarch loop entirely, and bounds the loop's march interval.
bool intersectColumnBounds(vec3 ro, vec3 rd, vec3 base, float boundRadius, float yLow, float yHigh,
		out float tEnter, out float tExit) {
	float ocx = ro.x - base.x;
	float ocz = ro.z - base.z;
	float a = rd.x * rd.x + rd.z * rd.z;
	float cylEnter, cylExit;
	if (a < 1e-6) {
		// Near-vertical ray: horizontal position barely changes, so it's either always
		// inside the infinite cylinder or never.
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

// ---------------------------------------------------------------------------
// Eruption model
// ---------------------------------------------------------------------------

// Radial spread of the flow between vent and ceiling. Doubles as the scale of
// the noise lattice, so smoke structures physically grow as they rise instead
// of the whole column sharing one feature size.
const float EXPAND_R = 3.0;
// Vertical cell growth is deliberately weaker than radial: it keeps the noise
// roughly isotropic low down and lets it flatten out near the top, which is
// what gives the canopy its wide, rolled-over lobes.
const float EXPAND_Y = 1.8;
// Lattice scroll rates, in lattice units per second. The rate runs from
// SCROLL_UP to -SCROLL_FALL as the plume collapses, so the smoke visibly stops
// rising and starts sinking. Both are spatially uniform, so the reversal adds no
// shear to the lattice however long the eruption runs.
const float SCROLL_UP   = 4.5;
const float SCROLL_FALL = 1.6;

struct Erupt {
	vec3  base;
	float R0;        // vent radius
	float R1;        // reference radius at the ceiling
	float H;         // nominal ceiling height
	float seed;
	float t;         // age, seconds
	float life;      // total lifetime, seconds
	float lf;        // t / life

	float tau;       // rise time constant
	float frontH;    // highest point the plume has reached

	float press;     // vent pressure, 1 -> 0; everything late is downstream of it
	float activeH;   // top of the still-driven jet; walks down as pressure fails
	float jetWiden;  // the jet broadens as it loses coherence

	float topH;      // ceiling of the stalled mantle; sinks as the mass drains
	float fallBot;   // leading edge of the falling material
	float capR;      // mantle width at the head
	float skirtR;    // mantle width at its base
	float headY;     // centre of the head; falling branches are shed from here
	float scroll;    // integrated lattice scroll phase
	float grain;     // size of one noise lattice unit, in blocks
	float fallFlare; // how far past the canopy the landed smoke spreads
	float spent;     // 1 once the ceiling has sunk near the ground
	float fallAmp;   // presence of the falling branches (ramps in only)
	float fallDrop;  // how far the branches have descended, in blocks

	float blastR;
	float blastRise;
	float blastErode;

	float glowSpread;
	float glowI;
	float coreI;
};

// Antiderivative of smoothstep(a, b, x), used to turn the collapsing scroll RATE
// into a scroll PHASE. Integrating matters: writing rate(t) * t instead would
// drag the whole lattice bodily through the volume whenever the rate changed.
float smoothstepIntegral(float x0, float a, float b) {
	float x = clamp((x0 - a) / (b - a), 0.0, 1.0);
	float x3 = x * x * x;
	return (b - a) * (x3 - 0.5 * x3 * x) + max(x0 - b, 0.0);
}

Erupt setupEruption(vec3 base, float R0, float R1, float H, float seed, float t, float life) {
	Erupt e;
	e.base = base; e.R0 = R0; e.R1 = R1; e.H = H;
	// Wrapped into a small range: the raw seed is derived from world coordinates
	// and would otherwise push the noise lattice out to magnitudes where float
	// precision starts to cost hash quality.
	e.seed = fract(abs(seed) * 0.0143) * 53.0;
	e.t = t; e.life = max(life, 0.001);
	e.lf = clamp(t / e.life, 0.0, 1.0);

	// One noise lattice unit, in blocks. Scaling it with the eruption keeps the
	// number of visible lobes roughly constant, instead of handing a big blow
	// proportionally FINER - and so more aliased - structure than a small one.
	e.grain = max(R1 * 0.145, 1.3);
	// The ground spread is generous for a small blow but must not keep scaling
	// linearly with the canopy: a large blow already has an enormous cap, and
	// flaring that by the same factor buries the landscape.
	e.fallFlare = mix(1.45, 1.02, smoothstep(9.0, 20.0, R1));

	e.tau = 1.35 + H * 0.048;
	e.frontH = H * (1.0 - exp(-t / e.tau));

	// Vent pressure. The jet does not switch off, it runs out of push - and a jet
	// can only hold material up as high as its pressure allows, so the top of the
	// actively rising column walks back down rather than being cut off.
	e.press = 1.0 - smoothstep(0.28, 1.0, e.lf);
	e.jetWiden = 1.0 + 0.90 * (1.0 - e.press);
	// Floored, so the jet ends up a shrinking stub that erodes away rather than
	// having its top cross zero - a top crossing zero reads as being switched off.
	e.activeH = min(e.frontH, max(H * pow(max(e.press, 0.0), 0.55), H * 0.05));

	// The mantle is everything the jet has already lifted and can no longer hold
	// up. Its ceiling starts at the rise front and sinks once nothing is feeding
	// it; its leading edge descends from just under the front all the way past
	// the ground. So the canopy grows DOWNWARD out of the column it came from,
	// with no moment where a layer appears at a fixed height.
	float sink = pow(smoothstep(0.55, 1.02, e.lf), 1.35);
	e.topH = mix(e.frontH, H * 0.07, sink);
	// Once the ceiling has sunk toward the ground there is very little material
	// left in the air. The mantle and the falling branches both read this, so
	// they thin out together rather than on two separate schedules.
	e.spent = 1.0 - clamp(e.topH / (H * 0.60), 0.0, 1.0);
	float descend = pow(smoothstep(0.08, 0.72, e.lf), 1.40);
	// Stops short of the ground rather than sweeping all the way down to it.
	// What bridges the canopy to the ground is fallDensity, which has actual
	// descending masses in it instead of a smoothly contracting cone.
	e.fallBot = e.topH * mix(0.84, mix(0.18, -0.20, e.spent), descend);

	// Lateral spread. The head balloons first; once material starts landing, the
	// base of the mantle spreads out along the ground and overtakes it. Both are
	// scaled by how far the column has actually risen, so a young plume gets a
	// small head proportional to itself rather than a full-size cap.
	float grown  = mix(0.40, 1.0, smoothstep(0.05, 0.75, clamp(e.frontH / H, 0.0, 1.0)));
	float mature = smoothstep(0.10, 0.78, e.lf);
	float ground = smoothstep(0.62, 1.02, e.lf);
	e.capR   = R1 * (1.00 + mature * 1.45) * grown;
	e.skirtR = R1 * (0.45 + mature * 1.05 + ground * 0.80) * grown;
	e.headY  = mix(e.fallBot, e.topH, 0.68);

	// Falling branches. fallAmp only ever ramps IN - what removes them is
	// reaching the ground and spreading out, never the clock.
	e.fallAmp  = smoothstep(0.26, 0.55, e.lf);
	e.fallDrop = H * 0.085 * max(t - e.life * 0.24, 0.0);

	e.scroll = SCROLL_UP * t
	         - (SCROLL_UP + SCROLL_FALL) * e.life * smoothstepIntegral(e.lf, 0.42, 0.90);

	// Stage 1 - the explosion. Expands fast then coasts. It is removed by
	// eroding the density field away rather than by fading alpha, so it breaks
	// into thinning wisps and opens up to reveal the geyser behind it.
	e.blastR = R1 * 1.55 * (1.0 - exp(-t / 0.78));
	e.blastRise = smoothstep(0.0, 0.22, t);
	e.blastErode = smoothstep(1.8, 5.3, t);

	// The glow starts as a tight hot core, follows the column up, then spreads
	// through the head and softens as the smoke there becomes diffuse.
	e.glowSpread = smoothstep(0.28, 0.72, e.lf);
	e.coreI = smoothstep(0.0, 0.12, t) * (1.0 - smoothstep(0.7, 4.4, t));
	e.glowI = smoothstep(0.4, 2.0, t) * (1.0 - smoothstep(0.70, 0.97, e.lf));
	return e;
}

// Lateral meander of the centreline. Amplitude grows with height, so the base
// stays a tight jet while the upper plume leans and snakes.
vec2 columnDrift(Erupt e, float h) {
	float hn = clamp(h / e.H, 0.0, 1.0);
	float amp = e.R1 * 0.38 * smoothstep(0.05, 0.9, hn);
	float u = hn * 2.6 - e.t * 0.20 + e.seed;
	vec2 o = vec2(sin(u * 1.61), cos(u * 1.27)) * 0.62
	       + vec2(sin(u * 0.57 + 2.1), cos(u * 0.71 + 4.3)) * 0.38;
	return o * amp;
}

// World point -> noise lattice coordinate for the column. Radial distance is
// divided by the spread of the flow, so a structure keeps its identity while
// growing as it rises; the vertical coordinate is the integral of 1/spread so
// the lattice stays roughly isotropic instead of smearing into stripes.
vec3 columnLattice(Erupt e, vec2 d, float hn, float S) {
	float eulY = (e.H / (EXPAND_Y - 1.0)) * log(1.0 + (EXPAND_Y - 1.0) * hn);
	return vec3(d.x / S, eulY - e.scroll, d.y / S) / e.grain
	     + vec3(e.seed * 3.7, 0.0, e.seed * 2.3);
}

// The jet and the mantle it feeds are ONE field sampled through ONE lattice.
// They are combined before any noise is applied, so there is no seam and no
// moment where a second volume switches on - only a boundary between them that
// migrates downward as the eruption loses pressure.
float columnDensity(Erupt e, vec3 p, float detail) {
	vec3 q = p - e.base;
	float h = q.y;
	if (h < -1.0 || h > max(e.activeH, e.topH) + e.H * 0.10) return 0.0;

	float hn = clamp(h / e.H, 0.0, 1.0);
	vec2 d = q.xz - columnDrift(e, h);
	float r = length(d);

	// --- the driven jet ---------------------------------------------------
	// No hard cutoff anywhere: as pressure decays it broadens, thins, and its
	// top walks down, and by the time it is gone the mantle already occupies
	// the space it used to.
	float jetR = (e.R0 + (e.R1 - e.R0) * pow(hn, 0.85)) * e.jetWiden;
	float jetSoft = e.H * 0.09 + e.activeH * 0.28;
	float jetFade = smoothstep(e.activeH - jetSoft, e.activeH + jetSoft * 0.5, h);
	float jetShape = 1.0 - r / max(jetR, 0.001)
	               - jetFade * 1.70
	               - (1.0 - e.press) * 0.75;

	// --- the stalled mantle -----------------------------------------------
	float span = max(e.topH - e.fallBot, 0.001);
	float u = clamp((h - e.fallBot) / span, 0.0, 1.0); // 0 = leading edge, 1 = ceiling
	// Rounded crown, measured DOWN from the ceiling in world units. Tying it
	// to u instead would make the dome's height depend on where the mantle
	// floor happens to be, and it collapses into a flat lid as the floor
	// rises. The quarter-circle profile closes the radius to nothing at the
	// ceiling, so the silhouette is domed by the radius rather than being
	// sliced off by the vertical mask.
	float crownT = max(e.capR * 0.65, e.H * 0.15);
	float ct = 1.0 - clamp((e.topH - h) / crownT, 0.0, 1.0);
	float crown = sqrt(max(1.0 - ct * ct, 0.0));
	float mR = mix(e.skirtR, e.capR, smoothstep(0.0, 0.72, u));
	float vSoft = span * 0.26 + e.H * 0.03;
	float mVert = smoothstep(e.fallBot - vSoft, e.fallBot + vSoft * 0.7, h)
	            * (1.0 - smoothstep(e.topH, e.topH + vSoft * 0.5, h));
	// Three spatial reasons for the mantle to thin, and no temporal one - it
	// disappears because of where it has got to, not because a timer expired:
	//   - material that has fallen furthest has spread furthest (u -> 0), which
	//     also puts a real density gradient between the dense head and the
	//     thin skirt near the ground;
	//   - a mantle wider than the plume ever was is stretched thinner;
	//   - and once the ceiling has sunk near the ground (e.spent) there is
	//     hardly anything left in the air, so what remains is residual dust.
	// Below the mantle the descending branches of fallDensity take over.
	float thin = (1.0 - u) * 0.20
	           + max(mR / (e.R1 * 2.4) - 1.0, 0.0) * 0.55
	           + e.spent * 1.25;
	// The crown enters as a pinch on the field rather than as a shrinking
	// divisor: r / (mR * crown) would blow up as the dome closes and leave a
	// needle of density standing on the axis at the apex. This way the apex
	// just reaches zero and stops.
	float mantleShape = crown - r / max(mR, 0.001)
	                  - (1.0 - mVert) * 1.70
	                  - thin;

	float shape = max(jetShape, mantleShape);
	if (shape < -0.85) return 0.0;

	// How stalled this sample is. Same lattice either way, so the two blend
	// seamlessly, but rising smoke stays tight and pulsed while stalled smoke
	// goes broad, soft and undifferentiated.
	float stall = clamp((mantleShape - jetShape) * 2.2 + 0.5, 0.0, 1.0);

	float S = 1.0 + (EXPAND_R - 1.0) * hn;
	vec3 m = columnLattice(e, d, hn, S);
	vec3 evo = vec3(e.t * 0.22, e.t * 0.09, e.t * -0.17);

	float macro = fbm2(m * F_MACRO);
	float lobe  = billow2(m * F_LOBE + evo * 0.40);

	// The vent pulses rather than emitting a smooth ribbon. Phase is jittered by
	// the macro field so the pulses read as separate rising masses instead of
	// stacked rings - and it dies out in stalled smoke, which is not being
	// driven by anything any more.
	float puff = sin(m.y * 0.9 + macro * 6.0) * 0.5 + 0.5;

	// Capping the shape term is what lets the interior have structure at all: an
	// uncapped 1-at-the-axis falloff always out-votes the noise, and the volume
	// ends up a solid slug with a billowy rind. Capped, the noise carves cavities
	// and density gradients right through the middle, and the glow inside has
	// thin spots to escape through.
	float field = min(shape, 0.55) * 1.50
	            + (macro - 0.46) * 0.95
	            + (lobe  - 0.40) * 0.78
	            + (puff  - 0.5)  * 0.26 * (1.0 - 0.80 * stall);

	if (detail > 0.01) {
		float fine = billow2(m * F_FINE + evo * 1.05);
		field += (fine - 0.42) * 0.30 * detail * (1.0 - 0.55 * stall);
	}

	return smoothstep(0.0, mix(0.24, 0.46, stall), field);
}

float blastDensity(Erupt e, vec3 p, float detail) {
	if (e.blastRise <= 0.003 || e.blastErode >= 0.999) return 0.0;
	vec3 q = p - e.base;
	float R = max(e.blastR, 0.001);

	// Dome sitting on the ground, plus the low wide skirt of dust thrown out
	// along the surface.
	float dome  = 1.0 - length(vec3(q.x, (q.y - R * 0.26) * 1.30, q.z)) / R;
	float skirt = 1.0 - length(vec3(q.x, (q.y - R * 0.04) * 5.0, q.z)) / (R * 1.55);
	float shape = max(dome, skirt * 0.85);
	if (shape < -0.7) return 0.0;

	// Lattice frozen to the expanding ball: structures inflate with it instead
	// of boiling in place.
	vec3 m = q * (e.R1 * 1.55 / (R * e.grain))
	       + vec3(e.seed * 5.1, 71.0, e.seed * 1.9);
	vec3 evo = vec3(e.t * 0.55, e.t * -0.22, e.t * 0.33);

	float macro = fbm2(m * F_MACRO);
	float lobe  = billow2(m * F_LOBE + evo * 0.35);

	float field = min(shape, 0.62) * 1.42
	            + (macro - 0.46) * 0.80
	            + (lobe  - 0.40) * 0.72;

	if (detail > 0.01) {
		float fine = billow2(m * F_FINE + evo);
		field += (fine - 0.42) * 0.30 * detail;
	}

	field -= e.blastErode * 1.06;
	return smoothstep(0.0, mix(0.22, 0.50, e.blastErode), field) * e.blastRise;
}

// Smoke shed from inside the canopy, falling under gravity. Its own field
// rather than more of the mantle, for two reasons. Its lattice translates
// straight down through world space, so the motion reads as genuine falling
// instead of a silhouette contracting inward. And its plan-view mask picks out
// a handful of discrete streamers, so the canopy sheds branches rather than
// lowering a continuous cone.
float fallDensity(Erupt e, vec3 p, float detail) {
	if (e.fallAmp <= 0.004) return 0.0;
	vec3 q = p - e.base;
	float h = q.y;

	// Shed from INSIDE the canopy, and the top edge is buried well down inside it
	// where the mantle is already dense - an edge that reaches the outside of the
	// canopy shows up as a flat lid across the top of the plume.
	float top = min(e.headY + e.H * 0.04, e.topH - e.H * 0.10);
	if (h > top + e.H * 0.07) return 0.0;
	// Conservative floor: the fastest any column can have fallen.
	if (h < e.headY - e.fallDrop * 1.20 - e.H * 0.14) return 0.0;

	float r = length(q.xz - columnDrift(e, h));

	// Footprint, flaring near the ground where landed material spreads outward.
	float low = 1.0 - smoothstep(0.0, e.H * 0.22, h);
	float rOut = e.capR * mix(0.95, e.fallFlare, low);
	float band = 1.0 - smoothstep(rOut * 0.50, rOut, r);
	if (band <= 0.004) return 0.0;

	// Ragged leading edge: every column falls at its own rate. The rate comes
	// from a PLAN-VIEW field with no height term, which matters - sampling it
	// through a height-dependent warp (as an earlier version did) makes the
	// boundary fold back through itself, and a self-intersecting isosurface is
	// what produced the broken, shimmering geometry.
	float lag = fbm2(vec3(q.x, 41.0, q.z) * 0.042
	               + vec3(e.seed * 2.1, 0.0, e.seed * 3.3));
	float lead = e.headY - e.fallDrop * mix(0.80, 1.20, lag);

	// Smooth products throughout. min() intersections leave creases in the field,
	// and creases alias badly against a raymarch step of this size.
	float vBand = smoothstep(lead - e.H * 0.11, lead + e.H * 0.11, h)
	            * (1.0 - smoothstep(top - e.H * 0.05, top + e.H * 0.07, h));
	float shape = band * vBand;
	if (shape <= 0.004) return 0.0;

	// Lattice translating straight down at the fall speed, mildly elongated
	// vertically so the masses read as descending streamers.
	vec3 m = vec3(q.x, (h + e.fallDrop) * 0.70, q.z) * 0.150
	       + vec3(e.seed * 1.3, 0.0, e.seed * 4.7);

	// A mass only starts dispersing once it has actually landed, and the ones
	// that landed first disperse first, so the ground layer breaks up in stages.
	float landed = clamp(-lead / (e.H * 0.75), 0.0, 1.0);

	float field = shape * 1.55
	            + (fbm2(m) - 0.50) * 0.95
	            - 0.44
	            - low * (0.15 + 0.35 * landed)  // spreads out where it lands
	            - e.spent * 0.75;               // nothing left above to feed it
	if (detail > 0.01) field += (billow2(m * 3.0) - 0.42) * 0.22 * detail;

	return smoothstep(0.0, 0.42, field) * e.fallAmp;
}

float densityAt(Erupt e, vec3 p, float detail) {
	return min(columnDensity(e, p, detail) + blastDensity(e, p, detail)
	         + fallDensity(e, p, detail), 1.45);
}

// ---------------------------------------------------------------------------
// Lighting
// ---------------------------------------------------------------------------

// No sun-direction uniform reaches this post chain, so the key/fill pair is a
// fixed high side light. It only ever provides the cool rim; the dominant light
// on this effect is the eruption glow, which is what carves the lobes.
const vec3  SUN_DIR       = vec3(0.3775, 0.7947, 0.4569);
const vec3  SUN_COLOR     = vec3(0.54, 0.52, 0.70);
const vec3  SKY_COLOR     = vec3(0.16, 0.16, 0.28);
const vec3  SMOKE_SHADOW  = vec3(0.040, 0.034, 0.078);
const vec3  GLOW_COLOR    = vec3(1.00, 0.40, 0.95);
const vec3  GLOW_HOT      = vec3(1.00, 0.92, 1.00);

// Normalised Henyey-Greenstein: 1.0 would be isotropic. Forward scattering is
// what makes smoke lit from behind glow at its thin edges.
float hg(float c, float g) {
	float g2 = g * g;
	float d = 1.0 + g2 - 2.0 * g * c;
	return (1.0 - g2) / pow(max(d, 1e-4), 1.5);
}

// Where the light actually lives at this height. Early on it is the hot core in
// the blast; through the geyser it is a line source running up the axis, so
// smoke is lit from the nearest part of the core rather than one distant point;
// late it migrates up into the canopy.
vec3 glowSource(Erupt e, vec3 p) {
	float yLo = e.base.y + e.H * 0.03;
	float yHi = e.base.y + max(e.activeH * 0.92, e.H * 0.05);
	float y = clamp(p.y, yLo, yHi);
	y = mix(y, e.base.y + e.headY, e.glowSpread * 0.85);
	vec2 c = columnDrift(e, y - e.base.y);
	return vec3(e.base.x + c.x, y, e.base.z + c.y);
}

// Irradiance from the eruption core, ignoring occlusion. `src` returns whichever
// of the two emitters dominates here, for the shadow march to aim at.
float glowField(Erupt e, vec3 p, out vec3 src) {
	// Column / canopy emitter. Its radius widens as the smoke thins out, which
	// is what spreads the glow through the whole head late in the eruption
	// instead of leaving a narrow glowing line.
	vec3 gs = glowSource(e, p);
	float gr = mix(max(e.R0, 1.0) * 1.7, e.R1 * 1.25, e.glowSpread);
	float dg = length(p - gs) / max(gr, 0.001);
	// Inverse-square core with a gaussian cutoff. A bare inverse-square tail
	// still carries meaningful energy tens of blocks out, which is what used to
	// trace the emitter as a column of light in the clear air around the plume.
	// The gaussian kills it by ~3 radii while leaving the core untouched.
	float gi = e.glowI * mix(1.15, 0.80, e.glowSpread)
	         / (1.0 + dg * dg * 2.2) * exp(-dg * dg * 0.40);

	// Explosion core.
	vec3 cp = e.base + vec3(0.0, e.blastR * 0.30, 0.0);
	float cr = max(e.blastR * 0.55, 1.0);
	float dc = length(p - cp) / cr;
	float ci = e.coreI * 1.5 / (1.0 + dc * dc * 2.2) * exp(-dc * dc * 0.40);

	src = ci > gi ? cp : gs;
	return gi + ci;
}

// Optical depth between p and a light, on a geometric partition.
//
// The spacing is the whole point. Both shadow rays used to take uniform steps
// whose FIRST sample landed about two blocks out - and the lobe octave's cell
// is 2.07 blocks, so that first sample was already past the entire billow
// scale. No lobe could shadow itself, no fold could shadow the fold behind it,
// and every surface ended up lit by the same broad mass-scale gradient. That
// is what made the volume read as flat.
//
// Segment bounds are 0, 0.7u, 2.1u, 5.6u, 14u with taps at the midpoints, so
// the near pair resolves occlusion at the scale the density actually varies
// at while the last one still reaches the next mass over for the big
// overlapping shadows. Same total path length as before - this redistributes
// the samples rather than adding reach.
float shadowDepth(Erupt e, vec3 p, float dens, vec3 dir, float reach) {
	float u = reach * (1.0 / 14.0);
	// The first segment is anchored on the sample's OWN density rather than on a
	// tap. That is the one place the fine octave is already available at full
	// resolution for free, so fine-scale density gets a direct say in the
	// occlusion. Running the near taps at full detail instead measured out at
	// about 1% of the lobe-scale shaping - a sub-block path just cannot
	// accumulate much optical depth - for a lattice lookup on four taps per lit
	// sample, which is not a trade worth making.
	return dens                                    * (u * 0.30)
	     + densityAt(e, p + dir * (u * 0.45), 0.0) * (u * 0.40)
	     + densityAt(e, p + dir * (u * 1.40), 0.0) * (u * 1.40)
	     + densityAt(e, p + dir * (u * 3.85), 0.0) * (u * 3.50)
	     + densityAt(e, p + dir * (u * 9.80), 0.0) * (u * 8.40);
}

float sunDepth(Erupt e, vec3 p, float dens) {
	return shadowDepth(e, p, dens, SUN_DIR, e.H * 0.24) * SIGMA_T * 0.78;
}

float glowDepth(Erupt e, vec3 p, float dens, vec3 src) {
	vec3 dir = src - p;
	float dist = length(dir);
	if (dist < 1e-3) return 0.0;
	return shadowDepth(e, p, dens, dir / dist, min(dist, e.H * 0.32))
	     * SIGMA_T * 0.80;
}

// ---------------------------------------------------------------------------

void main() {
	vec3 rayOrigin = cameraPos;
	vec3 rayDir = normalize(getFarWorldPos(texCoord) - cameraPos);

	float tMaxScene = RENDER_DISTANCE_CAP;
	if (getDepth(MainDepthSampler, texCoord) < 0.9999) {
		vec3 sceneWorldPos = getWorldPos(MainDepthSampler, texCoord, invProjMat, invViewMat, cameraPos);
		tMaxScene = length(sceneWorldPos - cameraPos);
	}

	// Interleaved gradient noise. Offsetting each ray start inside its own step
	// trades banding for a fine stipple, which the low-res upscale then blurs
	// away. Deliberately not animated - at this resolution a temporal jitter
	// would shimmer instead of resolving.
	float dither = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));

	vec3 acc = vec3(0.0);      // premultiplied radiance
	float trans = 1.0;         // transmittance along the view ray

	for (int instance = 0; instance < InstanceCount; instance++) {
		if (trans < 0.015) break;

		int idx = instance * 9;
		vec3 base = fetch3(DataBuffer, idx);
		Erupt e = setupEruption(
			base,
			fetch(DataBuffer, idx + 3),   // baseRadius
			fetch(DataBuffer, idx + 4),   // topRadius
			fetch(DataBuffer, idx + 5),   // height
			fetch(DataBuffer, idx + 6),   // seed
			fetch(DataBuffer, idx + 7),   // age
			fetch(DataBuffer, idx + 8));  // lifetime
		if (e.lf >= 1.0) continue;

		float boundR = max(max(e.capR * e.fallFlare, e.skirtR), e.R1 * e.jetWiden) * 1.12
		             + e.R1 * 0.45;
		boundR = max(boundR, e.blastR * 1.65);
		float yHigh = max(max(max(e.activeH, e.topH) + e.H * 0.12, e.headY + e.H * 0.16),
				e.blastR * 1.5) + 1.0;
		float tEnter, tExit;
		if (!intersectColumnBounds(rayOrigin, rayDir, base, boundR, -2.5, yHigh, tEnter, tExit)) continue;
		tExit = min(tExit, tMaxScene);
		if (tEnter >= tExit) continue;

		// Distance LOD - a plume on the horizon does not need the full budget.
		int steps = int(mix(float(MAX_STEPS), float(MIN_STEPS),
				clamp((tEnter - 32.0) / 220.0, 0.0, 1.0)));
		float dt = (tExit - tEnter) / float(steps);

		// Procedural LOD. Detail finer than whichever is coarser - the march step
		// or one output pixel - cannot be resolved, and sampling it anyway only
		// converts it into noise. invProjMat[1][1] is tan(fovY/2), so this is the
		// world height that one pixel of the low-res target covers at the plume.
		float pixW = (tEnter + tExit) * abs(invProjMat[1][1]) / max(OutSize.y, 1.0);
		float lodSize = max(dt, pixW);
		float detail = clamp(1.35 - lodSize * F_FINE / e.grain, 0.0, 1.0);
		float t = tEnter + dt * dither;

		for (int i = 0; i < MAX_STEPS; i++) {
			if (i >= steps || trans < 0.015) break;
			vec3 p = rayOrigin + rayDir * t;
			t += dt;

			float dens = densityAt(e, p, detail);

			vec3 gsrc;
			float g = glowField(e, p, gsrc);

			if (dens > 0.004) {
				// Deep inside the volume the remaining contribution is tiny, so
				// estimate optical depth from the local density instead of paying
				// for shadow rays. Note it still TRACKS density - the old fallback
				// was a flat sT of 0.5, which lit every buried sample identically
				// no matter how deep it sat, and was a large part of why interiors
				// read as uniform.
				bool cheapLight = trans < 0.12 || dens < 0.03;
				bool litByGlow = g > 0.004;

				float sunOD = cheapLight
						? dens * e.grain * 2.20 * SIGMA_T * 0.78
						: sunDepth(e, p, dens);
				float glowOD = (litByGlow && !cheapLight)
						? glowDepth(e, p, dens, gsrc)
						: dens * e.grain * 0.90 * SIGMA_T * 0.80;

				// Two-lobe transmittance. The direct lobe is sharp, so a fold throws
				// a hard shadow onto whatever is behind it; the multiple-scattering
				// lobe re-uses the same optical depth at a fifth of the extinction,
				// so deep pockets fill with dim coloured light instead of crushing
				// to black. Real smoke gets its depth from this split, and it is
				// what separates this from simply turning the contrast up.
				float sT  = exp(-sunOD);
				float sMs = exp(-sunOD * 0.20);
				float gT  = exp(-glowOD);
				float gMs = exp(-glowOD * 0.26);

				vec3 toSrc = gsrc - p;
				float srcDist = length(toSrc);
				float cosT = srcDist > 1e-4 ? dot(toSrc / srcDist, rayDir) : 0.0;
				float phase = mix(1.0, hg(cosT, 0.42), 0.62);
				// Mild forward scattering on the sun as well, so a lobe's sunward
				// face and its shaded face separate further when backlit.
				float sPhase = mix(1.0, hg(dot(SUN_DIR, rayDir), 0.25), 0.35);
				vec3 glowCol = mix(GLOW_COLOR, GLOW_HOT, clamp(g * 0.5, 0.0, 1.0));

				// Emission is gated on local density, so the glow can only ever
				// be seen where there is smoke to scatter it - it cannot paint a
				// halo in clear air. Thin smoke still reveals it: the ray keeps
				// most of its transmittance through a thin patch and goes on to
				// pick up the much brighter samples deeper inside.
				acc += trans * glowCol * (g * dens * 0.42 * dt);

				// Powder: too little medium at a thin edge to scatter much, so
				// edges facing the light stay dark. Deliberately milder than before
				// - a thin edge or a gap should still catch light, and with the
				// shadow rays now resolving the near field the transmittance term
				// already handles most of what this used to be doing.
				float powder = 1.0 - exp(-dens * 3.4);
				float pw = mix(0.62, 1.0, powder);

				// Fine-scale self-occlusion, below the scale any shadow ray can
				// afford to resolve. dens carries the fine octave, so this is what
				// gives the interior its detail rather than just the silhouette.
				float localOcc = mix(0.42, 1.0, exp(-dens * 1.30));

				// Ambient has to work its way in through the same medium, so occlude
				// it with the soft lobe. The old flat 0.22 floor sat under every
				// shadow in the volume and was most of why pockets did not read as
				// pockets. Sky is also weighted down relative to the sun: when half
				// the light arrives from no particular direction, nothing can look
				// three-dimensional.
				float ao = (0.13 + 0.87 * sMs) * localOcc;

				vec3 L = (SMOKE_SHADOW + SKY_COLOR * 0.68) * ao
				       + SUN_COLOR * ((sT * sPhase * 1.05 + sMs * 0.12) * pw)
				       + glowCol * (g * (gT * phase * 1.24 + gMs * 0.18) * pw);

				float a = 1.0 - exp(-dens * SIGMA_T * dt);
				acc += trans * L * a;
				trans *= 1.0 - a;
			}
		}
	}

	fragColor = vec4(acc, alpha);
}
