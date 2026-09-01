package net.jelly.echoesofwar.block;

import net.jelly.echoesofwar.EchoesofWar;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, EchoesofWar.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TalosBoxBlockEntity>> TALOS_PANDORAS_BOX = BLOCK_ENTITY_TYPES.register(
            "talos_pandoras_box",
            () -> new BlockEntityType<>(TalosBoxBlockEntity::new, ModBlocks.TALOS_PANDORAS_BOX.get())
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ApophisBoxBlockEntity>> APOPHIS_PANDORAS_BOX = BLOCK_ENTITY_TYPES.register(
            "apophis_pandoras_box",
            () -> new BlockEntityType<>(ApophisBoxBlockEntity::new, ModBlocks.APOPHIS_PANDORAS_BOX.get())
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrucibleOfCalamityBlockEntity>> CRUCIBLE_OF_CALAMITY = BLOCK_ENTITY_TYPES.register(
            "crucible_of_calamity",
            () -> new BlockEntityType<>(CrucibleOfCalamityBlockEntity::new, ModBlocks.CRUCIBLE_OF_CALAMITY.get())
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DestroyerOfWorldsBlockEntity>> DESTROYER_OF_WORLDS = BLOCK_ENTITY_TYPES.register(
            "destroyer_of_worlds",
            () -> new BlockEntityType<>(DestroyerOfWorldsBlockEntity::new, ModBlocks.DESTROYER_OF_WORLDS.get())
    );

    public static void init() {
    }
}
