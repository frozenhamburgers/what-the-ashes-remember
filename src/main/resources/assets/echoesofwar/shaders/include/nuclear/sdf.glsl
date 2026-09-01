// Shared distance/intersection primitives for the nuclear volumes.
// NOT used for lighting, just density fields marched at fixed steps
// so use for cheap bounding tests and measuring distance from axis for noise applications

// ------------------ BOUNDING TESTS

// ray vs sphere, returns the entry/exit distances along rd (which must be normalised)
bool intersectSphereBounds(vec3 ro, vec3 rd, vec3 centre, float radius,
		out float tEnter, out float tExit) {
	vec3 oc = ro - centre;
	float b = dot(oc, rd);
	float c = dot(oc, oc) - radius * radius;
	float h = b * b - c;
	if (h < 0.0) return false;
	h = sqrt(h);
	tEnter = max(-b - h, 0.0); // clamp to zero, camera in volume case
	tExit = -b + h;
	return tExit > tEnter;
}

// whether a ray passes within radius of the segment a..b. Used once per pixel
// to decide which attacks a ray could possibly hit, so that the march itself
// only ever walks those that survive rather than ray. big performance improvement
bool rayNearSegment(vec3 ro, vec3 rd, vec3 a, vec3 b, float radius) {
	vec3 ab = b - a;
	vec3 ao = ro - a;
	float abLen2 = dot(ab, ab);
	if (abLen2 < 1e-8) {
		// degenerate fallback
		float t = max(dot(a - ro, rd), 0.0);
		return distance(ro + rd * t, a) <= radius;
	}
	float abd = dot(ab, rd);
	float abao = dot(ab, ao);
	float rdao = dot(rd, ao);

	float denom = abLen2 - abd * abd;
	float tRay, sSeg;
	if (abs(denom) < 1e-6) {
		// near parallel, any direction is fine
		sSeg = 0.0;
		tRay = max(-rdao, 0.0);
	} else {
		sSeg = clamp((abd * rdao - abao) / denom, 0.0, 1.0);
		tRay = max(dot(a + ab * sSeg - ro, rd), 0.0);
	}

	vec3 rp = ro + rd * tRay;
	sSeg = clamp(dot(rp - a, ab) / abLen2, 0.0, 1.0);
	return distance(rp, a + ab * sSeg) <= radius;
}

// interval of the ray INSIDE a capsule
bool rayCapsuleInterval(vec3 ro, vec3 rd, vec3 pa, vec3 pb, float ra,
		out float t0, out float t1) {
	vec3 ab = pb - pa;
	float abLen = length(ab);
	if (abLen < 1e-5) return intersectSphereBounds(ro, rd, pa, ra, t0, t1);

	vec3 ax = ab / abLen;
	vec3 a = pa - ax * ra;      // padded start
	float len = abLen + 2.0 * ra;

	vec3 oa = ro - a;
	float rdA = dot(rd, ax);
	float oaA = dot(oa, ax);
	vec3 rdPerp = rd - ax * rdA;
	vec3 oaPerp = oa - ax * oaA;

	float A = dot(rdPerp, rdPerp);
	float B = 2.0 * dot(rdPerp, oaPerp);
	float C = dot(oaPerp, oaPerp) - ra * ra;

	if (A < 1e-8) {
		if (C > 0.0) return false;
		t0 = -1e30;
		t1 = 1e30;
	} else {
		float disc = B * B - 4.0 * A * C;
		if (disc < 0.0) return false;
		float sq = sqrt(disc);
		t0 = (-B - sq) / (2.0 * A);
		t1 = (-B + sq) / (2.0 * A);
	}

	// clip to the slab
	if (abs(rdA) < 1e-6) {
		if (oaA < 0.0 || oaA > len) return false;
	} else {
		float s0 = (0.0 - oaA) / rdA;
		float s1 = (len - oaA) / rdA;
		t0 = max(t0, min(s0, s1));
		t1 = min(t1, max(s0, s1));
	}

	t0 = max(t0, 0.0);
	return t1 > t0;
}

// ----- AXIAL FRAME

// Decomposes p into distance ALONG an attack's axis and distance OFF it.
// Every cone and cylinder in the fight is described this way instead pf a
// true SDF: the profile (tapering for a cone, constant for a cylinder) is then
// just a function of the axial coordinate, and the noise wraps the radius. much cheaper than SDF
void axialFrame(vec3 p, vec3 origin, vec3 axis, out float along, out float off) {
	vec3 v = p - origin;
	along = dot(v, axis);
	off = length(v - axis * along);
}

// ------ OTHER
// Smooth max, for joining two density fields without a crease. Used where an
// attack meets the body, since max() leaves a visible seam at the root
float smaxDensity(float a, float b, float k) {
	float h = clamp(0.5 + 0.5 * (a - b) / k, 0.0, 1.0);
	return mix(b, a, h) + k * h * (1.0 - h);
}
