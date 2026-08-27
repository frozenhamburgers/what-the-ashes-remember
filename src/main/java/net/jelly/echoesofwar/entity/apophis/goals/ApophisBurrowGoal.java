package net.jelly.echoesofwar.entity.apophis.goals;

import net.jelly.echoesofwar.entity.apophis.ApophisEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

// two-stage burrow-and-breach cycle shared by every movement goal below. a subclass just supplies a
// goal point; this drives it there. STAGE_DRIVE thrusts straight at the point until the goal drifts
// too far off-facing or the head breaks the surface, then hands over to STAGE_BALLISTIC (a committed
// arc with little to no steering) until it's back underground, clear of the goal and deep enough to
// line up another charge. Apophis can't hover or circle, so this cycle is the whole shape of the fight.
public abstract class ApophisBurrowGoal extends Goal {
    /** thrust applied per tick while tunneling under power */
    private static final double DRIVE_ACCELERATION = 0.1 * ApophisEntity.SPEED_SCALE;
    /** gravity applied per tick to an airborne breach arc */
    private static final double AIRBORNE_GRAVITY = 0.0375;
    /** the token mid-air steering nudge - small enough that a breach still reads as a committed leap */
    private static final double AIRBORNE_STEERING = 0.01;
    /** downward thrust while burrowed with no charge lined up yet, to gain the depth for the next one */
    private static final double DIVE_ACCELERATION = 0.1 * ApophisEntity.SPEED_SCALE;

    /** below this dot product between "way to the goal" and "way it's moving", the charge is spent */
    private static final double CHARGE_SPENT_DOT = 0.25;
    /** how far past the goal, horizontally, the dive has to get before another charge can line up */
    private static final double RELOCK_MIN_DISTANCE = 4.0 * ApophisEntity.SPEED_SCALE;
    /** and how far below it, so the next charge comes up from underneath rather than sideways */
    private static final double RELOCK_MIN_DEPTH = 30.0 * ApophisEntity.SPEED_SCALE;
    /** a goal higher than this above the head is out of reach of midair steering, so don't bother */
    private static final double AIRBORNE_STEERING_CEILING = 20.0;
    /** mid-leap, a target this far overhead is unreachable*/
    private static final double ABANDON_TARGET_HEIGHT = 30.0;

    protected final ApophisEntity apophis;

    protected ApophisBurrowGoal(ApophisEntity apophis) {
        this.apophis = apophis;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /** where this goal wants the head to go this tick, or null to coast */
    protected abstract Vec3 goalPoint();

    /** how hard the arc may bend while airborne, overridden by charge goal */
    protected double airborneSteering() {
        return AIRBORNE_STEERING;
    }

    /** whether midair steering may aim below the horizon, makes boss kinda really hard */
    protected boolean maySteerDownward() {
        return false;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        Vec3 goal = goalPoint();
        if (goal == null) return;

        Vec3 headPos = apophis.headPosition();
        Vec3 toGoal = goal.subtract(headPos).normalize();

        // not else-if: a charge that runs out mid-tick should start its arc on the same tick rather
        // than spending one doing nothing
        if (apophis.getChargeStage() == ApophisEntity.STAGE_DRIVE) {
            tickDrive(toGoal);
        }
        if (apophis.getChargeStage() == ApophisEntity.STAGE_BALLISTIC) {
            tickBallistic(goal, toGoal, headPos);
        }
    }

    private void tickDrive(Vec3 toGoal) {
        apophis.applyAcceleration(toGoal.scale(DRIVE_ACCELERATION));

        Vec3 velocity = apophis.getChaseVelocity();
        boolean chargedPast = velocity.lengthSqr() > 1.0E-6 && toGoal.dot(velocity.normalize()) < CHARGE_SPENT_DOT;
        if (chargedPast || !apophis.isBurrowed()) {
            apophis.setChargeStage(ApophisEntity.STAGE_BALLISTIC);
        }
    }

    private void tickBallistic(Vec3 goal, Vec3 toGoal, Vec3 headPos) {
        if (!apophis.isBurrowed()) {
            if (targetHeightAbove(headPos) > ABANDON_TARGET_HEIGHT) apophis.setTarget(null);

            apophis.applyAcceleration(new Vec3(0, -AIRBORNE_GRAVITY, 0));

            Vec3 steer = toGoal.y < 0 && !maySteerDownward() ? new Vec3(toGoal.x, 0, toGoal.z) : toGoal;
            if (goal.y - headPos.y < AIRBORNE_STEERING_CEILING) {
                apophis.applyAcceleration(steer.normalize().scale(airborneSteering()));
            }
        } else if (headPos.distanceTo(goal) > RELOCK_MIN_DISTANCE && goal.y - headPos.y >= RELOCK_MIN_DEPTH) {
            apophis.setChaseVelocity(Vec3.ZERO);
            apophis.setChargeStage(ApophisEntity.STAGE_DRIVE);
        } else {
            apophis.applyAcceleration(new Vec3(0, -DIVE_ACCELERATION, 0));
        }
    }

    /** how far above the head the live target is, or 0 if there isn't one */
    private double targetHeightAbove(Vec3 headPos) {
        return apophis.getTarget() == null ? 0.0 : apophis.getTarget().getY() - headPos.y;
    }
}
