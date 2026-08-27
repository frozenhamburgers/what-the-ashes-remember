package net.jelly.echoesofwar.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class TalosBoxBlockEntity extends PandorasBoxBlockEntity {
    public TalosBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TALOS_PANDORAS_BOX.get(), pos, state);
    }
}
