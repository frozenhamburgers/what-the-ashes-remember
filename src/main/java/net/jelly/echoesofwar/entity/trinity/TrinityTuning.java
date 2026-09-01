package net.jelly.echoesofwar.entity.trinity;

import net.jelly.echoesofwar.Config;

/**
 * holds constants for Trinity, other than per attack numbers which are in the attack classes
 */
public final class TrinityTuning {
    private TrinityTuning() {}

    // ---------------------------------------------------- criticality

    public static final float MAX_HEALTH = 100f;

    public static final float[] CRITICALITY_THRESHOLDS = { 25f, 50f, 75f, 100f };

    public static final float CRITICALITY_PER_SECOND = 0.25f;

    // ------------------------------------------------------- difficulty

    public static final float DIFFICULTY_BASE = 0.70f;
    public static final float DIFFICULTY_STEP = 0.15f;

    public static float difficultyForDegree(int degree) {
        return DIFFICULTY_BASE + degree * DIFFICULTY_STEP;
    }

    // ---------------------------------------------------------- shape

    public static final double SPAWN_HEIGHT = 36.0;
    public static final float BODY_RADIUS = 20f;

    // --------------------------------------------------------- targeting

    public static double targetRange() {
        return Config.TRINITY_ACTIVE_RANGE.get();
    }

    public static int abandonTimeoutTicks() {
        return Config.TRINITY_ABANDON_TIMEOUT.get() * 20;
    }

    public static final int DESPAWN_TICKS = 60;

    // --------------------------------------------------------- staging

    public static int meltdownTicks() {
        return Config.TRINITY_CRITICAL_COUNTDOWN.get() * 20;
    }  // warning ticks before detonation

    public static int spawnCountdownTicks() {
        return Config.TRINITY_SPAWN_COUNTDOWN.get() * 20;
    }

    public static final int REFORM_TICKS = 150;

    public static final float REFORM_BODY_START = 0.34f;

    // --------------------------------------------------------------- scheduling

    public static final int ATTACKS_MIN = 1;
    public static final int ATTACKS_MAX = 2;

    // degrees/phases 0..3 -> 15/30/45/60%
    public static float extraAttackChance(float difficulty) {
        return Math.max(0f, Math.min(1f, difficulty - 0.55f));
    }

    public static final int PERSISTENT_RECYCLE_TICKS = 10; // gap/cooldown in finished persistent pattern

    public static final int ATTACK_CUT_TICKS = 16; // how many ticks to tick attacks during meltdown before dissolving

    // ------------------------------------------------------ detonation.

    public static final int OPENING_DETONATION_TICKS = 1500; // 75s full/nominal
    public static final int MELTDOWN_DETONATION_TICKS = 1000; // 50s full/nominal
    public static final int FINAL_DETONATION_TICKS = 1500; // 75s, uncut

    public static final float REFORM_START_FRAC = 0.52f; // fraction of detonation's full lifetime at which reformation takes over

    public static final float DETONATION_HEIGHT = 220f;
    public static final float DETONATION_CAP_RADIUS = 90f;
    public static final float DETONATION_SURGE = 1.0f; // how much of the detonation's ground surge should survive

    // ---------------------------------------------------------- damage

    public static final float CONE_DAMAGE = 9f;
    public static final float BEAM_DAMAGE = 6f;
    public static final float BULLET_DAMAGE = 5f;

    // fraction of cone and beam visual volume that can hit
    public static final float HIT_RADIUS_FRAC = 0.80f;

    // same but for bullets
    public static final float BULLET_HIT_RADIUS_FRAC = 1.0f;

    public static final int HIT_COOLDOWN_TICKS = 12; // i frames
    public static final double HIT_KNOCKBACK = 0.55;

    // detonation's damage --------------------------------
    public static final double BLAST_DAMAGE_RADIUS = 480.0;
    public static final float BLAST_DAMAGE_MIN = 2f;
    public static final float BLAST_DAMAGE_MAX = 8f;


    // -------------------------------------------------- demolition
    public static final int DEMOLITION_RADIUS = 40;
    public static final int DEMOLITION_HEIGHT = 120;
    public static final int DEMOLITION_DELAY_TICKS = 20;
    public static final int DEMOLITION_SPREAD_TICKS = 20;
}
