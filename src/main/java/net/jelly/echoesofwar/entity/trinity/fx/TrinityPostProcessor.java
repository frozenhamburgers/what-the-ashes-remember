package net.jelly.echoesofwar.entity.trinity.fx;

import java.util.Map;

import net.jelly.echoesofwar.Config;
import net.jelly.echoesofwar.EchoesofWar;
import net.minecraft.resources.Identifier;
import team.lodestar.lodestone.modules.rendering.postprocess.MultiInstancePostProcessor;

public class TrinityPostProcessor extends MultiInstancePostProcessor<TrinityFx> {
    public static final TrinityPostProcessor INSTANCE = new TrinityPostProcessor();

    @Override
    public Identifier getPostChainLocation() {
        return Identifier.fromNamespaceAndPath(EchoesofWar.MODID, "nuclear/trinity/chain");
    }

    @Override
    protected int getMaxInstances() {
        return 1;
    }

    @Override
    protected int getDataSizePerInstance() {
        return TrinityFx.DATA_SIZE;
    }

    @Override
    protected Map<String, Float> getScaledTargets() {
        float volumetric = (float) (double) Config.VOLUMETRIC_RESOLUTION_SCALE.get();
        return Map.of(
                "trinity_low_res", volumetric,
                "trinity_bloom_a", 0.25f,
                "trinity_bloom_b", 0.25f,
                "trinity_impact", 0.5f);
    }

    @Override
    public void afterProcess() {
    }
}
