package team.lodestar.lodestone.events;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import team.lodestar.lodestone.modules.toolkit.worldevent.WorldEventHandler;

@EventBusSubscriber
public class WorldEventGameEvents {

    @SubscribeEvent
    public static void entityJoin(EntityJoinLevelEvent event) {
        WorldEventHandler.playerJoin(event);
    }

    @SubscribeEvent
    public static void worldTick(LevelTickEvent.Post event) {
        WorldEventHandler.worldTick(event);
    }
}
