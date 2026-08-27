package net.jelly.marionette_lib.utility;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Implemented by an {@link Entity} made of one or more {@link Limb}s. The entity must forward these
 * to the defaults below (NeoForge's {@code Entity} already implements the first three, which win
 * over interface defaults, so they need an explicit override to reach this interface's version):
 * <pre>{@code
 * @Override public boolean isMultipartEntity() { return true; }
 * @Override public PartEntity<?>[] getParts() { return getMarionetteParts(); }
 * @Override public void remove(RemovalReason reason) { super.remove(reason); removeMarionette(reason); }
 * @Override public void setId(int id) { super.setId(id); setMarionettePartIds(id); }
 * }</pre>
 * The constructor must also reserve a contiguous network-id block after its limb(s) are built:
 * <pre>{@code
 * this.setId(ENTITY_COUNTER.getAndAdd(getMarionetteParts().length + 1) + 1);
 * }</pre>
 */
public interface Marionette {
    List<Limb<?>> getLimbs();

    default PartEntity<?>[] getMarionetteParts() {
        List<PartEntity<?>> allParts = new ArrayList<>();
        for (Limb<?> limb : getLimbs()) {
            for (MarionettePart<?> part : limb.parts()) allParts.add(part);
        }
        return allParts.toArray(new PartEntity<?>[0]);
    }

    default void tickMarionette() {
        getLimbs().forEach(limb -> limb.animator().tickMultipart());
    }

    default void removeMarionette(Entity.RemovalReason reason) {
        for (PartEntity<?> part : getMarionetteParts()) part.remove(reason);
    }

    /**
     * Bounding box covering {@code entity} and all its parts. Override a renderer's
     * {@code getBoundingBoxForCulling} to return this - the default is just the entity's own box,
     * which is usually smaller than the limbs it can stretch out to.
     */
    default AABB getMarionetteBoundingBoxForCulling(Entity entity) {
        AABB box = entity.getBoundingBox();
        for (PartEntity<?> part : getMarionetteParts()) {
            box = box.minmax(part.getBoundingBox());
        }
        return box;
    }

    /**
     * Reassigns every part's network id as {@code id + (index in getMarionetteParts() order) + 1},
     * mirroring vanilla {@code EnderDragon#setId} (NeoForge's fix for MC-158205). Keeps every part
     * resolvable by id on both sides, which is what makes hit detection, projectile collision, and
     * interaction work correctly for parts.
     */
    default void setMarionettePartIds(int id) {
        PartEntity<?>[] parts = getMarionetteParts();
        for (int i = 0; i < parts.length; i++) {
            parts[i].setId(id + i + 1);
        }
    }

    /**
     * Per-segment facing directions, in the same order as {@link #getMarionetteParts()}. Added for
     * the 26.1.2 port: {@code setupAnim} no longer receives the live entity, only an immutable
     * render-state snapshot, so the {@code EntityRenderer} must copy this into a render state
     * implementing {@link MarionetteModel.MarionetteRenderState} (usually via {@code extractRenderState}).
     */
    default Vec3[] getMarionettePartDirections() {
        PartEntity<?>[] parts = getMarionetteParts();
        Vec3[] directions = new Vec3[parts.length];
        for (int i = 0; i < parts.length; i++) {
            directions[i] = ((MarionettePart<?>) parts[i]).getPartDirection().normalize();
        }
        return directions;
    }

    /**
     * Per-segment position offsets, same order and port rationale as {@link #getMarionettePartDirections()}.
     * {@code entity} must be {@code this} - it's a separate parameter only because this interface
     * doesn't extend {@link Entity}.
     */
    default Vec3[] getMarionettePartOffsets(Entity entity, float partialTick) {
        PartEntity<?>[] parts = getMarionetteParts();
        Vec3[] offsets = new Vec3[parts.length];
        Vec3 entityPos = entity.getPosition(partialTick);
        for (int i = 0; i < parts.length; i++) {
            MarionettePart<?> part = (MarionettePart<?>) parts[i];
            Vec3 partPos = part.getPosition(partialTick);
            offsets[i] = new Vec3(
                    partPos.x - entityPos.x,
                    partPos.y - entityPos.y + part.getBbHeight() / 2,
                    partPos.z - entityPos.z
            );
        }
        return offsets;
    }
}
