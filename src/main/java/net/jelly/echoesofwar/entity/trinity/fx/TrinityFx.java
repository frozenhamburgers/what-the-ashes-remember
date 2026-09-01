package net.jelly.echoesofwar.entity.trinity.fx;

import net.jelly.echoesofwar.entity.trinity.TrinityTuning;
import net.jelly.echoesofwar.entity.trinity.TrinityWorldEvent;
import net.jelly.echoesofwar.entity.trinity.attack.AttackSlot;
import net.jelly.echoesofwar.entity.trinity.attack.BulletField;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import team.lodestar.lodestone.modules.rendering.postprocess.DynamicShaderFxInstance;

import java.util.function.BiConsumer;

/**
 * The entire instance of Trinity as the shader sees it: one instance, one parameter block, one chain.
 *
 * Everything Trinity ever draws needs to go through here. Splitting into multi post processing shaders
 * will not work due to depth sorting issues in Lodestone I have not figured out.
 *
 * DATA_SIZE, TrinityPostProcessor#getDataSizePerInstance(), and data[] length and stride constants
 * in trinity/instance.glsl MUST match.
 */
public class TrinityFx extends DynamicShaderFxInstance {

    // ----------------------------------------------------- layout

    // floats before the attack array begins
    public static final int HEADER_SIZE = 48;
    // floats per attack slot
    public static final int ATTACK_STRIDE = 11;
    // Attack slots. Must equal TrinityWorldEvent#ATTACK_SLOTS
    public static final int ATTACK_SLOTS = TrinityWorldEvent.ATTACK_SLOTS;

    public static final int DATA_SIZE = HEADER_SIZE + ATTACK_STRIDE * ATTACK_SLOTS; // 928

    // header indices, mirrored in instance.glsl
    private static final int H_CENTRE_X       = 0;  // 0..2
    private static final int H_SEED           = 3;
    private static final int H_TIME           = 4;  // seconds, partial-tick smoothed
    private static final int H_BODY_RADIUS    = 5;
    private static final int H_BODY_SCALE     = 6;  // 0..1, grows through the reformation
    private static final int H_CORE_INTENSITY = 7;
    private static final int H_PHASE          = 8;  // TrinityPhase ordinal
    private static final int H_PHASE_PROGRESS = 9;
    private static final int H_CRITICALITY    = 10;
    private static final int H_DEGREE         = 11;
    private static final int H_MELTDOWN       = 12; // 0..1
    private static final int H_REFORM         = 13; // 0..1
    private static final int H_DET_ACTIVE     = 14;
    private static final int H_DET_AGE        = 15; // seconds
    private static final int H_DET_LIFETIME   = 16; // seconds
    private static final int H_DET_H          = 17;
    private static final int H_DET_R1         = 18;
    private static final int H_DET_R0         = 19;
    private static final int H_DET_INTENSITY  = 20;
    private static final int H_DET_SURGE      = 21;
    private static final int H_ATTACK_COUNT   = 22;
    private static final int H_TURBULENCE     = 23;
    private static final int H_CLOUD_HEIGHT   = 24;
    private static final int H_DET_BASE_X     = 25; // 25..27 - ground zero, BELOW Trinity
    // 28..31 spare

    // The projectile field, 32..47. Every projectile is generated from these
    private static final int H_BF_ACTIVE       = 32;
    private static final int H_BF_AGE          = 33;
    private static final int H_BF_SEED         = 34;
    private static final int H_BF_WAVES        = 35; // window size not count from 0
    private static final int H_BF_INTERVAL     = 36;
    private static final int H_BF_LEAD         = 37;
    private static final int H_BF_SPEED        = 38;
    private static final int H_BF_TRAVEL       = 39;
    private static final int H_BF_LAUNCH       = 40;
    private static final int H_BF_RADIUS       = 41;
    private static final int H_BF_JITTER       = 42;
    private static final int H_BF_SPEED_JITTER = 43;
    private static final int H_BF_FADE_FROM    = 44;
    private static final int H_BF_CUT          = 45;
    private static final int H_BF_FIRST_WAVE   = 46;
    // 47 spare for now

    // ------------------------------------------------------- state
    // fields refreshed from the world event every client tick and read back out during the render pass

