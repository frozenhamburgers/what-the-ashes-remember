package net.jelly.echoesofwar.block;

import com.mojang.serialization.MapCodec;
import net.jelly.echoesofwar.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

// invisible blocks to fill cells of crucible of calamity structure, forwards misery of man interaction to nearest controller cell in radius
public class CrucibleOfCalamityPartBlock extends Block {
    private static final MapCodec<CrucibleOfCalamityPartBlock> CODEC = simpleCodec(CrucibleOfCalamityPartBlock::new);

    private static final int CONTROLLER_SEARCH_RADIUS = 5;

    public CrucibleOfCalamityPartBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<CrucibleOfCalamityPartBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.is(ModItems.MISERY_OF_MAN.get())) {
            BlockPos controllerPos = findNearestController(level, pos);
            if (controllerPos != null) {
                if (!level.isClientSide() && level.getBlockState(controllerPos).getBlock() instanceof CrucibleOfCalamityBlock controller) {
                    controller.onMiseryOfManUsed(level, controllerPos, player);
                }
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    private static @Nullable BlockPos findNearestController(Level level, BlockPos from) {
        BlockPos nearest = null;
        double nearestDistSqr = Double.MAX_VALUE;

        for (BlockPos candidate : BlockPos.betweenClosed(
                from.offset(-CONTROLLER_SEARCH_RADIUS, -CONTROLLER_SEARCH_RADIUS, -CONTROLLER_SEARCH_RADIUS),
                from.offset(CONTROLLER_SEARCH_RADIUS, CONTROLLER_SEARCH_RADIUS, CONTROLLER_SEARCH_RADIUS))) {
            if (level.getBlockState(candidate).getBlock() instanceof CrucibleOfCalamityBlock) {
                double distSqr = candidate.distSqr(from);
                if (distSqr < nearestDistSqr) {
                    nearestDistSqr = distSqr;
                    nearest = candidate.immutable();
                }
            }
        }

        return nearest;
    }
}
