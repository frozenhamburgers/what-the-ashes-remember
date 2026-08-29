package net.jelly.echoesofwar.block;

import net.jelly.echoesofwar.entity.BossSummonWorldEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import team.lodestar.lodestone.modules.toolkit.worldevent.WorldEventHandler;

public abstract class PandorasBoxBlock<T extends PandorasBoxBlockEntity> extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    protected static final VoxelShape SHAPE_EAST_WEST = Block.box(5, 0, 2, 11, 6, 14);
    protected static final VoxelShape SHAPE_NORTH_SOUTH = Block.box(2, 0, 5, 14, 6, 11);


    protected PandorasBoxBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false));
    }

    protected abstract BossSummonWorldEvent createSummonEvent(Vec3 pos);

    protected abstract BlockEntityType<T> blockEntityType();

    protected boolean canOpen(Level level, BlockPos pos, Player player) {
        return true;
    }

    // shown when canOpen() refuses
    protected Component openFailureMessage() {
        return null;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.X ? SHAPE_EAST_WEST : SHAPE_NORTH_SOUTH;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return blockEntityType().create(pos, state);
    }

    @Override
    public <E extends BlockEntity> @Nullable BlockEntityTicker<E> getTicker(Level level, BlockState state, BlockEntityType<E> type) {
        return level.isClientSide() ? null : createTickerHelper(type, blockEntityType(), PandorasBoxBlockEntity::serverTick);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && !state.getValue(OPEN) && level.getBlockEntity(pos) instanceof PandorasBoxBlockEntity box) {
            if (!canOpen(level, pos, player)) {
                Component message = openFailureMessage();
                if (message != null) player.sendOverlayMessage(message);
                return InteractionResult.FAIL;
            }
            level.setBlock(pos, state.setValue(OPEN, true), Block.UPDATE_ALL);
            box.onBossSummoned();
            WorldEventHandler.addWorldEvent(level, createSummonEvent(Vec3.atCenterOf(pos)));
        }
        return InteractionResult.SUCCESS;
    }

    // using key on box
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!state.getValue(OPEN) && level.getBlockEntity(pos) instanceof PandorasBoxBlockEntity box
                && !box.isRunningKeyOpeningSequence() && stack.is(box.keyItem())) {
            if (!level.isClientSide()) {
                stack.shrink(1);
                level.setBlock(pos, state.setValue(OPEN, true), Block.UPDATE_ALL);
                box.beginKeyOpeningSequence();
            }
            return InteractionResult.SUCCESS;
        }

        // not our key (or an empty hand) - let the game fall through to useWithoutItem
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
}