    private Vec3 centre = Vec3.ZERO;
    private Vec3 detonationBase = Vec3.ZERO;
    private float seed;
    private float bodyRadius = TrinityTuning.BODY_RADIUS;
    private float bodyScale;
    private float coreIntensity = 1f;
    private int phase;
    private float phaseProgress;
    private float criticality;
    private int degree;
    private float meltdown;
    private float reform;
    private boolean detonationActive;
    private float detonationAge;
    private float detonationLifetime = 1f;
    private int attackCount;

    // snapshots for the render thread to avoid having to read the tick thread's array
    // should make things a lot faster
    private final Slot[] slots = new Slot[ATTACK_SLOTS];

    private final BulletField field = new BulletField();
    private float prevFieldAge;

    private float ageSeconds; // for shader

    // prev ticks values for partialtick interpolation
    private float prevBodyScale, prevMeltdown, prevReform, prevCore;
    private boolean primed;

    public TrinityFx() {
        for (int i = 0; i < slots.length; i++) slots[i] = new Slot();
    }

    private static final class Slot {
        int type;
        float telegraph, extend, fade, length, radius, intensity, seed;
        float prevTelegraph, prevExtend, prevFade;
        double dx, dy, dz;
    }

    // Pulls a fresh snapshot out of the world event, called every client tick
    public void write(TrinityWorldEvent trinity) {
        prevBodyScale = primed ? bodyScale : trinity.bodyScale();
        prevMeltdown = primed ? meltdown : trinity.meltdownProgress();
        prevReform = primed ? reform : trinity.reformProgress();
        prevCore = primed ? coreIntensity : 1f;

        centre = trinity.shaderCentre();
        detonationBase = trinity.detonationBase();
        seed = trinity.shaderSeed();
        bodyRadius = TrinityTuning.BODY_RADIUS;
        bodyScale = trinity.bodyScale();
        phase = trinity.phase().ordinal();
        criticality = trinity.criticality();
        degree = trinity.criticalityDegree();
        meltdown = trinity.meltdownProgress();
        reform = trinity.reformProgress();
        detonationActive = trinity.detonationActive();
        detonationAge = trinity.detonationAge();
        detonationLifetime = trinity.detonationLifetime();
        ageSeconds = trinity.clockSeconds();
        phaseProgress = trinity.phaseTicks() / 20f;

        coreIntensity = 1f + meltdown * MELTDOWN_CORE_GAIN + degree * DEGREE_CORE_GAIN;

        BulletField live = trinity.bulletField;
        prevFieldAge = field.active && live.active ? field.age : live.age;
        field.active = live.active;
        field.age = live.age;
        field.seed = live.seed;
        field.firstWave = live.firstWave;
        field.waveWindow = live.waveWindow;
        field.waveInterval = live.waveInterval;
        field.lead = live.lead;
        field.speed = live.speed;
        field.travel = live.travel;
        field.launch = live.launch;
        field.radius = live.radius;
        field.jitter = live.jitter;
        field.speedJitter = live.speedJitter;
        field.fadeFrom = live.fadeFrom;
        field.cut = live.cut;

        int count = 0;
        for (int i = 0; i < ATTACK_SLOTS; i++) {
            AttackSlot src = trinity.slots[i];
            Slot dst = slots[i];
            // snap don't itnerpolate when swapping attacks
            boolean sameAttack = dst.type == src.type;
            dst.type = src.type;
            if (src.type == AttackSlot.TYPE_NONE) continue;
            count = i + 1;
            dst.prevTelegraph = sameAttack ? dst.telegraph : src.telegraph;
            dst.prevExtend = sameAttack ? dst.extend : src.extend;
            dst.prevFade = sameAttack ? dst.fade : src.fade;
            dst.telegraph = src.telegraph;
            dst.extend = src.extend;
            dst.fade = src.fade;
            dst.length = src.length;
            dst.radius = src.radius;
            dst.intensity = src.intensity;
            dst.seed = src.seed;
            dst.dx = src.dir.x; dst.dy = src.dir.y; dst.dz = src.dir.z;
        }
        attackCount = count;
        primed = true;
    }

    // brighten core during detonation
    private static final float MELTDOWN_CORE_GAIN = 7.0f;
    private static final float DEGREE_CORE_GAIN = 0.25f;

