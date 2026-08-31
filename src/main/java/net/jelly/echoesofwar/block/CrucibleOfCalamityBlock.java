package net.jelly.echoesofwar.block;

import com.mojang.serialization.MapCodec;
import net.jelly.echoesofwar.item.ModItems;
import net.jelly.echoesofwar.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

// controller cell of the crucible multiblock
public class CrucibleOfCalamityBlock extends BaseEntityBlock {
    private static final MapCodec<CrucibleOfCalamityBlock> CODEC = simpleCodec(CrucibleOfCalamityBlock::new);

    public CrucibleOfCalamityBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<CrucibleOfCalamityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrucibleOfCalamityBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.is(ModItems.MISERY_OF_MAN.get())) {
            if (!level.isClientSide()) {
                onMiseryOfManUsed(level, pos, player);
            }
            return InteractionResult.SUCCESS;
        }

        player.sendOverlayMessage(Component.translatable("message.echoesofwar.crucible_of_calamity.interaction_failure"));
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    // TODO: hook for using misery of man on block
    protected void onMiseryOfManUsed(Level level, BlockPos pos, Player player) {
        player.sendSystemMessage(Component.translatable("message.echoesofwar.crucible_of_calamity.misery_used"));

        if (level.getBlockEntity(pos) instanceof CrucibleOfCalamityBlockEntity blockEntity) {
            blockEntity.triggerAnim(CrucibleOfCalamityBlockEntity.PULLEY_CONTROLLER, CrucibleOfCalamityBlockEntity.LIFT_ANIMATION);
            level.playSound(null, pos, ModSounds.MECHANICAL_CREAK.get(), SoundSource.BLOCKS, 9F, 1.0F);
        }
    }
}
