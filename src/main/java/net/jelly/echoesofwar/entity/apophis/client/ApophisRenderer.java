package net.jelly.echoesofwar.entity.apophis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.jelly.echoesofwar.EchoesofWar;
import net.jelly.echoesofwar.entity.apophis.ApophisEntity;
import net.jelly.echoesofwar.entity.apophis.smog.ApophisPostProcessor;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

public class ApophisRenderer extends MobRenderer<ApophisEntity, ApophisRenderState, ApophisModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(EchoesofWar.MODID, "textures/entity/apophis.png");
    private static final Identifier EMISSIVE_TEXTURE = Identifier.fromNamespaceAndPath(EchoesofWar.MODID, "textures/entity/apophis_e.png");

    public ApophisRenderer(EntityRendererProvider.Context context) {
        super(context, new ApophisModel(context.bakeLayer(ApophisModel.LAYER_LOCATION)), 0.0F);

//        this.addLayer(new LivingEntityEmissiveLayer<>(
//                this,
//                state -> EMISSIVE_TEXTURE,
//                (state, ageInTicks) -> 1.0F,
//                this.getModel(),
//                EchoesofWarRenderTypes::apophisEmissive,
//                false
//        ));

        // draws into ApophisGlowTarget instead of the screen
        this.addLayer(new LivingEntityEmissiveLayer<>(
                this,
                state -> EMISSIVE_TEXTURE,
                (state, ageInTicks) -> 1.0F,
                this.getModel(),
                EchoesofWarRenderTypes::apophisGlow,
                false
        ));
    }

    @Override
    public void extractRenderState(ApophisEntity entity, ApophisRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        // must happen before the glow layer submits, or OutputTarget falls back to the main target and draws Apophis's emissive twice on screen
        ApophisGlowTarget.requestThisFrame();
        // wakes the chain even with no cloud out since it can't switch itself back on, just in case
        ApophisPostProcessor.INSTANCE.setActive(true);
        state.partDirections = entity.getMarionettePartDirections();
        state.partOffsets = entity.getMarionettePartOffsets(entity, partialTick);
        state.partScales = entity.getSegmentScales();
    }

    @Override
    protected AABB getBoundingBoxForCulling(ApophisEntity entity) {
        return entity.getMarionetteBoundingBoxForCulling(entity);
    }

    @Override
    public boolean shouldRender(ApophisEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }

    // lit as if always in partial light to prevent embedded darkening
    @Override
    protected int getBlockLightLevel(ApophisEntity entity, BlockPos blockPos) {
        return 12;
    }

    @Override
    protected int getSkyLightLevel(ApophisEntity entity, BlockPos blockPos) {
        return 0;
    }

    @Override
    protected void setupRotations(ApophisRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
    }

    @Override
    public Identifier getTextureLocation(ApophisRenderState state) {
        return TEXTURE;
    }

    @Override
    public ApophisRenderState createRenderState() {
        return new ApophisRenderState();
    }
}