    @Override
    public void writeDataToBuffer(BiConsumer<Integer, Float> writer) {
        float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float smooth = partialTick / 20f;
        float a = partialTick;

        writer.accept(H_CENTRE_X,       (float) centre.x);
        writer.accept(H_CENTRE_X + 1,   (float) centre.y);
        writer.accept(H_CENTRE_X + 2,   (float) centre.z);
        writer.accept(H_SEED,           seed);
        writer.accept(H_TIME,           ageSeconds + smooth);
        writer.accept(H_BODY_RADIUS,    bodyRadius);
        writer.accept(H_BODY_SCALE,     lerp(prevBodyScale, bodyScale, a));
        writer.accept(H_CORE_INTENSITY, lerp(prevCore, coreIntensity, a));
        writer.accept(H_PHASE,          (float) phase);
        writer.accept(H_PHASE_PROGRESS, phaseProgress + smooth);
        writer.accept(H_CRITICALITY,    criticality);
        writer.accept(H_DEGREE,         (float) degree);
        writer.accept(H_MELTDOWN,       lerp(prevMeltdown, meltdown, a));
        writer.accept(H_REFORM,         lerp(prevReform, reform, a));
        writer.accept(H_DET_ACTIVE,     detonationActive ? 1f : 0f);
        writer.accept(H_DET_AGE,        detonationAge + (detonationActive ? smooth : 0f));
        writer.accept(H_DET_LIFETIME,   detonationLifetime);
        writer.accept(H_DET_H,          TrinityTuning.DETONATION_HEIGHT);
        writer.accept(H_DET_R1,         TrinityTuning.DETONATION_CAP_RADIUS);
        writer.accept(H_DET_R0,         Math.max(TrinityTuning.DETONATION_CAP_RADIUS * 0.16f, 3f));
        writer.accept(H_DET_INTENSITY,  1f);
        writer.accept(H_DET_SURGE,      TrinityTuning.DETONATION_SURGE);
        writer.accept(H_ATTACK_COUNT,   (float) attackCount);
        writer.accept(H_TURBULENCE,     1f + lerp(prevMeltdown, meltdown, a));
        writer.accept(H_CLOUD_HEIGHT,
                (float) (TrinityTuning.DETONATION_HEIGHT - TrinityTuning.SPAWN_HEIGHT));
        writer.accept(H_DET_BASE_X,     (float) detonationBase.x);
        writer.accept(H_DET_BASE_X + 1, (float) detonationBase.y);
        writer.accept(H_DET_BASE_X + 2, (float) detonationBase.z);
        for (int i = 28; i < 32; i++) writer.accept(i, 0f);

        writer.accept(H_BF_ACTIVE,       field.active ? 1f : 0f);
        writer.accept(H_BF_AGE,          lerp(prevFieldAge, field.age, a));
        writer.accept(H_BF_SEED,         (float) field.seed);
        writer.accept(H_BF_WAVES,        (float) field.waveWindow);
        writer.accept(H_BF_INTERVAL,     field.waveInterval);
        writer.accept(H_BF_LEAD,         field.lead);
        writer.accept(H_BF_SPEED,        field.speed);
        writer.accept(H_BF_TRAVEL,       field.travel);
        writer.accept(H_BF_LAUNCH,       field.launch);
        writer.accept(H_BF_RADIUS,       field.radius);
        writer.accept(H_BF_JITTER,       field.jitter);
        writer.accept(H_BF_SPEED_JITTER, field.speedJitter);
        writer.accept(H_BF_FADE_FROM,    field.fadeFrom);
        writer.accept(H_BF_CUT,          field.cut);
        writer.accept(H_BF_FIRST_WAVE,   (float) field.firstWave);
        for (int i = 47; i < HEADER_SIZE; i++) writer.accept(i, 0f);

        for (int i = 0; i < ATTACK_SLOTS; i++) {
            int base = HEADER_SIZE + i * ATTACK_STRIDE;
            Slot s = slots[i];
            writer.accept(base,      (float) s.type);
            if (s.type == AttackSlot.TYPE_NONE) {
                // for empty slot, read type but zero everything else b/c stale
                for (int k = 1; k < ATTACK_STRIDE; k++) writer.accept(base + k, 0f);
                continue;
            }
            writer.accept(base + 1,  lerp(s.prevTelegraph, s.telegraph, a));
            writer.accept(base + 2,  lerp(s.prevExtend, s.extend, a));
            writer.accept(base + 3,  lerp(s.prevFade, s.fade, a));
            writer.accept(base + 4,  (float) s.dx);
            writer.accept(base + 5,  (float) s.dy);
            writer.accept(base + 6,  (float) s.dz);
            writer.accept(base + 7,  s.length);
            writer.accept(base + 8,  s.radius);
            writer.accept(base + 9,  s.intensity);
            writer.accept(base + 10, s.seed);
        }
    }

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }
}
