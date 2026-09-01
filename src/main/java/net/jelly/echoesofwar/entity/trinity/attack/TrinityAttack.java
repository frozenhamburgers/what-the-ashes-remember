package net.jelly.echoesofwar.entity.trinity.attack;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * One attack pattern Trinity uses
 * Attacks run on the server, write geometry into AttackSlot, as for sync when something discrete happens
 * Continuous and deterministic motion is rederived on client
 * isDirty is not set every tick, and rather set manually.
 */
public abstract class TrinityAttack {

    /** context an attack needs from Trinity */
    public interface Context {
        Vec3 centre();
        float bodyRadius();
        float difficulty();
        RandomSource random();
        @Nullable Player pickTarget(); // random target in range
        @Nullable Player playerById(int id);
        BulletField bulletField();
    }

    protected boolean dirty;
    protected boolean finished;

    /** attack's owned slots */
    protected AttackSlot[] slots;

    public final void bind(AttackSlot[] slots) {
        this.slots = slots;
    }

    public abstract int slotsNeeded(Context ctx);

    /** called once server side once this attack is chosen */
    public abstract void start(Context ctx);

    /** Called every tick on BOTH sides. MUST be deterministic given the same synced state. */
    public abstract void tick(Context ctx, int age);

    /** true once the attack has fully retracted, can choose new attack. */
    public boolean isFinished() {
        return finished;
    }

    /** true means a discrete change the client cannot infer */
    public final boolean isDirty() {
        boolean d = dirty;
        dirty = false;
        return d;
    }

    protected final void markDirty() {
        dirty = true;
    }

    /** Release every slot, call when attack ends or cult short */
    public void clearSlots() {
        if (slots == null) return;
        for (AttackSlot slot : slots) slot.clear();
    }

    /** Whatever the client cannot re-derive. Note that Geometry should already exist in the slots. */
    public abstract void save(CompoundTag tag);

    public abstract void load(CompoundTag tag);

    // point o Trinity's surface attack aimed at target should come from.
    protected static Vec3 surfaceDirTowards(Context ctx, Vec3 target) {
        Vec3 d = target.subtract(ctx.centre());
        double len = d.length();
        return len < 1.0e-4 ? new Vec3(0, 1, 0) : d.scale(1.0 / len);
    }

    protected static Vec3 lead(Player player, double ticks) {
        return player.position().add(player.getDeltaMovement().scale(ticks));
    }

    // aim at body center not feet
    protected static Vec3 aimAt(Player player) {
        return player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
    }
}
