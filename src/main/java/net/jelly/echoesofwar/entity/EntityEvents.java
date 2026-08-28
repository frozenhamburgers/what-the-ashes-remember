package net.jelly.echoesofwar.entity;

import net.jelly.echoesofwar.EchoesofWar;
import net.jelly.echoesofwar.block.ApophisBoxRenderer;
import net.jelly.echoesofwar.block.ModBlocks;
import net.jelly.echoesofwar.block.ModBlockEntities;
import net.jelly.echoesofwar.entity.apophis.ApophisEntity;
import net.jelly.echoesofwar.entity.apophis.client.ApophisModel;
import net.jelly.echoesofwar.entity.apophis.client.ApophisRenderer;
import net.jelly.echoesofwar.entity.apophis.smog.ApophisPostProcessor;
import net.jelly.echoesofwar.entity.talos.TalosEntity;
import net.jelly.echoesofwar.block.TalosBoxRenderer;
import net.jelly.echoesofwar.entity.talos.client.TalosModel;
import net.jelly.echoesofwar.entity.talos.client.TalosRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import team.lodestar.lodestone.modules.rendering.postprocess.PostProcessHandler;

public class EntityEvents {

    @EventBusSubscriber
    public static class Common {
        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
        }

        @SubscribeEvent
        public static void onAttributeCreate(EntityAttributeCreationEvent event) {
            event.put(ModEntities.TALOS.get(), TalosEntity.createAttributes().build());
            event.put(ModEntities.APOPHIS.get(), ApophisEntity.createAttributes().build());
        }

        // cancels shift dismount while Talos is carrying the player, server only
        @SubscribeEvent
        public static void onEntityMount(EntityMountEvent event) {
            if (event.isMounting() || event.getLevel().isClientSide()) return;
            if (event.getEntityBeingMounted() instanceof TalosEntity talos && talos.isCarrying(event.getEntityMounting())) {
                event.setCanceled(true);
            }
        }
    }

    @EventBusSubscriber(value = Dist.CLIENT)
    public static class Client {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            PostProcessHandler.addInstance(ApophisPostProcessor.INSTANCE);
        }

        @SubscribeEvent
        public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(TalosModel.LAYER_LOCATION, TalosModel::createBodyLayer);
            event.registerLayerDefinition(ApophisModel.LAYER_LOCATION, ApophisModel::createBodyLayer);
        }

        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntities.TALOS.get(), TalosRenderer::new);
            event.registerEntityRenderer(ModEntities.APOPHIS.get(), ApophisRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.TALOS_PANDORAS_BOX.get(), TalosBoxRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.APOPHIS_PANDORAS_BOX.get(), ApophisBoxRenderer::new);

        }
    }
}
