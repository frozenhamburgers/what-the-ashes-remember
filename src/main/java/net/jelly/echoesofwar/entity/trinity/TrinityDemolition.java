package net.jelly.echoesofwar.entity.trinity;

import net.jelly.echoesofwar.block.CrucibleOfCalamityBlock;
import net.jelly.echoesofwar.block.CrucibleOfCalamityPartBlock;
import net.jelly.echoesofwar.entity.nuclear.ScheduledDemolition;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import static net.jelly.echoesofwar.entity.trinity.TrinityTuning.*;

/**
 * Server only, destroys structure Trinity spawns in
 */
public class TrinityDemolition extends ScheduledDemolition {

    // cached offsets to save cost in radius tests per pos per layer
    private static int[] offsets;
    private static int offsetRadius = -1;

    private int nextY;
    private int floorY;

    public void schedule(BlockPos groundZero) {
        schedule(groundZero, DEMOLITION_DELAY_TICKS);
    }

    @Override
    protected void begin() {
        floorY = centreY;
        nextY = floorY + DEMOLITION_HEIGHT;
    }

    @Override
    protected boolean step(ServerLevel level) {
        int layers = Math.max(1, (DEMOLITION_HEIGHT + DEMOLITION_SPREAD_TICKS - 1) / DEMOLITION_SPREAD_TICKS);

        int[] circle = circleOffsets(DEMOLITION_RADIUS);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int n = 0; n < layers && nextY > floorY; n++, nextY--) {
            int y = nextY;
            if (y > level.getMaxY() || y < level.getMinY()) continue;

            for (int i = 0; i < circle.length; i += 2) {
                pos.set(centreX + circle[i], y, centreZ + circle[i + 1]);

                BlockState state = level.getBlockState(pos);
                if (state.isAir()) continue;
                Block block = state.getBlock();
                if (block instanceof CrucibleOfCalamityBlock
                        || block instanceof CrucibleOfCalamityPartBlock) continue;

                level.setBlock(pos, Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }

        return nextY > floorY;
    }

    private static int[] circleOffsets(int radius) {
        if (offsets != null && offsetRadius == radius) return offsets;

        int r2 = radius * radius;
        int count = 0;
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (dx * dx + dz * dz <= r2) count++;
            }
        }
        int[] built = new int[count * 2];
        int n = 0;
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (dx * dx + dz * dz > r2) continue;
                built[n++] = dx;
                built[n++] = dz;
            }
        }
        offsets = built;
        offsetRadius = radius;
        return built;
    }
}
