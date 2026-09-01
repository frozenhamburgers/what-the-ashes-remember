package net.jelly.echoesofwar.entity.apophis;

import net.jelly.echoesofwar.entity.BossSummonWorldEvent;
import net.jelly.echoesofwar.entity.ModEntities;
import net.jelly.echoesofwar.entity.apophis.goals.ApophisFlightGoal;
import net.jelly.echoesofwar.entity.apophis.smog.ApophisSmogWorldEvent;
import net.jelly.echoesofwar.entity.apophis.smog.ApophisWorldEvents;
import net.jelly.echoesofwar.sound.ModMusicManager;
import net.jelly.echoesofwar.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import team.lodestar.lodestone.modules.toolkit.worldevent.WorldEventHandler;

import static net.jelly.echoesofwar.entity.apophis.ApophisEntity.nextNoon;
import static net.jelly.echoesofwar.entity.apophis.ApophisEntity.setOverworldClockTime;

// opening the box runs the flight phase entrance in reverse
public class ApophisSummonWorldEvent extends BossSummonWorldEvent {
    // how long the smog rises before spawn
    private static final int CHARGE_DURATION_TICKS = 220;

    // delay before the box starts pouring smog
    private static final int SMOG_SPAWN_DELAY_TICKS = 32;

    // how far above the box the cloud gathers
    private static final double CLOUD_HEIGHT = 60.0;

    // spawned cloud y offset
    private static final double SPAWN_HEIGHT_ABOVE_CENTRE = 18.0;

    private ApophisSmogWorldEvent smog;

    private long timeTransitionStart = -1L;
    private long timeTransitionTarget = -1L;

    // whether a box is charging on the client, for BiomeMusicTrigger to defer to
    private static boolean clientCharging;

    public static boolean isChargingOnClient() {
        return clientCharging;
    }

    public ApophisSummonWorldEvent() {
        super(ApophisWorldEvents.APOPHIS_SUMMON.get());
    }

    @Override
    protected int chargeDurationTicks() {
        return CHARGE_DURATION_TICKS;
    }

    @Override
    public void tick(Level level) {
        if (level.isClientSide()) clientCharging = true;
        super.tick(level);
        if (discarded && level.isClientSide()) clientCharging = false;
    }

    @Override
    protected void tickCharge(Level level, float progress) {
        if (level.isClientSide()) {
            ModMusicManager.requestTrack(ModSounds.APOPHIS_THEME.get());
            return;
        }

        tickTimeTransition(level, progress);

        if (smog != null) return;

        if (progress < SMOG_SPAWN_DELAY_TICKS / (float) CHARGE_DURATION_TICKS) return;

        Vec3 centre = position.add(0.0, CLOUD_HEIGHT, 0.0);
        float seed = (float) ((position.x * 12.9898 + position.z * 78.233) % 1000.0);
        smog = new ApophisSmogWorldEvent().setupFromSource(position, centre, ApophisFlightGoal.smogRadius(), seed);
        WorldEventHandler.addWorldEvent(level, smog);

        level.playSound(null, BlockPos.containing(position), SoundEvents.EVOKER_PREPARE_SUMMON,
                SoundSource.HOSTILE, 4.0F, 0.5F);
    }

    // eases the day time to the nearest noon over the charge
    private void tickTimeTransition(Level level, float progress) {
        ServerLevel serverLevel = (ServerLevel) level;
        if (timeTransitionStart < 0) {
            timeTransitionStart = serverLevel.getOverworldClockTime();
            timeTransitionTarget = nextNoon(timeTransitionStart);
        }
        long time = timeTransitionStart + Math.round((timeTransitionTarget - timeTransitionStart) * (double) progress);
        setOverworldClockTime(serverLevel, time);
    }

    @Override
    protected void spawnBoss(ServerLevel level, Vec3 pos) {
        Vec3 centre = pos.add(0.0, CLOUD_HEIGHT, 0.0);
        Vec3 spawnAt = centre.add(0.0, SPAWN_HEIGHT_ABOVE_CENTRE, 0.0);

        ApophisEntity apophis = new ApophisEntity(ModEntities.APOPHIS.get(), level);
        apophis.snapTo(spawnAt.x, spawnAt.y, spawnAt.z, level.getRandom().nextFloat() * 360f, 0f);
        EventHooks.finalizeMobSpawn(apophis, level, level.getCurrentDifficultyAt(BlockPos.containing(spawnAt)),
                EntitySpawnReason.EVENT, null);

        // must happen before the entity joins the world, so its first tick already knows it's flying
        if (smog != null) apophis.inheritCloud(smog, centre);

        level.tryAddFreshEntityWithPassengers(apophis);
        level.playSound(null, BlockPos.containing(pos), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 4.0F, 0.7F);
    }
}
