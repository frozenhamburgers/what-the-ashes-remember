package net.jelly.echoesofwar.entity.apophis.smog;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import team.lodestar.lodestone.modules.rendering.postprocess.DynamicShaderFxInstance;

import java.util.function.BiConsumer;

// per-frame parameter block for post/apophis/smog/raymarch.fsh. everything about the cloud comes
// from ApophisSmogWorldEvent's tick, except the mouth position, which is read off the live entity every fram
public class ApophisSmogFx extends DynamicShaderFxInstance {
    /** must match ApophisPostProcessor and the shader */
    public static final int DATA_SIZE = 13;

    // not final: a summoned cloud is handed from its box to Apophis part-way through its life
    private int ownerId;
    private final float seed;

    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }

    public Vector3f cloudCenter;
    public float cloudRadius;
    /** 1 while smog is pouring from the mouth, easing to 0 once it stops */
    public float emit;
    /** 1 while the cloud is intact, easing to 0 as it dissipates */
    public float density;
    public float age;
    /** drives jet's noise scroll */
    public float jetAge;
    /** how far the emitted smoke has reached from the mouth, in blocks, truncate jet using this */
    public float frontDistance;

    private Vector3f lastMouth;

    /** non-null while the mouth is retracting into the cloud instead of tracking a live entity e.g. spawn */
    public Vector3f fadeOrigin;
    /** where the jet retracts to, always just the cloud's center */
    public Vector3f fadeTarget;
    public int fadePhaseTicks;

    public ApophisSmogFx(int ownerId, Vector3f cloudCenter, Vector3f mouth, float seed) {
        this.ownerId = ownerId;
        this.cloudCenter = cloudCenter;
        this.lastMouth = mouth;
        this.seed = seed;
    }

    private Vector3f mouthPosition(float partialTick) {
        if (fadeOrigin != null) {
            // eased quadratically so the retraction accelerates into the cloud; timed on
            // EMIT_STOP_TICKS (so the cone finishes disappearing right as emit decays to 0, instead of disappearing when fade
            float t = Mth.clamp((fadePhaseTicks + partialTick / 20f) / ApophisSmogWorldEvent.EMIT_STOP_TICKS, 0f, 1f);
            float eased = t * t;
            lastMouth = new Vector3f(fadeOrigin).lerp(fadeTarget, eased);
            return lastMouth;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            Entity owner = minecraft.level.getEntity(ownerId);
            if (owner != null) {
                Vec3 position = owner.getPosition(partialTick);
                lastMouth = new Vector3f((float) position.x, (float) position.y, (float) position.z);
            }
        }
        return lastMouth;
    }

    public Vector3f currentMouthPosition() {
        return mouthPosition(0f);
    }

    @Override
    public void writeDataToBuffer(BiConsumer<Integer, Float> writer) {
        float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float smoothAge = age + partialTick / 20f;
        float smoothJetAge = jetAge + partialTick / 20f;
        Vector3f mouth = mouthPosition(partialTick);

        writer.accept(0, cloudCenter.x());
        writer.accept(1, cloudCenter.y());
        writer.accept(2, cloudCenter.z());
        writer.accept(3, cloudRadius);
        writer.accept(4, mouth.x());
        writer.accept(5, mouth.y());
        writer.accept(6, mouth.z());
        writer.accept(7, emit);
        writer.accept(8, seed);
        writer.accept(9, smoothAge);
        writer.accept(10, density);
        writer.accept(11, frontDistance);
        writer.accept(12, smoothJetAge);
    }
}
