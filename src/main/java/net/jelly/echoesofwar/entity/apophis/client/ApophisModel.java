// Made with Blockbench 5.0.0
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

package net.jelly.echoesofwar.entity.apophis.client;

import net.jelly.echoesofwar.EchoesofWar;
import net.jelly.echoesofwar.entity.apophis.ApophisEntity;
import net.jelly.marionette_lib.utility.MarionetteModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;

// segments are head-first in Blockbench (segment1 = head, segment4+ = repeats of the
// standard body segment down to the tail), but ApophisEntity's chain runs tail-first, so
// segmentNames() reverses the order to match
public class ApophisModel extends MarionetteModel<ApophisRenderState> {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(Identifier.fromNamespaceAndPath(EchoesofWar.MODID, "apophis"), "main");

	public ApophisModel(ModelPart root) {
		super(root, segmentNames());
	}

	private static String[] segmentNames() {
		int count = ApophisEntity.SEGMENT_COUNT;
		String[] names = new String[count];
		for (int i = 0; i < count; i++) names[i] = "segment" + (count - i);
		return names;
	}

	// per-segment scale (not global scale)
	@Override
	public void setupAnim(ApophisRenderState state) {
		super.setupAnim(state);
		float[] scales = state.partScales;
		for (int i = 0; i < allSegments.length; i++) {
			float scale = i < scales.length ? scales[i] : 1.0f;
			allSegments[i].xScale = scale;
			allSegments[i].yScale = scale;
			allSegments[i].zScale = scale;
		}
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition segment1 = partdefinition.addOrReplaceChild("segment1", CubeListBuilder.create().texOffs(120, 16).addBox(-7.9058F, -8.8041F, 5.3038F, 16.0F, 3.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition cube_r1 = segment1.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(110, 114).addBox(-17.275F, -3.5F, -4.95F, 17.0F, 7.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(11.2063F, -3.3F, 2.9242F, -0.0868F, 1.1326F, -0.1021F));

		PartDefinition cube_r2 = segment1.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(112, 66).addBox(0.275F, -3.5F, -4.95F, 17.0F, 7.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-11.2063F, -3.3F, 2.9242F, -0.0868F, -1.1326F, 0.1021F));

		PartDefinition cube_r3 = segment1.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(0, 96).addBox(-7.0F, -4.0F, -2.5F, 14.0F, 5.0F, 15.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0942F, 5.3292F, 8.1038F, 0.6109F, 0.0F, 0.0F));

		PartDefinition cube_r4 = segment1.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(116, 83).addBox(-8.0F, -1.0F, -4.5F, 16.0F, 2.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0942F, -7.0042F, 16.5038F, -0.1745F, 0.0F, 0.0F));

