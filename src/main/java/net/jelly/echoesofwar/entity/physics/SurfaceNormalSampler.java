package net.jelly.echoesofwar.entity.physics;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// samples terrain under a point/box to estimate a ground normal
public class SurfaceNormalSampler {

    public enum Contact { EMBEDDED, SURFACE, AIRBORNE }

    public record Sample(Contact contact, Vec3 normal, double groundY) {
        static final Sample AIRBORNE = new Sample(Contact.AIRBORNE, new Vec3(0, 1, 0), Double.NEGATIVE_INFINITY);

        public boolean grounded() {
            return contact != Contact.AIRBORNE;
        }
    }

    private SurfaceNormalSampler() {}

    public static Sample sampleAABB(Level level, AABB box) {
        int minBX = Mth.floor(box.minX);
        int maxBX = Mth.floor(box.maxX);
        int minBZ = Mth.floor(box.minZ);
        int maxBZ = Mth.floor(box.maxZ);
        int topY = Mth.floor(box.maxY);
        int bottomY = Mth.floor(box.minY) - 1;
        int centerX = Mth.floor((box.minX + box.maxX) / 2.0);
        int centerZ = Mth.floor((box.minZ + box.maxZ) / 2.0);
        return sampleSurface(level, minBX, maxBX, minBZ, maxBZ, centerX, centerZ, topY, bottomY);
    }

    public static Sample sample(Level level, Vec3 center, double halfWidth, int probeUp, int probeDown) {
        BlockPos pointPos = BlockPos.containing(center);
        BlockState pointState = level.getBlockState(pointPos);
        if (pointState.isSuffocating(level, pointPos)) {
            // solid ground already supports this point on all sides
            return new Sample(Contact.EMBEDDED, new Vec3(0, 1, 0), center.y);
        }

        int minBX = Mth.floor(center.x - halfWidth);
        int maxBX = Mth.floor(center.x + halfWidth);
        int minBZ = Mth.floor(center.z - halfWidth);
        int maxBZ = Mth.floor(center.z + halfWidth);
        int topY = Mth.floor(center.y) + probeUp;
        int bottomY = Mth.floor(center.y) - probeDown;
        return sampleSurface(level, minBX, maxBX, minBZ, maxBZ, Mth.floor(center.x), Mth.floor(center.z), topY, bottomY);
    }

    public static Sample sample(Level level, Vec3 center, double halfWidth) {
        // probeDown exceeds a falling control point's per-tick speed cap, so a fast fall can't skip
        // past the surface without sampling it as nearby
        return sample(level, center, halfWidth, 1, 5);
    }

    private static Sample sampleSurface(Level level, int minBX, int maxBX, int minBZ, int maxBZ,
                                        int centerX, int centerZ, int topY, int bottomY) {
        BlockPos centerPos = findSurfaceBelow(level, centerX, centerZ, topY, bottomY);
        // no solid ground directly under the point at all, freefall
        if (centerPos == null) return Sample.AIRBORNE;

        BlockPos[] corners = new BlockPos[]{
                findSurfaceBelow(level, minBX, minBZ, topY, bottomY),
                findSurfaceBelow(level, maxBX, minBZ, topY, bottomY),
                findSurfaceBelow(level, maxBX, maxBZ, topY, bottomY),
                findSurfaceBelow(level, minBX, maxBZ, topY, bottomY)
        };

        int validCorners = 0;
        for (BlockPos p : corners) if (p != null) validCorners++;
        double groundY = centerPos.getY() + 1.0;
        // not enough surrounding terrain to fit a slope (e.g. at a ledge), treat as flat
        if (validCorners < 2) return new Sample(Contact.SURFACE, new Vec3(0, 1, 0), groundY);

        // fan-triangulate 4 corners around center sample and average the triangle normals
        // skip any triangle with a missing corner
        Vec3 centerVec = centerPos.getCenter();
        Vec3 normalSum = Vec3.ZERO;
        int triangles = 0;
        for (int i = 0; i < 4; i++) {
            BlockPos a = corners[i];
            BlockPos b = corners[(i + 1) % 4];
            if (a == null || b == null) continue;
            Vec3 triNormal = a.getCenter().subtract(centerVec).cross(b.getCenter().subtract(centerVec));
            if (triNormal.lengthSqr() < 1.0E-6) continue;
            triNormal = triNormal.normalize();
            if (triNormal.y < 0) triNormal = triNormal.scale(-1);
            normalSum = normalSum.add(triNormal);
            triangles++;
        }
        if (triangles == 0) return new Sample(Contact.SURFACE, new Vec3(0, 1, 0), groundY);
        return new Sample(Contact.SURFACE, normalSum.scale(1.0 / triangles).normalize(), groundY);
    }

    private static BlockPos findSurfaceBelow(Level level, int x, int z, int topY, int bottomY) {
        for (int y = topY; y >= bottomY; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (state.isSuffocating(level, pos) && !level.getBlockState(pos.above()).isSuffocating(level, pos.above())) {
                return pos;
            }
        }
        return null;
    }
}
