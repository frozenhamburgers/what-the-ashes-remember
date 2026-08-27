package net.jelly.marionette_lib.utility;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import org.jspecify.annotations.Nullable;

/**
 * One segment of a {@link Limb}. Directly instantiable for the common case
 * (hurt/interact forward to the parent entity, as most multipart creatures want).
 * Subclass only when a segment needs custom hit/interaction behavior.
 */
public class MarionettePart<T extends Entity> extends PartEntity<T> {
    private Vec3 newPosition = null;
    private final EntityDimensions size;
    public float length;
    public Vec3 direction = new Vec3(0, 1, 0);

    public MarionettePart(T parent, float sizeXZ, float sizeY, float length) {
        super(parent);
        this.blocksBuilding = true;
        this.size = EntityDimensions.fixed(sizeXZ, sizeY);
        this.length = length;
        this.refreshDimensions();
    }

    public void setPartPos(Vec3 pos) {
        setPartPos(pos.x, pos.y, pos.z);
    }

    public void setPartPos(double x, double y, double z) {
        newPosition = new Vec3(x, y, z);
    }

    @Override
    public Vec3 position() {
        if (newPosition != null) return newPosition;
        else return super.position();
    }

    // call after finalizing positions for the tick
    public void tick() {
        if (newPosition != null) {
            this.xo = this.getX();
            this.yo = this.getY();
            this.zo = this.getZ();
            this.xOld = this.getX();
            this.yOld = this.getY();
            this.zOld = this.getZ();
            setPos(newPosition);
        }
        super.tick();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return size;
    }

    // Note: Entity#getBoundingBoxForCulling() no longer exists in 26.1.2, render-side culling
    // moved to EntityRenderer#getBoundingBoxForCulling(T entity), which defaults to
    // entity.getBoundingBox() with no inflation. A consumer's custom EntityRenderer can override
    // that method the same way this used to inflate the box by 1 block, if needed.

    @Override
    public boolean fireImmune() {
        return true;
    }

    // forwards to the parent's own hurtServer, so damage still goes through the normal
    // server-authoritative path (weapon/attributes/crits, etc.)
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        Entity parent = this.getParent();
        return parent != null && !this.isInvulnerableToBase(source) && parent.hurtServer(level, source, amount);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        Entity parent = this.getParent();
        return parent == null ? InteractionResult.PASS : parent.interact(player, hand, location);
    }

    @Override
    public boolean is(Entity entityIn) {
        return this == entityIn || this.getParent() == entityIn;
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        Entity parent = this.getParent();
        return parent != null && parent.canBeCollidedWith(other);
    }

    @Override
    public boolean isPickable() {
        Entity parent = this.getParent();
        return parent != null && parent.isPickable();
    }

    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }

    public float getLength() {
        return length;
    }

    public Vec3 getPartDirection() {
        return direction;
    }

    public void setPartDirection(Vec3 direction) {
        this.direction = direction.normalize();
    }

    public Vec3 getRootPos() {
        return this.position().subtract(direction.scale(length / 2));
    }

    public Vec3 getEndPos() {
        return this.position().add(direction.scale(length / 2));
    }

    public void setRootPos(Vec3 root) {
        setPartPos(root.add(direction.scale(length / 2)));
    }

    public void setEndPos(Vec3 end) {
        setPartPos(end.subtract(direction.scale(length / 2)));
    }
}
