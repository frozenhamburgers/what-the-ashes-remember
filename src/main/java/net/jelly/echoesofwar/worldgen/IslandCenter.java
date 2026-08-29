package net.jelly.echoesofwar.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.DensityFunction;

// holds math for calculating island center
public final class IslandCenter {
    // central difference grad step
    private static final double GRAD_EPS = 4.0;

    private static final int DESCENT_STEPS = 24;

    // radius of exhaustive int search, must exceed descent's residual error to ensure every window contains a spawnable chunk
    private static final int REFINE_RADIUS = 16;

    private IslandCenter() {
    }

    // resolves the island centre from an arbitrary nearby point via newton descent onto the distance
    // field then exhaustive integer argmin to make independent of starting point
    public static BlockPos findCentre(DensityFunction siteDistance, int startX, int startZ) {
        double x = startX;
        double z = startZ;
        double bestValue = sample(siteDistance, x, z);
        double bestX = x;
        double bestZ = z;

        for (int step = 0; step < DESCENT_STEPS; step++) {
            double d = sample(siteDistance, x, z);
            if (d <= 0.0) {
                break;
            }

            double gx = sample(siteDistance, x + GRAD_EPS, z) - sample(siteDistance, x - GRAD_EPS, z);
            double gz = sample(siteDistance, x, z + GRAD_EPS) - sample(siteDistance, x, z - GRAD_EPS);
            double length = Math.sqrt(gx * gx + gz * gz);
            if (length < 1.0E-6) {
                break;
            }

            // mag of grad d is close to 1, so a full step of length d lands is close to exactly on the site
            x -= gx / length * d;
            z -= gz / length * d;

            double value = sample(siteDistance, x, z);
            if (value < bestValue) {
                bestValue = value;
                bestX = x;
                bestZ = z;
            }
        }

        int originX = (int) Math.round(bestX);
        int originZ = (int) Math.round(bestZ);
        int resultX = originX;
        int resultZ = originZ;
        double resultValue = Double.MAX_VALUE;

        for (int dx = -REFINE_RADIUS; dx <= REFINE_RADIUS; dx++) {
            for (int dz = -REFINE_RADIUS; dz <= REFINE_RADIUS; dz++) {
                double value = sample(siteDistance, originX + dx, originZ + dz);
                if (value < resultValue) {
                    resultValue = value;
                    resultX = originX + dx;
                    resultZ = originZ + dz;
                }
            }
        }

        return new BlockPos(resultX, 0, resultZ);
    }

    public static double sample(DensityFunction function, double x, double z) {
        return function.compute(
                new DensityFunction.SinglePointContext((int) Math.round(x), 0, (int) Math.round(z)));
    }
}