		PartDefinition cube_r5 = segment1.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(0, 116).addBox(-8.0F, -1.0F, -4.5F, 16.0F, 2.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0942F, -7.0042F, 1.2282F, 0.1745F, 0.0F, 0.0F));

		PartDefinition cube_r6 = segment1.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(120, 0).addBox(-6.0F, -5.5F, -2.5F, 12.0F, 7.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.3982F, 4.6F, 11.9927F, 0.5672F, -1.309F, 0.0F));

		PartDefinition cube_r7 = segment1.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(116, 94).addBox(-6.0F, -5.5F, -2.5F, 12.0F, 7.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.3982F, 4.6F, 11.9927F, 0.5672F, 1.309F, 0.0F));

		PartDefinition top_jaw = segment1.addOrReplaceChild("top_jaw", CubeListBuilder.create(), PartPose.offset(0.0F, -2.9F, -11.134F));

		PartDefinition cube_r8 = top_jaw.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(0, 0).addBox(-19.9F, -4.0F, -10.9F, 17.0F, 7.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 1.309F, 0.0F));

		PartDefinition cube_r9 = top_jaw.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(58, 114).addBox(-8.0F, -4.0F, -5.0F, 13.0F, 6.0F, 13.0F, new CubeDeformation(0.0F))
				.texOffs(110, 131).mirror().addBox(5.0F, -3.0F, -6.0F, 3.0F, 5.0F, 14.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.2F, 0.0F, 0.0617F, 0.7844F, 0.0436F));

		PartDefinition cube_r10 = top_jaw.addOrReplaceChild("cube_r10", CubeListBuilder.create().texOffs(100, 168).mirror().addBox(7.0F, 0.0F, -6.0F, 0.0F, 7.0F, 8.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 2.2F, 0.0F, 0.0617F, 0.7844F, 0.0436F));

		PartDefinition cube_r11 = top_jaw.addOrReplaceChild("cube_r11", CubeListBuilder.create().texOffs(100, 168).addBox(-7.0F, 0.0F, -6.0F, 0.0F, 7.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 2.2F, 0.0F, 0.0617F, -0.7844F, -0.0436F));

		PartDefinition cube_r12 = top_jaw.addOrReplaceChild("cube_r12", CubeListBuilder.create().texOffs(112, 150).mirror().addBox(0.0F, -2.0F, -7.0F, 0.0F, 4.0F, 14.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(6.1518F, 4.4047F, -4.6459F, -3.0799F, 0.7844F, 0.0436F));

		PartDefinition cube_r13 = top_jaw.addOrReplaceChild("cube_r13", CubeListBuilder.create().texOffs(112, 150).addBox(0.0F, -2.0F, -7.0F, 0.0F, 4.0F, 14.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.1518F, 4.4047F, -4.6459F, -3.0799F, -0.7844F, -0.0436F));

		PartDefinition cube_r14 = top_jaw.addOrReplaceChild("cube_r14", CubeListBuilder.create().texOffs(110, 131).addBox(-8.0F, -3.0F, -6.0F, 3.0F, 5.0F, 14.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.2F, 0.0F, 0.0617F, -0.7844F, -0.0436F));

		PartDefinition cube_r15 = top_jaw.addOrReplaceChild("cube_r15", CubeListBuilder.create().texOffs(50, 116).addBox(-1.5F, 2.5F, -1.5F, 3.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(98, 133).addBox(-1.5F, -2.5F, -1.5F, 3.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.1014F, -8.5054F, 0.0438F, -0.0872F, -0.0038F));

		PartDefinition cube_r16 = top_jaw.addOrReplaceChild("cube_r16", CubeListBuilder.create().texOffs(66, 133).addBox(0.0F, -0.9F, -6.0F, 2.0F, 3.0F, 14.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.5813F, -3.0832F, -5.1369F, 0.2169F, -0.9578F, -0.0753F));

		PartDefinition cube_r17 = top_jaw.addOrReplaceChild("cube_r17", CubeListBuilder.create().texOffs(34, 133).addBox(-2.0F, -0.9F, -6.0F, 2.0F, 3.0F, 14.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.5813F, -3.0832F, -5.1369F, 0.2169F, 0.9578F, 0.0753F));

		PartDefinition cube_r18 = top_jaw.addOrReplaceChild("cube_r18", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(2.9F, -4.0F, -10.9F, 17.0F, 7.0F, 16.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.309F, 0.0F));

		PartDefinition bot_jaw = segment1.addOrReplaceChild("bot_jaw", CubeListBuilder.create(), PartPose.offset(0.0F, -2.9F, -11.134F));

		PartDefinition cube_r19 = bot_jaw.addOrReplaceChild("cube_r19", CubeListBuilder.create().texOffs(116, 110).addBox(-7.5F, -1.0F, 0.0F, 15.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.3033F, 7.3424F, -3.3595F, -0.4754F, -0.7268F, -2.812F));

		PartDefinition cube_r20 = bot_jaw.addOrReplaceChild("cube_r20", CubeListBuilder.create().texOffs(116, 110).mirror().addBox(-7.5F, -1.0F, 0.0F, 15.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(5.3033F, 7.3424F, -3.3595F, -0.4754F, 0.7268F, 2.812F));

		PartDefinition cube_r21 = bot_jaw.addOrReplaceChild("cube_r21", CubeListBuilder.create().texOffs(58, 46).addBox(-8.0F, -4.0F, -12.5F, 16.0F, 5.0F, 15.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0942F, 9.1042F, 15.3622F, -0.1745F, 0.0F, 0.0F));

		PartDefinition cube_r22 = bot_jaw.addOrReplaceChild("cube_r22", CubeListBuilder.create().texOffs(0, 75).mirror().addBox(3.9F, 2.0F, -9.9F, 14.0F, 6.0F, 15.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.309F, 0.0F));

		PartDefinition cube_r23 = bot_jaw.addOrReplaceChild("cube_r23", CubeListBuilder.create().texOffs(66, 27).addBox(-4.4645F, 0.0F, -10.5355F, 15.0F, 2.0F, 15.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 5.0F, 6.0F, 0.4754F, 0.7268F, 0.3295F));

		PartDefinition cube_r24 = bot_jaw.addOrReplaceChild("cube_r24", CubeListBuilder.create().texOffs(0, 75).addBox(-17.9F, 2.0F, -9.9F, 14.0F, 6.0F, 15.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 1.309F, 0.0F));

		PartDefinition bone5 = segment1.addOrReplaceChild("bone5", CubeListBuilder.create(), PartPose.offset(-7.2F, -10.4F, 5.866F));

		PartDefinition bone6 = bone5.addOrReplaceChild("bone6", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.8727F));

		PartDefinition cube_r25 = bone6.addOrReplaceChild("cube_r25", CubeListBuilder.create().texOffs(167, 185).addBox(-0.8047F, -3.4063F, -0.5399F, 16.0F, 7.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-13.0063F, 7.9F, -2.9418F, 3.0546F, -1.1334F, -3.0862F));

		PartDefinition bone7 = bone5.addOrReplaceChild("bone7", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 19.0F, 0.0F, 0.0F, 0.0F, 0.8727F));

		PartDefinition cube_r26 = bone7.addOrReplaceChild("cube_r26", CubeListBuilder.create().texOffs(167, 174).addBox(-0.8047F, -3.5938F, -0.5399F, 16.0F, 7.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-13.0063F, -7.9F, -2.9418F, -3.0546F, -1.1334F, 3.0862F));

		PartDefinition bone8 = bone5.addOrReplaceChild("bone8", CubeListBuilder.create(), PartPose.offset(7.2F, 14.0F, -3.0F));

		PartDefinition cube_r27 = bone8.addOrReplaceChild("cube_r27", CubeListBuilder.create().texOffs(167, 166).addBox(2.1952F, -3.5938F, -0.5399F, 15.0F, 7.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-14.6063F, -6.9F, -0.6418F, 2.9502F, -1.1265F, -2.8896F));

		PartDefinition bone2 = segment1.addOrReplaceChild("bone2", CubeListBuilder.create(), PartPose.offset(7.2F, -10.4F, 5.866F));

		PartDefinition bone3 = bone2.addOrReplaceChild("bone3", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.8727F));

		PartDefinition cube_r28 = bone3.addOrReplaceChild("cube_r28", CubeListBuilder.create().texOffs(167, 185).mirror().addBox(-15.1953F, -3.4063F, -0.5399F, 16.0F, 7.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(13.0063F, 7.9F, -2.9418F, 3.0546F, 1.1334F, 3.0862F));

		PartDefinition bone4 = bone2.addOrReplaceChild("bone4", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 19.0F, 0.0F, 0.0F, 0.0F, -0.8727F));

		PartDefinition cube_r29 = bone4.addOrReplaceChild("cube_r29", CubeListBuilder.create().texOffs(167, 174).mirror().addBox(-15.1953F, -3.5938F, -0.5399F, 16.0F, 7.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(13.0063F, -7.9F, -2.9418F, -3.0546F, 1.1334F, -3.0862F));

		PartDefinition bone9 = bone2.addOrReplaceChild("bone9", CubeListBuilder.create(), PartPose.offset(-7.2F, 14.0F, -3.0F));

		PartDefinition cube_r30 = bone9.addOrReplaceChild("cube_r30", CubeListBuilder.create().texOffs(167, 166).mirror().addBox(-17.1953F, -3.5938F, -0.5399F, 15.0F, 7.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(14.6063F, -6.9F, -0.6418F, 2.9502F, 1.1265F, 2.8896F));

		PartDefinition segment2 = partdefinition.addOrReplaceChild("segment2", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition cube_r31 = segment2.addOrReplaceChild("cube_r31", CubeListBuilder.create().texOffs(56, 150).addBox(-5.341F, -6.73F, -7.82F, 4.0F, 10.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0364F, 2.6399F, -1.3695F, 0.0F, -0.1309F, -0.7854F));

		PartDefinition cube_r32 = segment2.addOrReplaceChild("cube_r32", CubeListBuilder.create().texOffs(28, 150).addBox(-5.341F, -6.73F, -7.82F, 4.0F, 10.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0364F, 2.6399F, 7.7865F, 0.0F, -0.1309F, -0.7854F));

		PartDefinition cube_r33 = segment2.addOrReplaceChild("cube_r33", CubeListBuilder.create().texOffs(144, 131).addBox(1.341F, -6.73F, -7.82F, 4.0F, 10.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.0364F, 2.6399F, -1.3695F, 0.0F, 0.1309F, 0.7854F));

		PartDefinition cube_r34 = segment2.addOrReplaceChild("cube_r34", CubeListBuilder.create().texOffs(0, 146).addBox(1.341F, -6.73F, -7.82F, 4.0F, 10.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.0364F, 2.6399F, 7.7865F, 0.0F, 0.1309F, 0.7854F));

		PartDefinition cube_r35 = segment2.addOrReplaceChild("cube_r35", CubeListBuilder.create().texOffs(0, 46).addBox(-4.37F, -7.64F, -8.28F, 12.0F, 12.0F, 17.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0308F, 2.6399F, -0.9335F, 0.0F, 0.0F, -0.7854F));

		PartDefinition segment3 = partdefinition.addOrReplaceChild("segment3", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition cube_r36 = segment3.addOrReplaceChild("cube_r36", CubeListBuilder.create().texOffs(126, 26).addBox(-7.0F, -1.0F, 0.5F, 7.0F, 2.0F, 13.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(8.1058F, -5.2096F, -10.2575F, 0.0472F, -0.3923F, -0.0181F));

		PartDefinition cube_r37 = segment3.addOrReplaceChild("cube_r37", CubeListBuilder.create().texOffs(120, 44).addBox(0.0F, -1.0F, 0.5F, 7.0F, 2.0F, 13.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-8.1058F, -5.2096F, -10.2575F, 0.0472F, 0.3923F, 0.0181F));

		PartDefinition cube_r38 = segment3.addOrReplaceChild("cube_r38", CubeListBuilder.create().texOffs(164, 112).addBox(-4.9F, -6.0F, -7.0F, 4.0F, 9.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0308F, 2.1F, 7.1782F, 0.0F, -0.1309F, -0.7854F));

		PartDefinition cube_r39 = segment3.addOrReplaceChild("cube_r39", CubeListBuilder.create().texOffs(160, 41).addBox(0.9F, -6.0F, -7.0F, 4.0F, 9.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.0308F, 2.1F, -1.2218F, 0.0F, 0.1309F, 0.7854F));

		PartDefinition cube_r40 = segment3.addOrReplaceChild("cube_r40", CubeListBuilder.create().texOffs(158, 94).addBox(0.9F, -6.0F, -7.0F, 4.0F, 9.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.0308F, 2.1F, 7.1782F, 0.0F, 0.1309F, 0.7854F));

		PartDefinition cube_r41 = segment3.addOrReplaceChild("cube_r41", CubeListBuilder.create().texOffs(140, 151).addBox(-4.9F, -6.0F, -7.0F, 4.0F, 9.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0308F, 2.1F, -1.2218F, 0.0F, -0.1309F, -0.7854F));

		PartDefinition cube_r42 = segment3.addOrReplaceChild("cube_r42", CubeListBuilder.create().texOffs(66, 0).addBox(-4.0F, -7.0F, -8.0F, 11.0F, 11.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0308F, 2.1F, -0.8218F, 0.0F, 0.0F, -0.7854F));

		// segment4 is the standard body segment; the rest of the body (BODY_SEGMENT_COUNT copies in
		// total, tail included) is built by repeating it under fresh names (5, 6...)
		addBodySegment(partdefinition, "segment4");
		for (int i = ApophisEntity.HEAD_SEGMENT_COUNT + 2; i <= ApophisEntity.SEGMENT_COUNT; i++) {
			addBodySegment(partdefinition, "segment" + i);
		}

		return LayerDefinition.create(meshdefinition, 256, 256);
	}

	/** builds one independent copy of the standard body segment (segment4's geometry) named {@code name}. */
	private static PartDefinition addBodySegment(PartDefinition parent, String name) {
		PartDefinition segment = parent.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.offset(0.0308F, 20.6296F, -4.6068F));

		PartDefinition cube_r43 = segment.addOrReplaceChild("cube_r43", CubeListBuilder.create().texOffs(166, 59).addBox(-4.9F, -6.0F, -7.0F, 4.0F, 9.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0308F, 2.1F, 6.6504F, 0.0F, -0.1309F, -0.7854F));

		PartDefinition cube_r44 = segment.addOrReplaceChild("cube_r44", CubeListBuilder.create().texOffs(166, 18).addBox(0.9F, -6.0F, -7.0F, 4.0F, 9.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.0308F, 2.1F, -1.7496F, 0.0F, 0.1309F, 0.7854F));

		PartDefinition cube_r45 = segment.addOrReplaceChild("cube_r45", CubeListBuilder.create().texOffs(166, 0).addBox(0.9F, -6.0F, -7.0F, 4.0F, 9.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.0308F, 2.1F, 6.6504F, 0.0F, 0.1309F, 0.7854F));

		PartDefinition cube_r46 = segment.addOrReplaceChild("cube_r46", CubeListBuilder.create().texOffs(0, 166).addBox(-4.9F, -6.0F, -7.0F, 4.0F, 9.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0308F, 2.1F, -1.7496F, 0.0F, -0.1309F, -0.7854F));

		PartDefinition cube_r47 = segment.addOrReplaceChild("cube_r47", CubeListBuilder.create().texOffs(58, 66).addBox(-4.0F, -7.0F, -8.0F, 11.0F, 11.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0308F, 2.1F, -1.3496F, 0.0F, 0.0F, -0.7854F));

		return segment;
	}
}