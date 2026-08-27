package team.lodestar.lodestone.modules.toolkit.worldevent;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import team.lodestar.lodestone.registry.client.LodestoneWorldEventRenderers;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * TODO: port Lodestone's command line worldevent debug features
 */
public class WorldEventType {
    public final Identifier id;
    public final EventInstanceSupplier<?> supplier;
    public final boolean clientSynced;

    /**
     * @param clientSynced whether this event exists on the client; auto-synced on creation via
     *                      {@link WorldEventInstance#sync(net.minecraft.world.level.Level)}
     */
    public WorldEventType(Identifier id, EventInstanceSupplier<?> supplier, boolean clientSynced) {
        this.id = id;
        this.supplier = supplier;
        this.clientSynced = clientSynced;
    }

    // defaults to not client-synced
    public WorldEventType(Identifier id, EventInstanceSupplier<?> supplier) {
        this(id, supplier, false);
    }

    public boolean isClientSynced() {
        return clientSynced;
    }

    public WorldEventInstance createInstance(CompoundTag tag) {
        return supplier.getInstance().deserializeNBT(tag);
    }

    public static class Builder<T extends WorldEventInstance> {
        private final Identifier id;
        private final EventInstanceSupplier<T> supplier;
        private boolean clientSynced;
        private @Nullable Supplier<WorldEventRenderer<T>> rendererSupplier;

        private Builder(EventInstanceSupplier<T> supplier, Identifier id) {
            this.id = id;
            this.supplier = supplier;
        }

        public static <T extends WorldEventInstance> Builder<T> of(EventInstanceSupplier<T> supplier, Identifier id) {
            return new Builder<>(supplier, id);
        }

        public Builder<T> clientSynced(@Nullable Supplier<WorldEventRenderer<T>> rendererSupplier) {
            this.clientSynced = true;
            this.rendererSupplier = rendererSupplier;
            return this;
        }

        public Builder<T> clientSynced() {
            return clientSynced(null);
        }

        public WorldEventType build() {
            WorldEventType type = new WorldEventType(this.id, this.supplier, this.clientSynced);
            if (FMLEnvironment.getDist().equals(Dist.CLIENT)) {
                LodestoneWorldEventRenderers.registerRenderer(type, this.rendererSupplier != null ? this.rendererSupplier.get() : null);
            }
            return type;
        }
    }

    public interface EventInstanceSupplier<T extends WorldEventInstance> {
        T getInstance();
    }
}
