package net.jelly.echoesofwar.entity.trinity.attack;

/**
 * The projectile field's entire state in 14 nums
 * thousands of projectiles mean we cannot have a field for every projectile
 * instead, generate deterministically on both the server (AttackHitDetection) and the client (TrinityFx) without the need for exchange.
 * Written by BulletPatternAttack every tick on both sides
 */
public class BulletField {

    public boolean active;
    public float age;

    // int pattern seed so both sides agree bit for bit
    public int seed;

    // index of the oldest pulse that may still have anything, absolute and ever increasing
    // for drawing sliding window from the youngest wave to the oldest wave
    public int firstWave;

    public int waveWindow; // pulses that can be alive
    public float waveInterval;
    public float lead; // telegraph, seconds

    public float speed;
    public float travel;
    public float launch;
    public float radius;


    public float jitter; // positional jitter, in cells,<1
    public float speedJitter;

    public float fadeFrom; // fraction flight to fade

    public float cut; // force dissolve

    public void clear() {
        active = false;
        age = 0f;
        cut = 0f;
        firstWave = 0;
        waveWindow = 0;
    }

    // computes projectile positions from age on both sides, mirrored in shader
    public void updateWindow() {
        float interval = Math.max(waveInterval, 0.05f);
        // slowest projectile in a pulse determines when its gone
        float slowest = speed * Math.max(1f - speedJitter, 0.05f);
        float flight = travel / Math.max(slowest, 0.001f);
        float since = age - lead;
        if (since < 0f) {
            firstWave = 0;
            waveWindow = 0;
            return;
        }
        int newest = (int) (since / interval);
        int oldest = Math.max(0, (int) Math.ceil((since - flight) / interval));
        firstWave = oldest;
        waveWindow = Math.max(0, Math.min(MAX_WINDOW, newest - oldest + 1));
    }

    public static final int MAX_WINDOW = 10; // hard cap on pulses
}
