package team.lodestar.lodestone.registry.common;

import net.jelly.echoesofwar.EchoesofWar;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import team.lodestar.lodestone.modules.toolkit.worldevent.WorldEventAttachment;

import java.util.function.Supplier;

/**
 * Only the world-event attachment is ported here; Lodestone's other (unrelated) attachment
 * types are out of scope for this port.
 */
public class LodestoneAttachmentTypes {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, EchoesofWar.MODID);

    public static final Supplier<AttachmentType<WorldEventAttachment>> WORLD_EVENT_DATA = ATTACHMENT_TYPES.register(
            "world_event_data", () -> AttachmentType.serializable(WorldEventAttachment::new).build()
    );
}
