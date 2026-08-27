package team.lodestar.lodestone.modules.rendering.postprocess;

import java.util.function.BiConsumer;

public abstract class DynamicShaderFxInstance {
    private float time = 0F;

    private boolean removed;

    /**
     * Called every frame (before the effect is rendered)
     */
    public void update(double deltaTime) {
        time += deltaTime / 20F;
    }

    /**
     * Write this fx instance's data to the shader data buffer, to be uploaded to the shader.
     */
    public abstract void writeDataToBuffer(BiConsumer<Integer, Float> writer);

    public final void remove() {
        removed = true;
    }

    public final boolean isRemoved() {
        return removed;
    }

    /**
     * @return the time since update() was called for the first time in seconds
     */
    public final float getTime() {
        return time;
    }
}
