package net.jelly.echoesofwar.entity.nuclear;

import net.jelly.echoesofwar.EchoesofWar;
import net.minecraft.resources.Identifier;
import team.lodestar.lodestone.modules.toolkit.worldevent.WorldEventType;
import team.lodestar.lodestone.registry.common.LodestoneWorldEventTypes;

import java.util.function.Supplier;

public class NuclearWorldEvents {
    public static final Supplier<WorldEventType> TRINITY_DETONATION = LodestoneWorldEventTypes.WORLD_EVENT_TYPES.register(
            "trinity_detonation",
            () -> WorldEventType.Builder.of(NuclearDetonationWorldEvent::new, Identifier.fromNamespaceAndPath(EchoesofWar.MODID, "trinity_detonation"))
                    .clientSynced()
                    .build()
    );

    // must run before LodestoneWorldEventTypes.WORLD_EVENT_TYPES.register(modEventBus) runs
    public static void init() {
    }
}
