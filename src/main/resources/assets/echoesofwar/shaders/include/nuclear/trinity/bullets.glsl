// ---------------------------------------------------------------------------
// Trinity's projectile field, generated
// other half of pair of BulletFieldMath.java, generating the asme projectiles on CPU
// Requires post_defaults, noise, nuclear/palette and nuclear/trinity/instance.
// ---------------------------------------------------------------------------

const int BF_CELLS = 6;      // per cube face edge
const int BF_SEARCH = 2;     // how far outside its own cell a sample looks
const int BF_WINDOW = 10;	// Compile-time bound on the sliding window of live pulses

//----------------- SHAPE
// projectile is the same material as everything else Trinity throws, same old

const float BF_GRAIN       = 0.50;  // lattice unit, as a fraction of radius
const float BF_WARP_LOBE   = 0.40;
const float BF_WARP_MACRO  = 0.20;
const float BF_FALLOFF_POW = 2.2;
const float BF_DENSITY     = 1.45;
const float BF_BOUND       = 1.85;  // how far past the radius the warp can reach

// subtle backward lattice scroll, in grain units per second.
const float BF_CHURN = 1.30;

const float BF_CORE_FRAC = 0.40;
const float BF_CORE_GAIN = 1.15;

// ------------------------- CUBE SPHERE MAPPING
// Face 0/1 are +X/-X, 2/3 are +Y/-Y, 4/5 are +Z/-Z. bfFace and bfDir are exact inverses

int bfFace(vec3 d, out vec2 ab) {
	vec3 a = abs(d);
	if (a.x >= a.y && a.x >= a.z) {
		ab = vec2(d.y, d.z) / a.x;
		return d.x > 0.0 ? 0 : 1;
	}
	if (a.y >= a.z) {
		ab = vec2(d.x, d.z) / a.y;
		return d.y > 0.0 ? 2 : 3;
	}
	ab = vec2(d.x, d.y) / a.z;
	return d.z > 0.0 ? 4 : 5;
}

vec3 bfDir(int face, vec2 ab) {
	if (face == 0) return normalize(vec3(1.0, ab.x, ab.y));
	if (face == 1) return normalize(vec3(-1.0, ab.x, ab.y));
	if (face == 2) return normalize(vec3(ab.x, 1.0, ab.y));
	if (face == 3) return normalize(vec3(ab.x, -1.0, ab.y));
	if (face == 4) return normalize(vec3(ab.x, ab.y, 1.0));
	return normalize(vec3(ab.x, ab.y, -1.0));
}

// cell coordinate (even out of range) to in face coordinates
vec2 bfCellCoord(vec2 cell) { return cell / float(BF_CELLS) * 2.0 - 1.0; }

ivec2 bfCellIndex(vec2 ab) {
	return clamp(ivec2(floor((ab + 1.0) * 0.5 * float(BF_CELLS))), 0, BF_CELLS - 1);
}

// =------------------ HASHING

uint bfHash(uint x) {
	x ^= x >> 16u;
	x *= 0x7feb352du;
	x ^= x >> 15u;
	x *= 0x846ca68bu;
	x ^= x >> 16u;
	return x;
}

float bfUnit(uint h) { return float(h >> 8u) * (1.0 / 16777216.0); }

uint bfKey(int face, ivec2 cell, int wave) {
	int c = face + 6 * (cell.x + BF_CELLS * (cell.y + BF_CELLS * wave));
	return bfHash(bfHash(uint(c + 1)) ^ uint(tBfSeed()));
}

// --------------------- PROJECTILES

// projectile rebuilt every time, jitter applied in cell coordinates before projecting to sphere
void bfBullet(int face, ivec2 cell, int wave, out vec3 dir, out float speedMul, out float radiusMul) {
	uint h1 = bfKey(face, cell, wave);
	uint h2 = bfHash(h1);
	uint h3 = bfHash(h2);
	float r1 = bfUnit(h1), r2 = bfUnit(h2), r3 = bfUnit(h3);

	vec2 jittered = vec2(cell) + 0.5 + (vec2(r1, r2) - 0.5) * tBfJitter();
	dir = bfDir(face, bfCellCoord(jittered));
	speedMul = 1.0 + (r3 - 0.5) * 2.0 * tBfSpeedJitter();
	radiusMul = 0.78 + 0.5 * r1;
}

// resolve potentially out of range cell index to its actual cell
void bfResolve(int face, ivec2 cell, out int outFace, out ivec2 outCell) {
	if (cell.x >= 0 && cell.x < BF_CELLS && cell.y >= 0 && cell.y < BF_CELLS) {
		outFace = face;
		outCell = cell;
		return;
	}
	vec3 d = bfDir(face, bfCellCoord(vec2(cell) + 0.5));
	vec2 ab;
	outFace = bfFace(d, ab);
	outCell = bfCellIndex(ab);
}

// dsitance a pulse's projectiles have covered, -1 if not launched yet
float bfWaveDistance(int wave) {
	float age = tBfAge() - tBfLead() - float(wave) * tBfInterval();
	return age <= 0.0 ? -1.0 : age * tBfSpeed();
}

