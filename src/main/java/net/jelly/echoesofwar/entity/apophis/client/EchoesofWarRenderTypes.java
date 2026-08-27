package net.jelly.echoesofwar.entity.apophis.client;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import net.jelly.echoesofwar.EchoesofWar;
import net.minecraft.util.Util;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

import java.util.function.Function;

@EventBusSubscriber(modid = EchoesofWar.MODID, value = Dist.CLIENT)
public final class EchoesofWarRenderTypes {

    // routes a draw into ApophisGlowTarget instead of the main framebuffer RenderType.draw
    // resolves this supplier when it opens its render pass, same as vanilla entites
    public static final OutputTarget APOPHIS_GLOW_TARGET =
            new OutputTarget("echoesofwar:apophis_glow", ApophisGlowTarget::getOrNull);

    // vanilla's emissive pipeline with the alpha cutout lowered:
    public static final RenderPipeline EMISSIVE_NO_CUTOUT_PIPELINE = RenderPipeline
            .builder(RenderPipelines.ENTITY_EMISSIVE_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(EchoesofWar.MODID, "pipeline/emissive_no_cutout"))
            .withShaderDefine("ALPHA_CUTOUT", 1.0F / 255.0F)
            .withShaderDefine("PER_FACE_LIGHTING")
            .withSampler("Sampler1")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(false)
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .build();

    // memoized: render types are used as map keys for buffer lookup by identity, so a fresh
    // instance per frame would defeat batching
    private static final Function<Identifier, RenderType> APOPHIS_GLOW = Util.memoize(texture ->
            RenderType.create("apophis_glow", RenderSetup.builder(EMISSIVE_NO_CUTOUT_PIPELINE)
                    .withTexture("Sampler0", texture)
                    .useOverlay()
                    // no sortOnUpload, mask is depth tested and read only for intensity, so
                    // sorting overlapping quads is unnecessary
                    .setOutputTarget(APOPHIS_GLOW_TARGET)
                    .setOutline(RenderSetup.OutlineProperty.NONE)
                    .createRenderSetup()));

    private EchoesofWarRenderTypes() {
    }

    // vanilla's entityTranslucentEmissive setup recipe on the low-cutout pipeline
    private static final Function<Identifier, RenderType> APOPHIS_EMISSIVE = Util.memoize(texture ->
            RenderType.create("apophis_emissive", RenderSetup.builder(EMISSIVE_NO_CUTOUT_PIPELINE)
                    .withTexture("Sampler0", texture)
                    .useOverlay()
                    .affectsCrumbling()
                    .sortOnUpload()
                    .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                    .createRenderSetup()));

    /** draws an emissive texture into the glow mask rather than onto the screen */
    public static RenderType apophisGlow(Identifier texture) {
        return APOPHIS_GLOW.apply(texture);
    }

    /** same as RenderTypes::entityTranslucentEmissive minus the alpha cutout */
    public static RenderType apophisEmissive(Identifier texture) {
        return APOPHIS_EMISSIVE.apply(texture);
    }

    @SubscribeEvent
    static void onRegisterRenderPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(EMISSIVE_NO_CUTOUT_PIPELINE);
    }
}
