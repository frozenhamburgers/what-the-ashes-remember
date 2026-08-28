package net.jelly.echoesofwar.worldgen;

import java.util.stream.Stream;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Vanilla's {@link net.minecraft.world.level.biome.TheEndBiomeSource} with exactly one extra branch.
 */
public class ThornEndBiomeSource extends BiomeSource {
    public static final MapCodec<ThornEndBiomeSource> CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                            RegistryOps.retrieveElement(Biomes.THE_END),
                            RegistryOps.retrieveElement(Biomes.END_HIGHLANDS),
                            RegistryOps.retrieveElement(Biomes.END_MIDLANDS),
                            RegistryOps.retrieveElement(Biomes.SMALL_END_ISLANDS),
                            RegistryOps.retrieveElement(Biomes.END_BARRENS),
                            Biome.CODEC.fieldOf("biome").forGetter(s -> s.thorns),
                            Biome.CODEC.fieldOf("blocks_biome").forGetter(s -> s.blocks))
                    .apply(i, i.stable(ThornEndBiomeSource::new)));

    private final Holder<Biome> end;
    private final Holder<Biome> highlands;
    private final Holder<Biome> midlands;
    private final Holder<Biome> islands;
    private final Holder<Biome> barrens;
    private final Holder<Biome> thorns;
    private final Holder<Biome> blocks;

    private ThornEndBiomeSource(
            Holder<Biome> end,
            Holder<Biome> highlands,
            Holder<Biome> midlands,
            Holder<Biome> islands,
            Holder<Biome> barrens,
            Holder<Biome> thorns,
            Holder<Biome> blocks) {
        this.end = end;
        this.highlands = highlands;
        this.midlands = midlands;
        this.islands = islands;
        this.barrens = barrens;
        this.thorns = thorns;
        this.blocks = blocks;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.of(this.end, this.highlands, this.midlands, this.islands, this.barrens, this.thorns, this.blocks);
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
        int blockX = QuartPos.toBlock(quartX);
        int blockY = QuartPos.toBlock(quartY);
        int blockZ = QuartPos.toBlock(quartZ);
        int chunkX = SectionPos.blockToSectionCoord(blockX);
        int chunkZ = SectionPos.blockToSectionCoord(blockZ);
        if ((long) chunkX * chunkX + (long) chunkZ * chunkZ <= 15625L) {
            return this.end;
        }

        // vanilla samples the climate functions at the centre of the section, not at the quart pos
        int weirdBlockX = (chunkX * 2 + 1) * 8;
        int weirdBlockZ = (chunkZ * 2 + 1) * 8;
        DensityFunction.SinglePointContext at = new DensityFunction.SinglePointContext(weirdBlockX, blockY, weirdBlockZ);

        // forbid/mask is positive only on the central plateau, which is always inside a thorn island,
        // so it has to be tested first
        if (sampler.humidity().compute(at) > 0.0) {
            return this.blocks;
        }

        // island_mask is positive only inside a thorn island
        if (sampler.temperature().compute(at) > 0.0) {
            return this.thorns;
        }

        double heightValue = sampler.erosion().compute(at);
        if (heightValue > 0.25) {
            return this.highlands;
        } else if (heightValue >= -0.0625) {
            return this.midlands;
        } else {
            return heightValue < -0.21875 ? this.islands : this.barrens;
        }
    }
}
