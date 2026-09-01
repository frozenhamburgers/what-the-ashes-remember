package net.jelly.echoesofwar.entity.trinity.attack;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * spike attack that extends at the player's predicted future position
 */
public class PredictiveSpikeAttack extends SpikeVolleyAttack {

    private static final int MIN_SPIKES = 2;
    private static final int MAX_SPIKES = 6;

    private static final int TELEGRAPH_TICKS = 18;
    private static final int EXTEND_TICKS = 9;
    private static final int HOLD_TICKS = 4;
    private static final int RETRACT_TICKS = 8;
    private static final int BASE_INTERVAL_TICKS = 26;

    private static final float LENGTH = 170f;
    private static final float RADIUS = 3.2f;

    private static final double LEAD_TICKS = 22.0; // how far ahead to aim in ticks
    private static final double LEAD_CAP = 60.0;

    @Override protected int minSpikes() { return MIN_SPIKES; }
    @Override protected int maxSpikes() { return MAX_SPIKES; }
    @Override protected int telegraphTicks() { return TELEGRAPH_TICKS; }
    @Override protected int extendTicks() { return EXTEND_TICKS; }
    @Override protected int holdTicks() { return HOLD_TICKS; }
    @Override protected int retractTicks() { return RETRACT_TICKS; }
    @Override protected int baseIntervalTicks() { return BASE_INTERVAL_TICKS; }
    @Override protected float spikeLength() { return LENGTH; }
    @Override protected float spikeRadius() { return RADIUS; }
    @Override protected int slotType() { return AttackSlot.TYPE_PREDICTIVE; }

    @Override
    protected Vec3 aimPoint(Context ctx, Player target) {
        Vec3 from = aimAt(target);
        Vec3 offset = target.getDeltaMovement().scale(LEAD_TICKS);
        if (offset.length() > LEAD_CAP) offset = offset.normalize().scale(LEAD_CAP);
        return from.add(offset);
    }
}
