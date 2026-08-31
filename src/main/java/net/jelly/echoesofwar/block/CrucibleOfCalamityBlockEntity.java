package net.jelly.echoesofwar.block;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CrucibleOfCalamityBlockEntity extends BlockEntity implements GeoBlockEntity {
    public static final String PULLEY_CONTROLLER = "pulley_controller";
    public static final String LIFT_ANIMATION = "lift_animation";

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);

    public CrucibleOfCalamityBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRUCIBLE_OF_CALAMITY.get(), pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(PULLEY_CONTROLLER, 0, test -> PlayState.STOP)
                .triggerableAnim(LIFT_ANIMATION, RawAnimation.begin().thenPlay(LIFT_ANIMATION)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableCache;
    }
}
