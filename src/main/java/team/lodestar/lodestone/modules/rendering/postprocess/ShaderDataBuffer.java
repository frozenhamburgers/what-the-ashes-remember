package team.lodestar.lodestone.modules.rendering.postprocess;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;

/**
 * Uploads packed per-instance float data to the GPU as a std140 uniform buffer, for
 * {@link MultiInstancePostProcessor} to bind into its shader.
 * <p>
 * The pre-GPU-abstraction version of this class used a raw OpenGL texture buffer object
 * (a samplerBuffer), which has no equivalent in the current rendering API. A uniform
 * buffer bound by name (see {@link PostProcessor}) is the modern replacement.
 * <p>
 * Expected GLSL declaration:
 * <pre>{@code
 * layout(std140) uniform InstanceData {
 *     int count;
 *     // std140 pads every element of a float[] array to 16 bytes - indexing data[i] is
 *     // unaffected, this only matters for the Java-side buffer layout below.
 *     float data[MAX_INSTANCES * DATA_SIZE_PER_INSTANCE];
 * };
 * }</pre>
 */
public class ShaderDataBuffer {
    private GpuBuffer buffer;
    private int capacityFloats;

    /**
     * Allocate (or reallocate) the buffer.
     *
     * @param size how many float numbers it can store (getMaxInstances() * getDataSizePerInstance())
     */
    public void generate(long size) {
        destroy();
        capacityFloats = (int) size;
        // 16 bytes per float (std140 array element stride) + 16 bytes for the "count" header slot
        long byteSize = 16L + capacityFloats * 16L;
        buffer = RenderSystem.getDevice()
                .createBuffer(() -> "Lodestone Shader Data Buffer", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, byteSize);
    }

    public void destroy() {
        if (buffer != null) {
            buffer.close();
            buffer = null;
        }
    }

    public GpuBuffer getBuffer() {
        return buffer;
    }

    /**
     * @param instanceCount number of active instances, written to the buffer's "count" header slot
     * @param data          packed instance data; length must not exceed the capacity passed to {@link #generate(long)}
     */
    public void upload(int instanceCount, float[] data) {
        if (buffer == null) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Std140Builder builder = Std140Builder.onStack(stack, 16 + data.length * 16);
            builder.putInt(instanceCount);
            for (float value : data) {
                builder.putVec4(value, 0F, 0F, 0F);
            }
            ByteBuffer bytes = builder.get();
            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            encoder.writeToBuffer(buffer.slice(0, bytes.remaining()), bytes);
        }
    }
}
