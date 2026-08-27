package team.lodestar.lodestone.modules.rendering.postprocess;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;
import org.joml.Matrix4f;
import team.lodestar.lodestone.LodestoneLib;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link PostProcessor} that renders an arbitrary number (up to {@link #getMaxInstances()})
 * of {@link DynamicShaderFxInstance}s in a single draw, by packing all of their data into
 * one uniform buffer each frame (see {@link ShaderDataBuffer}) and reading it back as an
 * array in the shader.
 * <p>
 * Ported from Lodestone 1.21. The original packed instance data into a raw OpenGL texture
 * buffer object (a samplerBuffer) via direct LWJGL calls - there is no equivalent to that
 * in the current rendering API, so {@link ShaderDataBuffer} now uses a std140 uniform
 * buffer instead (see its javadoc for the expected GLSL layout). The uniform is bound
 * automatically to every pass under the name returned by {@link #getInstanceBufferName()}
 * - unlike the original's {@code setDataBufferUniform(EffectInstance, bufferName, countName)},
 * which subclasses called manually from {@code beforeProcess()}, since the current API
 * requires uniform names to be declared before a pass's pipeline is built rather than at
 * arbitrary points during rendering.
 */
public abstract class MultiInstancePostProcessor<I extends DynamicShaderFxInstance> extends PostProcessor {
    private final List<DynamicShaderFxInstance> instances = new ArrayList<>();
    private final ShaderDataBuffer dataBuffer = new ShaderDataBuffer();

    /**
     * THIS VALUE SHOULD NOT CHANGE!!!
     *
     * @return max fx instance count
     */
    protected abstract int getMaxInstances();

    /**
     * THIS VALUE SHOULD NOT CHANGE!!!
     *
     * @return the size of data (how many floats) it takes for passing one fx instance to the shader
     */
    protected abstract int getDataSizePerInstance();

    /**
     * @return the name of the std140 uniform block the instance data buffer is bound to
     * (see {@link ShaderDataBuffer} for the expected GLSL layout). Defaults to "InstanceData".
     */
    protected String getInstanceBufferName() {
        return "InstanceData";
    }

    @Override
    public void init() {
        super.init();
        dataBuffer.generate((long) getMaxInstances() * getDataSizePerInstance());
    }

    /**
     * Add a fx instance
     *
     * @return the instance that got added, or null if the amount of instances has reached the max
     */
    @Nullable
    public I addFxInstance(I instance) {
        if (instances.size() >= getMaxInstances()) {
            LodestoneLib.LOGGER.warn("Failed to add fx instance to " + this + ": reached max instance count of " + getMaxInstances());
            return null;
        }
        instances.add(instance);
        setActive(true);
        return instance;
    }

    @Override
    public void beforeProcess(Matrix4f viewModelMatrix) {
        float partialTicks = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        for (int i = instances.size() - 1; i >= 0; i--) {
            DynamicShaderFxInstance instance = instances.get(i);
            instance.update(partialTicks);
            if (instance.isRemoved()) {
                instances.remove(i);
            }
        }

        int dataSizePerInstance = getDataSizePerInstance();

        if (instances.isEmpty()) {
            if (!shouldRemainActiveWhileEmpty()) {
                setActive(false);
                return;
            }
            // Still running, but with nothing to draw, so the count header has to be zeroed or the
            // shaders would go on reading whatever the last live frame left in the buffer. Sized as
            // one instance rather than as an empty array purely so the write stays a comfortable
            // multiple of 16 bytes; count is 0, so not a float of it is ever read.
            dataBuffer.upload(0, new float[dataSizePerInstance]);
            return;
        }

        float[] data = new float[instances.size() * dataSizePerInstance];
        for (int i = 0; i < instances.size(); i++) {
            DynamicShaderFxInstance instance = instances.get(i);
            int offset = i * dataSizePerInstance;
            instance.writeDataToBuffer((index, value) -> {
                if (index >= dataSizePerInstance || index < 0) {
                    throw new IndexOutOfBoundsException(index);
                }
                data[offset + index] = value;
            });
        }

        dataBuffer.upload(instances.size(), data);
    }

    /**
     * Whether the chain should keep running once its last instance is gone. Normally no, since an
     * effect with nothing to draw is pure cost - override for a chain that does more than render
     * its instances. Returning true cannot revive a chain that has already stopped.
     */
    protected boolean shouldRemainActiveWhileEmpty() {
        return false;
    }

    @Override
    protected void declareExtraUniforms(RenderPipeline.Builder builder) {
        builder.withUniform(getInstanceBufferName(), UniformType.UNIFORM_BUFFER);
    }

    @Override
    protected void bindExtraUniforms(RenderPass renderPass) {
        if (dataBuffer.getBuffer() != null) {
            renderPass.setUniform(getInstanceBufferName(), dataBuffer.getBuffer());
        }
    }

    @Override
    public void onClientLevelUnload() {
        instances.clear();
        setActive(false);
    }

    public ImmutableList<DynamicShaderFxInstance> getInstances() {
        return ImmutableList.copyOf(instances);
    }
}
