package net.jelly.echoesofwar.entity.apophis.smog;

import net.jelly.echoesofwar.entity.apophis.ApophisEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import team.lodestar.lodestone.modules.toolkit.worldevent.WorldEventInstance;

// the smog cloud Apophis lays down during flight phase, living as long as that phase plus
// its dissipation. server owns the phase and pushes transitions with setDirty(), continuous
// values are ran on both sides independently
public class ApophisSmogWorldEvent extends WorldEventInstance {

    public enum Phase {
        EMITTING,
        SETTLED,
        FADING
    }

    private static final int GROWTH_TICKS = 200;
    private static final float FRONT_SPEED = 1.4f;
    public static final int FADE_TICKS = 400;
    // how long the mouth jet takes to trail off after Apophis stops spewign
    public static final int EMIT_STOP_TICKS = 24;
    // how long an unbound cloud (no Apophis keeping it alive) lasts before dissipating on its own
    // covers the debug command and the gap between a summoning box and its boss arriving to claim it
    private static final int UNBOUND_LIFETIME_TICKS = 1200;

    private int ownerId = -1;
    /** where the jet comes from with no owner entity - a summoning box, say */
    private Vec3 source = Vec3.ZERO;
    // bound clouds live as long as the owning flight; unbound ones (summon before its
    // boss exists) run on UNBOUND_LIFETIME_TICKS instead
    private boolean flightBound;
    private Vec3 destination = Vec3.ZERO;
    private float radius = 32f;
    private float seed;

    private Phase phase = Phase.EMITTING;
    private int phaseTicks;
    // for the unbound cap
    private int totalTicks;
    // only starts once the jet  reaches the destination
    private int growthTicks;
    // counted from the first tick of emission
    // since it drives the jet's noise scroll and the jet is on screen throughout the climb
    private int emitTicks;
    // how far the leading edge of the emission jet has travelled from the mouth
    private float frontDistance;
    private boolean frontArrived;

    /** jet strength when fading began, so emitStrength() eases to zero from there instead of snapping */
    private float emitAtFadeStart;

    // MOUTH RETRACTION (client-only)
    private int renderedOwnerId = Integer.MIN_VALUE;
    private Vec3 retractOrigin;
    private int retractTicks;

    private ApophisSmogFx fx;

    public ApophisSmogWorldEvent() {
        super(ApophisWorldEvents.APOPHIS_SMOG.get());
    }

    public ApophisSmogWorldEvent setupForFlight(int ownerId, Vec3 destination, float radius, float seed) {
        this.ownerId = ownerId;
        this.source = destination;
        this.flightBound = true;
        this.destination = destination;
        this.radius = radius;
        this.seed = seed;
        return this;
    }

    /** a cloud poured from a fixed point rather than a creature (summoning box) */
    public ApophisSmogWorldEvent setupFromSource(Vec3 source, Vec3 destination, float radius, float seed) {
        this.ownerId = -1;
        this.source = source;
        this.flightBound = false;
        this.destination = destination;
        this.radius = radius;
        this.seed = seed;
        return this;
    }

    /** hands an already poured cloud over to the Apophis that just arrived (spawning sequence)*/
    public void bindToFlight(int ownerId) {
        this.ownerId = ownerId;
        this.flightBound = true;
        setDirty();
    }

    public void stopEmitting() {
        if (phase != Phase.EMITTING) return;
        phase = Phase.SETTLED;
        phaseTicks = 0;
        setDirty();
    }

    public void beginFade() {
        if (phase == Phase.FADING) return;
        // captured before the phase flips, so a flight cut short mid-emission still eases down
        // from whatever strength the jet had instead of snapping straight to zero
        emitAtFadeStart = emitStrength();
        phase = Phase.FADING;
        phaseTicks = 0;
        setDirty();
    }

    @Override
    public boolean shouldSave() {
        return false;
    }

    public boolean isFading() {
        return phase == Phase.FADING;
    }

    /** zero until the jet reaches the destination, rather than the cloud's eventual size */
    public float currentRadius() {
        return frontArrived ? radius * growth() : 0.0f;
    }

    // uses the level parameter rather than the inherited `level` field b/c not populated on client
    @Override
    public void tick(Level level) {
        phaseTicks++;
        totalTicks++;
        tickFront(level);
        if (frontArrived && phase != Phase.FADING) growthTicks++;
        if (phase == Phase.EMITTING) emitTicks++;

        if (!level.isClientSide() && phase != Phase.FADING) checkStillWanted(level);

        if (phase == Phase.FADING && phaseTicks >= FADE_TICKS) {
            removeFx();
            this.discarded = true;
            return;
        }

        if (level.isClientSide()) tickShader(level);
    }

    // advances the leading edge of the emitted smoke
    // both sides integrate this independently, only the client draws anything with it
    private void tickFront(Level level) {
        if (phase != Phase.EMITTING) {
            frontArrived = true;
            return;
        }

        frontDistance += FRONT_SPEED;
        if (frontArrived) return;

        if (frontDistance >= mouthPosition(level).distanceTo(destination)) frontArrived = true;
    }

