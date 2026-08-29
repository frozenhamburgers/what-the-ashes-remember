package net.jelly.echoesofwar.entity.talos;

import net.jelly.echoesofwar.item.ModItems;
import net.jelly.marionette_lib.utility.FabrikAnimator;
import net.jelly.marionette_lib.utility.Limb;
import net.jelly.marionette_lib.utility.Marionette;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

// hovering multipart boss: the entity itself is the head, and the torso and both arms are separate
// FABRIK chains rooted off the head/shoulders that ease toward a rest pose when not being dragged along
public class TalosEntity extends Monster implements Marionette {
    /** multiply every spatial constant below by this to resize the whole boss */
    private static final double SIZE_SCALE = 2.0;

    private static final double MOVE_SPEED = 0.03;
    private static final double GRAB_APPROACH_SPEED = MOVE_SPEED * 0.5; // slower creep during grab wind-up/hold, so a target near the edge of standoff range doesn't sit just out of reach

    private static final double HOVER_GROUND_HEIGHT = 4.0 * SIZE_SCALE;
    private static final double HOVER_BAND = 0.25;
    private static final double HOVER_THRUST = 0.02;

    // torso's rest position, relative to the head
    private static final double TORSO_REST_DOWN = 5.5 * SIZE_SCALE;
    private static final double TORSO_REST_BACK = 0.5 * SIZE_SCALE;

    // shoulder position relative to the torso chain's shoulders segment, mirrored per arm
    private static final Vec3 SHOULDER_OFFSET = new Vec3(1.25 * SIZE_SCALE, 0.0, -0.3 * SIZE_SCALE);

    // arm's rest position, relative to its shoulder
    private static final double ARM_REST_SIDE = 1.0 * SIZE_SCALE;
    private static final double ARM_REST_FORWARD = 0.6 * SIZE_SCALE;
    private static final double ARM_REST_DOWN = 2.0 * SIZE_SCALE;

    // fraction of outward lean in the arm's idle prime direction - keeps FABRIK's starting guess from tucking the elbow into the torso
    private static final double ARM_PRIME_OUTWARD = 0.6;

    // preferred distance from a target, as a fraction of reachLength
    private static final double STANDOFF_FRACTION = 0.75;

    // speed fraction used once inside threshold distance, instead of stopping outright - see approachTarget()
    private static final double CLOSE_SPEED_FRACTION = 0.2;

    // grab attack timing, in ticks
    private static final int GRAB_WINDUP_MAX_TICKS = 15; // safety cap in case the arm never settles into the charge pose
    private static final int GRAB_HOLD_TICKS = 10;
    private static final int GRAB_REACH_TICKS = 8;
    private static final int GRAB_COOLDOWN_TICKS = 40;
    private static final double GRAB_WINDUP_SNAP = 0.4 * SIZE_SCALE; // how close the hand must get to the charge pose to count as "reached"

    // charge pose, relative to the reaching arm's shoulder
    private static final double GRAB_CHARGE_BACK = 0.3 * SIZE_SCALE;
    private static final double GRAB_CHARGE_SIDE = 3.2 * SIZE_SCALE;
    private static final double GRAB_CHARGE_UP = -1.2 * SIZE_SCALE;

    // how far the reach arc's midpoint bows outward
    private static final double GRAB_ARC_BULGE = 2.0 * SIZE_SCALE;

    // throw attack timing, in ticks right after grab
    private static final int THROW_WINDUP_MAX_TICKS = 12;
    private static final int THROW_HOLD_TICKS = 20;
    private static final int THROW_TICKS = 10;
    private static final int THROW_RELEASE_TICK = 5; // which tick of the throw the target lets go on
    private static final double THROW_WINDUP_SNAP = 0.4 * SIZE_SCALE;

    // cocked position, relative to the throwing arm's shoulder
    private static final double THROW_CHARGE_UP = 2.0 * SIZE_SCALE;
    private static final double THROW_CHARGE_BACK = 0.5 * SIZE_SCALE;
    private static final double THROW_CHARGE_SIDE = 2.25 * SIZE_SCALE;

    // where the arm ends up after following all the way through the throw, relative to the shoulder
    private static final double THROW_FOLLOWTHROUGH_FORWARD = 3.5 * SIZE_SCALE;
    private static final double THROW_FOLLOWTHROUGH_DOWN = 1.5 * SIZE_SCALE;
    private static final double THROW_FOLLOWTHROUGH_SIDE = -1.0 * SIZE_SCALE;

