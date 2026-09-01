package net.jelly.echoesofwar.entity.trinity;

import net.jelly.echoesofwar.EchoesofWar;
import net.minecraft.resources.Identifier;
import team.lodestar.lodestone.modules.toolkit.worldevent.WorldEventType;
import team.lodestar.lodestone.registry.common.LodestoneWorldEventTypes;

import java.util.function.Supplier;

public class TrinityWorldEvents {
    public static final Supplier<WorldEventType> TRINITY = LodestoneWorldEventTypes.WORLD_EVENT_TYPES.register(
            "trinity",
            () -> WorldEventType.Builder.of(TrinityWorldEvent::new, Identifier.fromNamespaceAndPath(EchoesofWar.MODID, "trinity"))
                    .clientSynced()
                    .build()
    );

    public static void init() { //f or preloading
    }
}