    // derives if  cloud still exist from live state
    private void checkStillWanted(Level level) {
        if (flightBound) {
            Entity owner = ownerId >= 0 ? level.getEntity(ownerId) : null;
            if (!(owner instanceof ApophisEntity apophis && apophis.isFlying() && apophis.isAlive())) {
                beginFade();
            }
        } else if (totalTicks >= UNBOUND_LIFETIME_TICKS) {
            beginFade();
        }
    }

    // 0 -> 1 as the cloud swells eased
    private float growth() {
        return (float) Math.pow(Math.min(growthTicks / (float) GROWTH_TICKS, 1.0f), 0.55);
    }

    private float emitStrength() {
        return switch (phase) {
            case EMITTING -> 1.0f;
            case SETTLED -> Math.max(0.0f, 1.0f - phaseTicks / (float) EMIT_STOP_TICKS);
            case FADING -> Math.max(0.0f, emitAtFadeStart * (1.0f - phaseTicks / (float) EMIT_STOP_TICKS));
        };
    }

    private float density() {
        if (phase != Phase.FADING) return 1.0f;
        return Math.max(0.0f, 1.0f - phaseTicks / (float) FADE_TICKS);
    }

    private Vec3 mouthPosition(Level level) {
        Entity owner = ownerId >= 0 ? level.getEntity(ownerId) : null;
        return owner != null ? owner.position() : source;
    }

    // ------------------------ CLIENT

    private void tickShader(Level level) {
        if (fx == null) {
            fx = new ApophisSmogFx(ownerId, destination.toVector3f(), source.toVector3f(), seed);
            if (ApophisPostProcessor.INSTANCE.addFxInstance(fx) == null) {
                fx = null; // post processor is full; skip this cloud rather
                return;
            }
            ApophisPostProcessor.INSTANCE.setActive(true);
            renderedOwnerId = ownerId;
        }

        // either emission just stopped or ownership just changed hands (the summon case), both need
        // the mouth to stop tracking a live entity and ease into the cloud instead
        boolean ownerHandoff = ownerId != renderedOwnerId;
        boolean justStoppedEmitting = phase != Phase.EMITTING && retractOrigin == null;
        if (ownerHandoff || justStoppedEmitting) {
            retractOrigin = new Vec3(fx.currentMouthPosition());
            retractTicks = 0;
        }
        renderedOwnerId = ownerId;

        fx.setOwnerId(ownerId);
        fx.cloudCenter = destination.toVector3f();
        fx.cloudRadius = currentRadius();
        fx.frontDistance = frontDistance;
        fx.emit = emitStrength();
        fx.density = density();
        fx.age = growthTicks / 20f;
        fx.jetAge = emitTicks / 20f;

        if (retractOrigin != null) {
            fx.fadeOrigin = retractOrigin.toVector3f();
            fx.fadeTarget = destination.toVector3f();
            fx.fadePhaseTicks = retractTicks;
            retractTicks++;
        } else {
            fx.fadeOrigin = null;
        }
    }

    private void removeFx() {
        if (fx == null) return;
        fx.remove();
        fx = null;
    }

    // ------------------------- persistence / sync

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("owner", ownerId);
        tag.putBoolean("flightBound", flightBound);
        tag.putDouble("sx", source.x);
        tag.putDouble("sy", source.y);
        tag.putDouble("sz", source.z);
        tag.putDouble("x", destination.x);
        tag.putDouble("y", destination.y);
        tag.putDouble("z", destination.z);
        tag.putFloat("radius", radius);
        tag.putFloat("seed", seed);
        tag.putString("phase", phase.name());
        tag.putInt("phaseTicks", phaseTicks);
        tag.putInt("growthTicks", growthTicks);
        tag.putInt("totalTicks", totalTicks);
        tag.putFloat("frontDistance", frontDistance);
        tag.putBoolean("frontArrived", frontArrived);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ownerId = tag.getIntOr("owner", -1);
        flightBound = tag.getBooleanOr("flightBound", false);
        source = new Vec3(tag.getDoubleOr("sx", 0), tag.getDoubleOr("sy", 0), tag.getDoubleOr("sz", 0));
        destination = new Vec3(tag.getDoubleOr("x", 0), tag.getDoubleOr("y", 0), tag.getDoubleOr("z", 0));
        radius = tag.getFloatOr("radius", 32f);
        seed = tag.getFloatOr("seed", 0f);
        phase = Phase.valueOf(tag.getStringOr("phase", Phase.EMITTING.name()));
        phaseTicks = tag.getIntOr("phaseTicks", 0);
        growthTicks = tag.getIntOr("growthTicks", 0);
        totalTicks = tag.getIntOr("totalTicks", 0);
        frontDistance = tag.getFloatOr("frontDistance", 0f);
        frontArrived = tag.getBooleanOr("frontArrived", false);
    }
}
