package net.jelly.echoesofwar.entity;

import net.jelly.echoesofwar.EchoesofWar;
import net.jelly.echoesofwar.entity.apophis.ApophisEntity;
import net.jelly.echoesofwar.entity.talos.TalosEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {

    public static final DeferredHolder<EntityType<?>, EntityType<TalosEntity>> TALOS = EchoesofWar.ENTITY_TYPES.registerEntityType(
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

    public static final DeferredHolder<EntityType<?>, EntityType<ApophisEntity>> APOPHIS = EchoesofWar.ENTITY_TYPES.registerEntityType(
            "apophis",
            ApophisEntity::new,
            MobCategory.MONSTER,
            builder -> builder
                    .sized(4.0f, 3.5f)
                    .eyeHeight(1.75f)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .setShouldReceiveVelocityUpdates(true)
                    .fireImmune()
                    .notInPeaceful()
    );

    public static void init() {
    }
}
