package net.jelly.echoesofwar.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import team.lodestar.lodestone.modules.toolkit.worldevent.WorldEventInstance;
import team.lodestar.lodestone.modules.toolkit.worldevent.WorldEventType;

public abstract class BossSummonWorldEvent extends WorldEventInstance {
    public Vec3 position = Vec3.ZERO;
    private int chargeDuration = -1;
    private int chargeTicksRemaining;

    protected BossSummonWorldEvent(WorldEventType type) {
        super(type);
    }

    @SuppressWarnings("unchecked")
    public <T extends BossSummonWorldEvent> T setPosition(Vec3 pos) {
        this.position = pos;
        return (T) this;
    }

    protected abstract int chargeDurationTicks();

    /** called every tick of the charge on both sides with progress in [0, 1) */
    protected abstract void tickCharge(Level level, float progress);

    /** called once on the server once the charge completes */
    protected abstract void spawnBoss(ServerLevel level, Vec3 pos);

    @Override
    public boolean shouldSave() {
        return false;
    }

    @Override
    public void tick(Level level) {
        if (chargeDuration < 0) {
            chargeDuration = chargeDurationTicks();
            chargeTicksRemaining = chargeDuration;
        }

        tickCharge(level, 1f - (chargeTicksRemaining / (float) chargeDuration));

        if (--chargeTicksRemaining <= 0) {
            if (level instanceof ServerLevel serverLevel) {
                spawnBoss(serverLevel, position);
            }
            discarded = true;
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putDouble("x", position.x);
        tag.putDouble("y", position.y);
        tag.putDouble("z", position.z);
        tag.putInt("chargeDuration", chargeDuration);
        tag.putInt("chargeTicksRemaining", chargeTicksRemaining);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        position = new Vec3(tag.getDoubleOr("x", 0), tag.getDoubleOr("y", 0), tag.getDoubleOr("z", 0));
        chargeDuration = tag.getIntOr("chargeDuration", -1);
        chargeTicksRemaining = tag.getIntOr("chargeTicksRemaining", 0);
    }
}
