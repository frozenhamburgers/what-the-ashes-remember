package net.jelly.echoesofwar.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class ApophisBoxBlockEntity extends PandorasBoxBlockEntity {
    public ApophisBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.APOPHIS_PANDORAS_BOX.get(), pos, state);
    }
}
