package team.lodestar.lodestone.registry.common;

import net.jelly.echoesofwar.EchoesofWar;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredRegister;
import team.lodestar.lodestone.modules.toolkit.worldevent.WorldEventType;

import java.util.List;

public class LodestoneWorldEventTypes {

    public static ResourceKey<Registry<WorldEventType>> WORLD_EVENT_TYPE_KEY = ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(EchoesofWar.MODID, "world_event_type"));
    public static final DeferredRegister<WorldEventType> WORLD_EVENT_TYPES = createRegistry(EchoesofWar.MODID);
    public static final Registry<WorldEventType> WORLD_EVENT_TYPE_REGISTRY = WORLD_EVENT_TYPES.makeRegistry(builder -> builder.sync(true));


    public static DeferredRegister<WorldEventType> createRegistry(String modId) {
        return DeferredRegister.create(WORLD_EVENT_TYPE_KEY, modId);
    }

    public static List<WorldEventType> getEventTypes() {
        return WORLD_EVENT_TYPE_REGISTRY.stream().toList();
    }
}
