package net.jelly.echoesofwar.entity.nuclear.fx;

import java.util.Map;

import net.jelly.echoesofwar.Config;
import net.jelly.echoesofwar.EchoesofWar;
import net.minecraft.resources.Identifier;
import team.lodestar.lodestone.modules.rendering.postprocess.MultiInstancePostProcessor;

// drives the detonation post chain (post/nuclear/bomb_block/chain.json): a low-res raymarch of the volume, a
// quarter-size bloom pyramid over its hot core, and a full-res composite that also carries the
// detonation flash.
//
// simpler than ApophisPostProcessor: no external targets, no extra uniform block, and no reason to
// stay awake with no instances - nothing in this chain draws anything when there is no detonation
public class NuclearDetonationPostProcessor extends MultiInstancePostProcessor<NuclearDetonationFx> {
    public static final NuclearDetonationPostProcessor INSTANCE = new NuclearDetonationPostProcessor();

    @Override
    public Identifier getPostChainLocation() {
        return Identifier.fromNamespaceAndPath(EchoesofWar.MODID, "nuclear/bomb_block/chain");
    }

    @Override
    protected int getMaxInstances() {
        return 2;
    }

    @Override
    protected int getDataSizePerInstance() {
        return NuclearDetonationFx.DATA_SIZE;
    }

    // shares the one volumetric resolution knob with the Apophis chain rather than adding a second
    @Override
    protected Map<String, Float> getScaledTargets() {
        float volumetric = (float) (double) Config.VOLUMETRIC_RESOLUTION_SCALE.get();
        // the impact frames are a hard black/white threshold over a long radial blur, so half res
        // costs nothing visually and keeps the 17-tap depth reconstruction in edges.fsh affordable
        return Map.of(
                "nuclear_low_res", volumetric,
                "nuclear_bloom_a", 0.25f,
                "nuclear_bloom_b", 0.25f,
                "nuclear_impact", 0.5f);
    }

    // nothing to reset between frames - unlike the Apophis chain there is no visibility flag or
    // external target whose state has to be consumed once the chain has run
    @Override
    public void afterProcess() {
    }
}
