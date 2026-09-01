package net.jelly.echoesofwar.sound;

import net.jelly.echoesofwar.EchoesofWar;
import net.jelly.echoesofwar.entity.trinity.TrinityWorldEvent;
import net.jelly.echoesofwar.worldgen.ModWorldgen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

// drives EchoesMusicManager based on biome
@EventBusSubscriber(modid = EchoesofWar.MODID, value = Dist.CLIENT)
public final class BiomeMusicTrigger {
    private BiomeMusicTrigger() {
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) return;

        boolean noBosses = TrinityWorldEvent.find(level) == null;


        Holder<Biome> biome = level.getBiome(mc.player.blockPosition());
        if (biome.is(ModWorldgen.LANDSCAPE_OF_THORNS) || biome.is(ModWorldgen.FORBIDDING_BLOCKS) && noBosses) {
            ModMusicManager.requestTrack(ModSounds.THIS_PLACE_IS_A_MESSAGE.get());
        } else if (biome.is(ModWorldgen.CONTAINMENT_ZONE) && noBosses) {
            ModMusicManager.requestSilence();
        } else if (noBosses){
            ModMusicManager.release();
        }
    }
}
