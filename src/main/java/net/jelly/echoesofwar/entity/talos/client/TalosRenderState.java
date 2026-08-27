package net.jelly.echoesofwar.entity.talos.client;

import net.jelly.marionette_lib.utility.MarionetteModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.phys.Vec3;

// carries per-frame Marionette segment data since models can no longer read it off the live entity
// TODO: integrate this into Marionette
public class TalosRenderState extends LivingEntityRenderState implements MarionetteModel.MarionetteRenderState {
    public Vec3[] partDirections = new Vec3[0];
    public Vec3[] partOffsets = new Vec3[0];

    @Override
    public Vec3[] marionettePartDirections() {
        return partDirections;
    }

    @Override
    public Vec3[] marionettePartOffsets() {
        return partOffsets;
    }
}
