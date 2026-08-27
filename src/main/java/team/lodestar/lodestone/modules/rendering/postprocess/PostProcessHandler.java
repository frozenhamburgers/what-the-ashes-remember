package team.lodestar.lodestone.modules.rendering.postprocess;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.jelly.echoesofwar.EchoesofWar;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles world-space post-processing, driving every registered {@link PostProcessor}
 * once per frame.
 * <p>
 * Ported from Lodestone 1.21.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public class PostProcessHandler {
    private static final List<PostProcessor> instances = new ArrayList<>();

    /**
     * Add a PostProcessor for it to be handled automatically.
     * IMPORTANT: processors have to be added in the right order!!!
     * There's no way of getting an instance, so you need to keep the instance yourself.
     */
    public static void addInstance(PostProcessor instance) {
        instances.add(instance);
    }

    public static void render() {
        instances.forEach(PostProcessor::applyPostProcess);
    }

    public static void copyDepthBuffer() {
        instances.forEach(PostProcessor::copyDepthBuffer);
    }

    public static void resize(int width, int height) {
        instances.forEach(i -> i.resize(width, height));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAfterLevel(RenderLevelStageEvent.AfterLevel event) {
        copyDepthBuffer();
        render();
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            instances.forEach(PostProcessor::onClientLevelUnload);
        }
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath(EchoesofWar.MODID, "lodestone_postprocess"), new SimplePreparableReloadListener<Object>() {
            private static final Object MARKER = new Object();

            @Override
            protected Object prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
                return MARKER;
            }

            @Override
            protected void apply(Object marker, ResourceManager resourceManager, ProfilerFiller profiler) {
                instances.forEach(PostProcessor::init);
            }
        });
    }
}
