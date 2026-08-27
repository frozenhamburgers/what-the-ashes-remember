package net.jelly.echoesofwar.entity.apophis.client;

import net.jelly.echoesofwar.Config;
import net.jelly.echoesofwar.EchoesofWar;
import net.jelly.echoesofwar.entity.apophis.ApophisEntity;
import net.jelly.echoesofwar.entity.apophis.smog.ApophisPostProcessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

// 0..1 strength for the fight's weather (ambient skylight drop, horizon smog, distant noise fog)
// triggered when Apophis is viewed on client
@EventBusSubscriber(modid = EchoesofWar.MODID, value = Dist.CLIENT)
public final class ApophisAtmosphere {
    private static final int FADE_IN_TICKS = 60;
    private static final int FADE_OUT_TICKS = 120;

    // ambient lighting factor
    private static final float SKY_FACTOR_AT_FULL = 0.5f;

    private static float intensity;
    // previous ticks intensity for interpolation
    private static float prevIntensity;

    private static float baseSkyFactor;
    private static boolean hasBase;
    private static boolean overriding;

    private ApophisAtmosphere() {
    }

    /** smoothly interpolated strength for this frame, scaled by the client config */
    public static float intensity(float partialTick) {
        return Mth.lerp(partialTick, prevIntensity, intensity)
                * (float) (double) Config.ATMOSPHERE_INTENSITY.get();
    }

    public static boolean isActive() {
        return intensity > 0.0f || prevIntensity > 0.0f;
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            reset();
            return;
        }
        if (minecraft.isPaused()) return; // freeze the ramp rather than letting it run in the menu

        prevIntensity = intensity;
        boolean present = apophisPresent(level);
        float step = 1.0f / (present ? FADE_IN_TICKS : FADE_OUT_TICKS);
        intensity = Mth.clamp(intensity + (present ? step : -step), 0.0f, 1.0f);

        // the chain can't switch itself back on (applyPostProcess tests isActive before
        // beforeProcess), so wake it here too just in case, since the
        // atmosphere needs to run even while Apophis is off screen
        if (isActive()) ApophisPostProcessor.INSTANCE.setActive(true);
    }

    private static boolean apophisPresent(ClientLevel level) {
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof ApophisEntity apophis && apophis.isAlive()) return true;
        }
        return false;
    }

    // dims the world's ambient skylight via LightmapRenderState.skyFactor
    @SubscribeEvent
    static void onExtractLevelRenderState(ExtractLevelRenderStateEvent event) {
        LightmapRenderState lightmap =
                Minecraft.getInstance().gameRenderer.getGameRenderState().lightmapRenderState;

        // `|| !hasBase` covers the first frame: if vanilla hasn't ticked yet, the base would
        // be captured as 0 and the world would go pitch black for up to a tick
        if (lightmap.needsUpdate || !hasBase) {
            baseSkyFactor = lightmap.skyFactor;
            hasBase = true;
        }

        float i = intensity(event.getDeltaTracker().getGameTimeDeltaPartialTick(false));
        if (i <= 0.0f && !overriding) return; // idle - leave vanilla's lightmap entirely alone

        lightmap.skyFactor = baseSkyFactor * Mth.lerp(i, 1.0f, SKY_FACTOR_AT_FULL);
        lightmap.needsUpdate = true; // Lightmap.render skips the upload unless this is set
        // one extra frame of writing after the fade reaches zero, so the base value is
        // restored rather than the last dimmed value being left in place
        overriding = i > 0.0f;
    }

    @SubscribeEvent
    static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) reset();
    }

    private static void reset() {
        prevIntensity = intensity = 0.0f;
        hasBase = false;
        overriding = false;
    }
}
