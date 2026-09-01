#version 330

// Lays the marched volume over the scene, adds its bloom, and - for the first
// second after the blast - throws the whole frame away and draws impact frames
// over it instead.

#moj_import <echoesofwar:post_defaults.glsl>
#moj_import <echoesofwar:noise.glsl>
#moj_import <echoesofwar:nuclear/bomb_block/instance.glsl>
#moj_import <echoesofwar:nuclear/bomb_block/impact.glsl>
#moj_import <echoesofwar:nuclear/palette.glsl>

uniform sampler2D DiffuseSampler;
uniform sampler2D CloudSampler;
uniform sampler2D BloomSampler;
uniform sampler2D ImpactSampler;

layout(std140) uniform SamplerInfo {
	vec2 OutSize;
	vec2 DiffuseSize;
	vec2 CloudSize;
	vec2 BloomSize;
	vec2 ImpactSize;
};

in vec2 texCoord;
out vec4 fragColor;

// How much of the finished bloom buffer reaches the screen.
const float BLOOM_STRENGTH = 1.0;

// The radial blur. Zoom-style - each tap walks a fraction of the way from the
// fragment toward ground zero - so a spot's streak is proportional to how far
// out it sits, which is what makes the lines read as radiating rather than as a
// uniform smear.
const int   IMPACT_TAPS     = 48;
const float IMPACT_BLUR_MIN = 0.12; // fraction of the way to the centre, at t=0
const float IMPACT_BLUR_MAX = 0.88; // and at the end of the sequence

// Where the smeared coverage crushes to ink. A hard step, not a ramp: the frame
// is two colours and nothing between, so anti-aliasing the streak edges here
// would put exactly the grey fringe the look is defined by not having. Low,
// because the blur is averaging a mostly empty buffer along each line.
const float IMPACT_INK_T = 0.050;

void main() {
	vec3 bg = texture(DiffuseSampler, texCoord).rgb;

	// CloudSampler is PREMULTIPLIED: post/nuclear/bomb_block/detonation/raymarch.fsh already
	// composited front-to-back, so rgb must be added, not lerped.
	vec4 cloud = texture(CloudSampler, texCoord);
	float cloudA = clamp(cloud.a, 0.0, 1.0);
	vec3 color = bg * (1.0 - cloudA) + cloud.rgb;

	// Bloom goes over the smoke: a core spilling light into the air lights the
	// smoke in front of it too.
	color += texture(BloomSampler, texCoord).rgb * BLOOM_STRENGTH;

	// --- impact frames -----------------------------------------------------
	// Read straight off the same age slot the raymarch uses, so the frames and
	// the fireball behind them can never drift apart. The pick is shared with
	// the edge pass, so the disc it bounded and the centre blurred around here
	// are the same point by construction.
	ImpactPick pick = pickImpact();

	if (pick.weight * impactFrames(pick.age) > 0.002) {
		float len = mix(IMPACT_BLUR_MIN, IMPACT_BLUR_MAX, impactStretch(pick.age));

		// Interleaved gradient noise on the tap parameter. Without it the taps
		// land on visibly separate copies of the outline once the blur is long -
		// 48 taps over 88% of the screen is a 1.8% step - and the threshold turns
		// that into a ladder. Dithered, it turns into grain along the streak.
		float jitter = fract(52.9829189
				* fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));

		float streak = 0.0;
		for (int i = 0; i < IMPACT_TAPS; i++) {
			float s = (float(i) + jitter) / float(IMPACT_TAPS);
			streak += texture(ImpactSampler, mix(texCoord, pick.centre, s * len)).r;
		}
		streak /= float(IMPACT_TAPS);

		float ink = step(IMPACT_INK_T, streak);
		// Polarity flips the roles of ground and ink wholesale, so the streaks
		// survive the inversion instead of the screen simply strobing.
		float v = mix(1.0 - ink, ink, impactPolarity(pick.age));

		// Inside the disc the frame simply inverts - it is a region, not a drawn
		// shape, so it needs no colour of its own: white on the black frames and
		// black on the white one, always exactly opposite its surroundings. XOR
		// rather than an overwrite, so the streaks continue through it as their
		// own negative instead of the disc punching a flat hole in them.
		//
		// The boundary itself is never drawn. It is warped by impactDiscField
		// so it is not a clean arc, and the last fraction of a radius is
		// dithered with the same jitter the blur taps use - which in a two-
		// colour image is the only way to soften an edge without introducing
		// the grey fringe the whole look is defined by not having.
		float df = impactDiscField(impactOffset(texCoord, pick.centre, OutSize),
				pick.radius, time);
		float disc = step(df + (jitter - 0.5) * IMPACT_DISC_SOFT, 0.0);
		v = abs(v - disc);

		color = vec3(v);
	}

	// The frames cut to this rather than back to the scene: a plain white
	// screen, held and then fading, which is the detonation flash itself. It is
	// applied outside the branch above because it outlives the frames - by the
	// time it is fading, the edge buffer has already been abandoned.
	color = mix(color, vec3(1.0), pick.weight * impactFlash(pick.age));

	fragColor = vec4(color, 1.0);
}
