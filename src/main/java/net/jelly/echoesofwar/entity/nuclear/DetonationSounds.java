package net.jelly.echoesofwar.entity.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

public final class DetonationSounds {
    private DetonationSounds() {}

    // TODO: custom detonation sound effects
    public static void playDetonation(Level level, BlockPos at, boolean fatal) {
        level.playSound(null, at, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 10.0F, 0.35F);
        level.playSound(null, at, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 10.0F, 0.5F);
        if (fatal) {
            level.playSound(null, at, SoundEvents.WITHER_DEATH, SoundSource.HOSTILE, 10.0F, 0.4F);
        }
    }

    // TODO: countdown sound effects, for both spawning detonation and phase changes
    public static void tickCountdownWarning(Level level, BlockPos at, int elapsedTicks, int lengthTicks) {
        float progress = Mth.clamp(elapsedTicks / (float) Math.max(lengthTicks, 1), 0f, 1f);
        int interval = Math.max(1, Math.round(Mth.lerp(progress, 10f, 1f)));
        if (elapsedTicks % interval != 0) return;
        level.playSound(null, at, SoundEvents.NOTE_BLOCK_BIT.value(),
                SoundSource.HOSTILE, 3.0F, 0.6F + progress * 1.2F);
    }
}
