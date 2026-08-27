package team.lodestar.lodestone.modules.toolkit.worldevent;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.*;
import team.lodestar.lodestone.registry.common.LodestoneWorldEventTypes;

import java.util.UUID;

/**
 * World events are tickable instanced objects which are saved in a level capability, which means they are unique per dimension.
 * They can exist on the client and are ticked separately.
 * @see <a href="https://github.com/LodestarMC/Lodestone/wiki/World-Events">Lodestone World Events Wiki</a>
 */
public abstract class WorldEventInstance {
    public UUID uuid;
    public WorldEventType type;
    public Level level;
    public boolean discarded;
    public boolean dirty;
    public boolean frozen;

    public WorldEventInstance(WorldEventType type) {
        if (type == null) throw new IllegalArgumentException("World event type cannot be null");
        this.uuid = UUID.randomUUID();
        this.type = type;
    }

    public void start(Level level) {
        this.level = level;
    }

    public abstract void tick(Level level);

    protected abstract void addAdditionalSaveData(CompoundTag tag);

    protected abstract void readAdditionalSaveData(CompoundTag tag);

    public void end(Level level) {
        discarded = true;
    }

    // synced to the client next tick, then cleared
    public void setDirty() {
        dirty = true;
    }

    /**
     * Whether this event belongs in the save file.
     */
    public boolean shouldSave() {
        return true;
    }

    // skipped by WorldEventHandler.tick(Level) while true
    public boolean isFrozen() {
        return frozen;
    }

    public Level getLevel() {
        return level;
    }

    @ApiStatus.Internal
    public final CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.store("uuid", UUIDUtil.CODEC, uuid);
        tag.putString("type", type.id.toString());
        tag.putBoolean("discarded", discarded);
        tag.putBoolean("frozen", frozen);
        this.addAdditionalSaveData(tag);
        return tag;
    }

    @ApiStatus.Internal
    public final WorldEventInstance deserializeNBT(CompoundTag tag) {
        uuid = tag.read("uuid", UUIDUtil.CODEC).orElseThrow();
        type = LodestoneWorldEventTypes.WORLD_EVENT_TYPE_REGISTRY.getValue(Identifier.parse(tag.getStringOr("type", "")));
        discarded = tag.getBooleanOr("discarded", false);
        frozen = tag.getBooleanOr("frozen", false);
        this.readAdditionalSaveData(tag);
        return this;
    }

    @ApiStatus.Internal
    public void sync(Level level) {
        if (!level.isClientSide() && type.isClientSynced()) {
            sync(this);
        }
    }

    @ApiStatus.Internal
    public CompoundTag synchronizeNBT() {
        return serializeNBT();
    }

    // only call once per world event instance
    @ApiStatus.Internal
    public static <T extends WorldEventInstance> void sync(T instance) {
        sync(instance, null);
    }

    @ApiStatus.Internal
    public static <T extends WorldEventInstance> void sync(T instance, @Nullable ServerPlayer player) {
        if (player != null) {
            PacketDistributor.sendToPlayer(player, new SyncWorldEventPayload(instance, false));
            return;
        }
        PacketDistributor.sendToAllPlayers(new SyncWorldEventPayload(instance, false));
    }
}
