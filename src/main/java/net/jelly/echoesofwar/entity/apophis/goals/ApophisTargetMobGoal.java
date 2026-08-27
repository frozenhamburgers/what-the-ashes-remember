package net.jelly.echoesofwar.entity.apophis.goals;

import net.jelly.echoesofwar.entity.apophis.ApophisEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;

// fallback targeting so Apophis stays active with no player around, rather than settling underground.
// yields to ApophisTargetPlayerGoal the moment a player comes in range.
public class ApophisTargetMobGoal extends NearestAttackableTargetGoal<LivingEntity> {
    private final ApophisEntity apophis;

    public ApophisTargetMobGoal(ApophisEntity apophis) {
        super(apophis, LivingEntity.class, 10, false, false,
                (target, level) -> !(target instanceof Player) && !(target instanceof ApophisEntity));
        this.apophis = apophis;
        this.targetConditions.ignoreLineOfSight();
    }

    @Override
    public boolean canUse() {
        return !hasPlayerInRange() && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return !hasPlayerInRange() && super.canContinueToUse();
    }

    private boolean hasPlayerInRange() {
        return apophis.level().getNearestPlayer(apophis.getX(), apophis.getY(), apophis.getZ(), getFollowDistance(), true) != null;
    }
}
