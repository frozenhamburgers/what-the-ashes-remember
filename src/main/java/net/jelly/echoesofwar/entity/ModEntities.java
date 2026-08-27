package net.jelly.echoesofwar.entity;

import net.jelly.echoesofwar.EchoesofWar;
import net.jelly.echoesofwar.entity.apophis.ApophisEntity;
import net.jelly.echoesofwar.entity.talos.TalosEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    public static final DeferredRegister.Entities ENTITY_TYPES = DeferredRegister.createEntities(EchoesofWar.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<TalosEntity>> TALOS = ENTITY_TYPES.registerEntityType(
            "talos",
            TalosEntity::new,
            MobCategory.MONSTER,
            builder -> builder
                    .sized(1.5f, 3.6f)
                    .eyeHeight(3.2f)
                    .clientTrackingRange(64)
                    .fireImmune()
                    .notInPeaceful()
    );

    public static final DeferredHolder<EntityType<?>, EntityType<ApophisEntity>> APOPHIS = ENTITY_TYPES.registerEntityType(
            "apophis",
            ApophisEntity::new,
            MobCategory.MONSTER,
            builder -> builder
                    // the entity itself is only the head - the ~100 blocks of body behind it are
                    // Marionette parts, which have their own hitboxes and are not tracked separately
                    .sized(4.0f, 3.5f)
                    .eyeHeight(1.75f)
                    // it covers over a block a tick, so a coarse tracking range would leave it
                    // popping in already on top of the player. every-tick updates keep the head
                    // authoritative, which is the one thing the client can't re-derive for itself
                    // (see ApophisEntity's class javadoc)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .setShouldReceiveVelocityUpdates(true)
                    .fireImmune()
                    .notInPeaceful()
    );

    public static void init() {
    }
}
