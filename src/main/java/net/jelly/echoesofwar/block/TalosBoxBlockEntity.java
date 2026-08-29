package net.jelly.echoesofwar.block;

import net.jelly.echoesofwar.entity.talos.TalosEntity;
import net.jelly.echoesofwar.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class TalosBoxBlockEntity extends PandorasBoxBlockEntity {
    public TalosBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TALOS_PANDORAS_BOX.get(), pos, state);
    }

    @Override
    protected Class<? extends LivingEntity> bossClass() {
        return TalosEntity.class;
    }

    @Override
    protected Item keyItem() {
        return ModItems.KEY_OF_CONQUEST.get();
    }

    @Override
    protected List<ItemStack> rewardItems() {
        return List.of(
                new ItemStack(ModItems.MISERY_OF_CONQUEST.get()),
                new ItemStack(ModItems.HOPE_OF_CREATION.get())
        );
    }
}
