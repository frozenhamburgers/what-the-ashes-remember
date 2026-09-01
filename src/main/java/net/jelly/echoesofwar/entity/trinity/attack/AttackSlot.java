package net.jelly.echoesofwar.entity.trinity.attack;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

/**
 * One piece of geometry for shader and hit detection
 */
public class AttackSlot {

    // --- type ids. MMUST match the ATK_* constants in instance.glsl ---
    public static final int TYPE_NONE        = 0;
    public static final int TYPE_PREDICTIVE  = 1; // cone, narrow, leads the player
    public static final int TYPE_LASER       = 2; // cylinder, sustained, lagged tracking
    public static final int TYPE_CONTAINMENT = 3; // cylinder, rotates with the lattice
    public static final int TYPE_WANDERING   = 4; // cylinder, own random drift
    // The projectile field has no type slot as its generated procedurally instead on boths sides, see BulletField and bullets.glsl.

    public int type = TYPE_NONE;

    // 0..1. The glowing spot on the surface
    public float telegraph;
    // 0..1 fraction of length, how far the volume currently reaches past the surface.
    public float extend;
    // 0..1. dissolve applied on top of extend.
    public float fade;

    // unit vector from Trinity's center between center and surface where the geometry should protrude
    public Vec3 dir = new Vec3(0, 1, 0);

    // full reach past the surface at extend == 1
    public float length;
    // cone/cylinder base radius
    public float radius;
    public float intensity = 1f; // for glow and density
    public float seed; // per slot seed for randomness

    public boolean isActive() {
        return type != TYPE_NONE;
    }

    public void clear() {
        type = TYPE_NONE;
        telegraph = 0f;
        extend = 0f;
        fade = 0f;
        length = 0f;
        radius = 0f;
        intensity = 1f;
    }

    // set geom should not change after attack is committed unless driven
    public void set(int type, Vec3 dir, float length, float radius, float seed) {
        this.type = type;
        this.dir = dir.normalize();
        this.length = length;
        this.radius = radius;
        this.seed = seed;
    }

    // -------------------------------------------------------------- SYNC

    public void save(CompoundTag tag) {
        tag.putInt("t", type);
        if (type == TYPE_NONE) return;
        tag.putFloat("tg", telegraph);
        tag.putFloat("ex", extend);
        tag.putFloat("fd", fade);
        tag.putFloat("dx", (float) dir.x);
        tag.putFloat("dy", (float) dir.y);
        tag.putFloat("dz", (float) dir.z);
        tag.putFloat("ln", length);
        tag.putFloat("rd", radius);
        tag.putFloat("in", intensity);
        tag.putFloat("sd", seed);
    }

    public void load(CompoundTag tag) {
        type = tag.getIntOr("t", TYPE_NONE);
        if (type == TYPE_NONE) {
            clear();
            return;
        }
        telegraph = tag.getFloatOr("tg", 0f);
        extend = tag.getFloatOr("ex", 0f);
        fade = tag.getFloatOr("fd", 0f);
        dir = new Vec3(tag.getFloatOr("dx", 0f), tag.getFloatOr("dy", 1f), tag.getFloatOr("dz", 0f));
        length = tag.getFloatOr("ln", 0f);
        radius = tag.getFloatOr("rd", 0f);
        intensity = tag.getFloatOr("in", 1f);
        seed = tag.getFloatOr("sd", 0f);
    }
}
