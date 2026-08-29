package net.jelly.echoesofwar.sound;

import net.jelly.echoesofwar.EchoesofWar;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.jspecify.annotations.Nullable;

@EventBusSubscriber(modid = EchoesofWar.MODID, value = Dist.CLIENT)
public final class ModMusicManager {
    private static final int FADE_OUT_TICKS = 60;
    private static final int FADE_IN_TICKS = 50;

    private enum Target { VANILLA, SILENCE, TRACK }

    private static Target activeTarget = Target.VANILLA;
    private static Target desiredTarget = Target.VANILLA;
    private static @Nullable SoundEvent desiredTrack;

    private static boolean fadingOut;
    private static boolean fadingIn;
    private static int fadeTicksRemaining;
    private static float fadeOutStartGain;

    private static @Nullable BiomeMusicSoundInstance customInstance;

    private ModMusicManager() {
    }

    // fades out whatever is currently playing, then fades this track in
    public static void requestTrack(SoundEvent sound) {
        if (desiredTarget == Target.TRACK && sound.equals(desiredTrack)) return;
        desiredTarget = Target.TRACK;
        desiredTrack = sound;
    }

    // fades out whatever is currently playing and holds silence until released.
    public static void requestSilence() {
        if (desiredTarget == Target.SILENCE) return;
        desiredTarget = Target.SILENCE;
        desiredTrack = null;
    }

    // stops overriding and lets vanilla's own music selection resume normally.
    public static void release() {
        if (desiredTarget == Target.VANILLA) return;
        desiredTarget = Target.VANILLA;
        desiredTrack = null;
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) return;

        if (desiredTarget != activeTarget && !fadingOut && !fadingIn) {
            beginFadeOut(mc);
        }

        if (fadingOut) {
            progressFadeOut(mc);
        } else if (fadingIn) {
            progressFadeIn(mc);
        }

        if (activeTarget != Target.VANILLA && !fadingOut) {
            // holding a custom track or silence: never let vanilla's own selection sneak back in
            mc.getMusicManager().stopPlaying();
        }
    }

    private static void beginFadeOut(Minecraft mc) {
        fadingOut = true;
        fadeTicksRemaining = FADE_OUT_TICKS;
        fadeOutStartGain = activeTarget == Target.TRACK && customInstance != null
                ? customInstance.getRawVolume()
                : mc.getMusicVolume();
    }

    private static void progressFadeOut(Minecraft mc) {
        fadeTicksRemaining--;
        float t = Mth.clamp(1.0f - fadeTicksRemaining / (float) FADE_OUT_TICKS, 0.0f, 1.0f);
        float gain = Mth.lerp(t, fadeOutStartGain, 0.0f);

        if (activeTarget == Target.TRACK && customInstance != null) {
            customInstance.setVolume(gain);
        } else {
            mc.getSoundManager().updateCategoryVolume(SoundSource.MUSIC, gain);
        }

        if (fadeTicksRemaining <= 0) {
            if (activeTarget == Target.TRACK && customInstance != null) {
                mc.getSoundManager().stop(customInstance);
                customInstance = null;
            } else {
                mc.getMusicManager().stopPlaying();
            }
            mc.getSoundManager().updateCategoryVolume(SoundSource.MUSIC, mc.getMusicVolume());

            fadingOut = false;
            activeTarget = desiredTarget;
            if (activeTarget == Target.TRACK) {
                beginFadeIn(mc);
            }
        }
    }

    private static void beginFadeIn(Minecraft mc) {
        if (desiredTrack == null) {
            activeTarget = Target.VANILLA;
            return;
        }
        customInstance = new BiomeMusicSoundInstance(desiredTrack);
        mc.getSoundManager().play(customInstance);
        fadingIn = true;
        fadeTicksRemaining = FADE_IN_TICKS;
    }

    private static void progressFadeIn(Minecraft mc) {
        if (customInstance == null) {
            fadingIn = false;
            return;
        }
        fadeTicksRemaining--;
        float t = Mth.clamp(1.0f - fadeTicksRemaining / (float) FADE_IN_TICKS, 0.0f, 1.0f);
        customInstance.setVolume(t);

        if (fadeTicksRemaining <= 0) {
            customInstance.setVolume(1.0f);
            fadingIn = false;
        }
    }

    @SubscribeEvent
    static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) reset();
    }

    private static void reset() {
        activeTarget = Target.VANILLA;
        desiredTarget = Target.VANILLA;
        desiredTrack = null;
        fadingOut = false;
        fadingIn = false;
        customInstance = null;
    }
}
