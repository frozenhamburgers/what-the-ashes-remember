package net.jelly.echoesofwar.entity.trinity.attack;

import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Generates the projectile field, and finds projectiles near a point.
 * this file is one half of a pair. The other half is
 * trinity/bullets.glsl, which does the same thing on the GPU. Both must
 * produce identical projectiles from identical inputs -> every hash here is int arithmetic
 **/
public final class BulletFieldMath {
    private BulletFieldMath() {}

    // cells/cube face
    public static final int CELLS = 6;

    // 6 faces * 6 * 6 = 216 projectiles/pulse.
    public static final int PER_WAVE = 6 * CELLS * CELLS;
    private static final int SEARCH = 2;

    // --- cube-sphere mapping -------------------------------
    //
    // face 0/1 are +X/-X, 2/3 are +Y/-Y, 4/5 are +Z/-Z
    // face a direction is on
    public static int face(double x, double y, double z, double[] ab) {
        double ax = Math.abs(x), ay = Math.abs(y), az = Math.abs(z);
        if (ax >= ay && ax >= az) {
            ab[0] = y / ax;
            ab[1] = z / ax;
            return x > 0 ? 0 : 1;
        }
        if (ay >= az) {
            ab[0] = x / ay;
            ab[1] = z / ay;
            return y > 0 ? 2 : 3;
        }
        ab[0] = x / az;
        ab[1] = y / az;
        return z > 0 ? 4 : 5;
    }

    // unit direction of an in-face point, inverse of face
    public static Vec3 dir(int face, double a, double b) {
        return switch (face) {
            case 0 -> new Vec3(1, a, b).normalize();
            case 1 -> new Vec3(-1, a, b).normalize();
            case 2 -> new Vec3(a, 1, b).normalize();
            case 3 -> new Vec3(a, -1, b).normalize();
            case 4 -> new Vec3(a, b, 1).normalize();
            default -> new Vec3(a, b, -1).normalize();
        };
    }

    // cell index -> in-face coordinate at an arbritrary consistent offset within the cell
    private static double cellCoord(double index) {
        return index / CELLS * 2.0 - 1.0;
    }

    // --- HASHING -------------------------------------------

    // integer avalanche hash identical to bfHash() in bullets.glsl
    public static int hash(int x) {
        x ^= x >>> 16;
        x *= 0x7feb352d;
        x ^= x >>> 15;
        x *= 0x846ca68b;
        x ^= x >>> 16;
        return x;
    }

    // top 24 bits represneted as float in [0,1), matches shader
    public static float unitFloat(int h) {
        return (h >>> 8) * (1.0f / 16777216.0f);
    }

    private static int key(int face, int i, int j, int wave, int seed) {
        int cell = face + 6 * (i + CELLS * (j + CELLS * wave));
        return hash(hash(cell + 1) ^ seed);
    }

    // --- PROJECTILES  --------------------------------------------

    // one generated projectile, always rebuilt
    public static final class Bullet {
        public Vec3 dir = new Vec3(0, 1, 0);
        public double speedMul = 1.0;
        public double radiusMul = 1.0;
    }


    // fills in projectile belonggin to one cell of a pulse
    public static void bullet(int face, int i, int j, int wave, int seed, float jitter,
                              float speedJitter, Bullet out) {
        int h1 = key(face, i, j, wave, seed);
        int h2 = hash(h1);
        int h3 = hash(h2);
        float r1 = unitFloat(h1), r2 = unitFloat(h2), r3 = unitFloat(h3);

        // jitter applied in cell's coordinates BEFORE projecting to sphere, keeps proj in cell
        double a = cellCoord(i + 0.5 + (r1 - 0.5f) * jitter);
        double b = cellCoord(j + 0.5 + (r2 - 0.5f) * jitter);
        out.dir = dir(face, a, b);
        out.speedMul = 1.0 + (r3 - 0.5f) * 2.0 * speedJitter;

        out.radiusMul = 0.78 + 0.5 * r1;
    }


    // resolve potentially out of range cell index to its actual cell
    public static void resolveCell(int face, int i, int j, double[] scratch, int[] out) {
        if (i >= 0 && i < CELLS && j >= 0 && j < CELLS) {
            out[0] = face;
            out[1] = i;
            out[2] = j;
            return;
        }
        Vec3 d = dir(face, cellCoord(i + 0.5), cellCoord(j + 0.5));
        int f2 = face(d.x, d.y, d.z, scratch);
        out[0] = f2;
        out[1] = cellIndex(scratch[0]);
        out[2] = cellIndex(scratch[1]);
    }

    private static int cellIndex(double coord) {
        int c = (int) Math.floor((coord + 1.0) * 0.5 * CELLS);
        return Math.max(0, Math.min(CELLS - 1, c));
    }

    // ---

    // dsitance a pulse's projectiles have covered, -1 if not launched yet
    public static double waveDistance(BulletField field, int wave) {
        double waveAge = field.age - field.lead - wave * field.waveInterval;
        return waveAge <= 0.0 ? -1.0 : waveAge * field.speed;
    }

    // dir to push player if inside proj
    public static @Nullable Vec3 hit(BulletField field, Vec3 centre, Vec3 at, float radiusFrac) {
        if (!field.active || field.cut >= 1f) return null;

        Vec3 rel = at.subtract(centre);
        double r = rel.length();
        if (r < 1.0e-4) return null;

        double maxRadius = field.radius * 1.3;
        if (r < field.launch - maxRadius || r > field.launch + field.travel + maxRadius) return null;

        double[] ab = new double[2];
        int face = face(rel.x / r, rel.y / r, rel.z / r, ab);
        int i0 = cellIndex(ab[0]);
        int j0 = cellIndex(ab[1]);

        double[] scratch = new double[2];
        int[] cell = new int[3];
        Bullet bullet = new Bullet();


        // only iterate thru pulses that are sitll in the air, not count from zero
        for (int k = 0; k < field.waveWindow; k++) {
            int wave = field.firstWave + k;
            double base = waveDistance(field, wave);
            if (base < 0.0 || base > BulletPatternAttack.waveLimit(field)) continue;

            for (int di = -SEARCH; di <= SEARCH; di++) {
                for (int dj = -SEARCH; dj <= SEARCH; dj++) {
                    resolveCell(face, i0 + di, j0 + dj, scratch, cell);
                    bullet(cell[0], cell[1], cell[2], wave, field.seed,
                            field.jitter, field.speedJitter, bullet);

                    // A projectile past the end of its own flight has dissolved, however far
                    // the pulse as a whole has got.
                    if (base * bullet.speedMul >= field.travel) continue;

                    double dist = field.launch + base * bullet.speedMul;
                    Vec3 offset = at.subtract(centre.add(bullet.dir.scale(dist)));
                    double d = offset.length();
                    if (d > field.radius * bullet.radiusMul * radiusFrac) continue;
                    return d < 1.0e-4 ? bullet.dir : offset.scale(1.0 / d);
                }
            }
        }
        return null;
    }
}
