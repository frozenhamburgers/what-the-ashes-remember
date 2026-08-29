package net.jelly.echoesofwar.worldgen;

import com.mojang.serialization.MapCodec;

import net.jelly.echoesofwar.EchoesofWar;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Worldgen registration for the Landscape of Thorns to add a 6th biome to the end, and a custom structure placement
 * for the center of nuclear islands
 */
public class ModWorldgen {
    public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, EchoesofWar.MODID);

    public static final DeferredRegister<StructurePlacementType<?>> STRUCTURE_PLACEMENTS =
            DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, EchoesofWar.MODID);

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, EchoesofWar.MODID);

    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, EchoesofWar.MODID);

    public static final DeferredHolder<MapCodec<? extends BiomeSource>, MapCodec<EchoesEndBiomeSource>> THORN_END =
            BIOME_SOURCES.register("thorn_end", () -> EchoesEndBiomeSource.CODEC);

    public static final DeferredHolder<StructurePlacementType<?>, StructurePlacementType<IslandCenterPlacement>>
            ISLAND_CENTER_PLACEMENT = STRUCTURE_PLACEMENTS.register(
                    "island_center",
                    () -> (StructurePlacementType<IslandCenterPlacement>) () -> IslandCenterPlacement.CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<ContainmentArenaStructure>>
            CONTAINMENT_ARENA_TYPE = STRUCTURE_TYPES.register(
                    "containment_arena",
                    () -> (StructureType<ContainmentArenaStructure>) () -> ContainmentArenaStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> CONTAINMENT_ARENA_PIECE =
            STRUCTURE_PIECES.register(
                    "containment_arena",
                    () -> (StructurePieceType.StructureTemplateType)
                            (manager, tag) -> new ContainmentArenaPiece(manager, tag));

    public static final ResourceKey<Biome> LANDSCAPE_OF_THORNS = ResourceKey.create(
            Registries.BIOME, Identifier.fromNamespaceAndPath(EchoesofWar.MODID, "landscape_of_thorns"));

    public static final ResourceKey<Biome> FORBIDDING_BLOCKS = ResourceKey.create(
            Registries.BIOME, Identifier.fromNamespaceAndPath(EchoesofWar.MODID, "forbidding_blocks"));

    public static final ResourceKey<Biome> CONTAINMENT_ZONE = ResourceKey.create(
            Registries.BIOME, Identifier.fromNamespaceAndPath(EchoesofWar.MODID, "containment_zone"));

    public static void init() {
    }
}
