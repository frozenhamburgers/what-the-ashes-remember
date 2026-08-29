package net.jelly.echoesofwar.worldgen;

import java.util.Optional;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

/**
 * Places exactly one structure on the exact centre of every nuclear island.
 *
 * Vanilla's random_spread cannot do this: it is an axis-aligned jittered grid keyed only
 * on the world seed, so it has no idea where our islands are and can only ever give "usually one per
 * island". Our island centres, however, are already a sampleable quantity:
 * nuclear_island/island/site_distance is a distance field in blocks whose global minimum of 0
 * IS the island site, the center of the island, also where we want the structure to spawn.
 *
 */
public class IslandCenterPlacement extends StructurePlacement {
    public static final MapCodec<IslandCenterPlacement> CODEC =
            RecordCodecBuilder.mapCodec(i -> placementCodec(i).apply(i, IslandCenterPlacement::new));

    // this is a calculation done by: island_warp gradient magnitude + 1 * 11.32 (length of diagonal by chunk)
    // as of writing this that is about 21, so as long as the gate is above that at least one chunk per island
    // will properly spawn the structure. This is subject to change if island spawn args change.
    private static final double GATE = 32.0;

    private IslandCenterPlacement(
            Vec3i locateOffset,
            StructurePlacement.FrequencyReductionMethod frequencyReductionMethod,
            float frequency,
            int salt,
            Optional<StructurePlacement.ExclusionZone> exclusionZone) {
        super(locateOffset, frequencyReductionMethod, frequency, salt, exclusionZone);
    }

    @Override
    protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int sourceX, int sourceZ) {
        RandomState randomState = state.randomState();
        // barrier is a unused router slot here so we'll use it to hand island/site_distance to this class from thorn_end.json (:
        DensityFunction siteDistance = randomState.router().barrierNoise();

        int centreX = sourceX * 16 + 8;
        int centreZ = sourceZ * 16 + 8;

        // cheap gate first
        if (sample(siteDistance, centreX, centreZ) >= GATE) {
            return false;
        }

        BlockPos centre = IslandCenter.findCentre(siteDistance, centreX, centreZ);
        if ((centre.getX() >> 4) != sourceX || (centre.getZ() >> 4) != sourceZ) {
            return false;
        }

        // cell whose island was removed (origin_cell/origin_fade) is not an actual island
        // vegetation carries forbidding/mask so check that
        return sample(randomState.router().vegetation(), centre.getX(), centre.getZ()) > 0.0;
    }

    private static double sample(DensityFunction function, double x, double z) {
        return IslandCenter.sample(function, x, z);
    }

    @Override
    public StructurePlacementType<?> type() {
        return ModWorldgen.ISLAND_CENTER_PLACEMENT.get();
    }
}
