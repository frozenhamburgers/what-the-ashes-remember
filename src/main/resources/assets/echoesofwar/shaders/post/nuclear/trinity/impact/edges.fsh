#version 330

// Stage one of the impact frames: the spotty outline buffer that the composite
// smears into streaks. See include/nuclear/trinity/impact.glsl for the whole pipeline.
//
// Writes coverage in .r. Runs at half resolution (trinity_impact in
// TrinityPostProcessor#getScaledTargets) - the radial blur destroys most of the
// detail anyway, and it is a hard threshold at the end, so there is nothing for
// the extra resolution to buy.

#moj_import <echoesofwar:common_math.glsl>
#moj_import <echoesofwar:post_defaults.glsl>
#moj_import <echoesofwar:noise.glsl>
#moj_import <echoesofwar:nuclear/trinity/instance.glsl>
#moj_import <echoesofwar:nuclear/trinity/impact.glsl>

uniform sampler2D MainDepthSampler;

layout(std140) uniform SamplerInfo {
	vec2 OutSize;
	vec2 InSize;
};

in vec2 texCoord;
out vec4 fragColor;

// Ring radius in output pixels. This IS the line thickness: a discontinuity is
// picked up by every fragment within EDGE_R of it, so the outline comes out
// 2*EDGE_R wide with no separate dilation pass. Wide on purpose - the streak
// this eventually becomes is only ever as thick as the outline that fed it.
const float EDGE_R = 6.0;
// Depth step that counts as an edge, as a FRACTION of the distance to the
// surface. Relative rather than absolute so a silhouette 400 blocks away reads
// as strongly as one at 20.
//
// Deliberately coarse: at a few percent this finds the corner of every block on
// a hillside, and a screen full of block-sized outlines blurs into an even grey
// wash with no readable direction. A fifth of the distance to the surface means
// only real silhouettes - a ridge against the sky, a wall against the ground
// behind it - survive, which is the "general shapes" this wants.
const float EDGE_T = 0.22;

// Holes. The outlines have to be broken up before the blur, or the radial smear
// turns them into smooth ramps instead of separate lines. The cell size sets the
// streak SPACING, so it is large: small cells give many thin streaks, which is
// the realistic-looking result and not the one wanted here.
const float EDGE_NOISE_CELL   = 78.0; // pixels per noise cell
const float EDGE_NOISE_SCROLL = 1.1;  // cells per second
const float EDGE_NOISE_T      = 0.50;

// Distance from the camera to whatever the ray hits, or a number far past the
// far plane for sky. Sky has to read as a hard discontinuity, not as "very
// distant geometry", or every silhouette against the horizon would be missed.
float sceneDist(vec2 uv) {
	if (getDepth(MainDepthSampler, uv) >= 0.9999) return 1.0e5;
	return length(getWorldPos(MainDepthSampler, uv, invProjMat, invViewMat, cameraPos)
			- cameraPos);
}

void main() {
	// Nothing wants this buffer unless a detonation is inside the stylised part
	// of its opening - not the white flash that follows, which needs no edges at
	// all - and it is a 17-tap depth reconstruction, so bail first. Same pick as
	// the composite, so the disc bounded here lands exactly on the centre the
	// blur is about to be taken around.
	ImpactPick pick = pickImpact();
	if (pick.weight * impactFrames(pick.age) <= 0.001) {
		fragColor = vec4(0.0);
		return;
	}

	vec2 px = EDGE_R / max(OutSize, vec2(1.0));
	float c = sceneDist(texCoord);

	// Eight taps on a ring rather than four neighbours: diagonals matter here,
	// because a diagonal silhouette sampled on a cross comes out dashed at
	// exactly the frequency the blur then stretches into streaks, and the
	// dashing would follow the screen axes instead of the noise.
	const vec2 RING[8] = vec2[8](
			vec2( 1.0,  0.0), vec2(-1.0,  0.0),
			vec2( 0.0,  1.0), vec2( 0.0, -1.0),
			vec2( 0.7071,  0.7071), vec2(-0.7071,  0.7071),
			vec2( 0.7071, -0.7071), vec2(-0.7071, -0.7071));

	float step_ = 0.0;
	for (int i = 0; i < 8; i++) {
		step_ = max(step_, abs(sceneDist(texCoord + RING[i] * px) - c));
	}

	// Binary, not a ramp. Every soft edge in this buffer becomes a soft-edged
	// streak downstream, and the whole look depends on the streaks having hard
	// borders. The one place a partial value is wanted is the blur's average,
	// and that gets its greys from coverage rather than from opacity.
	float edge = step(EDGE_T, step_ / max(min(c, 1.0e4), 1.0));

	// The disc marking ground zero contributes only its BOUNDARY here - it
	// enters the buffer as one more silhouette, the same width as every other,
	// so it comes back out of the blur as rays rather than as a smeared blob.
	// The inversion inside it happens in the composite, after the threshold.
	//
	// The field is normalised, so scaling back by the radius keeps the line the
	// same number of pixels wide however far the disc has stepped out.
	float df = impactDiscField(impactOffset(texCoord, pick.centre, OutSize),
			pick.radius, time) * pick.radius;
	edge = max(edge, step(abs(df), EDGE_R / max(OutSize.y, 1.0)));

	// Scrolling in z rather than translating in xy: the spots seethe in place
	// instead of sliding across the screen, which would fight the radial smear.
	// Applied last so the disc is broken up exactly like the terrain is.
	float n = fbm2(vec3(gl_FragCoord.xy / EDGE_NOISE_CELL, time * EDGE_NOISE_SCROLL));
	edge *= step(EDGE_NOISE_T, n);

	fragColor = vec4(edge, 0.0, 0.0, 1.0);
}
