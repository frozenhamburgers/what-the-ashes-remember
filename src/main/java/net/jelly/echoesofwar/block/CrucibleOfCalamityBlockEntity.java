package net.jelly.echoesofwar.block;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CrucibleOfCalamityBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);

    public CrucibleOfCalamityBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRUCIBLE_OF_CALAMITY.get(), pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableCache;
    }
}
