package team.lodestar.lodestone.modules.toolkit.worldevent;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import team.lodestar.lodestone.registry.common.LodestoneWorldEventTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * Ported from Lodestone 1.21's {@code WorldEventAttachment}, which implemented
 * {@code INBTSerializable<CompoundTag>}. That no longer exists on {@code AttachmentType} in
 * 26.1.2, so this implements {@link ValueIOSerializable} instead, bridging to the original
 * CompoundTag-based read/write via {@code ValueOutput.store}/{@code ValueInput.read} with
 * {@code CompoundTag.CODEC} - the tag layout itself is unchanged.
 */
public class WorldEventAttachment implements ValueIOSerializable {

    public final ArrayList<WorldEventInstance> activeWorldEvents = new ArrayList<>();
    public final ArrayList<WorldEventInstance> inboundWorldEvents = new ArrayList<>();

    @Override
    public void serialize(ValueOutput output) {
        // filtered first and indexed afterwards, so skipping one doesn't leave a hole in the
        // worldEvent_<i> sequence that deserialize() would read back as an empty tag
        List<WorldEventInstance> saved = new ArrayList<>();
        for (WorldEventInstance instance : activeWorldEvents) {
            if (instance.shouldSave()) saved.add(instance);
        }

        CompoundTag worldTag = new CompoundTag();
        worldTag.putInt("worldEventCount", saved.size());
        for (int i = 0; i < saved.size(); i++) {
            worldTag.put("worldEvent_" + i, saved.get(i).serializeNBT());
        }
        output.store("worldEventData", CompoundTag.CODEC, worldTag);
    }

    @Override
    public void deserialize(ValueInput input) {
        activeWorldEvents.clear();
        CompoundTag worldTag = input.read("worldEventData", CompoundTag.CODEC).orElse(new CompoundTag());
        int worldEventCount = worldTag.getIntOr("worldEventCount", 0);
        for (int i = 0; i < worldEventCount; i++) {
            CompoundTag instanceTag = worldTag.getCompoundOrEmpty("worldEvent_" + i);
            WorldEventType type = LodestoneWorldEventTypes.WORLD_EVENT_TYPE_REGISTRY.getValue(Identifier.parse(instanceTag.getStringOr("type", "")));
            WorldEventInstance eventInstance = type.createInstance(instanceTag);
            // also filtered on the way in, so a save written before an event opted out of
            // persistence is cleaned up rather than replaying one last time
            if (eventInstance.shouldSave()) activeWorldEvents.add(eventInstance);
        }
    }
}
