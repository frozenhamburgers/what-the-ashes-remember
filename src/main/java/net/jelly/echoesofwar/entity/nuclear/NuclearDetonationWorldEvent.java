package net.jelly.echoesofwar.entity.nuclear;

import net.jelly.echoesofwar.entity.nuclear.fx.NuclearDetonationFx;
import net.jelly.echoesofwar.entity.nuclear.fx.NuclearDetonationPostProcessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import team.lodestar.lodestone.modules.toolkit.worldevent.WorldEventInstance;

// the standalone nuclear detonation. owns the whole lifecycle: it is spawned
// at a ground zero, runs for a fixed number of ticks, and discards itself.
//
// there is deliberately no server-side phase machine like ApophisSmogWorldEvent's - nothing external
// can cut a detonation short, so both sides just count ticks from the single sync at creation and
// every stage of the animation is derived in the shader from age / lifetime. that keeps the timing
// in one place (this file plus the constants in include/nuclear/detonation.glsl) and means no
// setDirty() traffic for the whole 75 seconds
public class NuclearDetonationWorldEvent extends WorldEventInstance {

    /** the single knob for how long the whole detonation -> dissipation sequence takes */
    public static final int DEFAULT_LIFETIME_TICKS = 1500; // 75 seconds
    /** nominal mature cap height above the base, in blocks */
    public static final float DEFAULT_HEIGHT = 220f;
    /** nominal mature cap radius, in blocks */
    public static final float DEFAULT_CAP_RADIUS = 90f;
    /** plume radius at ground level, in blocks */
    public static final float DEFAULT_VENT_RADIUS = 14f;

    private Vec3 base = Vec3.ZERO;
    private float height = DEFAULT_HEIGHT;
    private float capRadius = DEFAULT_CAP_RADIUS;
    private float ventRadius = DEFAULT_VENT_RADIUS;
    private float seed;
    private int lifetimeTicks = DEFAULT_LIFETIME_TICKS;

    private int ticks;

    // CLIENT ONLY
    private NuclearDetonationFx fx;

    public NuclearDetonationWorldEvent() {
        super(NuclearWorldEvents.TRINITY_DETONATION.get());
    }

    public NuclearDetonationWorldEvent setup(Vec3 base, float height, float capRadius, float seed,
                                             int lifetimeTicks) {
        this.base = base;
        this.height = height;
        this.capRadius = capRadius;
        // the vent scales with the cap, so passing a height/radius alone still gives a sane stalk
        this.ventRadius = Math.max(capRadius * 0.16f, 3f);
        this.seed = seed;
        this.lifetimeTicks = Math.max(lifetimeTicks, 20);
        return this;
    }

    @Override
    public boolean shouldSave() {
        // purely transient, same as the Apophis smog: a detonation half-way through its sequence is
        // not worth restoring across a save
        return false;
    }

    // uses the level parameter rather than the inherited `level` field, which isn't populated client side
    @Override
    public void tick(Level level) {
        ticks++;

        if (ticks >= lifetimeTicks) {
            removeFx();
            this.discarded = true;
            return;
        }

        if (level.isClientSide()) tickShader();
    }

    // ------------------------ CLIENT

    private void tickShader() {
        if (fx == null) {
            fx = new NuclearDetonationFx(base.toVector3f(), seed);
            if (NuclearDetonationPostProcessor.INSTANCE.addFxInstance(fx) == null) {
                fx = null; // post processor is full; skip this detonation rather than replacing one
                return;
            }
            NuclearDetonationPostProcessor.INSTANCE.setActive(true);
        }

        fx.height = height;
        fx.capRadius = capRadius;
        fx.ventRadius = ventRadius;
        fx.age = ticks / 20f;
        fx.lifetime = lifetimeTicks / 20f;
    }

    private void removeFx() {
        if (fx == null) return;
        fx.remove();
        fx = null;
    }

    // ------------------------- persistence / sync

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putDouble("x", base.x);
        tag.putDouble("y", base.y);
        tag.putDouble("z", base.z);
        tag.putFloat("height", height);
        tag.putFloat("capRadius", capRadius);
        tag.putFloat("ventRadius", ventRadius);
        tag.putFloat("seed", seed);
        tag.putInt("lifetimeTicks", lifetimeTicks);
        tag.putInt("ticks", ticks);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        base = new Vec3(tag.getDoubleOr("x", 0), tag.getDoubleOr("y", 0), tag.getDoubleOr("z", 0));
        height = tag.getFloatOr("height", DEFAULT_HEIGHT);
        capRadius = tag.getFloatOr("capRadius", DEFAULT_CAP_RADIUS);
        ventRadius = tag.getFloatOr("ventRadius", DEFAULT_VENT_RADIUS);
        seed = tag.getFloatOr("seed", 0f);
        lifetimeTicks = tag.getIntOr("lifetimeTicks", DEFAULT_LIFETIME_TICKS);
        ticks = tag.getIntOr("ticks", 0);
    }
}
