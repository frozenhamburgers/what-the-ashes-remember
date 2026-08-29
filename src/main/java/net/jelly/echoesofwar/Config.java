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

    static final ModConfigSpec SPEC = BUILDER.build();

    // ------------------------------------------------------------------ client

    private static final ModConfigSpec.Builder CLIENT_BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue VOLUMETRIC_RESOLUTION_SCALE = CLIENT_BUILDER
            .comment("Fraction of the window resolution every volumetric effect is raymarched at.",
                    "Heavily effects resolution and smoothness of volumetrics and client FPS ")
            .defineInRange("volumetricResolutionScale", 0.25, 0.05, 1.0);

    public static final ModConfigSpec.DoubleValue ATMOSPHERE_INTENSITY = CLIENT_BUILDER
            .comment("Apophis fight ambient weather effects, 0 to 1.")
            .defineInRange("atmosphereIntensity", 1.0, 0.0, 1.0);

    static final ModConfigSpec CLIENT_SPEC = CLIENT_BUILDER.build();

    private static boolean validateItemName(final Object obj) {
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(Identifier.parse(itemName));
    }
}
