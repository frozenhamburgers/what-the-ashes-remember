package net.jelly.echoesofwar.entity.apophis.smog;

import java.util.Map;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import net.jelly.echoesofwar.Config;
import net.jelly.echoesofwar.entity.apophis.client.ApophisAtmosphere;
import net.jelly.echoesofwar.entity.apophis.client.ApophisGlowTarget;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import net.jelly.echoesofwar.EchoesofWar;
import net.minecraft.resources.Identifier;
import team.lodestar.lodestone.modules.rendering.postprocess.MultiInstancePostProcessor;

// drives the smog post chain (post/apophis/chain.json): low-res raymarch of the volume, a
// low-res march of the cloud's shadow, a quarter-size bloom pyramid, and a full-res composite.
// the shadow is a separate pass so the composite can tell it apart from the volume's own coverage
public class ApophisPostProcessor extends MultiInstancePostProcessor<ApophisSmogFx> {
    public static final ApophisPostProcessor INSTANCE = new ApophisPostProcessor();

    @Override
    public Identifier getPostChainLocation() {
        return Identifier.fromNamespaceAndPath(EchoesofWar.MODID, "apophis/chain");
    }

    @Override
    protected int getMaxInstances() {
        return 2;
    }

    @Override
    protected int getDataSizePerInstance() {
        return ApophisSmogFx.DATA_SIZE;
    }


    @Override
    protected Map<String, Float> getScaledTargets() {
        float volumetric = (float) (double) Config.VOLUMETRIC_RESOLUTION_SCALE.get();
        return Map.of(
                "smog_low_res", volumetric,
                "smog_shadow", volumetric,
                "smog_bloom_a", 0.25f,
                "smog_bloom_b", 0.25f);
    }

    // the glow mask, see ApophisGlowTarget. uses getOrCreate() not getOrNull(): a missing target
    // here is a hard failure, and this also covers smog hanging over the world with no Apophis in
    // sight yet to have requested the buffer. key is namespaced or it'd resolve to minecraft:apophis_glow
    @Override
    protected Map<String, RenderTarget> getExternalTargets() {
        return Map.of("echoesofwar:apophis_glow", ApophisGlowTarget.getOrCreate());
    }

    // keeps the chain running with no cloud out: the composite also lights Apophis's emissive from
    // ApophisGlowTarget's mask and blooms it regardless of whether smog exists, and with count == 0
    // raymarch targets fall out at no instances
    @Override
    protected boolean shouldRemainActiveWhileEmpty() {
        // want this for apophis glow and atmosphere even if no raymarched cloud
        return ApophisGlowTarget.isApophisVisible() || ApophisAtmosphere.isActive();
    }

    // separate uniform block rather than a slot in InstanceData, since the atmosphere is a property
    // of the fight, not of any smog cloud, and has to survive frames where the instance count is 0
    private GpuBuffer atmosphereBuffer;

    @Override
    public void beforeProcess(Matrix4f viewModelMatrix) {
        if (atmosphereBuffer == null) {
            atmosphereBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "Apophis Atmosphere",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 16);
        }
        super.beforeProcess(viewModelMatrix);

        float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Std140Builder builder = Std140Builder.onStack(stack, 16);
            builder.putFloat(ApophisAtmosphere.intensity(partialTick));
            builder.putFloat(0F).putFloat(0F).putFloat(0F); // padded to a full vec4
            ByteBuffer bytes = builder.get();
            RenderSystem.getDevice().createCommandEncoder()
                    .writeToBuffer(atmosphereBuffer.slice(0, bytes.remaining()), bytes);
        }
    }

    @Override
    protected void declareExtraUniforms(RenderPipeline.Builder builder) {
        super.declareExtraUniforms(builder); // InstanceData - without it the raymarch goes blind
        builder.withUniform("AtmosphereData", UniformType.UNIFORM_BUFFER);
    }

    @Override
    protected void bindExtraUniforms(RenderPass renderPass) {
        super.bindExtraUniforms(renderPass);
        if (atmosphereBuffer != null) renderPass.setUniform("AtmosphereData", atmosphereBuffer);
    }

    @Override
    public void afterProcess() {
        ApophisGlowTarget.consumeVisibility();
    }
}
