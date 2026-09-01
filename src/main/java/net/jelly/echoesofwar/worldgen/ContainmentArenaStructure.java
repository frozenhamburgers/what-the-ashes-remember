package net.jelly.echoesofwar.worldgen;

import java.util.Optional;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * Single-piece structure that spawns containment_arena.nbt centred exactly on a nuclear
 * island's centre instead of minecraft:jigsaw which anchors corner to the spawn position and gives a random rotation.
 * recomputes island center directly and places structure with offset of half its radius
 */
public class ContainmentArenaStructure extends Structure {
    public static final MapCodec<ContainmentArenaStructure> CODEC = simpleCodec(ContainmentArenaStructure::new);

    // radius/halflength of structure
    private static final int HALF_SIZE = 38;

    private static final int GENERATION_HEIGHT = 84;

    public ContainmentArenaStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        RandomState randomState = context.randomState();
        // barrier is a unused router slot here so we'll use it to hand island/site_distance to this class from thorn_end.json (:
        DensityFunction siteDistance = randomState.router().barrierNoise();

        ChunkPos chunkPos = context.chunkPos();
        BlockPos centre = IslandCenter.findCentre(siteDistance, chunkPos.getMiddleBlockX(), chunkPos.getMiddleBlockZ());

        BlockPos origin = new BlockPos(centre.getX() - HALF_SIZE, GENERATION_HEIGHT, centre.getZ() - HALF_SIZE);
        return Optional.of(new Structure.GenerationStub(
                origin, builder -> builder.addPiece(new ContainmentArenaPiece(context.structureTemplateManager(), origin))));
    }

    @Override
    public StructureType<?> type() {
        return ModWorldgen.CONTAINMENT_ARENA_TYPE.get();
    }
}
