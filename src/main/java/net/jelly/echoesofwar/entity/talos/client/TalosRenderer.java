package net.jelly.echoesofwar.entity.talos.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.jelly.echoesofwar.EchoesofWar;
import net.jelly.echoesofwar.entity.talos.TalosEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

public class TalosRenderer extends MobRenderer<TalosEntity, TalosRenderState, TalosModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(EchoesofWar.MODID, "textures/entity/talos.png");

    public TalosRenderer(EntityRendererProvider.Context context) {
        super(context, new TalosModel(context.bakeLayer(TalosModel.LAYER_LOCATION)), 0.9F * TalosEntity.sizeScale());
    }

    @Override
    public void extractRenderState(TalosEntity entity, TalosRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.partDirections = entity.getMarionettePartDirections();
        state.partOffsets = entity.getMarionettePartOffsets(entity, partialTick);
    }

    // the head's own hitbox is tiny compared to the body/arms it can stretch out, so cull against all of them instead
    @Override
    protected AABB getBoundingBoxForCulling(TalosEntity entity) {
        return entity.getMarionetteBoundingBoxForCulling(entity);
    }

    // segments already compute their own world-space rotation, so skip the usual "face body rotation" step and just flip the model the right way round
    @Override
    protected void setupRotations(TalosRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
    }

    @Override
    public Identifier getTextureLocation(TalosRenderState state) {
        return TEXTURE;
    }

    @Override
    public TalosRenderState createRenderState() {
        return new TalosRenderState();
    }
}
