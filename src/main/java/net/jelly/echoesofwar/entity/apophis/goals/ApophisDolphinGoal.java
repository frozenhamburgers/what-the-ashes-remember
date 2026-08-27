package net.jelly.echoesofwar.entity.apophis.goals;

import net.jelly.echoesofwar.entity.apophis.ApophisEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

// travel: closes on anything further off than ApophisChargeGoal#ATTACK_RANGE by feeding
// ApophisBurrowGoal a rolling waypoint HOP_DISTANCE ahead instead of one distant point, so it
// porpoises across the landscape in a series of short arcs ("dolphining") rather than one huge dive
public class ApophisDolphinGoal extends ApophisBurrowGoal {
    /** how far ahead each hop aims */
    private static final double HOP_DISTANCE = 20.0;
    /** once this close to the current waypoint horizontally, pick the next */
    private static final double HOP_REFRESH_DISTANCE = 10.0;
    /** hysteresis on handover to charge goal to prevent oscillation */
    private static final double RELEASE_RANGE = ApophisChargeGoal.ATTACK_RANGE * 0.9;

    private Vec3 waypoint;

    public ApophisDolphinGoal(ApophisEntity apophis) {
        super(apophis);
    }

    @Override
    public boolean canUse() {
        return isChasing(ApophisChargeGoal.ATTACK_RANGE);
    }

    @Override
    public boolean canContinueToUse() {
        return isChasing(RELEASE_RANGE);
    }

    private boolean isChasing(double range) {
        LivingEntity target = apophis.getTarget();
        if (target == null || !target.isAlive()) return false;
        return target.position().subtract(apophis.headPosition()).horizontalDistance() > range;
    }

    @Override
    public void start() {
        waypoint = null;
    }

    @Override
    protected Vec3 goalPoint() {
        LivingEntity target = apophis.getTarget();
        if (target == null) return waypoint;

        Vec3 headPos = apophis.headPosition();
        if (waypoint == null || headPos.subtract(waypoint).horizontalDistance() <= HOP_REFRESH_DISTANCE) {
            Vec3 targetPos = target.position();
            // normalize happens in 3D before dropping the vertical component, so a target far above
            // or below shortens the hop - arcs bunch up to correct depth faster
            Vec3 hop = targetPos.subtract(headPos).normalize().multiply(HOP_DISTANCE, 0, HOP_DISTANCE);
            waypoint = new Vec3(headPos.x + hop.x, targetPos.y, headPos.z + hop.z);
        }
        return waypoint;
    }
}
