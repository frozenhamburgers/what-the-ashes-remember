package net.jelly.echoesofwar.entity.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public abstract class ScheduledDemolition {

    protected int centreX;
    protected int centreY;
    protected int centreZ;

    private int delay = -1; // tick suntil demolition begin, negative = not scheduled
    private boolean running;

    protected final void schedule(BlockPos groundZero, int delayTicks) {
        centreX = groundZero.getX();
        centreY = groundZero.getY();
        centreZ = groundZero.getZ();
        delay = delayTicks;
        running = true;
        begin();
    }

    public final boolean isRunning() {
        return running;
    }

    public final void cancel() {
        running = false;
        delay = -1;
    }

    public final void tick(ServerLevel level) {
        if (!running) return;
        if (delay > 0) {
            delay--;
            return;
        }
        if (!step(level)) running = false;
    }

    protected abstract void begin();

    protected abstract boolean step(ServerLevel level);
}
