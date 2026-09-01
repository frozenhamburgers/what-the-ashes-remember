package net.jelly.echoesofwar.entity.nuclear.fx;

import net.minecraft.client.Minecraft;
import org.joml.Vector3f;
import team.lodestar.lodestone.modules.rendering.postprocess.DynamicShaderFxInstance;

import java.util.function.BiConsumer;

// per-frame parameter block for post/nuclear/bomb_block/detonation/raymarch.fsh. everything here comes
// from NuclearDetonationWorldEvent's tick; unlike ApophisSmogFx nothing is read off a live entity,
// since the detonation is anchored to a fixed ground zero for its whole life
public class NuclearDetonationFx extends DynamicShaderFxInstance {
    /** must match NuclearDetonationPostProcessor and the idx * TRINITY_DATA_SIZE stride in the shader */
    public static final int DATA_SIZE = 10;

    private final Vector3f base;
    private final float seed;

    /** nominal mature cap height above the base, in blocks */
    public float height;
    /** nominal mature cap radius, in blocks */
    public float capRadius;
    /** plume radius at ground level, in blocks */
    public float ventRadius;
    /** seconds since detonation. every stage of the animation is derived from this in the shader */
    public float age;
    /** total seconds the sequence runs for; the shader stages on age / lifetime */
    public float lifetime;
    /** master density/emission scale. tuning knob, normally 1 */
    public float intensity = 1.0f;

    public NuclearDetonationFx(Vector3f base, float seed) {
        this.base = base;
        this.seed = seed;
    }

    @Override
    public void writeDataToBuffer(BiConsumer<Integer, Float> writer) {
        // smoothed the same way ApophisSmogFx smooths its ages, so the sequence advances per frame
        // rather than stepping 20 times a second
        float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float smoothAge = age + partialTick / 20f;

        writer.accept(0, base.x());
        writer.accept(1, base.y());
        writer.accept(2, base.z());
        writer.accept(3, seed);
        writer.accept(4, smoothAge);
        writer.accept(5, lifetime);
        writer.accept(6, height);
        writer.accept(7, capRadius);
        writer.accept(8, ventRadius);
        writer.accept(9, intensity);
    }
}
