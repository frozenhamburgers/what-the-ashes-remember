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
        if (level != null) {
            var worldData = level.getData(LodestoneAttachmentTypes.WORLD_EVENT_DATA);
            for (WorldEventInstance instance : worldData.activeWorldEvents) {
                if (instance.uuid.equals(payload.uuid())) {
                    instance.deserializeNBT(payload.eventData());
                    break;
                }
            }
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
