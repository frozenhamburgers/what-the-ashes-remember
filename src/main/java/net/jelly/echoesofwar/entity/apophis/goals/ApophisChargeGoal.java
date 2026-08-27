package net.jelly.echoesofwar.entity.apophis.goals;

import net.jelly.echoesofwar.entity.apophis.ApophisEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class ApophisChargeGoal extends ApophisBurrowGoal {
    public static final double ATTACK_RANGE = 40.0;

    private static final double BREACH_STANDOFF = 17.0;

    private static final double COMMIT_RANGE = 27.0;

    private static final double ARC_HOMING = 0.125;

    private Vec3 breachPoint;
    private int previousStage = -1;

    public ApophisChargeGoal(ApophisEntity apophis) {
        super(apophis);
    }

    @Override
    protected double airborneSteering() {
        return ARC_HOMING;
    }

    @Override
    protected boolean maySteerDownward() {
        return true;
    }

    @Override
    public boolean canUse() {
        LivingEntity target = apophis.getTarget();
        if (target == null || !target.isAlive()) return false;
        return horizontalDistanceTo(target) <= ATTACK_RANGE;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        breachPoint = null;
        previousStage = -1;
    }

    @Override
    protected Vec3 goalPoint() {
        LivingEntity target = apophis.getTarget();
        if (target == null) return breachPoint;

        int stage = apophis.getChargeStage();
        boolean startingNewRun = stage == ApophisEntity.STAGE_DRIVE && previousStage != ApophisEntity.STAGE_DRIVE;
        previousStage = stage;

        if (stage == ApophisEntity.STAGE_BALLISTIC && !apophis.isBurrowed() && !apophis.hasStruckTargetThisRun()) {
            return target.position();
        }

        if (breachPoint == null || startingNewRun || horizontalDistanceTo(target) > COMMIT_RANGE) {
            breachPoint = breachPointFor(target);
        }
        return breachPoint;
    }

    private Vec3 breachPointFor(LivingEntity target) {
        Vec3 targetPos = target.position();
        Vec3 toTarget = targetPos.subtract(apophis.headPosition()).multiply(1, 0, 1);
        Vec3 approach = toTarget;
        if (approach.lengthSqr() < 1.0E-4) {
            approach = apophis.headDirection().multiply(1, 0, 1);
            if (approach.lengthSqr() < 1.0E-4) approach = new Vec3(1, 0, 0);
        }
        double standoff = Math.min(BREACH_STANDOFF, toTarget.length() * 0.5);
        return targetPos.subtract(approach.normalize().scale(standoff));
    }

    private double horizontalDistanceTo(LivingEntity target) {
        return target.position().subtract(apophis.headPosition()).horizontalDistance();
    }
}
