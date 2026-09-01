package net.jelly.echoesofwar.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public class DestroyerOfWorldsBlock extends BaseEntityBlock {
    private static final MapCodec<DestroyerOfWorldsBlock> CODEC = simpleCodec(DestroyerOfWorldsBlock::new);

    public DestroyerOfWorldsBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<DestroyerOfWorldsBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DestroyerOfWorldsBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                            BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, ModBlockEntities.DESTROYER_OF_WORLDS.get(),
                DestroyerOfWorldsBlockEntity::serverTick);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (oldState.is(state.getBlock())) return;
        armIfPowered(level, pos);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                   @Nullable Orientation orientation, boolean movedByPiston) {
        armIfPowered(level, pos);
    }

    private static void armIfPowered(Level level, BlockPos pos) {
        if (level.isClientSide()) return;
        if (!level.hasNeighborSignal(pos)) return;
        if (level.getBlockEntity(pos) instanceof DestroyerOfWorldsBlockEntity bomb) bomb.arm();
    }
}
