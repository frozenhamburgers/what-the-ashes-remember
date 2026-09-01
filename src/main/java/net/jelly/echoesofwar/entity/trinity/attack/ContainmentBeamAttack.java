package net.jelly.echoesofwar.entity.trinity.attack;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Cylindrical beams standing off Trinity's surface along its normals, rotating around
 *  Origins are laid out as a lattice: eight latitude bands, evenly spaced in longitude,
 *  offset band to band so they do not line up vertically
 */
public class ContainmentBeamAttack extends TrinityAttack {

    private static final int LAYERS = 8;
    private static final int PER_LAYER = 6;
    private static final int BEAMS = LAYERS * PER_LAYER;

    private static final int TELEGRAPH_TICKS = 34;
    private static final int EXTEND_TICKS = 12;
    private static final int SUSTAIN_TICKS = 190;
    private static final int RETRACT_TICKS = 18;

    private static final float LENGTH = 210f;
    private static final float RADIUS = 6.0f;

    private static final double OMEGA = 0.021; // radians per tick at difficulty 1.0
    private final boolean alternating;

    public ContainmentBeamAttack(boolean alternating) {
        this.alternating = alternating;
    }

    @Override
    public int slotsNeeded(Context ctx) {
        return BEAMS;
    }

    @Override
    public void start(Context ctx) {
        markDirty(); // once, and then never again, pattern should be deterministic
    }

    private int span() {
        return TELEGRAPH_TICKS + EXTEND_TICKS + SUSTAIN_TICKS + RETRACT_TICKS;
    }

    @Override
    public void tick(Context ctx, int age) {
        if (slots == null) return;
        if (age > span()) {
            finished = true;
            clearSlots();
            return;
        }

        float telegraph, extend, fade;
        if (age < TELEGRAPH_TICKS) {
            telegraph = age / (float) TELEGRAPH_TICKS;
            extend = 0f;
            fade = 0f;
        } else if (age < TELEGRAPH_TICKS + EXTEND_TICKS) {
            telegraph = 1f;
            extend = (age - TELEGRAPH_TICKS) / (float) EXTEND_TICKS;
            fade = 0f;
        } else if (age < TELEGRAPH_TICKS + EXTEND_TICKS + SUSTAIN_TICKS) {
            telegraph = 1f;
            extend = 1f;
            fade = 0f;
        } else {
            telegraph = 1f;
            extend = 1f;
            fade = Mth.clamp(
                    (age - TELEGRAPH_TICKS - EXTEND_TICKS - SUSTAIN_TICKS) / (float) RETRACT_TICKS,
                    0f, 1f);
        }

        // rotate only when beams are up
        double spun = Math.max(age - TELEGRAPH_TICKS, 0) * OMEGA * ctx.difficulty();

        for (int i = 0; i < BEAMS && i < slots.length; i++) {
            int layer = i / PER_LAYER;
            int index = i % PER_LAYER;

            double theta = Math.PI * (layer + 0.5) / LAYERS;
            double phi = 2.0 * Math.PI * (index + 0.5 * (layer & 1)) / PER_LAYER;
            // counter rotate if alternating
            phi += (alternating && (layer & 1) == 1) ? -spun : spun;

            double st = Math.sin(theta);
            Vec3 dir = new Vec3(st * Math.cos(phi), Math.cos(theta), st * Math.sin(phi));

            AttackSlot slot = slots[i];
            // origin and axis are same vector
            slot.set(AttackSlot.TYPE_CONTAINMENT, dir, LENGTH, RADIUS, i * 5.13f);
            slot.telegraph = telegraph;
            slot.extend = extend;
            slot.fade = fade;
        }
    }


    @Override
    public void save(CompoundTag tag) {
        tag.putBoolean("alt", alternating);
    }

    @Override
    public void load(CompoundTag tag) {
    }
}
