package net.jelly.echoesofwar.entity.talos.client;

import net.jelly.echoesofwar.EchoesofWar;
import net.jelly.echoesofwar.entity.talos.TalosEntity;
import net.jelly.marionette_lib.utility.MarionetteModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;

/**
 * Positioned every frame by MarionetteModel#setupAnim from the corresponding part's direction/offset.
 * The segment names below must match the order TalosEntity builds its limbs in: spine (head, shoulders,
 * torso, hips, legs), then left arm (tricep, forearm, hand), then right arm (tricep, forearm, hand).
 */
public class TalosModel extends MarionetteModel<TalosRenderState> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(Identifier.fromNamespaceAndPath(EchoesofWar.MODID, "talos"), "main");

	public TalosModel(ModelPart root) {
		super(root, new String[] {
				"head", "shoulders", "torso", "hips", "legs",
				"left_tricep", "left_forearm", "left_hand",
				"right_tricep", "right_forearm", "right_hand"
		});
	}

	/** scales every segment's own geometry in place, on top of the positioning the base class already does */
	@Override
	public void setupAnim(TalosRenderState state) {
		super.setupAnim(state);
		float scale = TalosEntity.sizeScale();
		for (ModelPart segment : allSegments) {
			segment.xScale = scale;
			segment.yScale = scale;
			segment.zScale = scale;
		}
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(146, 41).addBox(-11.2423F, -18.8765F, -10.6506F, 21.3013F, 2.3668F, 21.3013F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 30.0F, 0.0F));

		PartDefinition cube_r1 = head.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(300, 134).addBox(-11.2423F, -1.4793F, -2.071F, 21.3013F, 2.3668F, 4.7336F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -11.8944F, -13.4908F, 1.5708F, 0.0F, 0.0F));

		PartDefinition cube_r2 = head.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(246, 312).addBox(-1.4F, -1.4793F, -12.3373F, 3.0F, 2.3668F, 15.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -11.8944F, -13.3908F, 1.5708F, 0.0F, 0.0F));

		PartDefinition cube_r3 = head.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(296, 68).addBox(-11.2423F, -1.3043F, -3.0459F, 21.3013F, 2.3668F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -15.7997F, -11.8932F, 0.7723F, 0.0F, 0.0F));

		PartDefinition bone15 = head.addOrReplaceChild("bone15", CubeListBuilder.create().texOffs(302, 0).addBox(0.6506F, -1.1834F, -6.6506F, 10.0F, 2.3668F, 14.0F, new CubeDeformation(0.0F))
				.texOffs(300, 86).addBox(-2.3494F, -3.1834F, -8.6506F, 4.0F, 6.3668F, 18.0F, new CubeDeformation(0.0F))
				.texOffs(236, 144).addBox(1.6506F, -1.1834F, -10.6506F, 9.0F, 2.3668F, 4.0F, new CubeDeformation(0.0F))
				.texOffs(70, 347).addBox(1.6506F, -1.1834F, 6.6506F, 9.0F, 2.3668F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.5917F, -29.6931F, 0.0F, 0.0F, 0.0F, 1.5708F));

		PartDefinition cube_r4 = bone15.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(208, 346).addBox(-1.1259F, -2.5917F, 0.1519F, 4.0F, 6.3668F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.1166F, -0.5917F, 16.725F, 0.0F, 0.6981F, 0.0F));

		PartDefinition cube_r5 = bone15.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(342, 144).addBox(-1.416F, -2.5917F, 0.4756F, 4.0F, 6.3668F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.1834F, -0.5917F, 8.0F, 0.0F, 0.3927F, 0.0F));

		PartDefinition cube_r6 = bone15.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(346, 234).addBox(-8.166F, -0.5917F, 0.6506F, 11.0F, 2.3668F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(10.8166F, -0.5917F, 10.0F, 0.0F, 0.6545F, 0.0F));

		PartDefinition cube_r7 = bone15.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(232, 346).addBox(-8.166F, -0.5917F, -1.3494F, 8.0F, 2.3668F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(10.8166F, -0.5917F, 10.0F, 0.0F, 0.0873F, 0.0F));

		PartDefinition cube_r8 = bone15.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(312, 350).addBox(-8.166F, -0.5917F, 0.6506F, 8.0F, 2.3668F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(10.8166F, -0.5917F, 10.0F, 0.0F, 0.3491F, 0.0F));

		PartDefinition cube_r9 = bone15.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(344, 100).addBox(-8.166F, -0.5917F, -4.6506F, 8.0F, 2.3668F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(10.8166F, -0.5917F, -10.0F, 0.0F, -0.0873F, 0.0F));

		PartDefinition cube_r10 = bone15.addOrReplaceChild("cube_r10", CubeListBuilder.create().texOffs(350, 68).addBox(-8.166F, -0.5917F, -4.6506F, 8.0F, 2.3668F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(10.8166F, -0.5917F, -10.0F, 0.0F, -0.3491F, 0.0F));

		PartDefinition cube_r11 = bone15.addOrReplaceChild("cube_r11", CubeListBuilder.create().texOffs(348, 280).addBox(-8.166F, -0.5917F, -4.6506F, 8.0F, 2.3668F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(10.8166F, -0.5917F, -10.0F, 0.0F, -0.6545F, 0.0F));

		PartDefinition cube_r12 = bone15.addOrReplaceChild("cube_r12", CubeListBuilder.create().texOffs(184, 346).addBox(-1.1259F, -2.5917F, -8.1519F, 4.0F, 6.3668F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.1166F, -0.5917F, -16.725F, 0.0F, -0.6981F, 0.0F));

		PartDefinition cube_r13 = bone15.addOrReplaceChild("cube_r13", CubeListBuilder.create().texOffs(342, 110).addBox(-1.416F, -2.5917F, -9.4756F, 4.0F, 6.3668F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.1834F, -0.5917F, -8.0F, 0.0F, -0.3927F, 0.0F));

		PartDefinition skull = head.addOrReplaceChild("skull", CubeListBuilder.create().texOffs(114, 150).addBox(-10.6506F, -24.8515F, -10.6506F, 21.3013F, 25.0F, 21.3013F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 7.492F, 0.0F));

		PartDefinition bone = skull.addOrReplaceChild("bone", CubeListBuilder.create(), PartPose.offset(5.7646F, 43.9131F, 0.4729F));

		PartDefinition bone3 = head.addOrReplaceChild("bone3", CubeListBuilder.create().texOffs(300, 110).addBox(0.3026F, -10.0589F, 1.8343F, 2.3668F, 4.7336F, 18.9345F, new CubeDeformation(0.0F)), PartPose.offset(-10.8907F, -2.6639F, -11.8932F));

		PartDefinition cube_r14 = bone3.addOrReplaceChild("cube_r14", CubeListBuilder.create().texOffs(90, 298).addBox(-2.3018F, -7.1004F, -3.4852F, 2.3668F, 7.0F, 4.7336F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(23.7814F, -10.0F, 0.0F, -0.5617F, 0.5818F, -0.853F));

		PartDefinition cube_r15 = bone3.addOrReplaceChild("cube_r15", CubeListBuilder.create().texOffs(200, 289).addBox(-2.1126F, -6.8254F, -10.1461F, 2.3668F, 7.0F, 21.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(10.8762F, -10.0F, 25.384F, 1.5708F, -1.0908F, -1.5708F));

		PartDefinition cube_r16 = bone3.addOrReplaceChild("cube_r16", CubeListBuilder.create().texOffs(256, 245).addBox(-2.1126F, -0.1004F, -3.1461F, 2.3668F, 23.0F, 21.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(17.8762F, -10.0F, 25.384F, -1.5708F, -1.5272F, 1.5708F));

		PartDefinition cube_r17 = bone3.addOrReplaceChild("cube_r17", CubeListBuilder.create().texOffs(152, 289).addBox(-1.3419F, -6.9446F, -10.6506F, 3.0F, 7.0F, 21.3013F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.6684F, -9.6004F, 11.4932F, 0.0F, 0.0F, 0.5236F));

		PartDefinition cube_r18 = bone3.addOrReplaceChild("cube_r18", CubeListBuilder.create().texOffs(62, 168).addBox(-1.6581F, -6.9446F, -10.6506F, 3.0F, 7.0F, 21.3013F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(23.4498F, -9.6004F, 11.4932F, 0.0F, 0.0F, -0.5236F));

		PartDefinition bone2 = head.addOrReplaceChild("bone2", CubeListBuilder.create().texOffs(164, 245).addBox(-2.515F, -1.1004F, -11.9438F, 2.3668F, 23.0F, 21.3013F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(13.8907F, -11.6639F, 0.8932F, 0.0F, 0.0F, 0.0436F));

		PartDefinition cube_r19 = bone2.addOrReplaceChild("cube_r19", CubeListBuilder.create().texOffs(342, 171).addBox(-0.8876F, -10.1004F, -2.0709F, 2.3668F, 23.0F, 4.7336F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, 9.0F, -12.7864F, 0.0F, 0.7854F, 0.0F));

		PartDefinition cube_r20 = bone2.addOrReplaceChild("cube_r20", CubeListBuilder.create().texOffs(352, 350).addBox(-1.4876F, -2.3668F, 0.5122F, 2.3668F, 15.0F, 3.0F, new CubeDeformation(0.0F))
				.texOffs(72, 353).addBox(-1.4876F, -2.3668F, -3.4878F, 2.3668F, 11.834F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.9052F, 9.0F, -14.384F, 0.0F, 1.5708F, 0.0F));

		PartDefinition cube_r21 = bone2.addOrReplaceChild("cube_r21", CubeListBuilder.create().texOffs(350, 0).addBox(-1.0834F, -1.875F, -3.8F, 2.3668F, 3.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.893F, 20.1332F, -14.0799F, 0.0F, 1.5708F, 0.7418F));

		PartDefinition cube_r22 = bone2.addOrReplaceChild("cube_r22", CubeListBuilder.create().texOffs(102, 344).addBox(-1.4626F, -9.1004F, -2.921F, 2.3668F, 23.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, 9.0F, 11.0F, 0.0F, -0.7854F, 0.0F));

		PartDefinition bone10 = head.addOrReplaceChild("bone10", CubeListBuilder.create().texOffs(210, 245).addBox(0.1482F, -1.1004F, -11.9438F, 2.3668F, 23.0F, 21.3013F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-13.8907F, -11.6639F, 0.8932F, 0.0F, 0.0F, -0.0436F));

		PartDefinition cube_r23 = bone10.addOrReplaceChild("cube_r23", CubeListBuilder.create().texOffs(278, 342).addBox(-1.4792F, -10.1004F, -2.0709F, 2.3668F, 23.0F, 4.7336F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.0F, 9.0F, -12.7864F, 0.0F, -0.7854F, 0.0F));

		PartDefinition cube_r24 = bone10.addOrReplaceChild("cube_r24", CubeListBuilder.create().texOffs(62, 353).addBox(-0.8792F, -2.3668F, 0.5122F, 2.3668F, 15.0F, 3.0F, new CubeDeformation(0.0F))
				.texOffs(84, 353).addBox(-0.8792F, -2.3668F, -3.4878F, 2.3668F, 11.834F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.9052F, 9.0F, -14.384F, 0.0F, -1.5708F, 0.0F));

		PartDefinition cube_r25 = bone10.addOrReplaceChild("cube_r25", CubeListBuilder.create().texOffs(292, 350).addBox(-1.2834F, -1.875F, -3.8F, 2.3668F, 3.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.893F, 20.1332F, -14.0799F, 0.0F, -1.5708F, -0.7418F));

		PartDefinition cube_r26 = bone10.addOrReplaceChild("cube_r26", CubeListBuilder.create().texOffs(114, 344).addBox(-0.9042F, -9.1004F, -2.921F, 2.3668F, 23.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.0F, 9.0F, 11.0F, 0.0F, 0.7854F, 0.0F));

		PartDefinition shoulders = partdefinition.addOrReplaceChild("shoulders", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition bone11 = shoulders.addOrReplaceChild("bone11", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -1.5708F, 0.0F, 3.1416F));

		PartDefinition cube_r27 = bone11.addOrReplaceChild("cube_r27", CubeListBuilder.create().texOffs(260, 227).addBox(-12.397F, 3.3277F, 13.3284F, 28.0F, 3.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.1474F, 0.4F, 0.0F, 1.5708F, 0.0F));

		PartDefinition cube_r28 = bone11.addOrReplaceChild("cube_r28", CubeListBuilder.create().texOffs(246, 29).addBox(-15.603F, 3.3277F, 13.3284F, 28.0F, 3.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.1474F, 0.4F, 0.0F, -1.5708F, 0.0F));

		PartDefinition cube_r29 = bone11.addOrReplaceChild("cube_r29", CubeListBuilder.create().texOffs(0, 0).addBox(-14.603F, -13.397F, -22.3284F, 28.0F, 20.7247F, 45.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 2.1473F, 0.0F, 0.0F, -1.5708F, 0.0F));

		PartDefinition cube_r30 = bone11.addOrReplaceChild("cube_r30", CubeListBuilder.create().texOffs(146, 0).addBox(-11.1642F, -10.7672F, -13.7194F, 22.3284F, 13.0F, 28.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-25.6776F, -6.784F, 0.0F, 0.0F, -1.5708F, 0.2618F));

		PartDefinition cube_r31 = bone11.addOrReplaceChild("cube_r31", CubeListBuilder.create().texOffs(134, 66).addBox(-11.1642F, -10.7672F, -13.7194F, 22.3284F, 13.0F, 28.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(25.6776F, -6.784F, 0.0F, 0.0F, 1.5708F, -0.2618F));

		PartDefinition torso = partdefinition.addOrReplaceChild("torso", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition bone25 = torso.addOrReplaceChild("bone25", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.1416F));

		PartDefinition t = bone25.addOrReplaceChild("t", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0556F, -8.4995F, 1.9247F, -1.5708F, 0.0F, 0.0F));

		PartDefinition cube_r32 = t.addOrReplaceChild("cube_r32", CubeListBuilder.create().texOffs(158, 341).addBox(12.1182F, -0.4772F, -11.4919F, 1.0F, 5.0F, 12.0F, new CubeDeformation(0.0F))
				.texOffs(344, 85).addBox(12.1182F, 5.5228F, -10.4919F, 1.0F, 4.0F, 11.0F, new CubeDeformation(0.0F))
				.texOffs(346, 302).addBox(12.1182F, -5.4772F, -10.4919F, 1.0F, 4.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.0556F, -1.0652F, 8.4995F, 0.0F, 1.5708F, 0.0F));

		PartDefinition cube_r33 = t.addOrReplaceChild("cube_r33", CubeListBuilder.create().texOffs(52, 234).addBox(-11.1068F, 0.3478F, -0.4419F, 23.0F, 27.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-22.0556F, -12.0652F, 8.4995F, 0.0F, 1.5708F, -0.5236F));

		PartDefinition cube_r34 = t.addOrReplaceChild("cube_r34", CubeListBuilder.create().texOffs(234, 41).addBox(-11.8932F, 0.3478F, -0.4419F, 23.0F, 27.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(21.9444F, -12.0652F, 8.4995F, 0.0F, -1.5708F, 0.5236F));

		PartDefinition cube_r35 = t.addOrReplaceChild("cube_r35", CubeListBuilder.create().texOffs(346, 254).addBox(-13.1182F, -5.4772F, -10.4919F, 1.0F, 4.0F, 10.0F, new CubeDeformation(0.0F))
				.texOffs(132, 341).addBox(-13.1182F, -0.4772F, -11.4919F, 1.0F, 5.0F, 12.0F, new CubeDeformation(0.0F))
				.texOffs(346, 240).addBox(-13.1182F, 5.5228F, -10.4919F, 1.0F, 4.0F, 10.0F, new CubeDeformation(0.0F))
				.texOffs(0, 108).addBox(-12.1182F, -14.4772F, -16.4919F, 24.2364F, 27.0F, 32.9837F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.0556F, -1.0652F, 8.4995F, 0.0F, -1.5708F, 0.0F));

		PartDefinition legs = partdefinition.addOrReplaceChild("legs", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition bone12 = legs.addOrReplaceChild("bone12", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.0F, -3.0F, -1.5708F, 0.0F, 3.1416F));

		PartDefinition leg = bone12.addOrReplaceChild("leg", CubeListBuilder.create(), PartPose.offset(5.7646F, 26.0962F, 0.4729F));

		PartDefinition cube_r36 = leg.addOrReplaceChild("cube_r36", CubeListBuilder.create().texOffs(206, 198).addBox(-8.2562F, -34.5F, -1.9561F, 16.5124F, 32.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-15.3046F, -7.0052F, -0.4729F, 0.0F, -1.5708F, -0.0436F));

		PartDefinition cube_r37 = leg.addOrReplaceChild("cube_r37", CubeListBuilder.create().texOffs(114, 234).addBox(-9.2562F, -2.637F, 8.4961F, 18.5124F, 34.0F, 6.0F, new CubeDeformation(0.0F))
				.texOffs(0, 224).addBox(-9.2562F, -2.637F, 2.4961F, 18.5124F, 36.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.7646F, -39.8681F, -0.4729F, 0.0F, -1.5708F, 0.0F));

		PartDefinition leg2 = bone12.addOrReplaceChild("leg2", CubeListBuilder.create(), PartPose.offset(5.7646F, 26.0962F, 0.4729F));

		PartDefinition cube_r38 = leg2.addOrReplaceChild("cube_r38", CubeListBuilder.create().texOffs(262, 173).addBox(-8.2562F, 0.3025F, -7.8186F, 16.5124F, 15.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(10.7754F, -10.0052F, -0.4729F, 0.0F, 1.5708F, 0.0436F));

		PartDefinition cube_r39 = leg2.addOrReplaceChild("cube_r39", CubeListBuilder.create().texOffs(206, 150).addBox(-8.2562F, -34.5F, -1.9561F, 16.5124F, 32.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.7754F, -7.0052F, -0.4729F, 0.0F, 1.5708F, 0.0436F));

		PartDefinition cube_r40 = leg2.addOrReplaceChild("cube_r40", CubeListBuilder.create().texOffs(246, 289).addBox(-8.2562F, -11.637F, 3.4961F, 16.5124F, 10.0F, 10.0F, new CubeDeformation(0.0F))
				.texOffs(0, 303).addBox(-9.2562F, -5.637F, 10.4961F, 18.5124F, 7.0F, 4.0F, new CubeDeformation(0.0F))
				.texOffs(0, 168).addBox(-9.2562F, -47.637F, 2.4961F, 18.5124F, 39.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.7646F, 5.1319F, -0.4729F, 0.0F, 1.5708F, 0.0F));

		PartDefinition cube_r41 = leg2.addOrReplaceChild("cube_r41", CubeListBuilder.create().texOffs(300, 289).addBox(-9.2562F, -2.6F, -2.7F, 18.5124F, 5.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.7315F, 1.9948F, -0.4729F, 0.0F, 1.5708F, 0.3927F));

		PartDefinition hips = partdefinition.addOrReplaceChild("hips", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, -3.1416F));

		PartDefinition bone13 = hips.addOrReplaceChild("bone13", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.0F, -71.0F, -1.5708F, 0.0F, 0.0F));

		PartDefinition cube_r42 = bone13.addOrReplaceChild("cube_r42", CubeListBuilder.create().texOffs(114, 108).addBox(-12.1182F, 0.913F, -18.4919F, 24.2364F, 5.1359F, 36.9837F, new CubeDeformation(0.0F))
				.texOffs(0, 66).addBox(-14.0262F, -1.949F, -19.4459F, 28.0524F, 3.2279F, 38.8917F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -71.542F, 0.0F, 0.0F, -1.5708F, 0.0F));

		PartDefinition bone5 = bone13.addOrReplaceChild("bone5", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -71.128F, -13.0F, 0.1745F, 0.0F, 0.0F));

		PartDefinition cube_r43 = bone5.addOrReplaceChild("cube_r43", CubeListBuilder.create().texOffs(338, 59).addBox(-9.5F, -1.975F, -16.275F, 18.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(330, 321).addBox(-10.5F, -1.975F, -14.275F, 19.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(338, 56).addBox(-9.5F, -1.975F, -12.275F, 18.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(330, 318).addBox(-10.5F, -1.975F, -10.275F, 19.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(90, 321).addBox(-11.5F, -1.975F, -8.275F, 20.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(338, 53).addBox(-9.5F, -1.975F, -6.275F, 18.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(260, 241).addBox(-10.5F, -1.975F, -4.275F, 19.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(260, 238).addBox(-10.5F, -1.975F, -18.275F, 19.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(188, 320).addBox(-11.5F, -1.975F, -20.275F, 20.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(260, 235).addBox(-10.5F, -1.975F, -22.275F, 19.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(338, 50).addBox(-9.5F, -1.975F, -24.275F, 18.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(164, 240).addBox(-10.5F, -1.975F, -26.275F, 19.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(18.5541F, 8.3517F, -1.8298F, -3.111F, -0.1719F, -1.748F));

		PartDefinition cube_r44 = bone5.addOrReplaceChild("cube_r44", CubeListBuilder.create().texOffs(302, 20).addBox(-0.0262F, 0.637F, -0.5541F, 24.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
				.texOffs(314, 168).addBox(-0.0262F, 0.637F, -10.5541F, 22.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(316, 222).addBox(-0.0262F, 0.637F, -8.5541F, 21.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(90, 313).addBox(-0.0262F, 0.637F, -6.5541F, 23.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(312, 32).addBox(-0.0262F, 0.637F, -4.5541F, 24.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(246, 309).addBox(-0.0262F, 0.637F, -2.5541F, 25.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.0F, 0.0F, 0.0F, 1.5708F, 0.3491F, 1.5708F));

		PartDefinition cube_r45 = bone5.addOrReplaceChild("cube_r45", CubeListBuilder.create().texOffs(28, 329).addBox(-3.9852F, -27.58F, -1.275F, 20.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(28, 335).addBox(-3.9852F, -27.58F, -3.275F, 19.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(90, 327).addBox(-3.9852F, -27.58F, -5.275F, 20.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(326, 231).addBox(-3.9852F, -27.58F, -7.275F, 20.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(330, 333).addBox(-3.9852F, -27.58F, -9.275F, 19.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(326, 228).addBox(-3.9852F, -27.58F, -11.275F, 20.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(28, 332).addBox(-3.9852F, -27.58F, -13.275F, 19.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(318, 79).addBox(-3.9852F, -27.58F, -15.275F, 21.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(330, 330).addBox(-3.9852F, -27.58F, -17.275F, 19.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(326, 225).addBox(-3.9852F, -27.58F, -19.275F, 20.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(318, 76).addBox(-3.9852F, -27.58F, -21.275F, 21.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(188, 326).addBox(-3.9852F, -27.58F, -23.275F, 20.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(330, 327).addBox(-3.9852F, -27.58F, -25.275F, 19.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(90, 324).addBox(-3.9852F, -27.58F, -27.275F, 20.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(188, 323).addBox(-3.9852F, -27.58F, -29.275F, 20.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(188, 317).addBox(-3.9852F, -27.58F, -31.275F, 21.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(330, 324).addBox(-3.9852F, -27.58F, -33.275F, 19.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(338, 62).addBox(-3.9852F, -27.58F, -35.275F, 18.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(318, 82).addBox(-3.9852F, -27.58F, -37.275F, 20.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-18.5541F, 8.3517F, -1.8298F, -1.5708F, 0.0F, 1.5708F));

		PartDefinition cube_r46 = bone5.addOrReplaceChild("cube_r46", CubeListBuilder.create().texOffs(338, 47).addBox(-8.5F, -1.975F, -26.275F, 18.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(342, 131).addBox(-8.5F, -1.975F, -24.275F, 17.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(338, 44).addBox(-8.5F, -1.975F, -22.275F, 18.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(164, 237).addBox(-8.5F, -1.975F, -20.275F, 19.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(338, 41).addBox(-8.5F, -1.975F, -18.275F, 18.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(342, 128).addBox(-8.5F, -1.975F, -16.275F, 17.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(338, 38).addBox(-8.5F, -1.975F, -14.275F, 18.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(342, 125).addBox(-8.5F, -1.975F, -12.275F, 17.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(338, 35).addBox(-8.5F, -1.975F, -10.275F, 18.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(164, 234).addBox(-8.5F, -1.975F, -8.275F, 19.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(338, 65).addBox(-8.5F, -1.975F, -6.275F, 17.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(0, 338).addBox(-8.5F, -1.975F, -4.275F, 18.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-18.5541F, 8.3517F, -1.8298F, -3.111F, 0.1719F, 1.748F));

		PartDefinition cube_r47 = bone5.addOrReplaceChild("cube_r47", CubeListBuilder.create().texOffs(90, 316).addBox(-8.5F, 0.5F, -3.0F, 19.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-18.5541F, 8.3517F, -1.8298F, 2.3863F, 0.2962F, 1.6995F));

		PartDefinition cube_r48 = bone5.addOrReplaceChild("cube_r48", CubeListBuilder.create().texOffs(234, 76).addBox(-0.0262F, 0.637F, -4.4459F, 33.0F, 1.0F, 8.8917F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 1.5708F, 0.3491F, 1.5708F));

		PartDefinition bone4 = bone13.addOrReplaceChild("bone4", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -71.128F, -13.0F, 0.1745F, 0.0F, 0.0F));

		PartDefinition cube_r49 = bone4.addOrReplaceChild("cube_r49", CubeListBuilder.create().texOffs(302, 16).addBox(-23.9738F, 0.637F, -0.5541F, 24.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
				.texOffs(316, 219).addBox(-20.9738F, 0.637F, -10.5541F, 21.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(312, 29).addBox(-23.9738F, 0.637F, -8.5541F, 24.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(246, 37).addBox(-22.9738F, 0.637F, -6.5541F, 23.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(90, 310).addBox(-23.9738F, 0.637F, -4.5541F, 24.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(300, 141).addBox(-24.9738F, 0.637F, -2.5541F, 25.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.0F, 0.0F, 0.0F, 1.5708F, -0.3491F, -1.5708F));

		PartDefinition cube_r50 = bone4.addOrReplaceChild("cube_r50", CubeListBuilder.create().texOffs(302, 24).addBox(-13.5F, 0.5F, -3.0F, 22.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(18.5541F, 8.3517F, -1.8298F, 2.3863F, -0.2962F, -1.6995F));

		PartDefinition left_forearm = partdefinition.addOrReplaceChild("left_forearm", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition bone16 = left_forearm.addOrReplaceChild("bone16", CubeListBuilder.create(), PartPose.offsetAndRotation(-30.0F, 0.0F, -71.7057F, -1.5708F, 0.0F, 0.0F));

		PartDefinition cube_r51 = bone16.addOrReplaceChild("cube_r51", CubeListBuilder.create().texOffs(248, 329).addBox(-1.5F, -2.5F, -6.0F, 3.0F, 5.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(33.0137F, -86.5548F, 0.1453F, 0.0F, 0.0F, -0.5672F));

		PartDefinition cube_r52 = bone16.addOrReplaceChild("cube_r52", CubeListBuilder.create().texOffs(296, 37).addBox(-3.06F, -14.14F, -5.94F, 9.0F, 19.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(27.6051F, -75.3471F, 0.0853F, 0.0F, 0.0F, -0.0873F));

		PartDefinition cube_r53 = bone16.addOrReplaceChild("cube_r53", CubeListBuilder.create().texOffs(0, 314).addBox(-4.5F, -8.0F, -6.0F, 6.0F, 16.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(29.1251F, -64.8471F, 0.0653F, -3.0107F, 0.0F, 3.1416F));

		PartDefinition cube_r54 = bone16.addOrReplaceChild("cube_r54", CubeListBuilder.create().texOffs(136, 317).addBox(-0.5F, -8.0F, -6.0F, 5.0F, 16.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(30.1251F, -64.8471F, 0.0653F, 0.1278F, -0.0283F, 0.2164F));

		PartDefinition cube_r55 = bone16.addOrReplaceChild("cube_r55", CubeListBuilder.create().texOffs(316, 171).addBox(-0.5F, -8.0F, -6.0F, 5.0F, 16.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(29.1251F, -64.8471F, 0.0653F, 0.1309F, 0.0F, 0.0F));

		PartDefinition cube_r56 = bone16.addOrReplaceChild("cube_r56", CubeListBuilder.create().texOffs(282, 318).addBox(-3.5F, -8.0F, -6.0F, 4.0F, 16.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(29.1251F, -64.8471F, 0.0653F, 0.1298F, 0.017F, -0.1298F));

		PartDefinition cube_r57 = bone16.addOrReplaceChild("cube_r57", CubeListBuilder.create().texOffs(262, 144).addBox(-3.98F, -5.5F, -7.02F, 12.0F, 15.0F, 14.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(27.8301F, -79.3471F, 0.0853F, 0.0F, 0.0F, -0.0436F));

		PartDefinition left_hand = partdefinition.addOrReplaceChild("left_hand", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition bone26 = left_hand.addOrReplaceChild("bone26", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition bone17 = bone26.addOrReplaceChild("bone17", CubeListBuilder.create(), PartPose.offsetAndRotation(-30.0F, 0.0F, -48.9057F, -1.5708F, 0.0F, 0.0F));

		PartDefinition cube_r58 = bone17.addOrReplaceChild("cube_r58", CubeListBuilder.create().texOffs(176, 360).addBox(-1.414F, -4.947F, -0.5462F, 3.0F, 5.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(33.4378F, -56.3883F, 6.7456F, 2.6215F, -0.7227F, -2.8475F));

		PartDefinition cube_r59 = bone17.addOrReplaceChild("cube_r59", CubeListBuilder.create().texOffs(40, 338).addBox(-4.0649F, -5.8013F, -0.3063F, 9.0F, 7.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(35.7251F, -56.8471F, 0.4653F, 3.029F, -1.4761F, 2.7541F));

		PartDefinition cube_r60 = bone17.addOrReplaceChild("cube_r60", CubeListBuilder.create().texOffs(102, 330).addBox(-1.5F, -4.0F, -5.0F, 5.0F, 4.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(29.1251F, -55.8471F, 0.0653F, 0.0F, 0.0F, 0.1745F));

		PartDefinition hand = bone17.addOrReplaceChild("hand", CubeListBuilder.create(), PartPose.offset(32.3283F, -53.4943F, 6.3973F));

		PartDefinition cube_r61 = hand.addOrReplaceChild("cube_r61", CubeListBuilder.create().texOffs(342, 159).addBox(-1.5F, 3.0F, -3.5F, 11.0F, 3.0F, 5.0F, new CubeDeformation(0.0F))
				.texOffs(342, 199).addBox(-2.25F, -2.0F, -3.5F, 10.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.2032F, -2.3528F, -8.332F, 0.0F, 0.0F, 0.2618F));

		PartDefinition cube_r62 = hand.addOrReplaceChild("cube_r62", CubeListBuilder.create().texOffs(260, 346).addBox(0.5F, -2.0F, -4.5F, 4.0F, 10.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.7968F, -1.3528F, -8.932F, -2.7306F, -1.4761F, 2.7541F));

		PartDefinition cube_r63 = hand.addOrReplaceChild("cube_r63", CubeListBuilder.create().texOffs(356, 171).addBox(0.6391F, -0.1725F, -0.2497F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.7968F, 13.6472F, -8.932F, -2.0325F, -1.4761F, 2.7541F));

		PartDefinition cube_r64 = hand.addOrReplaceChild("cube_r64", CubeListBuilder.create().texOffs(248, 354).addBox(1.0643F, 0.0949F, -0.3162F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.7968F, 6.6472F, -8.932F, -2.4688F, -1.4761F, 2.7541F));

		PartDefinition bone8 = hand.addOrReplaceChild("bone8", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0436F, 0.829F, 0.0F));

		PartDefinition cube_r65 = bone8.addOrReplaceChild("cube_r65", CubeListBuilder.create().texOffs(142, 310).addBox(0.1576F, 0.2614F, 0.5326F, 3.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 9.0F, 1.0F, -2.5019F, -0.0059F, -2.9557F));

		PartDefinition cube_r66 = bone8.addOrReplaceChild("cube_r66", CubeListBuilder.create().texOffs(30, 341).addBox(-0.9514F, 0.0819F, -0.412F, 3.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 3.0F, 1.0F, -2.9818F, -0.0059F, -2.9557F));

		PartDefinition cube_r67 = bone8.addOrReplaceChild("cube_r67", CubeListBuilder.create().texOffs(138, 358).addBox(-1.5F, -3.0F, -1.5F, 3.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -3.1127F, -0.0059F, -2.9557F));

		PartDefinition bone9 = hand.addOrReplaceChild("bone9", CubeListBuilder.create(), PartPose.offsetAndRotation(1.5468F, -0.3528F, -9.332F, 0.0F, 0.0F, -0.1309F));

		PartDefinition cube_r68 = bone9.addOrReplaceChild("cube_r68", CubeListBuilder.create().texOffs(152, 279).addBox(-1.1786F, 2.0048F, -3.8749F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.375F, 3.9F, 0.0F, -0.026F, -0.9595F, 0.1932F));

		PartDefinition cube_r69 = bone9.addOrReplaceChild("cube_r69", CubeListBuilder.create().texOffs(52, 224).addBox(-1.4398F, 1.0939F, -3.085F, 3.0F, 7.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.375F, 9.9F, 0.0F, -0.026F, -0.9595F, 0.4986F));

		PartDefinition cube_r70 = bone9.addOrReplaceChild("cube_r70", CubeListBuilder.create().texOffs(30, 351).addBox(-1.5F, -2.0F, -3.5F, 4.0F, 9.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.026F, -0.9595F, 0.0623F));

		PartDefinition bone7 = hand.addOrReplaceChild("bone7", CubeListBuilder.create(), PartPose.offsetAndRotation(0.7968F, -0.3528F, -8.332F, 0.1709F, 0.0828F, 0.0524F));

		PartDefinition cube_r71 = bone7.addOrReplaceChild("cube_r71", CubeListBuilder.create().texOffs(234, 317).addBox(-0.7219F, -0.4167F, -1.27F, 3.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.0F, 6.0F, 5.0F, -2.6118F, -1.0892F, 3.0644F));

		PartDefinition cube_r72 = bone7.addOrReplaceChild("cube_r72", CubeListBuilder.create().texOffs(324, 356).addBox(-1.8957F, 0.5592F, 0.2414F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 13.0F, 5.0F, -2.35F, -1.0892F, 3.0644F));

		PartDefinition cube_r73 = bone7.addOrReplaceChild("cube_r73", CubeListBuilder.create().texOffs(0, 354).addBox(2.5F, -2.0F, -5.5F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -3.0481F, -1.0892F, 3.0644F));

		PartDefinition bone6 = hand.addOrReplaceChild("bone6", CubeListBuilder.create(), PartPose.offsetAndRotation(-6.3896F, -2.4901F, -8.832F, 0.0F, 0.0F, -0.1309F));

		PartDefinition cube_r74 = bone6.addOrReplaceChild("cube_r74", CubeListBuilder.create().texOffs(232, 354).addBox(0.0F, -5.5F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.3888F, 8.8169F, 0.0F, 0.0F, 0.0F, 0.0873F));

		PartDefinition cube_r75 = bone6.addOrReplaceChild("cube_r75", CubeListBuilder.create().texOffs(28, 314).addBox(-1.5F, -3.0F, -3.5F, 5.0F, 7.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.2024F, 1.9542F, 1.0F, 0.0F, 0.0F, 0.5672F));

		PartDefinition cube_r76 = bone6.addOrReplaceChild("cube_r76", CubeListBuilder.create().texOffs(356, 191).addBox(-0.5F, 2.5F, -2.0F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.3888F, 8.8169F, 0.0F, 0.0F, 0.0F, -0.3491F));

		PartDefinition right_tricep = partdefinition.addOrReplaceChild("right_tricep", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition bone18 = right_tricep.addOrReplaceChild("bone18", CubeListBuilder.create(), PartPose.offsetAndRotation(30.0F, 0.0F, -102.9057F, 1.5708F, 0.0F, 0.0F));

		PartDefinition cube_r77 = bone18.addOrReplaceChild("cube_r77", CubeListBuilder.create().texOffs(64, 196).mirror().addBox(-9.1642F, -9.2328F, -14.7194F, 18.3284F, 16.0F, 16.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-25.6776F, 111.784F, 0.0F, 0.0F, -1.5708F, 0.6109F));

		PartDefinition cube_r78 = bone18.addOrReplaceChild("cube_r78", CubeListBuilder.create().texOffs(188, 329).mirror().addBox(-0.8277F, -0.6732F, -7.02F, 2.0F, 4.0F, 13.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-32.6451F, 90.3772F, 0.0853F, 0.0F, 0.0F, 0.5672F));

		PartDefinition cube_r79 = bone18.addOrReplaceChild("cube_r79", CubeListBuilder.create().texOffs(302, 273).mirror().addBox(-7.1F, -9.34F, -7.02F, 10.0F, 3.0F, 13.0F, new CubeDeformation(0.0F)).mirror(false)
				.texOffs(246, 0).mirror().addBox(-9.1F, -6.34F, -8.02F, 13.0F, 14.0F, 15.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-25.4451F, 98.0272F, 0.0853F, 0.0F, 0.0F, -0.0873F));

		PartDefinition cube_r80 = bone18.addOrReplaceChild("cube_r80", CubeListBuilder.create().texOffs(0, 272).mirror().addBox(-6.1F, -11.34F, -6.18F, 12.0F, 19.0F, 12.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-25.4451F, 105.5871F, 0.0853F, 0.0F, 0.0F, -0.0873F));

		PartDefinition cube_r81 = bone18.addOrReplaceChild("cube_r81", CubeListBuilder.create().texOffs(302, 235).mirror().addBox(-1.0F, -3.0F, -7.0F, 8.0F, 5.0F, 14.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-32.0378F, 108.3163F, -0.0147F, 0.0F, 0.0F, 0.0436F));

		PartDefinition cube_r82 = bone18.addOrReplaceChild("cube_r82", CubeListBuilder.create().texOffs(236, 86).mirror().addBox(-8.1F, -8.1F, -8.1F, 16.0F, 13.0F, 16.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-22.2051F, 113.1471F, 0.0853F, 0.0F, 0.0F, -0.5236F));

		PartDefinition right_forearm = partdefinition.addOrReplaceChild("right_forearm", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition bone19 = right_forearm.addOrReplaceChild("bone19", CubeListBuilder.create(), PartPose.offsetAndRotation(30.0F, 0.0F, -71.7057F, -1.5708F, 0.0F, 0.0F));

		PartDefinition cube_r83 = bone19.addOrReplaceChild("cube_r83", CubeListBuilder.create().texOffs(248, 329).mirror().addBox(-1.5F, -2.5F, -6.0F, 3.0F, 5.0F, 12.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-33.0137F, -86.5548F, 0.1453F, 0.0F, 0.0F, 0.5672F));

		PartDefinition cube_r84 = bone19.addOrReplaceChild("cube_r84", CubeListBuilder.create().texOffs(296, 37).mirror().addBox(-5.94F, -14.14F, -5.94F, 9.0F, 19.0F, 12.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-27.6051F, -75.3471F, 0.0853F, 0.0F, 0.0F, 0.0873F));

		PartDefinition cube_r85 = bone19.addOrReplaceChild("cube_r85", CubeListBuilder.create().texOffs(0, 314).mirror().addBox(-1.5F, -8.0F, -6.0F, 6.0F, 16.0F, 8.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-29.1251F, -64.8471F, 0.0653F, -3.0107F, 0.0F, -3.1416F));

		PartDefinition cube_r86 = bone19.addOrReplaceChild("cube_r86", CubeListBuilder.create().texOffs(136, 317).mirror().addBox(-4.5F, -8.0F, -6.0F, 5.0F, 16.0F, 8.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-30.1251F, -64.8471F, 0.0653F, 0.1278F, 0.0283F, -0.2164F));

		PartDefinition cube_r87 = bone19.addOrReplaceChild("cube_r87", CubeListBuilder.create().texOffs(316, 171).mirror().addBox(-4.5F, -8.0F, -6.0F, 5.0F, 16.0F, 8.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-29.1251F, -64.8471F, 0.0653F, 0.1309F, 0.0F, 0.0F));

		PartDefinition cube_r88 = bone19.addOrReplaceChild("cube_r88", CubeListBuilder.create().texOffs(282, 318).mirror().addBox(-0.5F, -8.0F, -6.0F, 4.0F, 16.0F, 8.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-29.1251F, -64.8471F, 0.0653F, 0.1298F, -0.017F, 0.1298F));

		PartDefinition cube_r89 = bone19.addOrReplaceChild("cube_r89", CubeListBuilder.create().texOffs(262, 144).mirror().addBox(-8.02F, -5.5F, -7.02F, 12.0F, 15.0F, 14.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-27.8301F, -79.3471F, 0.0853F, 0.0F, 0.0F, 0.0436F));

		PartDefinition left_tricep = partdefinition.addOrReplaceChild("left_tricep", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition bone14 = left_tricep.addOrReplaceChild("bone14", CubeListBuilder.create(), PartPose.offsetAndRotation(-30.0F, 0.0F, -102.9057F, -1.5708F, 0.0F, 0.0F));

		PartDefinition cube_r90 = bone14.addOrReplaceChild("cube_r90", CubeListBuilder.create().texOffs(64, 196).addBox(-9.1642F, -6.7672F, -16.7194F, 18.3284F, 16.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(25.6776F, -111.784F, 0.0F, 0.0F, 1.5708F, 0.6109F));

		PartDefinition cube_r91 = bone14.addOrReplaceChild("cube_r91", CubeListBuilder.create().texOffs(188, 329).addBox(-1.1723F, -3.3268F, -7.02F, 2.0F, 4.0F, 13.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(32.6451F, -90.3772F, 0.0853F, 0.0F, 0.0F, 0.5672F));

		PartDefinition cube_r92 = bone14.addOrReplaceChild("cube_r92", CubeListBuilder.create().texOffs(302, 273).addBox(-2.9F, 6.34F, -7.02F, 10.0F, 3.0F, 13.0F, new CubeDeformation(0.0F))
				.texOffs(246, 0).addBox(-3.9F, -7.66F, -8.02F, 13.0F, 14.0F, 15.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(25.4451F, -98.0272F, 0.0853F, 0.0F, 0.0F, -0.0873F));

		PartDefinition cube_r93 = bone14.addOrReplaceChild("cube_r93", CubeListBuilder.create().texOffs(0, 272).addBox(-5.9F, -7.66F, -6.18F, 12.0F, 19.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(25.4451F, -105.5871F, 0.0853F, 0.0F, 0.0F, -0.0873F));

		PartDefinition cube_r94 = bone14.addOrReplaceChild("cube_r94", CubeListBuilder.create().texOffs(302, 235).addBox(-7.0F, -2.0F, -7.0F, 8.0F, 5.0F, 14.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(32.0378F, -108.3163F, -0.0147F, 0.0F, 0.0F, 0.0436F));

		PartDefinition cube_r95 = bone14.addOrReplaceChild("cube_r95", CubeListBuilder.create().texOffs(236, 86).addBox(-7.9F, -4.9F, -8.1F, 16.0F, 13.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(22.2051F, -113.1471F, 0.0853F, 0.0F, 0.0F, -0.5236F));

		PartDefinition right_hand = partdefinition.addOrReplaceChild("right_hand", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition bone20 = right_hand.addOrReplaceChild("bone20", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition bone21 = bone20.addOrReplaceChild("bone21", CubeListBuilder.create(), PartPose.offsetAndRotation(30.0F, 0.0F, -48.9057F, -1.5708F, 0.0F, 0.0F));

		PartDefinition cube_r96 = bone21.addOrReplaceChild("cube_r96", CubeListBuilder.create().texOffs(176, 360).mirror().addBox(-1.586F, -4.947F, -0.5462F, 3.0F, 5.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-33.4378F, -56.3883F, 6.7456F, 2.6215F, 0.7227F, 2.8475F));

		PartDefinition cube_r97 = bone21.addOrReplaceChild("cube_r97", CubeListBuilder.create().texOffs(40, 338).mirror().addBox(-4.9351F, -5.8013F, -0.3063F, 9.0F, 7.0F, 6.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-35.7251F, -56.8471F, 0.4653F, 3.029F, 1.4761F, -2.7541F));

		PartDefinition cube_r98 = bone21.addOrReplaceChild("cube_r98", CubeListBuilder.create().texOffs(102, 330).mirror().addBox(-3.5F, -4.0F, -5.0F, 5.0F, 4.0F, 10.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-29.1251F, -55.8471F, 0.0653F, 0.0F, 0.0F, -0.1745F));

		PartDefinition hand2 = bone21.addOrReplaceChild("hand2", CubeListBuilder.create(), PartPose.offset(-32.3283F, -53.4943F, 6.3973F));

		PartDefinition cube_r99 = hand2.addOrReplaceChild("cube_r99", CubeListBuilder.create().texOffs(342, 159).mirror().addBox(-9.5F, 3.0F, -3.5F, 11.0F, 3.0F, 5.0F, new CubeDeformation(0.0F)).mirror(false)
				.texOffs(342, 199).mirror().addBox(-7.75F, -2.0F, -3.5F, 10.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(4.2032F, -2.3528F, -8.332F, 0.0F, 0.0F, -0.2618F));

		PartDefinition cube_r100 = hand2.addOrReplaceChild("cube_r100", CubeListBuilder.create().texOffs(260, 346).mirror().addBox(-4.5F, -2.0F, -4.5F, 4.0F, 10.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-0.7968F, -1.3528F, -8.932F, -2.7306F, 1.4761F, -2.7541F));

		PartDefinition cube_r101 = hand2.addOrReplaceChild("cube_r101", CubeListBuilder.create().texOffs(356, 171).mirror().addBox(-3.6391F, -0.1725F, -0.2497F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-2.7968F, 13.6472F, -8.932F, -2.0325F, 1.4761F, -2.7541F));

		PartDefinition cube_r102 = hand2.addOrReplaceChild("cube_r102", CubeListBuilder.create().texOffs(248, 354).mirror().addBox(-4.0643F, 0.0949F, -0.3162F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-4.7968F, 6.6472F, -8.932F, -2.4688F, 1.4761F, -2.7541F));

		PartDefinition bone22 = hand2.addOrReplaceChild("bone22", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0436F, -0.829F, 0.0F));

		PartDefinition cube_r103 = bone22.addOrReplaceChild("cube_r103", CubeListBuilder.create().texOffs(142, 310).mirror().addBox(-3.1576F, 0.2614F, 0.5326F, 3.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 9.0F, 1.0F, -2.5019F, 0.0059F, 2.9557F));

		PartDefinition cube_r104 = bone22.addOrReplaceChild("cube_r104", CubeListBuilder.create().texOffs(30, 341).mirror().addBox(-2.0486F, 0.0819F, -0.412F, 3.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 3.0F, 1.0F, -2.9818F, 0.0059F, 2.9557F));

		PartDefinition cube_r105 = bone22.addOrReplaceChild("cube_r105", CubeListBuilder.create().texOffs(138, 358).mirror().addBox(-1.5F, -3.0F, -1.5F, 3.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -3.1127F, 0.0059F, 2.9557F));

		PartDefinition bone23 = hand2.addOrReplaceChild("bone23", CubeListBuilder.create(), PartPose.offsetAndRotation(-1.5468F, -0.3528F, -9.332F, 0.0F, 0.0F, 0.1309F));

		PartDefinition cube_r106 = bone23.addOrReplaceChild("cube_r106", CubeListBuilder.create().texOffs(152, 279).mirror().addBox(-1.8214F, 2.0048F, -3.8749F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.375F, 3.9F, 0.0F, -0.026F, 0.9595F, -0.1932F));

		PartDefinition cube_r107 = bone23.addOrReplaceChild("cube_r107", CubeListBuilder.create().texOffs(52, 224).mirror().addBox(-1.5602F, 1.0939F, -3.085F, 3.0F, 7.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.375F, 9.9F, 0.0F, -0.026F, 0.9595F, -0.4986F));

		PartDefinition cube_r108 = bone23.addOrReplaceChild("cube_r108", CubeListBuilder.create().texOffs(30, 351).mirror().addBox(-2.5F, -2.0F, -3.5F, 4.0F, 9.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.026F, 0.9595F, -0.0623F));

		PartDefinition bone24 = hand2.addOrReplaceChild("bone24", CubeListBuilder.create(), PartPose.offsetAndRotation(-0.7968F, -0.3528F, -8.332F, 0.1709F, -0.0828F, -0.0524F));

		PartDefinition cube_r109 = bone24.addOrReplaceChild("cube_r109", CubeListBuilder.create().texOffs(234, 317).mirror().addBox(-2.2781F, -0.4167F, -1.27F, 3.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-2.0F, 6.0F, 5.0F, -2.6118F, 1.0892F, -3.0644F));

		PartDefinition cube_r110 = bone24.addOrReplaceChild("cube_r110", CubeListBuilder.create().texOffs(324, 356).mirror().addBox(-1.1043F, 0.5592F, 0.2414F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 13.0F, 5.0F, -2.35F, 1.0892F, -3.0644F));

		PartDefinition cube_r111 = bone24.addOrReplaceChild("cube_r111", CubeListBuilder.create().texOffs(0, 354).mirror().addBox(-6.5F, -2.0F, -5.5F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -3.0481F, 1.0892F, -3.0644F));

		PartDefinition bone27 = hand2.addOrReplaceChild("bone27", CubeListBuilder.create(), PartPose.offsetAndRotation(6.3896F, -2.4901F, -8.832F, 0.0F, 0.0F, 0.1309F));

		PartDefinition cube_r112 = bone27.addOrReplaceChild("cube_r112", CubeListBuilder.create().texOffs(232, 354).mirror().addBox(-4.0F, -5.5F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(4.3888F, 8.8169F, 0.0F, 0.0F, 0.0F, -0.0873F));

		PartDefinition cube_r113 = bone27.addOrReplaceChild("cube_r113", CubeListBuilder.create().texOffs(28, 314).mirror().addBox(-3.5F, -3.0F, -3.5F, 5.0F, 7.0F, 5.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(1.2024F, 1.9542F, 1.0F, 0.0F, 0.0F, -0.5672F));

		PartDefinition cube_r114 = bone27.addOrReplaceChild("cube_r114", CubeListBuilder.create().texOffs(356, 191).mirror().addBox(-2.5F, 2.5F, -2.0F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(4.3888F, 8.8169F, 0.0F, 0.0F, 0.0F, 0.3491F));

		return LayerDefinition.create(meshdefinition, 512, 512);
	}
}