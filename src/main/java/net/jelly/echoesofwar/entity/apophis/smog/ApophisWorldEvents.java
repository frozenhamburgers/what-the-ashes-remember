package net.jelly.echoesofwar.entity.apophis.smog;

import net.jelly.echoesofwar.EchoesofWar;
import net.jelly.echoesofwar.entity.apophis.ApophisSummonWorldEvent;
import net.minecraft.resources.Identifier;
import team.lodestar.lodestone.modules.toolkit.worldevent.WorldEventType;
import team.lodestar.lodestone.registry.common.LodestoneWorldEventTypes;

import java.util.function.Supplier;

public class ApophisWorldEvents {
    public static final Supplier<WorldEventType> APOPHIS_SMOG = LodestoneWorldEventTypes.WORLD_EVENT_TYPES.register(
            "apophis_smog",
            () -> WorldEventType.Builder.of(ApophisSmogWorldEvent::new, Identifier.fromNamespaceAndPath(EchoesofWar.MODID, "apophis_smog"))
                    .clientSynced()
                    .build()
    );

    public static final Supplier<WorldEventType> APOPHIS_SUMMON = LodestoneWorldEventTypes.WORLD_EVENT_TYPES.register(
            "apophis_summon",
            () -> WorldEventType.Builder.of(ApophisSummonWorldEvent::new, Identifier.fromNamespaceAndPath(EchoesofWar.MODID, "apophis_summon"))
                    .clientSynced()
                    .build()
    );

    // must run before LodestoneWorldEventTypes.WORLD_EVENT_TYPES.register(modEventBus) runs
    public static void init() {
    }
}
