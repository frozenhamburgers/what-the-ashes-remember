package team.lodestar.lodestone.registry.common;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import team.lodestar.lodestone.modules.toolkit.worldevent.SyncWorldEventPayload;
import team.lodestar.lodestone.modules.toolkit.worldevent.UpdateWorldEventPayload;

@EventBusSubscriber
public class LodestoneNetworking {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(SyncWorldEventPayload.TYPE, SyncWorldEventPayload.STREAM_CODEC, SyncWorldEventPayload::handle);
        registrar.playToClient(UpdateWorldEventPayload.TYPE, UpdateWorldEventPayload.STREAM_CODEC, UpdateWorldEventPayload::handle);
    }
}