    // throw arc
    private static final double THROW_ARC_PEAK_HEIGHT = 7.5 * SIZE_SCALE;
    private static final double THROW_ARC_PEAK_FORWARD = 1.0 * SIZE_SCALE;
    private static final double THROW_ARC_BULGE = 1.5 * SIZE_SCALE;

    private static final double THROW_SPEED = 2.6; // blocks/tick imparted to the target on release
    private static final double THROW_DRIFT_SPEED = MOVE_SPEED * 0.3;

    // midswing arm primes upward forward instead of the backward
    private static final double THROW_PRIME_UP = 0.6;

    // prime for cocked arm
    // during wind-up/hold the arm should prime forward backward with a bent-elbow bias
    private static final double THROW_LOAD_PRIME_BACK = 1.0;
    private static final double THROW_LOAD_PRIME_OUTWARD = 0;
    private static final double THROW_LOAD_PRIME_UP = -0.5;

    // damage dealt on a successful grab and on impact
    private static final double GRAB_DAMAGE = 6.0;
    private static final double IMPACT_DAMAGE_PER_SPEED = 2.0;
    private static final double IMPACT_MIN_DAMAGE = 2.0;

    // impact damage only applies within this many ticks of release
    private static final int IMPACT_WINDOW_TICKS = 40;

    // don't grab again right after throwing
    private static final int IMPACT_GRACE_TICKS = 3;

