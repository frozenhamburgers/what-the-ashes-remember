package team.lodestar.lodestone.modules.toolkit.worldevent;

import net.jelly.echoesofwar.EchoesofWar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import team.lodestar.lodestone.registry.common.LodestoneWorldEventTypes;

/**
 * Server -> client payload used to duplicate a {@link WorldEventInstance} to the client
 * when it is created, or when a player joins and existing client-synced events need to be
 * sent to them.
 * <p>
 * Ported from Lodestone 1.21's {@code SyncWorldEventPayload}, which used Lodestone's own
 * reflection-based payload registry, but this owns its own
 * {@link CustomPacketPayload.Type} and {@link StreamCodec} directly instead.
 */
public record SyncWorldEventPayload(Identifier eventTypeId, boolean start, CompoundTag eventData) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncWorldEventPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(EchoesofWar.MODID, "sync_world_event"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncWorldEventPayload> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, SyncWorldEventPayload::eventTypeId,
            ByteBufCodecs.BOOL, SyncWorldEventPayload::start,
            ByteBufCodecs.COMPOUND_TAG, SyncWorldEventPayload::eventData,
            SyncWorldEventPayload::new
    );

    public SyncWorldEventPayload(WorldEventInstance instance, boolean start) {
        this(instance.type.id, start, instance.serializeNBT());
    }

    public static void handle(SyncWorldEventPayload payload, IPayloadContext context) {
        WorldEventType eventType = LodestoneWorldEventTypes.WORLD_EVENT_TYPE_REGISTRY.getValue(payload.eventTypeId());
        ClientLevel level = Minecraft.getInstance().level;
        WorldEventHandler.addWorldEvent(level, payload.start(), eventType.createInstance(payload.eventData()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