// how far proj is thru its flight
float bfFade(float base, float speedMul) {
	float u = clamp(base * speedMul / max(tBfTravel(), 1.0), 0.0, 1.0);
	float fade = smoothstep(tBfFadeFrom(), 1.0, u);
	return clamp(1.0 - max(fade, tBfCut()), 0.0, 1.0);
}

// ----------------------------- FIELD

// desnity of entire projectile field at p, + nearest projectile center for lighting
float bulletFieldDensity(vec3 p, float detail, out vec3 gsrc, out float glow) {
	gsrc = tCentre();
	glow = 0.0;
	if (tBfActive() < 0.5 || tBfCut() >= 1.0) return 0.0;

	vec3 rel = p - tCentre();
	float r = length(rel);
	if (r < 1e-4) return 0.0;

	float maxR = tBfRadius() * 1.3 * BF_BOUND;
	if (r < tBfLaunch() - maxR || r > tBfLaunch() + tBfTravel() + maxR) return 0.0;

	vec3 d = rel / r;
	vec2 ab;
	int face = bfFace(d, ab);
	ivec2 base = bfCellIndex(ab);

	// how far to look
	float alpha = maxR / r;
	float reach = alpha * float(BF_CELLS);
	int rad = clamp(int(ceil(reach)), 1, BF_SEARCH);

	float dens = 0.0;
	float bestGlow = 0.0;

	for (int k = 0; k < BF_WINDOW; k++) {
		if (k >= tBfWindow()) break;
		int w = tBfFirstWave() + k;
		float wdist = bfWaveDistance(w);
		if (wdist < 0.0 || wdist * (1.0 - tBfSpeedJitter()) > tBfTravel()) continue;

		float lo = tBfLaunch() + wdist * (1.0 - tBfSpeedJitter()) - maxR;
		float hi = tBfLaunch() + wdist * (1.0 + tBfSpeedJitter()) + maxR;
		if (r < lo || r > hi) continue;

		for (int di = -BF_SEARCH; di <= BF_SEARCH; di++) {
			if (abs(di) > rad) continue;
			for (int dj = -BF_SEARCH; dj <= BF_SEARCH; dj++) {
				if (abs(dj) > rad) continue;

				int cf;
				ivec2 cc;
				bfResolve(face, base + ivec2(di, dj), cf, cc);

				vec3 bdir;
				float speedMul, radiusMul;
				bfBullet(cf, cc, w, bdir, speedMul, radiusMul);

				float R = tBfRadius() * radiusMul;
				vec3 c = tCentre() + bdir * (tBfLaunch() + wdist * speedMul);
				vec3 q = p - c;
				float qd = length(q);
				if (qd > R * BF_BOUND) continue;

				float fade = bfFade(wdist, speedMul);
				if (fade <= 0.004) continue;

				float grain = max(R * BF_GRAIN, 0.30);
				int wWrap = w - (w / 64) * 64;
				vec3 lat = (q - bdir * (tTime() * BF_CHURN * grain)) / grain
						+ float(cf) * 3.7 + float(cc.x) * 11.3 + float(cc.y) * 5.9
						+ float(wWrap) * 23.1;

				float macro = fbm2(lat * F_MACRO);
				float lobe = billow2(lat * F_LOBE);

				float surf = R * (1.0
						+ BF_WARP_MACRO * (macro - 0.5) * 2.0
						+ BF_WARP_LOBE * (lobe - 0.5) * 2.0);

				float k = clamp(1.0 - qd / max(surf, 0.001), 0.0, 1.0);
				float bd = pow(k, BF_FALLOFF_POW);
				if (bd > 0.0) {
					if (detail > 0.01) {
						float fine = billow2(lat * F_FINE);
						bd *= mix(1.0, 0.60 + 0.80 * fine, detail * 0.60);
					}
					dens = max(dens, bd * BF_DENSITY * fade);
				}

				float rn = qd / max(R * BF_CORE_FRAC, 0.05);
				float g = exp(-rn * rn) * BF_CORE_GAIN * fade * tCoreIntensity();
				if (g > bestGlow) {
					bestGlow = g;
					gsrc = c;
				}
			}
		}
	}

	glow = bestGlow;
	return dens;
}

// ---------- TELEPGRAH

const float BF_TG_SHELL_R = 0.88;
const float BF_TG_SHELL_W = 0.34;
const float BF_TG_GAIN    = 1.25;

float bulletFieldTelegraph(vec3 p) {
	if (tBfActive() < 0.5 || tBfCut() >= 1.0) return 0.0;
	float interval = max(tBfInterval(), 0.05);
	float lead = max(tBfLead(), 0.05);

	// time until next pulse
	float best = 1e9;
	int next = tBfFirstWave() + max(tBfWindow(), 0);
	float until = (tBfLead() + float(next) * interval) - tBfAge();
	if (until >= 0.0) best = until;
	if (best > lead) return 0.0;

	float ramp = 1.0 - clamp(best / lead, 0.0, 1.0);

	float R = tBodyR();
	if (R <= 0.001) return 0.0;
	float d = (length(p - tCentre()) - R * BF_TG_SHELL_R) / (R * BF_TG_SHELL_W);
	return exp(-d * d) * ramp * ramp * BF_TG_GAIN * tCoreIntensity();
}
