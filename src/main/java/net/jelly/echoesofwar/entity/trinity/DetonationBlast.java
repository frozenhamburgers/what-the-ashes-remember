package net.jelly.echoesofwar.entity.trinity;

import net.jelly.echoesofwar.block.CrucibleOfCalamityBlock;
import net.jelly.echoesofwar.block.CrucibleOfCalamityPartBlock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

import static net.jelly.echoesofwar.entity.trinity.TrinityTuning.*;

/**
 * Nuclear detonation's damage
 */
public final class DetonationBlast {
    private DetonationBlast() {}

    // sample heights of targets' hitbox
    private static final double[][] SAMPLE_POINTS = {
            {0.5, 0.5, 0.5},
            {0.5, 1.0, 0.5}, {0.5, 0.0, 0.5},
            {0.1, 0.9, 0.1}, {0.9, 0.1, 0.9},
    };

    public static void apply(ServerLevel level, Vec3 origin) {
        double radius = BLAST_DAMAGE_RADIUS;
        AABB box = new AABB(origin.x - radius, origin.y - radius, origin.z - radius,
                origin.x + radius, origin.y + radius, origin.z + radius);

        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && !e.isSpectator());
        if (targets.isEmpty()) return;

        double r2 = radius * radius;
        for (LivingEntity target : targets) {
            // The sphere the box above only approximates.
            double d2 = target.position().distanceToSqr(origin);
            if (d2 > r2) continue;
            if (!exposed(level, origin, target)) continue;

            double t = Math.sqrt(d2) / radius;
            float falloff = (float) ((1.0 - t) * (1.0 - t));
            float damage = BLAST_DAMAGE_MIN + (BLAST_DAMAGE_MAX - BLAST_DAMAGE_MIN) * falloff;

            // The reason this is not simply hurtServer: LivingEntity refuses damage while
            // invulnerableTime is running, which is what would turn twenty hits a second into
            // two. Clearing it is the point of the mechanic, not a workaround for it.
            target.invulnerableTime = 0;
            target.hurtServer(level, level.damageSources().explosion(null, null), damage);
        }
    }

    private static boolean exposed(ServerLevel level, Vec3 origin, Entity target) {
        AABB box = target.getBoundingBox();
        for (double[] point : SAMPLE_POINTS) {
            Vec3 at = new Vec3(
                    Mth.lerp(point[0], box.minX, box.maxX),
                    Mth.lerp(point[1], box.minY, box.maxY),
                    Mth.lerp(point[2], box.minZ, box.maxZ));
            if (!blocked(level, origin, at)) return true;
        }
        return false;
    }

    // raytrace from ground zero to target to see if anything occlusive OTHER than crucible blocks stand in the way
    private static boolean blocked(ServerLevel level, Vec3 origin, Vec3 at) {
        Boolean hit = BlockGetter.traverseBlocks(origin, at, at,
                (to, pos) -> {
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir()) return null;
                    if (isCrucible(state)) return null;

                    VoxelShape shape = state.getCollisionShape(level, pos);
                    if (shape.isEmpty()) return null;
                    return shape.clip(origin, to, pos) != null ? Boolean.TRUE : null;
                },
                to -> Boolean.FALSE);
        return hit != null && hit;
    }

    private static boolean isCrucible(BlockState state) {
        return state.getBlock() instanceof CrucibleOfCalamityBlock
                || state.getBlock() instanceof CrucibleOfCalamityPartBlock;
    }
}
