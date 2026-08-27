package net.jelly.echoesofwar.entity.apophis.goals;

import net.jelly.echoesofwar.entity.apophis.ApophisEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;

public class ApophisTargetPlayerGoal extends NearestAttackableTargetGoal<Player> {

    public ApophisTargetPlayerGoal(ApophisEntity apophis) {
        super(apophis, Player.class, false);
        this.targetConditions.ignoreLineOfSight();
    }
}
