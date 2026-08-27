package net.jelly.echoesofwar.block;

import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animatable.stateless.StatelessGeoBlockEntity;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class PandorasBoxBlockEntity extends BlockEntity implements StatelessGeoBlockEntity {
    public static final String OPEN_ANIMATION = "open"; // name from pandora_open.animation.json
    public static final String CLOSE_ANIMATION = "close"; // name from pandora_close.animation.json

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);

    public PandorasBoxBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void open() {
        playAndHoldAnimation(OPEN_ANIMATION);
    }

    /** not called anywhere yet - for when a summon needs to be cancelled or reset. */
    public void playCloseAnimation() {
        playAndHoldAnimation(CLOSE_ANIMATION);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableCache;
    }
}
