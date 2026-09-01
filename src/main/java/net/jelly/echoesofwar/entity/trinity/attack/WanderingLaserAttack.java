package net.jelly.echoesofwar.entity.trinity.attack;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Twenty beams drifting independently across Trinity's surface, forever.
 *
 * Synced via one int deterministically: axis, rate, period and phase comes off its hash
 */
public class WanderingLaserAttack extends TrinityAttack {

    private static final int BEAMS = 20;

    private static final int TELEGRAPH_TICKS = 30;
    private static final int EXTEND_TICKS = 10;
    private static final int RETRACT_TICKS = 16;

    private static final int SUSTAIN_MIN = 150;
    private static final int SUSTAIN_MAX = 320;

    private static final float LENGTH = 200f;
    private static final float RADIUS = 5.5f;

    // drift rate range, radians/tick, when difficulty is 1.0
    private static final double OMEGA_MIN = 0.010;
    private static final double OMEGA_MAX = 0.030;

    private int seed;

    @Override
    public int slotsNeeded(Context ctx) {
        return BEAMS;
    }

    @Override
    public void start(Context ctx) {
        seed = ctx.random().nextInt();
        markDirty();
    }

    @Override
    public boolean isFinished() {
        return false;
    } // system not temporary event, like projectiles

    @Override
    public void tick(Context ctx, int age) {
        if (slots == null) return;

        for (int i = 0; i < BEAMS && i < slots.length; i++) {
            AttackSlot slot = slots[i];

            int period = beamPeriod(i);
            int offset = hashRange(seed, i, 7717, 0, period);
            int t = age + offset;
            int generation = Math.floorDiv(t, period);
            int local = Math.floorMod(t, period);

            float telegraph, extend, fade;
            int sustain = period - TELEGRAPH_TICKS - EXTEND_TICKS - RETRACT_TICKS;
            if (local < TELEGRAPH_TICKS) {
                telegraph = local / (float) TELEGRAPH_TICKS;
                extend = 0f;
                fade = 0f;
            } else if (local < TELEGRAPH_TICKS + EXTEND_TICKS) {
                telegraph = 1f;
                extend = (local - TELEGRAPH_TICKS) / (float) EXTEND_TICKS;
                fade = 0f;
            } else if (local < TELEGRAPH_TICKS + EXTEND_TICKS + sustain) {
                telegraph = 1f;
                extend = 1f;
                fade = 0f;
            } else {
                telegraph = 1f;
                extend = 1f;
                fade = Mth.clamp(
                        (local - TELEGRAPH_TICKS - EXTEND_TICKS - sustain) / (float) RETRACT_TICKS,
                        0f, 1f);
            }

            // drift only starts when beam materializes
            double drift = Math.max(local - TELEGRAPH_TICKS, 0) * ctx.difficulty();
            Vec3 dir = beamDir(i, generation, drift);

            slot.set(AttackSlot.TYPE_WANDERING, dir, LENGTH, RADIUS,
                    i * 9.41f + generation * 3.17f + 2.3f);
            slot.telegraph = telegraph;
            slot.extend = extend;
            slot.fade = fade;
        }
        for (int i = BEAMS; i < slots.length; i++) slots[i].clear();
    }

    // ---------------------------- GEOMETRY

    // full cycle length for beam i
    private int beamPeriod(int i) {
        return TELEGRAPH_TICKS + EXTEND_TICKS + RETRACT_TICKS
                + hashRange(seed, i, 3313, SUSTAIN_MIN, SUSTAIN_MAX);
    }

    // where beam i points drift ticks into generation g
    private Vec3 beamDir(int i, int g, double drift) {
        // start direction computed by fibonacci sphere indexed by permuted slot for uniformity
        // since unfirom sampling has no memory this works better especially when recycling beams
        int slotIndex = hashRange(seed, i, 5051 + g * 131, 0, BEAMS);
        Vec3 start = fibonacci(slotIndex, BEAMS);

        // drift access cant be parallel to start direction
        Vec3 a = hashUnit(seed, i, 8191 + g * 977);
        Vec3 perp = a.subtract(start.scale(a.dot(start)));
        Vec3 axis = perp.lengthSqr() < 1.0e-6 ? anyPerpendicular(start) : perp.normalize();

        double omega = Mth.lerp(hashUnit01(seed, i, 6151 + g * 613), OMEGA_MIN, OMEGA_MAX)
                * ((hash(seed, i, 4093 + g * 449) & 1) == 0 ? 1 : -1);
        return rotateAbout(start, axis, omega * drift);
    }

    // ith of n pointos on a fibonacci sphere, latitude stepped by sin of polar angle and longitude by golden angle
    // works in three dimensions, unlike previous naive even step angle
    private static Vec3 fibonacci(int i, int n) {
        double y = 1.0 - 2.0 * (i + 0.5) / n;
        double r = Math.sqrt(Math.max(0.0, 1.0 - y * y));
        double phi = i * 2.399963229728653; // golden angle
        return new Vec3(r * Math.cos(phi), y, r * Math.sin(phi));
    }

    private static Vec3 anyPerpendicular(Vec3 v) {
        Vec3 pick = Math.abs(v.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        return v.cross(pick).normalize();
    }

    // rotate of v about k by ang via rogrigues rotation formula
    private static Vec3 rotateAbout(Vec3 v, Vec3 k, double ang) {
        double c = Math.cos(ang), s = Math.sin(ang);
        return v.scale(c).add(k.cross(v).scale(s)).add(k.scale(k.dot(v) * (1.0 - c)));
    }

    // ------------------------------------ HASHING
    // integer avalanche for same reason as projectile field: has to be recomputable from any tick from both sides
    private static int hash(int seed, int i, int salt) {
        int x = seed ^ (i * 0x9E3779B9) ^ (salt * 0x85EBCA6B);
        x ^= x >>> 16;
        x *= 0x7feb352d;
        x ^= x >>> 15;
        x *= 0x846ca68b;
        x ^= x >>> 16;
        return x;
    }

    private static double hashUnit01(int seed, int i, int salt) {
        return (hash(seed, i, salt) >>> 8) * (1.0 / 16777216.0);
    }

    private static int hashRange(int seed, int i, int salt, int min, int max) {
        if (max <= min) return min;
        return min + (int) (hashUnit01(seed, i, salt) * (max - min));
    }

    private static Vec3 hashUnit(int seed, int i, int salt) {
        double z = hashUnit01(seed, i, salt) * 2.0 - 1.0;
        double a = hashUnit01(seed, i, salt + 1) * Math.PI * 2.0;
        double r = Math.sqrt(Math.max(0.0, 1.0 - z * z));
        return new Vec3(r * Math.cos(a), z, r * Math.sin(a));
    }


    @Override
    public void save(CompoundTag tag) {
        tag.putInt("seed", seed);
    }

    @Override
    public void load(CompoundTag tag) {
        seed = tag.getIntOr("seed", 0);
    }
}
