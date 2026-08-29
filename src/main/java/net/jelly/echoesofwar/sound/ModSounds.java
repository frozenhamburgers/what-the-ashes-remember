package net.jelly.echoesofwar.sound;

import net.jelly.echoesofwar.EchoesofWar;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, EchoesofWar.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> THIS_PLACE_IS_A_MESSAGE = SOUND_EVENTS.register(
            "this_place_is_a_message",
            () -> SoundEvent.createVariableRangeEvent(
                    Identifier.fromNamespaceAndPath(EchoesofWar.MODID, "this_place_is_a_message")));

    public static void init() {
    }
}
