package team.lodestar.lodestone.modules.toolkit.worldevent;

import net.jelly.echoesofwar.EchoesofWar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import team.lodestar.lodestone.registry.common.LodestoneAttachmentTypes;

import java.util.UUID;

/**
 * Server -> client payload sent every tick a {@link WorldEventInstance} is marked dirty,
 * carrying its updated NBT so the client-side copy of the event can be refreshed in place.
 * <p>
 * See {@link SyncWorldEventPayload} for notes on why this owns its own
 * {@link CustomPacketPayload.Type}/{@link StreamCodec} instead of using Lodestone's
 * original payload-registry indirection.
 */
public record UpdateWorldEventPayload(UUID uuid, CompoundTag eventData) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<UpdateWorldEventPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(EchoesofWar.MODID, "update_world_event"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateWorldEventPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, UpdateWorldEventPayload::uuid,
            ByteBufCodecs.COMPOUND_TAG, UpdateWorldEventPayload::eventData,
            UpdateWorldEventPayload::new
    );

    public UpdateWorldEventPayload(WorldEventInstance instance) {
        this(instance.uuid, instance.serializeNBT());
    }

    public static void handle(UpdateWorldEventPayload payload, IPayloadContext context) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        var worldData = level.getData(LodestoneAttachmentTypes.WORLD_EVENT_DATA);
        if (apply(worldData.activeWorldEvents, payload)) return;
        // Also the inbound queue, or an event that changes state on its very first server tick
        // loses that update outright: the client only moves inbound -> active on ITS next tick,
        // and until then this lookup would not find the instance the sync packet just created.
        // The symptom is a client stuck on whatever state it was created with, until some later
        // update happens to arrive after it has been promoted.
        apply(worldData.inboundWorldEvents, payload);
    }

    private static boolean apply(Iterable<WorldEventInstance> instances, UpdateWorldEventPayload payload) {
        for (WorldEventInstance instance : instances) {
            if (instance.uuid.equals(payload.uuid())) {
                instance.deserializeNBT(payload.eventData());
                return true;
            }
        }
        return false;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
