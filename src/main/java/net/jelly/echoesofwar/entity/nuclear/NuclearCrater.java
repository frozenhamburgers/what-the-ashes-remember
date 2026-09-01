package net.jelly.echoesofwar.entity.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

public class NuclearCrater extends ScheduledDemolition {

    public static final int CRATER_RADIUS = 150;
    public static final int CRATER_DEPTH = 58;
    public static final int CRATER_PLUG_RADIUS = 20;
    public static final int CRATER_PLUG_RAMP = 16;
    public static final int CRATER_CLEAR_ABOVE = 24;
    public static final int CRATER_DELAY_TICKS = 10;
    public static final int CRATER_BLOCK_BUDGET = 8000;

    private static final double RIM_MARGIN = 1.10;
    private static final int COLUMNS_PER_CHUNK = 256;
    private static final int RETRY_PASSES = 3;
    private static final int RETRY_WAIT_TICKS = 100;
    private static final int SCAN_COST = 1;

    private final List<ChunkPos> queue = new ArrayList<>();
    private final List<ChunkPos> skipped = new ArrayList<>();

    private int chunkIndex;
    private int columnIndex;
    private int retryPasses;
    private int retryWait;

    public void schedule(BlockPos groundZero) {
        schedule(groundZero, CRATER_DELAY_TICKS);
    }

    @Override
    protected void begin() {
        queue.clear();
        skipped.clear();
        chunkIndex = 0;
        columnIndex = 0;
        retryPasses = RETRY_PASSES;
        retryWait = 0;

        int reach = (int) Math.ceil(CRATER_RADIUS * RIM_MARGIN);
        int minCx = SectionPos.blockToSectionCoord(centreX - reach);
        int maxCx = SectionPos.blockToSectionCoord(centreX + reach);
        int minCz = SectionPos.blockToSectionCoord(centreZ - reach);
        int maxCz = SectionPos.blockToSectionCoord(centreZ + reach);

        double reach2 = (double) reach * reach;
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                ChunkPos chunk = new ChunkPos(cx, cz);
                if (nearestDistanceSqr(chunk) <= reach2) queue.add(chunk);
            }
        }
        queue.sort((a, b) -> Double.compare(centreDistanceSqr(a), centreDistanceSqr(b)));
    }

    @Override
    protected boolean step(ServerLevel level) {
        int budget = CRATER_BLOCK_BUDGET;

        while (budget > 0) {
            if (chunkIndex >= queue.size()) {
                if (!beginRetryPass()) return false;
                return true;
            }

            ChunkPos at = queue.get(chunkIndex);
            if (!level.hasChunk(at.x(), at.z())) {
                skipped.add(at);
                chunkIndex++;
                columnIndex = 0;
                continue;
            }

            budget -= excavate(level, level.getChunk(at.x(), at.z()), budget);
            if (columnIndex >= COLUMNS_PER_CHUNK) {
                chunkIndex++;
                columnIndex = 0;
            }
        }
        return true;
    }

    private boolean beginRetryPass() {
        if (skipped.isEmpty() || retryPasses <= 0) return false;
        if (++retryWait < RETRY_WAIT_TICKS) return true;

        queue.clear();
        queue.addAll(skipped);
        skipped.clear();
        chunkIndex = 0;
        columnIndex = 0;
        retryWait = 0;
        retryPasses--;
        return true;
    }

    private int excavate(ServerLevel level, LevelChunk chunk, int budget) {
        int cost = 0;
        int baseX = chunk.getPos().getMinBlockX();
        int baseZ = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        while (columnIndex < COLUMNS_PER_CHUNK && cost < budget) {
            int local = columnIndex++;
            int x = baseX + (local & 15);
            int z = baseZ + (local >> 4);
            cost += SCAN_COST;

            int depth = depthAt(x, z);
            if (depth <= 0) continue;

            int floor = Math.max(level.getMinY(), centreY - depth);
            int surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            int top = Math.min(level.getMaxY(), Math.max(centreY, surface) + CRATER_CLEAR_ABOVE);
            cost += Math.max(0, top - floor + 1) >> 3;

            for (int y = top; y >= floor; y--) {
                pos.set(x, y, z);
                BlockState state = chunk.getBlockState(pos);
                if (state.isAir()) continue;
                if (state.getDestroySpeed(level, pos) < 0f) continue;

                level.setBlock(pos, Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                cost++;
            }
        }
        return cost;
    }

    private int depthAt(int x, int z) {
        double dx = x - centreX;
        double dz = z - centreZ;
        double r = Math.sqrt(dx * dx + dz * dz);
        if (r <= CRATER_PLUG_RADIUS) return 0;

        double angle = Math.atan2(dz, dx);
        double rim = CRATER_RADIUS * (1.0 + 0.060 * Math.sin(angle * 3.0 + 1.7)
                + 0.035 * Math.sin(angle * 7.0 - 0.9));
        if (r >= rim) return 0;

        double t = (r - CRATER_PLUG_RADIUS) / (rim - CRATER_PLUG_RADIUS);
        double bowl = Math.pow(1.0 - t, 1.6);

        double ramp = Math.min(1.0, (r - CRATER_PLUG_RADIUS) / CRATER_PLUG_RAMP);
        ramp = ramp * ramp * (3.0 - 2.0 * ramp);

        return (int) Math.round(CRATER_DEPTH * bowl * ramp);
    }

    private double nearestDistanceSqr(ChunkPos chunk) {
        double dx = Math.max(0, Math.max(chunk.getMinBlockX() - centreX, centreX - chunk.getMaxBlockX()));
        double dz = Math.max(0, Math.max(chunk.getMinBlockZ() - centreZ, centreZ - chunk.getMaxBlockZ()));
        return dx * dx + dz * dz;
    }

    private double centreDistanceSqr(ChunkPos chunk) {
        double dx = chunk.getMiddleBlockX() - centreX;
        double dz = chunk.getMiddleBlockZ() - centreZ;
        return dx * dx + dz * dz;
    }
}
