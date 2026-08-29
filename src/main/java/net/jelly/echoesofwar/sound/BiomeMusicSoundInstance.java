package net.jelly.echoesofwar.sound;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

// call client only
public class BiomeMusicSoundInstance extends AbstractTickableSoundInstance {
    public BiomeMusicSoundInstance(SoundEvent sound) {
        super(sound, SoundSource.MUSIC, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.delay = 0;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.relative = true;
        this.volume = 0.0F;
    }

    public float getRawVolume() {
        return this.volume;
    }

    public void setVolume(float volume) {
        this.volume = Mth.clamp(volume, 0.0F, 1.0F);
    }

    @Override
    public void tick() {
    }
}
