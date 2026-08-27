package net.jelly.echoesofwar.entity.talos;

import net.jelly.echoesofwar.entity.BossSummonWorldEvent;
import net.jelly.echoesofwar.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

public class TalosSummonWorldEvent extends BossSummonWorldEvent {
    private static final int CHARGE_DURATION_TICKS = 100; // 5s

    public TalosSummonWorldEvent() {
        super(TalosWorldEvents.TALOS_SUMMON.get());
    }

    @Override
    protected int chargeDurationTicks() {
        return CHARGE_DURATION_TICKS;
    }

    @Override
    protected void tickCharge(Level level, float progress) {
        BlockPos pos = BlockPos.containing(position);
        if (level.isClientSide()) {
            RandomSource random = level.getRandom();
            int particles = 1 + Math.round(progress * 4f);
            for (int i = 0; i < particles; i++) {
                double ox = (random.nextDouble() - 0.5) * 1.5;
                double oz = (random.nextDouble() - 0.5) * 1.5;
                level.addParticle(ParticleTypes.PORTAL, position.x + ox, position.y + 0.2, position.z + oz, 0, 0.05, 0);
            }
        } else if (progress == 0f) {
            level.playSound(null, pos, SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 4.0F, 0.6F);
        }
    }

    @Override
    protected void spawnBoss(ServerLevel level, Vec3 pos) {
        TalosEntity talos = new TalosEntity(ModEntities.TALOS.get(), level);
        talos.snapTo(pos.x, pos.y, pos.z, level.getRandom().nextFloat() * 360f, 0f);
        EventHooks.finalizeMobSpawn(talos, level, level.getCurrentDifficultyAt(BlockPos.containing(pos)), EntitySpawnReason.EVENT, null);
        level.tryAddFreshEntityWithPassengers(talos);
        level.playSound(null, BlockPos.containing(pos), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 4.0F, 1.0F);
    }
}
