package net.jelly.echoesofwar;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.jelly.echoesofwar.block.ModBlocks;
import net.jelly.echoesofwar.block.ModBlockEntities;
import net.jelly.echoesofwar.entity.ModEntities;
import net.jelly.echoesofwar.entity.apophis.smog.ApophisWorldEvents;
import net.jelly.echoesofwar.entity.talos.TalosWorldEvents;
import net.jelly.echoesofwar.entity.nuclear.NuclearWorldEvents;
import net.jelly.echoesofwar.entity.trinity.TrinityWorldEvents;
import net.jelly.echoesofwar.item.ModItems;
import net.jelly.echoesofwar.sound.ModSounds;
import net.jelly.echoesofwar.worldgen.ModWorldgen;
import team.lodestar.lodestone.registry.common.LodestoneAttachmentTypes;
import team.lodestar.lodestone.registry.common.LodestoneWorldEventTypes;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(EchoesofWar.MODID)
public class EchoesofWar {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "echoesofwar";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    // Deferred registers
    public static final DeferredRegister.Entities ENTITY_TYPES = DeferredRegister.createEntities(EchoesofWar.MODID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

//    // Creates a new Block with the id "echoesofwar:example_block", combining the namespace and path
//    public static final DeferredBlock<Block> EXAMPLE_BLOCK = BLOCKS.registerSimpleBlock("example_block", p -> p.mapColor(MapColor.STONE));
//    // Creates a new BlockItem with the id "echoesofwar:example_block", combining the namespace and path
//    public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("example_block", EXAMPLE_BLOCK);
//
//    // Creates a new food item with the id "echoesofwar:example_id", nutrition 1 and saturation 2
//    public static final DeferredItem<Item> EXAMPLE_ITEM = ITEMS.registerSimpleItem("example_item", p -> p.food(new FoodProperties.Builder()
//            .alwaysEdible().nutrition(1).saturationModifier(2f).build()));
//
    // Creates a creative tab with the id "echoesofwar:example_tab" for the example item, that is placed after the combat tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.echoesofwar")) //The language key for the title of your CreativeModeTab
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> ModBlocks.APOPHIS_PANDORAS_BOX.toStack())
            .displayItems((parameters, output) -> {
                output.accept(ModBlocks.TALOS_PANDORAS_BOX_ITEM.get());
                output.accept(ModBlocks.APOPHIS_PANDORAS_BOX_ITEM.get());
                output.accept(ModItems.KEY_OF_CONQUEST.get());
                output.accept(ModItems.KEY_OF_INDUSTRY.get());
                output.accept(ModItems.MISERY_OF_CONQUEST.get());
//                output.accept(ModItems.HOPE_OF_CREATION.get());
                output.accept(ModItems.MISERY_OF_INDUSTRY.get());
//                output.accept(ModItems.HOPE_OF_PROGRESS.get());
                output.accept(ModItems.MISERY_OF_MAN.get());
                output.accept(ModBlocks.DESTROYER_OF_WORLDS_ITEM.get());
            }).build());

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public EchoesofWar(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);
        LodestoneAttachmentTypes.ATTACHMENT_TYPES.register(modEventBus);
        LodestoneWorldEventTypes.WORLD_EVENT_TYPES.register(modEventBus);

        // Force load classes before deferred registeries are registered above
        ModBlocks.init();
        ModItems.init();
        TalosWorldEvents.init();
        ApophisWorldEvents.init();
        NuclearWorldEvents.init();
        TrinityWorldEvents.init();
        ModEntities.init();
        EchoesofWar.ENTITY_TYPES.register(modEventBus);
        ModBlockEntities.init();
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);

        // biome sources
        ModWorldgen.init();
        ModWorldgen.BIOME_SOURCES.register(modEventBus);
        ModWorldgen.STRUCTURE_PLACEMENTS.register(modEventBus);
        ModWorldgen.STRUCTURE_TYPES.register(modEventBus);
        ModWorldgen.STRUCTURE_PIECES.register(modEventBus);

        ModSounds.init();
        ModSounds.SOUND_EVENTS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (EchoesofWar) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
//        LOGGER.info("HELLO FROM COMMON SETUP");
//
//        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
//            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
//        }
//
//        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());
//
//        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
//        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
//            event.accept(EXAMPLE_BLOCK_ITEM);
//        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }
}
