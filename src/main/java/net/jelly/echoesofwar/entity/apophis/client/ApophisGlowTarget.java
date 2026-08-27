package net.jelly.echoesofwar.entity.apophis.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import net.jelly.echoesofwar.EchoesofWar;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.jspecify.annotations.Nullable;

// screen-sized buffer holding only Apophis's emissive texture, so the smog post chain can tell what to make glow.
// the emissive texture's alpha channel is a hand-authored glow-intensity map, and drawing it into a buffer of its own
// (instead of blending it into the shared color target) keeps that map intact as both an exact
// mask and a bloom source. routing is entirely via EchoesofWarRenderTypes#APOPHIS_GLOW_TARGET;
// this class  owns the buffer and gets it into the right state before draw
@EventBusSubscriber(modid = EchoesofWar.MODID, value = Dist.CLIENT)
public final class ApophisGlowTarget {
    private static @Nullable RenderTarget target;

    private static boolean apophisVisible;

    private ApophisGlowTarget() {
    }

    /** called during render-state extraction, before any geometry is submitted */
    public static void requestThisFrame() {
        apophisVisible = true;
        getOrCreate();
    }

    public static boolean isApophisVisible() {
        return apophisVisible;
    }

    /** called by the post chain once it has read {@link #isApophisVisible()}, ending the frame */
    public static void consumeVisibility() {
        apophisVisible = false;
    }

    public static @Nullable RenderTarget getOrNull() {
        return target;
    }

    public static RenderTarget getOrCreate() {
        if (target == null) {
            RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
            target = new TextureTarget("Apophis Glow", main.width, main.height, true);
            // a freshly allocated texture's contents are undefined, and this can be sampled on the
            // very frame it's created, before the per-frame clear below ever runs
            clearColor(target);
        }
        return target;
    }

    // AfterOpaqueFeatures: opaque terrain and entity geometry are
    // in the main depth buffer, so the copy carries Apophis's own body too, and before
    // AfterTranslucentFeatures where  emissive layer's draw is flushed
    @SubscribeEvent
    static void onAfterOpaqueFeatures(RenderLevelStageEvent.AfterOpaqueFeatures event) {
        if (target == null && !apophisVisible) return;

        RenderTarget glow = getOrCreate();
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        if (glow.width != main.width || glow.height != main.height) {
            glow.resize(main.width, main.height);
        }

        clearColor(glow);
        glow.copyDepthFrom(main);
    }

    private static void clearColor(RenderTarget glow) {
        GpuTexture color = glow.getColorTexture();
        if (color != null) {
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(color, 0);
        }
    }

    @SubscribeEvent
    static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide() && target != null) {
            target.destroyBuffers();
            target = null;
            apophisVisible = false;
        }
    }
}