    // the grab/throw goals only run serverside, so arms and target are synced explicitly.
    // only one arm ever attacks at a time so a single shared field is enough
    private static final byte ATTACK_ARM_NONE = 0;
    private static final byte ATTACK_ARM_LEFT = 1;
    private static final byte ATTACK_ARM_RIGHT = 2;
    private static final EntityDataAccessor<Byte> DATA_ATTACK_ARM =
            SynchedEntityData.defineId(TalosEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Vector3fc> DATA_ATTACK_TARGET_OFFSET =
            SynchedEntityData.defineId(TalosEntity.class, EntityDataSerializers.VECTOR3);
    // whether the target above is eased toward or snapped to directly
    private static final EntityDataAccessor<Boolean> DATA_ATTACK_DIRECT =
            SynchedEntityData.defineId(TalosEntity.class, EntityDataSerializers.BOOLEAN);
    // which baseline the arm's FABRIK guess primes toward each tick
    private static final byte PRIME_STYLE_IDLE = 0;  // backward, leaning outward
    private static final byte PRIME_STYLE_LOAD = 1;  // out and back, bent-elbow bias, for cocking throw
    private static final byte PRIME_STYLE_SWING = 2; // upward and forward, for throw motion itself
    private static final EntityDataAccessor<Byte> DATA_ATTACK_PRIME_STYLE =
            SynchedEntityData.defineId(TalosEntity.class, EntityDataSerializers.BYTE);

    private static final EntityDataAccessor<Byte> DATA_CARRIED_ARM =
            SynchedEntityData.defineId(TalosEntity.class, EntityDataSerializers.BYTE);

    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(
            Mth.createInsecureUUID(this.random), this.getDisplayName(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS
    ).setDarkenScreen(true);

    private final TalosPartEntity head;
    private final Limb<TalosPartEntity> torsoChain;
    private final Limb<TalosPartEntity> leftArm;
    private final Limb<TalosPartEntity> rightArm;

    // built in registerGoals()
    private GrabAttackGoal grabAttackGoal;
    private ThrowTargetGoal throwTargetGoal;

    private LivingEntity thrownTarget;
    private double thrownTargetLastSpeed;
    private long thrownTargetReleaseTick;
    private long thrownTargetDeadline;

    // how far the arms can physically reach from the shoulder
    private final double reachLength;

    public TalosEntity(EntityType<? extends TalosEntity> type, Level level) {
        super(type, level);
        this.xpReward = 50;
        this.setNoGravity(true); // it hovers instead of falling

        this.head = new TalosPartEntity(this, scaled(1.0f), scaled(0.6f), scaled(1.5f));

        this.torsoChain = Limb.builder(this, TalosPartEntity::new)
                .segment(scaled(1.8f), scaled(1.0f), scaled(1.45f))  // shoulders
                .segment(scaled(1.6f), scaled(1.2f), scaled(1.25f))  // torso
                .segment(scaled(1.6f), scaled(1.0f), scaled(0.25f))  // hips
                .segment(scaled(1.2f), scaled(3.2f), scaled(2.75f))  // legs
                .build();

        this.leftArm = Limb.builder(this, TalosPartEntity::new)
                .segment(scaled(0.9f), scaled(0.9f), scaled(1.85f))  // tricep
                .segment(scaled(0.7f), scaled(0.8f), scaled(1.65f))  // forearm
                .segment(scaled(0.5f), scaled(0.6f), scaled(0.8f))  // hand
                .build();

        this.rightArm = Limb.builder(this, TalosPartEntity::new)
                .segment(scaled(0.9f), scaled(0.9f), scaled(1.5f))
                .segment(scaled(0.7f), scaled(0.8f), scaled(1.5f))
                .segment(scaled(0.5f), scaled(0.6f), scaled(0.8f))
                .build();

        this.reachLength = Arrays.stream(leftArm.parts()).mapToDouble(TalosPartEntity::getLength).sum();

        // reserve a contiguous id block for this entity and all its parts, so setId() below can hand
        // out ids that won't collide with other entities
        this.setId(ENTITY_COUNTER.getAndAdd(getMarionetteParts().length + 1) + 1);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        this.spawnAtLocation(level, ModItems.KEY_OF_CONQUEST.get());
    }

    private static float scaled(float baseValue) {
        return (float) (baseValue * SIZE_SCALE);
    }

    public static float sizeScale() {
        return (float) SIZE_SCALE;
    }

    @Override
    public List<Limb<?>> getLimbs() {
        return List.of(torsoChain, leftArm, rightArm);
    }

    // head is not part of chain and managed directly
    @Override
    public PartEntity<?>[] getMarionetteParts() {
        PartEntity<?>[] rest = Marionette.super.getMarionetteParts();
        PartEntity<?>[] all = new PartEntity<?>[rest.length + 1];
        all[0] = head;
        System.arraycopy(rest, 0, all, 1, rest.length);
        return all;
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public PartEntity<?>[] getParts() {
        return getMarionetteParts();
    }

    @Override
    public void remove(RemovalReason removalReason) {
        super.remove(removalReason);
        removeMarionette(removalReason);
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        setMarionettePartIds(id);
    }

    @Override
    public void tickMarionette() {
        head.tick();
        Marionette.super.tickMarionette();
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return super.getDefaultDimensions(pose).scale((float) SIZE_SCALE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ATTACK_ARM, ATTACK_ARM_NONE);
        builder.define(DATA_ATTACK_TARGET_OFFSET, new Vector3f());
        builder.define(DATA_ATTACK_DIRECT, false);
        builder.define(DATA_ATTACK_PRIME_STYLE, PRIME_STYLE_IDLE);
        builder.define(DATA_CARRIED_ARM, ATTACK_ARM_NONE);
    }

    @Override
    public LivingEntity getControllingPassenger() {
        return null;
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        return passenger.position();
    }

    // rider needs to be positioned where hand is
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        byte carriedArm = this.entityData.get(DATA_CARRIED_ARM);
        if (carriedArm == ATTACK_ARM_NONE) return super.getPassengerAttachmentPoint(passenger, dimensions, scale);
        Limb<TalosPartEntity> arm = carriedArm == ATTACK_ARM_LEFT ? leftArm : rightArm;
        return handOf(arm).position().subtract(this.position());
    }

    @Override
    public void tick() {
        super.tick();

        Vec3 base = this.position();
        LivingEntity target = this.getTarget();

        if (!this.level().isClientSide()) {
            if (this.entityData.get(DATA_CARRIED_ARM) != ATTACK_ARM_NONE && this.getPassengers().isEmpty()) {
                this.entityData.set(DATA_CARRIED_ARM, ATTACK_ARM_NONE);
            }
            updateThrownTargetImpact();
        }

        updateHover();

        Vec3 forward = bodyForward();
        Vec3 right = bodyRight(forward);

        updateHead(target, forward, base);
        updateTorsoChain(forward);

        ArmOverride leftOverride = attackTargetFor(ATTACK_ARM_LEFT);
        ArmOverride rightOverride = attackTargetFor(ATTACK_ARM_RIGHT);
        updateArm(leftArm, -1, forward, right, leftOverride);
        updateArm(rightArm, 1, forward, right, rightOverride);

        tickMarionette();
    }

    /** where the grab/throw goals currently want an arm, whether to ease or snap to it, and which prime baseline to use */
    private record ArmOverride(Vec3 position, boolean direct, byte primeStyle) {}

    private ArmOverride attackTargetFor(byte armId) {
        if (!this.level().isClientSide()) {
            Limb<TalosPartEntity> arm = armId == ATTACK_ARM_LEFT ? leftArm : rightArm;
            ArmOverride override = grabAttackGoal != null ? grabAttackGoal.overrideFor(arm) : null;
            if (override == null) override = throwTargetGoal != null ? throwTargetGoal.overrideFor(arm) : null;
            if (override != null) {
                this.entityData.set(DATA_ATTACK_ARM, armId);
                this.entityData.set(DATA_ATTACK_DIRECT, override.direct());
                this.entityData.set(DATA_ATTACK_PRIME_STYLE, override.primeStyle());
                Vec3 offset = override.position().subtract(this.position());
                this.entityData.set(DATA_ATTACK_TARGET_OFFSET, new Vector3f((float) offset.x, (float) offset.y, (float) offset.z));
            } else if (this.entityData.get(DATA_ATTACK_ARM) == armId) {
                this.entityData.set(DATA_ATTACK_ARM, ATTACK_ARM_NONE);
            }
            return override;
        }

        if (this.entityData.get(DATA_ATTACK_ARM) != armId) return null;
        Vector3fc offset = this.entityData.get(DATA_ATTACK_TARGET_OFFSET);
        Vec3 position = this.position().add(offset.x(), offset.y(), offset.z());
        return new ArmOverride(position, this.entityData.get(DATA_ATTACK_DIRECT), this.entityData.get(DATA_ATTACK_PRIME_STYLE));
    }

    private Vec3 bodyForward() {
        return Vec3.directionFromRotation(0, this.yBodyRot);
    }

    private Vec3 bodyRight(Vec3 forward) {
        return forward.cross(new Vec3(0, 1, 0)).normalize();
    }

    private Vec3 shoulderCenter() {
        return torsoChain.parts()[0].position();
    }

    private Vec3 shoulderRoot(int sideSign, Vec3 forward, Vec3 right) {
        return shoulderCenter()
                .add(right.scale(SHOULDER_OFFSET.x * sideSign))
                .add(new Vec3(0, SHOULDER_OFFSET.y, 0))
                .add(forward.scale(SHOULDER_OFFSET.z));
    }

    private TalosPartEntity handOf(Limb<TalosPartEntity> arm) {
        return arm.parts()[arm.parts().length - 1];
    }

    // there's only ever realistically one passenger
    private LivingEntity carriedTarget() {
        for (Entity passenger : this.getPassengers()) {
            if (passenger instanceof LivingEntity living) return living;
        }
        return null;
    }

    public boolean isCarrying(Entity entity) {
        return this.entityData.get(DATA_CARRIED_ARM) != ATTACK_ARM_NONE && carriedTarget() == entity;
    }

    private void damage(LivingEntity target, double amount) {
        target.invulnerableTime = 0;
        target.hurtServer((ServerLevel) this.level(), this.damageSources().mobAttack(this), (float) amount);
    }

    private void updateThrownTargetImpact() {
        if (thrownTarget == null) return;

        long now = this.level().getGameTime();
        if (!thrownTarget.isAlive() || now > thrownTargetDeadline) {
            stopTrackingThrownTarget();
            return;
        }

        boolean pastGracePeriod = now - thrownTargetReleaseTick >= IMPACT_GRACE_TICKS;
        if (pastGracePeriod && (thrownTarget.horizontalCollision || thrownTarget.verticalCollision)) {
            double damageAmount = Math.max(thrownTargetLastSpeed * IMPACT_DAMAGE_PER_SPEED, IMPACT_MIN_DAMAGE);
            damage(thrownTarget, damageAmount);
            stopTrackingThrownTarget();
        } else {
            thrownTargetLastSpeed = thrownTarget.getDeltaMovement().length();
        }
    }

    private void stopTrackingThrownTarget() {
        thrownTarget = null;
    }

    /** horizontal-only offset from the entity to target, used for both facing and distance checks */
    private Vec3 horizontalOffsetTo(LivingEntity target) {
        Vec3 base = this.position();
        return new Vec3(target.getX() - base.x, 0, target.getZ() - base.z);
    }

    /** turns the body toward whatever direction is passed in, in the horizontal plane only */
    private void turnToward(Vec3 toTarget) {
        float desiredYRot = (float) (Mth.atan2(-toTarget.x, toTarget.z) * (180D / Math.PI));
        this.setYRot(Mth.approachDegrees(this.getYRot(), desiredYRot, 6.0F));
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();
    }

    /** nudges the body toward toTarget's direction at the given speed */
    private void moveToward(Vec3 toTarget, double speed) {
        setDeltaMovement(getDeltaMovement().add(toTarget.normalize().scale(speed)));
    }

    private void approachTarget(Vec3 toTarget, double threshold, double speed) {
        double actualSpeed = toTarget.length() > threshold ? speed : speed * CLOSE_SPEED_FRACTION;
        moveToward(toTarget, actualSpeed);
    }

    /** keeps the head hovering HOVER_GROUND_HEIGHT above the ground */
    private void updateHover() {
        double groundDistance = getGroundDistance();
        if (groundDistance > HOVER_GROUND_HEIGHT + HOVER_BAND) {
            this.addDeltaMovement(new Vec3(0, -HOVER_THRUST, 0));
        } else if (groundDistance < HOVER_GROUND_HEIGHT - HOVER_BAND) {
            this.addDeltaMovement(new Vec3(0, HOVER_THRUST, 0));
        }
    }

    /** distance straight down from the entity to the nearest solid block below it */
    private double getGroundDistance() {
        BlockPos pos = this.blockPosition();
        Level level = this.level();
        int y = pos.getY();
        while (y > level.getMinY() && level.getBlockState(new BlockPos(pos.getX(), y - 1, pos.getZ())).isAir()) {
            y--;
        }
        return this.getY() - y;
    }

    /** points the head at the target's eyes, or the body's own forward direction if there's no target */
    private void updateHead(LivingEntity target, Vec3 forward, Vec3 base) {
        Vec3 headDirection = forward;
        if (target != null) {
            Vec3 toTarget = target.getEyePosition().subtract(base);
            if (toTarget.lengthSqr() > 1.0E-4) headDirection = toTarget.normalize();
        }
        head.setPartDirection(headDirection);
        head.setPartPos(base);
    }

    /** torso chain roots at the head and its target eases toward a rest pose below and slightly behind it */
    private void updateTorsoChain(Vec3 forward) {
        Vec3 headPos = head.position();
        torsoChain.animator().setRoot(headPos);

        Vec3 restPos = headPos
                .add(new Vec3(0, -TORSO_REST_DOWN, 0))
                .add(forward.scale(-TORSO_REST_BACK));

        approachRest(torsoChain.animator(), restPos, torsoChain.animator().chainEndPos());
    }

    // resets the chain to a straight line every tick (per PRIME_STYLE) before solving, so a bad pose
    // from a previous tick never accumulates. handPos must be read before priming, since priming
    // overwrites it. windup/hold ease toward their target like idle rest does, the reach itself snaps
    // straight to its target since the arc it's already tracing supplies its own smoothness and prevents instability
    private void updateArm(Limb<TalosPartEntity> arm, int sideSign, Vec3 forward, Vec3 right, ArmOverride attackOverride) {
        Vec3 shoulderRoot = shoulderRoot(sideSign, forward, right);
        arm.animator().setRoot(shoulderRoot);
        Vec3 handPos = arm.animator().chainEndPos();
        byte primeStyle = attackOverride != null ? attackOverride.primeStyle() : PRIME_STYLE_IDLE;
        Vec3 primeDirection = switch (primeStyle) {
            case PRIME_STYLE_LOAD -> forward.scale(-THROW_LOAD_PRIME_BACK)
                    .add(right.scale(THROW_LOAD_PRIME_OUTWARD * sideSign))
                    .add(new Vec3(0, THROW_LOAD_PRIME_UP, 0));
            case PRIME_STYLE_SWING -> forward.add(new Vec3(0, THROW_PRIME_UP, 0));
            default -> forward.scale(-1).add(right.scale(ARM_PRIME_OUTWARD * sideSign));
        };
        arm.animator().primeMultipart(primeDirection);

        if (attackOverride != null && attackOverride.direct()) {
            arm.animator().setFabrikTarget(attackOverride.position());
            return;
        }

        Vec3 restPos = attackOverride != null ? attackOverride.position() : shoulderRoot
                .add(right.scale(ARM_REST_SIDE * sideSign))
                .add(forward.scale(ARM_REST_FORWARD))
                .add(new Vec3(0, -ARM_REST_DOWN, 0));

        approachRest(arm.animator(), restPos, handPos);
    }

    private void approachRest(FabrikAnimator animator, Vec3 restPos, Vec3 chainEndPos) {
        Vec3 toRest = restPos.subtract(chainEndPos);
        if (toRest.length() > 0.2) {
            animator.setFabrikTarget(chainEndPos
                    .add(toRest.normalize().scale(this.getDeltaMovement().dot(toRest) * 0.15))
                    .add(toRest.scale(0.1))
                    .add(toRest.normalize().scale(0.1)));
        } else {
            animator.setFabrikTarget(chainEndPos);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 200.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void registerGoals() {
        this.grabAttackGoal = new GrabAttackGoal();
        this.throwTargetGoal = new ThrowTargetGoal();
        this.goalSelector.addGoal(2, throwTargetGoal);
        this.goalSelector.addGoal(3, grabAttackGoal);
        this.goalSelector.addGoal(4, new ApproachTargetGoal());
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // turns toward the current target and closes in on it. also the only place
    // that changes Talos's facing
    private class ApproachTargetGoal extends Goal {
        ApproachTargetGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) return;

            Vec3 toTarget = horizontalOffsetTo(target);
            if (toTarget.lengthSqr() < 1.0E-4) return;

            turnToward(toTarget);
            approachTarget(toTarget, reachLength * STANDOFF_FRACTION, MOVE_SPEED);
        }
    }

    // rears one arm back, holds briefly, then reaches it through an arc toward the target,
    // grabbing them if the hand touches them along the way.
    private class GrabAttackGoal extends Goal {
        private enum Phase { WINDUP, HOLD, REACH }

        private Phase phase;
        private int phaseTicks;
        private boolean finished;
        private boolean grabbingLeftArm;
        private long cooldownUntil;

        private Vec3 reachStartPos;

        GrabAttackGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private Limb<TalosPartEntity> activeArm() {
            return grabbingLeftArm ? leftArm : rightArm;
        }

        private int activeArmSide() {
            return grabbingLeftArm ? -1 : 1;
        }

        @Override
        public boolean canUse() {
            if (level().getGameTime() < cooldownUntil) return false;
            LivingEntity target = getTarget();
            if (target == null || !target.isAlive()) return false;
            if (target == thrownTarget) return false; // dont regrab instantly
            return horizontalOffsetTo(target).length() <= reachLength * STANDOFF_FRACTION;
        }

        @Override
        public boolean canContinueToUse() {
            return !finished;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            Vec3 forward = bodyForward();
            Vec3 right = bodyRight(forward);
            boolean leftIsCloser = target == null
                    || shoulderRoot(-1, forward, right).distanceToSqr(target.position())
                    <= shoulderRoot(1, forward, right).distanceToSqr(target.position());
            this.grabbingLeftArm = leftIsCloser;
            this.phase = Phase.WINDUP;
            this.phaseTicks = 0;
            this.finished = false;
        }

        @Override
        public void stop() {
            this.phase = null;
            this.cooldownUntil = level().getGameTime() + GRAB_COOLDOWN_TICKS;
        }

        @Override
        public void tick() {
            phaseTicks++;
            switch (phase) {
                case WINDUP -> tickWindup();
                case HOLD -> tickHold();
                case REACH -> tickReach();
            }
        }

        private void trackAndCloseIn() {
            LivingEntity target = getTarget();
            if (target == null) return;

            Vec3 toTarget = horizontalOffsetTo(target);
            if (toTarget.lengthSqr() < 1.0E-4) return;

            turnToward(toTarget);
            approachTarget(toTarget, reachLength * STANDOFF_FRACTION / 2.0, GRAB_APPROACH_SPEED);
        }

        private void tickWindup() {
            trackAndCloseIn();

            Vec3 chargePos = chargePosition();
            boolean reached = activeArm().animator().chainEndPos().distanceTo(chargePos) < GRAB_WINDUP_SNAP;
            if (reached || phaseTicks >= GRAB_WINDUP_MAX_TICKS) {
                phase = Phase.HOLD;
                phaseTicks = 0;
            }
        }

        private void tickHold() {
            trackAndCloseIn();

            if (phaseTicks >= GRAB_HOLD_TICKS) {
                beginReach();
            }
        }

        private void beginReach() {
            phase = Phase.REACH;
            phaseTicks = 0;
            reachStartPos = activeArm().animator().chainEndPos();
        }

        private void tickReach() {
            LivingEntity target = getTarget();
            if (target != null && target != thrownTarget && handOf(activeArm()).getBoundingBox().intersects(target.getBoundingBox())) {
                entityData.set(DATA_CARRIED_ARM, grabbingLeftArm ? ATTACK_ARM_LEFT : ATTACK_ARM_RIGHT);
                target.startRiding(TalosEntity.this, true, true);
                damage(target, GRAB_DAMAGE);
                finished = true;
                return;
            }

            if (phaseTicks >= GRAB_REACH_TICKS) {
                finished = true;
            }
        }

        private Vec3 chargePosition() {
            Vec3 forward = bodyForward();
            Vec3 right = bodyRight(forward);
            int sideSign = activeArmSide();
            return shoulderRoot(sideSign, forward, right)
                    .add(right.scale(GRAB_CHARGE_SIDE * sideSign))
                    .add(forward.scale(-GRAB_CHARGE_BACK))
                    .add(new Vec3(0, GRAB_CHARGE_UP, 0));
        }

        private Vec3 reachArcPosition() {
            LivingEntity target = getTarget();
            Vec3 targetPos = target != null ? target.getBoundingBox().getCenter() : reachStartPos;

            Vec3 forward = bodyForward();
            Vec3 right = bodyRight(forward);
            int sideSign = activeArmSide();
            Vec3 midpoint = reachStartPos.add(targetPos).scale(0.5);
            Vec3 controlPos = midpoint
                    .add(right.scale(GRAB_ARC_BULGE * sideSign))
                    .add(forward.scale(GRAB_ARC_BULGE * 0.5));

            double t = Mth.clamp((double) phaseTicks / GRAB_REACH_TICKS, 0.0, 1.0);
            double u = 1.0 - t;
            return reachStartPos.scale(u * u)
                    .add(controlPos.scale(2 * u * t))
                    .add(targetPos.scale(t * t));
        }

        // world-space fabrik target for arm while this attack is driving it, or null otherwise
        ArmOverride overrideFor(Limb<TalosPartEntity> arm) {
            if (phase == null || arm != activeArm()) return null;
            return switch (phase) {
                case WINDUP, HOLD -> new ArmOverride(chargePosition(), false, PRIME_STYLE_IDLE);
                case REACH -> new ArmOverride(reachArcPosition(), true, PRIME_STYLE_IDLE);
            };
        }
    }

    // after successful grab draws the carrying arm into a cocked overhand position, holds briefly,
    // then swings through, releasing the target midswing
    private class ThrowTargetGoal extends Goal {
        private enum Phase { WINDUP, HOLD, THROW }

        private Phase phase;
        private int phaseTicks;
        private boolean finished;
        private boolean released;
        private boolean throwingLeftArm;

        private Vec3 throwStartPos;

        ThrowTargetGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private Limb<TalosPartEntity> activeArm() {
            return throwingLeftArm ? leftArm : rightArm;
        }

        private int activeArmSide() {
            return throwingLeftArm ? -1 : 1;
        }

        @Override
        public boolean canUse() {
            return entityData.get(DATA_CARRIED_ARM) != ATTACK_ARM_NONE && !getPassengers().isEmpty();
        }

        @Override
        public boolean canContinueToUse() {
            return !finished;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            this.throwingLeftArm = entityData.get(DATA_CARRIED_ARM) == ATTACK_ARM_LEFT;
            this.phase = Phase.WINDUP;
            this.phaseTicks = 0;
            this.finished = false;
            this.released = false;
            System.out.println("throwing target");
        }

        @Override
        public void stop() {
            System.out.println("stop");
            this.phase = null;
            setTarget(null);
        }

        @Override
        public void tick() {
            System.out.println("tick throwing target phase=" + phase + " ticks=" + phaseTicks);
            phaseTicks++;
            moveToward(bodyForward(), THROW_DRIFT_SPEED);
            switch (phase) {
                case WINDUP -> tickWindup();
                case HOLD -> tickHold();
                case THROW -> tickThrow();
            }
        }

        private void tickWindup() {
            Vec3 chargePos = chargePosition();
            boolean reached = activeArm().animator().chainEndPos().distanceTo(chargePos) < THROW_WINDUP_SNAP;
            if (reached || phaseTicks >= THROW_WINDUP_MAX_TICKS) {
                phase = Phase.HOLD;
                phaseTicks = 0;
            }
        }

        private void tickHold() {
            if (phaseTicks >= THROW_HOLD_TICKS) {
                beginThrow();
            }
        }

        private void beginThrow() {
            phase = Phase.THROW;
            phaseTicks = 0;
            throwStartPos = activeArm().animator().chainEndPos();
        }

        private void tickThrow() {
            if (!released && phaseTicks >= THROW_RELEASE_TICK) {
                release();
            }
            if (phaseTicks >= THROW_TICKS) {
                finished = true;
            }
        }

        private void release() {
            released = true;
            entityData.set(DATA_CARRIED_ARM, ATTACK_ARM_NONE);

            LivingEntity rider = carriedTarget();
            if (rider == null) return;

            Vec3 velocity = throwVelocityAt(THROW_RELEASE_TICK);
            rider.stopRiding();
            rider.hurtMarked = true;
            rider.setDeltaMovement(velocity);
            rider.needsSync = true;

            thrownTarget = rider;
            thrownTargetLastSpeed = velocity.length();
            thrownTargetReleaseTick = level().getGameTime();
            thrownTargetDeadline = thrownTargetReleaseTick + IMPACT_WINDOW_TICKS;
        }

        private Vec3 throwVelocityAt(int atTick) {
            Vec3 p0 = throwStartPos;
            Vec3 p1 = controlPosition();
            Vec3 p2 = throwEndPosition();
            double t = Mth.clamp((double) atTick / THROW_TICKS, 0.0, 1.0);
            Vec3 tangent = p1.subtract(p0).scale(2 * (1 - t)).add(p2.subtract(p1).scale(2 * t));
            return tangent.lengthSqr() > 1.0E-6 ? tangent.normalize().scale(THROW_SPEED) : bodyForward().scale(THROW_SPEED);
        }

        private Vec3 chargePosition() {
            Vec3 forward = bodyForward();
            Vec3 right = bodyRight(forward);
            int sideSign = activeArmSide();
            return shoulderRoot(sideSign, forward, right)
                    .add(new Vec3(0, THROW_CHARGE_UP, 0))
                    .add(forward.scale(-THROW_CHARGE_BACK))
                    .add(right.scale(THROW_CHARGE_SIDE * sideSign));
        }

        private Vec3 throwEndPosition() {
            Vec3 forward = bodyForward();
            Vec3 right = bodyRight(forward);
            int sideSign = activeArmSide();
            return shoulderRoot(sideSign, forward, right)
                    .add(forward.scale(THROW_FOLLOWTHROUGH_FORWARD))
                    .add(new Vec3(0, -THROW_FOLLOWTHROUGH_DOWN, 0))
                    .add(right.scale(THROW_FOLLOWTHROUGH_SIDE * sideSign));
        }

        // anchored off the shoulder o the peak is fixed
        private Vec3 controlPosition() {
            Vec3 forward = bodyForward();
            Vec3 right = bodyRight(forward);
            int sideSign = activeArmSide();
            return shoulderRoot(sideSign, forward, right)
                    .add(new Vec3(0, THROW_ARC_PEAK_HEIGHT, 0))
                    .add(forward.scale(THROW_ARC_PEAK_FORWARD))
                    .add(right.scale(THROW_ARC_BULGE * sideSign));
        }

        private Vec3 throwArcPosition() {
            Vec3 p0 = throwStartPos;
            Vec3 p1 = controlPosition();
            Vec3 p2 = throwEndPosition();
            double t = Mth.clamp((double) phaseTicks / THROW_TICKS, 0.0, 1.0);
            double u = 1.0 - t;
            return p0.scale(u * u).add(p1.scale(2 * u * t)).add(p2.scale(t * t));
        }

        // world-space fabrik target for arm while this attack is driving it, or null otherwise
        ArmOverride overrideFor(Limb<TalosPartEntity> arm) {
            if (phase == null || arm != activeArm()) return null;
            return switch (phase) {
                case WINDUP, HOLD -> new ArmOverride(chargePosition(), false, PRIME_STYLE_LOAD);
                case THROW -> new ArmOverride(throwArcPosition(), true, PRIME_STYLE_SWING);
            };
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide()) {
            this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }
}
