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

    // ------ COMMON
    //  these NEED to be the same across the server and client. it would be a good idea to sync them later
    // so its not wise to change these right now in multiplayer

    public static final ModConfigSpec.IntValue TRINITY_SPAWN_COUNTDOWN = BUILDER
            .comment("Seconds between Trinity being summoned and its opening nuclear detonation.")
            .defineInRange("trinity.spawnCountdownSeconds", 20, 1, 300);

    public static final ModConfigSpec.IntValue TRINITY_CRITICAL_COUNTDOWN = BUILDER
            .comment("Seconds Trinity spends in its critical state before each mid-fight detonation.")
            .defineInRange("trinity.criticalCountdownSeconds", 10, 1, 120);

    public static final ModConfigSpec.DoubleValue TRINITY_ACTIVE_RANGE = BUILDER
            .comment("Blocks from Trinity a player must be within to count as present.")
            .defineInRange("trinity.activeRange", 200.0, 32.0, 2048.0);

    public static final ModConfigSpec.IntValue TRINITY_ABANDON_TIMEOUT = BUILDER
            .comment("Seconds with no player inside the active range after which Trinity despawns.")
            .defineInRange("trinity.abandonTimeoutSeconds", 5, 1, 3600);

    public static final ModConfigSpec.IntValue DESTROYER_OF_WORLDS_COUNTDOWN = BUILDER
            .comment("Seconds between the Destroyer of Worlds being armed and its detonation.")
            .defineInRange("destroyerOfWorlds.countdownSeconds", 20, 1, 300);

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
