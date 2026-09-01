package net.jelly.echoesofwar.entity.trinity.attack;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * originally implemented as its own attack but removed cuz it kinda sucked
 */
public abstract class SpikeVolleyAttack extends TrinityAttack {

    protected abstract int minSpikes();
    protected abstract int maxSpikes();
    protected abstract int telegraphTicks();
    protected abstract int extendTicks();
    protected abstract int holdTicks();
    protected abstract int retractTicks();
    protected abstract int baseIntervalTicks();
    protected abstract float spikeLength();
    protected abstract float spikeRadius();
    protected abstract int slotType();

    protected abstract Vec3 aimPoint(Context ctx, Player target);

    private int spikes;
    private int interval;
    private int targetId = -1;

    private Vec3 @Nullable [] locked;

    @Override
    public int slotsNeeded(Context ctx) {
        return maxSpikes();
    }

    @Override
    public void start(Context ctx) {
        spikes = Mth.nextInt(ctx.random(), minSpikes(), maxSpikes());
        interval = Math.max(4, Math.round(baseIntervalTicks() / ctx.difficulty()));
        locked = new Vec3[spikes];

        Player target = ctx.pickTarget();
        targetId = target == null ? -1 : target.getId();
        markDirty();
    }

    private int spikeSpan() {
        return telegraphTicks() + extendTicks() + holdTicks() + retractTicks();
    }

    @Override
    public void tick(Context ctx, int age) {
        if (slots == null || locked == null) return;

        Player target = resolveTarget(ctx);
        boolean anyAlive = false;

        for (int i = 0; i < spikes && i < slots.length; i++) {
            AttackSlot slot = slots[i];
            int local = age - i * interval;

            if (local < 0 || local > spikeSpan()) {
                slot.clear();
                if (local < 0) anyAlive = true; // still to come
                continue;
            }
            anyAlive = true;
            driveSpike(ctx, slot, i, local, target);
        }

        if (!anyAlive) finished = true;
    }

    private void driveSpike(Context ctx, AttackSlot slot, int index, int local, @Nullable Player target) {
        int tg = telegraphTicks();
        int ex = extendTicks();
        int hold = holdTicks();
        int ret = retractTicks();

        Vec3 dir;
        if (local < tg) {
            dir = target != null
                    ? surfaceDirTowards(ctx, aimPoint(ctx, target))
                    : fallbackDir(ctx, index);
            slot.set(slotType(), dir, spikeLength(), spikeRadius(), seedFor(index));
            slot.telegraph = local / (float) tg;
            slot.extend = 0f;
            slot.fade = 0f;
            return;
        }

        if (locked[index] == null) { // locked behavior
            locked[index] = target != null
                    ? surfaceDirTowards(ctx, aimPoint(ctx, target))
                    : (slot.isActive() ? slot.dir : fallbackDir(ctx, index));
            markDirty();
        }
        dir = locked[index];
        slot.set(slotType(), dir, spikeLength(), spikeRadius(), seedFor(index));

        int since = local - tg;
        if (since < ex) {
            slot.telegraph = 1f;
            slot.extend = since / (float) ex;
            slot.fade = 0f;
        } else if (since < ex + hold) {
            slot.telegraph = 1f;
            slot.extend = 1f;
            slot.fade = 0f;
        } else {
            slot.telegraph = 1f;
            slot.extend = 1f;
            slot.fade = Mth.clamp((since - ex - hold) / (float) ret, 0f, 1f);
        }
    }

    private @Nullable Player resolveTarget(Context ctx) {
        if (targetId < 0) return null;
        return ctx.playerById(targetId);
    }


    private Vec3 fallbackDir(Context ctx, int index) {
        double a = index * 2.399963; // golden angle, so successive spikes never stack up
        return new Vec3(Math.cos(a), Math.sin(a * 0.5) * 0.4, Math.sin(a)).normalize();
    }

    private float seedFor(int index) {
        return index * 17.31f + slotType() * 3.7f;
    }

    @Override
    public void save(CompoundTag tag) {
        tag.putInt("spikes", spikes);
        tag.putInt("interval", interval);
        tag.putInt("target", targetId);
        if (locked == null) return;
        // locked directions cannot be recomputed by client
        CompoundTag l = new CompoundTag();
        for (int i = 0; i < locked.length; i++) {
            if (locked[i] == null) continue;
            l.putFloat(i + "x", (float) locked[i].x);
            l.putFloat(i + "y", (float) locked[i].y);
            l.putFloat(i + "z", (float) locked[i].z);
        }
        tag.put("locked", l);
    }

    @Override
    public void load(CompoundTag tag) {
        spikes = tag.getIntOr("spikes", 0);
        interval = Math.max(1, tag.getIntOr("interval", 1));
        targetId = tag.getIntOr("target", -1);
        locked = new Vec3[Math.max(spikes, 1)];
        tag.getCompound("locked").ifPresent(l -> {
            for (int i = 0; i < locked.length; i++) {
                if (l.getFloat(i + "x").isEmpty()) continue;
                locked[i] = new Vec3(l.getFloatOr(i + "x", 0f), l.getFloatOr(i + "y", 0f),
                        l.getFloatOr(i + "z", 0f));
            }
        });
    }
}
