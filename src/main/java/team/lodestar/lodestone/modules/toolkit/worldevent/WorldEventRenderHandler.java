package team.lodestar.lodestone.modules.toolkit.worldevent;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import team.lodestar.lodestone.events.types.worldevent.WorldEventRenderEvent;
import team.lodestar.lodestone.registry.client.LodestoneWorldEventRenderers;
import team.lodestar.lodestone.registry.common.LodestoneAttachmentTypes;

/**
 * Ported from Lodestone 1.21's {@code WorldEventRenderHandler}. The original sourced its buffer
 * from Lodestone's deferred rendering module, out of scope for this port; this uses the vanilla
 * immediate buffer source instead and explicitly flushes it with {@code endBatch()}, since
 * nothing else here will flush it.
 */
public class WorldEventRenderHandler {

    public static void renderWorldEvents(ClientLevel level, PoseStack poseStack, Camera camera, float partialTicks) {
        Vec3 cameraPos = camera.position();
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x(), -cameraPos.y(), -cameraPos.z());

        var worldData = level.getData(LodestoneAttachmentTypes.WORLD_EVENT_DATA);
        MultiBufferSource.BufferSource target = Minecraft.getInstance().renderBuffers().bufferSource();
        for (WorldEventInstance instance : worldData.activeWorldEvents) {
            WorldEventRenderer<WorldEventInstance> renderer = LodestoneWorldEventRenderers.RENDERERS.get(instance.type);
            if (renderer != null) {
                if (renderer.canRender(instance)) {
                    NeoForge.EVENT_BUS.post(new WorldEventRenderEvent(instance, renderer, poseStack, target, partialTicks));
                    renderer.render(instance, poseStack, target, partialTicks);
                }
            }
        }
        target.endBatch();
        poseStack.popPose();
    }
}
