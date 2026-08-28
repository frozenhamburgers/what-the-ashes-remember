package net.jelly.echoesofwar.worldgen;

import com.mojang.serialization.MapCodec;

import net.jelly.echoesofwar.EchoesofWar;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Worldgen registration for the Landscape of Thorns to add a 6th biome to the end
 */
public class ModWorldgen {
    public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, EchoesofWar.MODID);

    public static final DeferredHolder<MapCodec<? extends BiomeSource>, MapCodec<ThornEndBiomeSource>> THORN_END =
            BIOME_SOURCES.register("thorn_end", () -> ThornEndBiomeSource.CODEC);

    public static final ResourceKey<Biome> LANDSCAPE_OF_THORNS = ResourceKey.create(
            Registries.BIOME, Identifier.fromNamespaceAndPath(EchoesofWar.MODID, "landscape_of_thorns"));

    public static final ResourceKey<Biome> FORBIDDING_BLOCKS = ResourceKey.create(
            Registries.BIOME, Identifier.fromNamespaceAndPath(EchoesofWar.MODID, "forbidding_blocks"));

    public static void init() {
    }
}
