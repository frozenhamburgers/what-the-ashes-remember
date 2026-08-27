package net.jelly.echoesofwar;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
            .comment("Whether to log the dirt block on common setup")
            .define("logDirtBlock", true);

    public static final ModConfigSpec.IntValue MAGIC_NUMBER = BUILDER
            .comment("A magic number")
            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
            .comment("What you want the introduction message to be for the magic number")
            .define("magicNumberIntroduction", "The magic number is... ");

    // a list of strings that are treated as resource locations for items
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("A list of items to log on common setup.")
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), () -> "", Config::validateItemName);

    static final ModConfigSpec SPEC = BUILDER.build();

    // ------------------------------------------------------------------ client

    private static final ModConfigSpec.Builder CLIENT_BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue VOLUMETRIC_RESOLUTION_SCALE = CLIENT_BUILDER
            .comment("Fraction of the window resolution every volumetric effect is raymarched at -",
                    "Apophis's smog, the spice eruption, and anything else built on the same pipeline.",
                    "Raymarching is expensive per pixel, so this is the main performance dial for all",
                    "of them; because the result is upscaled bilinearly, low values read as softness",
                    "rather than as blockiness. 0.25 matches the 480x270 these effects were originally",
                    "authored against on a 1080p window. Takes effect immediately.")
            .defineInRange("volumetricResolutionScale", 0.25, 0.05, 1.0);

    public static final ModConfigSpec.DoubleValue ATMOSPHERE_INTENSITY = CLIENT_BUILDER
            .comment("How strongly the Apophis fight rewrites the weather, 0 to 1. Scales all three",
                    "atmospheric effects together: the drop in ambient skylight, the smog banked",
                    "across the horizon, and the noise fog swallowing distant terrain. 0 disables",
                    "them entirely and leaves the volumetric cloud and its glow untouched. The",
                    "effects still fade in and out with the fight; this only sets how far they go.",
                    "Takes effect immediately.")
            .defineInRange("atmosphereIntensity", 1.0, 0.0, 1.0);

    static final ModConfigSpec CLIENT_SPEC = CLIENT_BUILDER.build();

    private static boolean validateItemName(final Object obj) {
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(Identifier.parse(itemName));
    }
}
