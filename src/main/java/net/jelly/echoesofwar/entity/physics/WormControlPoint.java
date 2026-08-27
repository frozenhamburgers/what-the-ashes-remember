package net.jelly.echoesofwar.entity.physics;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// lightweight non-entity physics point that anchors one segment of a worm's FABRIK chain to the terrain
// basically no state, tick() resyncs position to the chain's solve every tick, so only velocity
// carries across ticks, which is zeroed on grounding anyways
public class WormControlPoint {
    private static final double GROUND_FRICTION = 0.8;

    private Vec3 position;
    private Vec3 velocity = Vec3.ZERO;
    private boolean grounded = false;

    public WormControlPoint(Vec3 initialPosition) {
        this.position = initialPosition;
    }

    public Vec3 getPosition() {
        return position;
    }

    public boolean isGrounded() {
        return grounded;
    }

    public void tick(Level level, Vec3 chainPosition, Vec3 gravity, double maxSpeed, double probeHalfWidth) {
        position = chainPosition;

        SurfaceNormalSampler.Sample ground = SurfaceNormalSampler.sample(level, position, probeHalfWidth);

        if (ground.contact() == SurfaceNormalSampler.Contact.EMBEDDED) {
            // fully surrounded by solid ground, so no physics
            velocity = Vec3.ZERO;
            grounded = true;
            return;
        }

        velocity = WormPhysics.resolveGroundVelocity(velocity, Vec3.ZERO, gravity, ground, GROUND_FRICTION);
        if (velocity.length() > maxSpeed) velocity = velocity.normalize().scale(maxSpeed);
        position = position.add(velocity);

        // groundY() -infinity when airborne
        if (position.y <= ground.groundY()) {
            position = new Vec3(position.x, ground.groundY(), position.z);
            velocity = Vec3.ZERO;
            grounded = true;
        } else {
            grounded = false;
        }
    }
}
