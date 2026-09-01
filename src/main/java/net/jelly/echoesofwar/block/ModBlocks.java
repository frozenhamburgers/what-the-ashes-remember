package net.jelly.echoesofwar.block;

import net.jelly.echoesofwar.EchoesofWar;
import net.jelly.echoesofwar.item.GradientNameBlockItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.function.UnaryOperator;

public class ModBlocks {
    public static final DeferredBlock<TalosBoxBlock> TALOS_PANDORAS_BOX = EchoesofWar.BLOCKS.registerBlock(
            "talos_pandoras_box",
            TalosBoxBlock::new,
            (UnaryOperator<BlockBehaviour.Properties>) properties -> properties
                    .mapColor(MapColor.COLOR_BROWN)
                    .sound(SoundType.WOOD)
                    .strength(1.0f, 3600000.0f)
                    .lightLevel(state -> 7)
                    .noOcclusion()
    );

    public static final DeferredItem<BlockItem> TALOS_PANDORAS_BOX_ITEM = EchoesofWar.ITEMS.registerSimpleBlockItem(
            "talos_pandoras_box", TALOS_PANDORAS_BOX
    );

    public static final DeferredBlock<ApophisBoxBlock> APOPHIS_PANDORAS_BOX = EchoesofWar.BLOCKS.registerBlock(
            "apophis_pandoras_box",
            ApophisBoxBlock::new,
            (UnaryOperator<BlockBehaviour.Properties>) properties -> properties
                    .mapColor(MapColor.COLOR_BROWN)
                    .sound(SoundType.WOOD)
                    .strength(1.0f, 3600000.0f)
                    .lightLevel(state -> 7)
                    .noOcclusion()
    );

    public static final DeferredItem<BlockItem> APOPHIS_PANDORAS_BOX_ITEM = EchoesofWar.ITEMS.registerSimpleBlockItem(
            "apophis_pandoras_box", APOPHIS_PANDORAS_BOX
    );

    // no item form/recipe/creative registration - only generated naturally, as part of a structure template
    public static final DeferredBlock<CrucibleOfCalamityBlock> CRUCIBLE_OF_CALAMITY = EchoesofWar.BLOCKS.registerBlock(
            "crucible_of_calamity",
            CrucibleOfCalamityBlock::new,
            (UnaryOperator<BlockBehaviour.Properties>) properties -> properties
                    .mapColor(MapColor.COLOR_BLACK)
                    .sound(SoundType.STONE)
                    .strength(-1.0f, 3600000.0f)
                    .noOcclusion()
    );

    // fills the other 44 cells of the crucible's 3x3x5 footprint - same properties, no item/recipe/creative registration
    public static final DeferredBlock<CrucibleOfCalamityPartBlock> CRUCIBLE_OF_CALAMITY_PART = EchoesofWar.BLOCKS.registerBlock(
            "crucible_of_calamity_part",
            CrucibleOfCalamityPartBlock::new,
            (UnaryOperator<BlockBehaviour.Properties>) properties -> properties
                    .mapColor(MapColor.COLOR_BLACK)
                    .sound(SoundType.STONE)
                    .strength(-1.0f, 3600000.0f)
                    .noOcclusion()
    );

    public static final DeferredBlock<DestroyerOfWorldsBlock> DESTROYER_OF_WORLDS = EchoesofWar.BLOCKS.registerBlock(
            "destroyer_of_worlds",
            DestroyerOfWorldsBlock::new,
            (UnaryOperator<BlockBehaviour.Properties>) properties -> properties
                    .mapColor(MapColor.COLOR_GRAY)
                    .sound(SoundType.METAL)
                    .strength(3.0f, 3600000.0f)
    );

    public static final DeferredItem<GradientNameBlockItem> DESTROYER_OF_WORLDS_ITEM = EchoesofWar.ITEMS.registerItem(
            "destroyer_of_worlds",
            properties -> new GradientNameBlockItem(DESTROYER_OF_WORLDS.get(), properties,
                    "Destroyer of Worlds", 0xF0E5A0, 0x8FB6A0),
            (UnaryOperator<Item.Properties>) properties -> properties.useBlockDescriptionPrefix()
    );

    public static void init() {
    }
}
