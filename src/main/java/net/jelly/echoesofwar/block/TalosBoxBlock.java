package net.jelly.echoesofwar.block;

import com.mojang.serialization.MapCodec;
import net.jelly.echoesofwar.entity.BossSummonWorldEvent;
import net.jelly.echoesofwar.entity.talos.TalosSummonWorldEvent;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.Vec3;

public class TalosBoxBlock extends PandorasBoxBlock<TalosBoxBlockEntity> {
    private static final MapCodec<TalosBoxBlock> CODEC = simpleCodec(TalosBoxBlock::new);

    public TalosBoxBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<TalosBoxBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockEntityType<TalosBoxBlockEntity> blockEntityType() {
        return ModBlockEntities.TALOS_PANDORAS_BOX.get();
    }

    @Override
    protected BossSummonWorldEvent createSummonEvent(Vec3 pos) {
        return new TalosSummonWorldEvent().setPosition(pos);
    }
}
