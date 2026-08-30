package net.jelly.echoesofwar.block;

import net.jelly.echoesofwar.entity.apophis.ApophisEntity;
import net.jelly.echoesofwar.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class ApophisBoxBlockEntity extends PandorasBoxBlockEntity {
    public ApophisBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.APOPHIS_PANDORAS_BOX.get(), pos, state);
    }

    @Override
    protected Class<? extends LivingEntity> bossClass() {
        return ApophisEntity.class;
    }

    @Override
    protected Item keyItem() {
        return ModItems.KEY_OF_INDUSTRY.get();
    }

    @Override
    protected List<ItemStack> rewardItems() {
        return List.of(
                new ItemStack(ModItems.MISERY_OF_INDUSTRY.get())
//                new ItemStack(ModItems.HOPE_OF_PROGRESS.get())
        );
    }
}
