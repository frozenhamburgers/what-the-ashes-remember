package net.jelly.echoesofwar.entity.apophis.goals;

import net.jelly.echoesofwar.entity.apophis.ApophisEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

// Apophis with nothing to chase: Sink until it is buried, then coast to a stop and lie until can retarget
public class ApophisIdleGoal extends Goal {
    private static final double SINK_ACCELERATION = 0.0375;
    private static final double BRAKING = 0.05;

    private final ApophisEntity apophis;

    public ApophisIdleGoal(ApophisEntity apophis) {
        this.apophis = apophis;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return apophis.getTarget() == null;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void stop() {
        // the next attack should begin from a fresh powered charge
        apophis.setChargeStage(ApophisEntity.STAGE_DRIVE);
    }

    @Override
    public void tick() {
        if (!apophis.isBurrowed()) {
            apophis.applyAcceleration(new Vec3(0, -SINK_ACCELERATION, 0));
        } else {
            apophis.applyAcceleration(apophis.getChaseVelocity().scale(-BRAKING));
        }
    }
}
