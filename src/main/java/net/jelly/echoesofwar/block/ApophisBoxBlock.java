package net.jelly.echoesofwar.block;

import com.mojang.serialization.MapCodec;
import net.jelly.echoesofwar.entity.BossSummonWorldEvent;
import net.jelly.echoesofwar.entity.apophis.ApophisSummonWorldEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.Vec3;

public class ApophisBoxBlock extends PandorasBoxBlock<ApophisBoxBlockEntity> {
    private static final MapCodec<ApophisBoxBlock> CODEC = simpleCodec(ApophisBoxBlock::new);

    public ApophisBoxBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<ApophisBoxBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockEntityType<ApophisBoxBlockEntity> blockEntityType() {
        return ModBlockEntities.APOPHIS_PANDORAS_BOX.get();
    }

    @Override
    protected BossSummonWorldEvent createSummonEvent(Vec3 pos) {
        return new ApophisSummonWorldEvent().setPosition(pos);
    }

    @Override
    protected boolean canOpen(Level level, BlockPos pos, Player player) {
        return level.isBrightOutside() && level.canSeeSky(pos);
    }

    @Override
    protected Component openFailureMessage() {
        return Component.translatable("message.echoesofwar.apophis_pandoras_box.needs_daylight");
    }
}
