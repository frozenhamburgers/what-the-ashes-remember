package net.jelly.echoesofwar.entity.trinity.attack;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Large beam that chases player.
 * Unlike the beam lattices, this one cannot be deterministically constructed from tick count,
 * needs player position
 */
public class SmokeLaserAttack extends TrinityAttack {

    private static final int TELEGRAPH_TICKS = 30;
    private static final int EXTEND_TICKS = 10;
    private static final int SUSTAIN_TICKS = 130;
    private static final int RETRACT_TICKS = 16;

    private static final float LENGTH = 260f;
    private static final float RADIUS = 9.5f;

    private static final double SLEW_PER_TICK = 0.014; // max angular velocity in radians/tick at difficulty 1.0

    private static final double AIM_INERTIA = 0.045;

    private static final int SYNC_EVERY = 4; // server sync every _ ticks

    private int targetId = -1;
    private Vec3 dir = new Vec3(0, 1, 0);
    private Vec3 aim = new Vec3(0, 1, 0);

    @Override
    public int slotsNeeded(Context ctx) {
        return 1;
    }

    @Override
    public void start(Context ctx) {
        Player target = ctx.pickTarget();
        targetId = target == null ? -1 : target.getId();
        dir = target != null ? surfaceDirTowards(ctx, aimAt(target)) : new Vec3(0, 1, 0);
        aim = dir;
        markDirty();
    }

    private int span() {
        return TELEGRAPH_TICKS + EXTEND_TICKS + SUSTAIN_TICKS + RETRACT_TICKS;
    }

    @Override
    public void tick(Context ctx, int age) {
        if (slots == null || slots.length == 0) return;
        if (age > span()) {
            finished = true;
            clearSlots();
            return;
        }

        Player target = targetId < 0 ? null : ctx.playerById(targetId);
        if (target != null) track(ctx, target);

        AttackSlot slot = slots[0];
        slot.set(AttackSlot.TYPE_LASER, dir, LENGTH, RADIUS, 5.5f);

        if (age < TELEGRAPH_TICKS) {
            slot.telegraph = age / (float) TELEGRAPH_TICKS;
            slot.extend = 0f;
            slot.fade = 0f;
        } else if (age < TELEGRAPH_TICKS + EXTEND_TICKS) {
            slot.telegraph = 1f;
            slot.extend = (age - TELEGRAPH_TICKS) / (float) EXTEND_TICKS;
            slot.fade = 0f;
        } else if (age < TELEGRAPH_TICKS + EXTEND_TICKS + SUSTAIN_TICKS) {
            slot.telegraph = 1f;
            slot.extend = 1f;
            slot.fade = 0f;
        } else {
            slot.telegraph = 1f;
            slot.extend = 1f;
            slot.fade = Mth.clamp(
                    (age - TELEGRAPH_TICKS - EXTEND_TICKS - SUSTAIN_TICKS) / (float) RETRACT_TICKS,
                    0f, 1f);
        }

        if (age % SYNC_EVERY == 0) markDirty();
    }

    private void track(Context ctx, Player target) {
        Vec3 toPlayer = surfaceDirTowards(ctx, aimAt(target));
        aim = slerp(aim, toPlayer, AIM_INERTIA);
        dir = slerpCapped(dir, aim, SLEW_PER_TICK * ctx.difficulty());
    }

    private static Vec3 slerp(Vec3 from, Vec3 to, double t) {
        double angle = Math.acos(Mth.clamp(from.dot(to), -1.0, 1.0));
        if (angle < 1.0e-4) return to;
        double s = Math.sin(angle);
        return from.scale(Math.sin((1 - t) * angle) / s)
                .add(to.scale(Math.sin(t * angle) / s))
                .normalize();
    }

    private static Vec3 slerpCapped(Vec3 from, Vec3 to, double maxStep) {
        double angle = Math.acos(Mth.clamp(from.dot(to), -1.0, 1.0));
        if (angle < 1.0e-4 || angle <= maxStep) return to;
        return slerp(from, to, maxStep / angle);
    }

    @Override
    public void save(CompoundTag tag) {
        tag.putInt("target", targetId);
        tag.putFloat("dx", (float) dir.x);
        tag.putFloat("dy", (float) dir.y);
        tag.putFloat("dz", (float) dir.z);
        // sync aim point too
        tag.putFloat("mx", (float) aim.x);
        tag.putFloat("my", (float) aim.y);
        tag.putFloat("mz", (float) aim.z);
    }

    @Override
    public void load(CompoundTag tag) {
        targetId = tag.getIntOr("target", -1);
        dir = unit(tag.getFloatOr("dx", 0f), tag.getFloatOr("dy", 1f), tag.getFloatOr("dz", 0f));
        aim = unit(tag.getFloatOr("mx", 0f), tag.getFloatOr("my", 1f), tag.getFloatOr("mz", 0f));
    }

    private static Vec3 unit(float x, float y, float z) {
        Vec3 v = new Vec3(x, y, z);
        return v.lengthSqr() < 1.0e-8 ? new Vec3(0, 1, 0) : v.normalize();
    }
}
