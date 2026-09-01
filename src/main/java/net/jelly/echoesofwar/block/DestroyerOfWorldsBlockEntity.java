package net.jelly.echoesofwar.block;

import net.jelly.echoesofwar.Config;
import net.jelly.echoesofwar.entity.nuclear.DetonationSounds;
import net.jelly.echoesofwar.entity.nuclear.NuclearDetonationWorldEvent;
import net.jelly.echoesofwar.entity.trinity.DetonationBlast;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import team.lodestar.lodestone.modules.toolkit.worldevent.WorldEventHandler;

public class DestroyerOfWorldsBlockEntity extends BlockEntity {

    public static final double INITIAL_BLAST_RADIUS = 100.0;
    public static final float INITIAL_BLAST_DAMAGE = 200f;

    private boolean armed;
    private int ticks;

    public DestroyerOfWorldsBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DESTROYER_OF_WORLDS.get(), pos, state);
    }

    public static int countdownTicks() {
        return Config.DESTROYER_OF_WORLDS_COUNTDOWN.get() * 20;
    }

    public void arm() {
        if (armed) return;
        armed = true;
        ticks = 0;
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  DestroyerOfWorldsBlockEntity bomb) {
        if (!bomb.armed) return;

        bomb.ticks++;
        int countdown = countdownTicks();
        if (bomb.ticks < countdown) {
            DetonationSounds.tickCountdownWarning(level, pos, bomb.ticks, countdown);
            return;
        }

        bomb.armed = false;
        detonate((ServerLevel) level, pos);
    }

    private static void detonate(ServerLevel level, BlockPos pos) {
        level.removeBlock(pos, false);

        Vec3 base = Vec3.atCenterOf(pos);
        DetonationBlast.applyInitial(level, base, INITIAL_BLAST_RADIUS, INITIAL_BLAST_DAMAGE);

        float seed = (float) ((base.x * 12.9898 + base.z * 78.233) % 1000.0);
        NuclearDetonationWorldEvent detonation = new NuclearDetonationWorldEvent()
                .setup(base, NuclearDetonationWorldEvent.DEFAULT_HEIGHT,
                        NuclearDetonationWorldEvent.DEFAULT_CAP_RADIUS, seed,
                        NuclearDetonationWorldEvent.DEFAULT_LIFETIME_TICKS)
                .withGroundEffects(pos);
        detonation.setDirty();
        WorldEventHandler.addWorldEvent(level, detonation);

        DetonationSounds.playDetonation(level, pos, false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("armed", armed);
        output.putInt("ticks", ticks);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        armed = input.getBooleanOr("armed", false);
        ticks = input.getIntOr("ticks", 0);
    }
}
