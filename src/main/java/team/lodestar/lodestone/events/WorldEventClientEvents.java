package team.lodestar.lodestone.events;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import team.lodestar.lodestone.modules.toolkit.worldevent.WorldEventHandler;
import team.lodestar.lodestone.modules.toolkit.worldevent.WorldEventRenderHandler;

// ported from Lodestone 1.21; partial tick is read from
// Minecraft.getInstance().getDeltaTracker() since RenderLevelStageEvent no longer exposes it
@EventBusSubscriber(value = Dist.CLIENT)
public class WorldEventClientEvents {

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        if (level != null) {
            if (minecraft.isPaused()) {
                return;
            }
            WorldEventHandler.tick(level);
        }
    }

    @SubscribeEvent
    public static void onAfterSky(RenderLevelStageEvent.AfterSky event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        Camera camera = minecraft.gameRenderer.getMainCamera();
        float partial = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        PoseStack poseStack = event.getPoseStack();
        WorldEventRenderHandler.renderWorldEvents(level, poseStack, camera, partial);
    }
}
