package net.jelly.echoesofwar.entity.talos;

import net.jelly.echoesofwar.EchoesofWar;
import net.minecraft.resources.Identifier;
import team.lodestar.lodestone.modules.toolkit.worldevent.WorldEventType;
import team.lodestar.lodestone.registry.common.LodestoneWorldEventTypes;

import java.util.function.Supplier;

public class TalosWorldEvents {
    public static final Supplier<WorldEventType> TALOS_SUMMON = LodestoneWorldEventTypes.WORLD_EVENT_TYPES.register(
            "talos_summon",
            () -> WorldEventType.Builder.of(TalosSummonWorldEvent::new, Identifier.fromNamespaceAndPath(EchoesofWar.MODID, "talos_summon"))
                    .clientSynced()
                    .build()
    );

    public static void init() {
    }
}
