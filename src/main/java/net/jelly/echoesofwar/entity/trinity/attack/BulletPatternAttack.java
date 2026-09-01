package net.jelly.echoesofwar.entity.trinity.attack;

import net.minecraft.nbt.CompoundTag;

/**
 * Persistent system managing trinity's projectile barrages
 * Sync thru just one int, everything else comes off it through an integer hash both
 * sides evaluate identically or via the tick count.
 */
public class BulletPatternAttack extends TrinityAttack {

    // ticks between pulses
    private static final int WAVE_INTERVAL_TICKS = 50;

    // surface telegraph ticks
    private static final int LEAD_TICKS = 22;

   // how far proj can fly
    private static final float TRAVEL = 200f;

    private static final float SPEED = 1.15f; // base speed at difficulty 1.0

    private static final float JITTER = 0.85f;
    private static final float SPEED_JITTER = 0.30f;

    private static final float RADIUS = 4.2f; // individual radius of proj

    private static final float FADE_FROM = 0.88f; // fraction of flight to begins fading

    private int patternSeed;

    @Override
    public boolean isFinished() {
        return false;
    } // never finished, world event just manually cuts it

    @Override
    public int slotsNeeded(Context ctx) {
        return 0;   // generated field does not need to be stored
    }

    @Override
    public void start(Context ctx) {
        patternSeed = ctx.random().nextInt();
        markDirty();
    }


    // how far a pulse may travel before every proj is gone
    static float waveLimit(BulletField field) {
        return field.travel / Math.max(1f - field.speedJitter, 0.05f);
    }

    @Override
    public void tick(Context ctx, int age) {
        BulletField field = ctx.bulletField();
        field.active = true;
        field.age = age / 20f;
        field.seed = patternSeed;
        field.waveInterval = WAVE_INTERVAL_TICKS / 20f;
        field.lead = LEAD_TICKS / 20f;
        field.speed = SPEED * 20f * ctx.difficulty();
        field.travel = TRAVEL;
        field.launch = ctx.bodyRadius();
        field.radius = RADIUS;
        field.jitter = JITTER;
        field.speedJitter = SPEED_JITTER;
        field.fadeFrom = FADE_FROM;
        field.updateWindow();
    }

    @Override
    public void save(CompoundTag tag) {
        tag.putInt("seed", patternSeed);
    }

    @Override
    public void load(CompoundTag tag) {
        patternSeed = tag.getIntOr("seed", 0);
    }
}
