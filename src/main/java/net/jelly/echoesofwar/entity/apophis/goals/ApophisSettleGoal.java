package net.jelly.echoesofwar.entity.apophis.goals;

import net.jelly.echoesofwar.entity.apophis.ApophisEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

// runs once after a flight ends: falls out of the sky steering toward the target, dives underneath
// them, then coils in a tight circle so the whole body is pulled underground before handing control
// back to the ground goals (which alone don't dive deep enough to bury a body this long)
public class ApophisSettleGoal extends Goal {
    private static final double FALL_GRAVITY = 0.0375;
    private static final double FALL_STEERING = 0.05;

    private static final double SETTLE_START_DEPTH = 20.0;
    private static final double SETTLE_END_DEPTH = 80.0;
    private static final double DIVE_ACCELERATION = 0.2 * ApophisEntity.SPEED_SCALE;
    private static final double DIVE_ARRIVAL_DISTANCE = 5.0;

    private static final double MEANDER_RADIUS = 65.0;
    private static final double MEANDER_ANGULAR_VELOCITY = 0.05;
    private static final int MEANDER_DURATION_TICKS = 80;

    private enum Phase { FALL, DIVE, MEANDER }

    private final ApophisEntity apophis;
    private Phase phase;
    private boolean finished;
    private int meanderTicks;
    private double meanderAngle;

    public ApophisSettleGoal(ApophisEntity apophis) {
        this.apophis = apophis;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return apophis.isSettlePending();
    }

    @Override
    public boolean canContinueToUse() {
        return !finished;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        phase = apophis.isBurrowed() ? Phase.DIVE : Phase.FALL;
        finished = false;
        meanderTicks = 0;
        meanderAngle = apophis.getRandom().nextDouble() * Math.PI * 2.0;
    }

    @Override
    public void stop() {
        apophis.clearSettlePending();
        apophis.setChaseVelocity(Vec3.ZERO);
        apophis.setChargeStage(ApophisEntity.STAGE_DRIVE);
    }

    @Override
    public void tick() {
        if (finished) return;
        switch (phase) {
            case FALL -> tickFall();
            case DIVE -> tickDive();
            case MEANDER -> tickMeander();
        }
    }

    private void tickFall() {
        apophis.applyAcceleration(new Vec3(0, -FALL_GRAVITY, 0));

        LivingEntity target = apophis.getTarget();
        if (target != null) {
            Vec3 toTarget = target.position().subtract(apophis.headPosition()).multiply(1, 0, 1);
            if (toTarget.lengthSqr() > 1.0) {
                apophis.applyAcceleration(toTarget.normalize().scale(FALL_STEERING));
            }
        }

        if (apophis.isBurrowed()) {
            phase = Phase.DIVE;
        }
    }

    private void tickDive() {
        Vec3 goal = settlePoint(SETTLE_START_DEPTH);
        Vec3 toGoal = goal.subtract(apophis.headPosition());
        if (toGoal.lengthSqr() <= DIVE_ARRIVAL_DISTANCE * DIVE_ARRIVAL_DISTANCE) {
            phase = Phase.MEANDER;
            meanderTicks = 0;
            return;
        }
        apophis.applyAcceleration(toGoal.normalize().scale(DIVE_ACCELERATION));
    }

    /** the point depth blocks below the live target, or below the head itself if there is none */
    private Vec3 settlePoint(double depth) {
        LivingEntity target = apophis.getTarget();
        Vec3 basis = target != null ? target.position() : apophis.headPosition();
        return new Vec3(basis.x, basis.y - depth, basis.z);
    }

    // orbits settlePoint() at MEANDER_RADIUS, sinking from SETTLE_START_DEPTH to SETTLE_END_DEPTH
    private void tickMeander() {
        meanderTicks++;
        meanderAngle += MEANDER_ANGULAR_VELOCITY;

        double progress = Mth.clamp(meanderTicks / (double) MEANDER_DURATION_TICKS, 0.0, 1.0);
        double depth = Mth.lerp(progress, SETTLE_START_DEPTH, SETTLE_END_DEPTH);

        Vec3 center = settlePoint(depth);
        Vec3 offset = new Vec3(Math.cos(meanderAngle), 0, Math.sin(meanderAngle)).scale(MEANDER_RADIUS);
        Vec3 desiredPos = center.add(offset);

        apophis.setChaseVelocity(desiredPos.subtract(apophis.headPosition()));

        if (meanderTicks >= MEANDER_DURATION_TICKS) {
            finished = true;
        }
    }
}
