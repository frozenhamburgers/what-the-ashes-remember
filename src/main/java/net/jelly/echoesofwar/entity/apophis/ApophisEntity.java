package net.jelly.echoesofwar.entity.apophis;

import net.jelly.echoesofwar.entity.apophis.goals.ApophisChargeGoal;
import net.jelly.echoesofwar.entity.apophis.goals.ApophisDolphinGoal;
import net.jelly.echoesofwar.entity.apophis.goals.ApophisFlightGoal;
import net.jelly.echoesofwar.entity.apophis.smog.ApophisSmogWorldEvent;
import net.jelly.echoesofwar.entity.apophis.goals.ApophisIdleGoal;
import net.jelly.echoesofwar.entity.apophis.goals.ApophisSettleGoal;
import net.jelly.echoesofwar.entity.apophis.goals.ApophisTargetMobGoal;
import net.jelly.echoesofwar.entity.apophis.goals.ApophisTargetPlayerGoal;
import net.jelly.echoesofwar.entity.physics.WormControlPoint;
import net.jelly.marionette_lib.utility.Limb;
import net.jelly.marionette_lib.utility.Marionette;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;

import java.util.Arrays;
import java.util.List;

// Entity itself is the tip of the head: the AI drives it through
// the world via an integrated velocity (getChaseVelocity()), body
// segments are solved by ApophisChainAnimator with per-segment
// terrain conforming pertub ormControlPoint layered on top.
//
// none of the body is synced over the network. the chain solve is strictly head-driven and has
// finite memory, so after SEGMENT_COUNT ticks every segment is derived purely from synced head history
public class ApophisEntity extends Monster implements Marionette {
    // hard coded segments, also take more damage
    public static final int HEAD_SEGMENT_COUNT = 3;

    public static final int BODY_SEGMENT_COUNT = 80;

    public static final int SEGMENT_COUNT = HEAD_SEGMENT_COUNT + BODY_SEGMENT_COUNT;

    private static final int TAPERED_SEGMENT_COUNT = SEGMENT_COUNT - 1; // everything except head

    private static final double SIZE_SCALE = 5.0;

    // these are model space
    private static final float HEAD_SEGMENT_MODEL_LENGTH = 1.5f;
    private static final float BODY_SEGMENT_MODEL_LENGTH = 0.5f;

    // gravity physics run per segment
    private static final Vec3 SEGMENT_GRAVITY = new Vec3(0, -0.006, 0);
    private static final double SEGMENT_MAX_FALL_SPEED = 0.25;

    // max speed, scaled to everything else
    public static final double SPEED_SCALE = 1.5;

    // dealing damage
    private static final double BODY_DAMAGE = 10.0;
    private static final double HEAD_DAMAGE_MULTIPLIER = 1.75;
    private static final Vec3 BODY_KNOCKBACK = new Vec3(3, 2, 3);
    private static final Vec3 HEAD_KNOCKBACK = new Vec3(5, 2, 5);

    // taking damage
    public static final int FULL_DAMAGE_SEGMENT_COUNT = 4;
    public static final double REAR_DAMAGE_TAKEN_MULTIPLIER = 0.5;

    // burrowing telegraph vfx & sfx
    public static final double DIG_SOUND_REFERENCE_DISTANCE = 17.5;
    public static final int DIG_SOUND_MAX_INTERVAL_TICKS = 6;
    public static final int DIG_SOUND_MIN_INTERVAL_TICKS = 0;
    public static final float DIG_SOUND_VOLUME = 24.0f;
    public static final float DIG_SOUND_MIN_VOLUME_SCALE = 0.5f;
    public static final float DIG_SOUND_MIN_PITCH = 0.75f;
    public static final float DIG_SOUND_MAX_PITCH = 2.0f;
    public static final int SURFACE_TELEGRAPH_RADIUS = 2;
    public static final int SURFACE_TELEGRAPH_PARTICLE_COUNT = 12;

    // flight phase
    // fraction of health to take as damage to trigger
    public static final double FLIGHT_TRIGGER_FRACTION = 0.25;
    public static final double FLIGHT_HEIGHT = 80.0;
    // raw damage taken to exit
    public static final double FLIGHT_EXIT_DAMAGE = 40.0;

    // time lock
    public static final long NOON_TIME_OF_DAY = 6000L; // lock at noon
    public static final long DAY_LENGTH_TICKS = 24000L;

    public static long nextNoon(long time) {
        long timeOfDay = Math.floorMod(time, DAY_LENGTH_TICKS);
        long delta = timeOfDay <= NOON_TIME_OF_DAY
                ? NOON_TIME_OF_DAY - timeOfDay
                : DAY_LENGTH_TICKS - timeOfDay + NOON_TIME_OF_DAY;
        return time + delta;
    }

    public static void setOverworldClockTime(ServerLevel level, long time) {
        level.registryAccess().get(WorldClocks.OVERWORLD)
                .ifPresent(clock -> level.clockManager().setTotalTicks(clock, time));
    }

    private long lockedNoonTime = -1L;

    // boolean when flight phase owns body
    private static final EntityDataAccessor<Boolean> DATA_FLYING =
            SynchedEntityData.defineId(ApophisEntity.class, EntityDataSerializers.BOOLEAN);

    // powered burrowing, actively steering toward the goal
    public static final int STAGE_DRIVE = 0;
    // committed, no steering authority riding out breach arc and post-entry dive
    public static final int STAGE_BALLISTIC = 1;

    // built directly rather than through Limb, since Limb.build() hard-wires a plain FabrikAnimator
    // and this needs an ApophisChainAnimator instead - see getMarionetteParts()
    private final ApophisPartEntity[] segments = new ApophisPartEntity[SEGMENT_COUNT];
    private final ApophisChainAnimator chain;
    private final WormControlPoint[] controlPoints = new WormControlPoint[SEGMENT_COUNT];
    private final float[] segmentScales = new float[SEGMENT_COUNT];

    private final double totalChainLength; // for priming purposes

    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(
            Mth.createInsecureUUID(this.random), this.getDisplayName(), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS
    ).setDarkenScreen(true);

    // head velocity drivin AI
    private Vec3 chaseVelocity = Vec3.ZERO;
    private int chargeStage = STAGE_DRIVE;
    private boolean hasStruckTargetThisRun;

    private boolean chainPrimed; // false at spawn to lay out chain
    private double contactDamageMultiplier = 1.0;
    private ApophisFlightGoal flightGoal;
    private ApophisSmogWorldEvent inheritedCloud;
    private Vec3 inheritedCloudCentre = Vec3.ZERO;

    private float damageTowardFlight;
    private float damageDuringFlight;
    private boolean flightPending;
    private boolean settlePending;
    private int digSoundTicks;

    public ApophisEntity(EntityType<? extends ApophisEntity> type, Level level) {
        super(type, level);
        this.xpReward = 50;
        this.setNoGravity(true);
        this.noPhysics = true;

        double totalLength = 0;
        for (int i = 0; i < SEGMENT_COUNT; i++) {
            boolean head = i == SEGMENT_COUNT - 1;
            // every segment but the head follows the taper profile starting at segment2
            float shape = head ? 1.0f : bodyTaper(TAPERED_SEGMENT_COUNT - 1 - i, TAPERED_SEGMENT_COUNT);
            segmentScales[i] = (float) (SIZE_SCALE * shape);
            float modelLength = head ? HEAD_SEGMENT_MODEL_LENGTH : BODY_SEGMENT_MODEL_LENGTH;
            float length = modelLength * shape * (float) SIZE_SCALE; // shrink length with scale
            double damageTakenMultiplier = i >= SEGMENT_COUNT - FULL_DAMAGE_SEGMENT_COUNT ? 1.0 : REAR_DAMAGE_TAKEN_MULTIPLIER;
            segments[i] = new ApophisPartEntity(this, segmentScales[i] * 0.875f, segmentScales[i] * 0.875f, length, head, damageTakenMultiplier);
            totalLength += length;
        }
        this.totalChainLength = totalLength;

        this.chain = new ApophisChainAnimator(this, segments);
        this.chain.setSegmentAdjuster(this::conformSegmentToTerrain);

        // reserve a contiguous id block for this entity and all its parts so setId() below can hand
        // out ids that wont collide with other entities
        this.setId(ENTITY_COUNTER.getAndAdd(getMarionetteParts().length + 1) + 1);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0) // never pathfinds - see travel()
                .add(Attributes.ATTACK_DAMAGE, BODY_DAMAGE)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 128.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FLYING, false);
    }

    // ----------------------------------------- FLIGHT PHASE

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        float healthBefore = this.getHealth();
        boolean hurt = super.hurtServer(level, source, amount);
        if (!hurt) return false;

        float dealt = healthBefore - this.getHealth();
        if (dealt <= 0.0f) return true;

        if (isFlying()) {
            damageDuringFlight += dealt;
        } else if (!flightPending) {
            damageTowardFlight += dealt;
            float threshold = (float) (this.getMaxHealth() * FLIGHT_TRIGGER_FRACTION);
            if (damageTowardFlight >= threshold) {
                damageTowardFlight -= threshold;
                flightPending = true;
            }
        }
        return true;
    }

    //  whether enough damage has been taken to owe a flight phase
    public boolean isFlightPending() {
        return flightPending;
    }

    public boolean isFlying() {
        return this.entityData.get(DATA_FLYING);
    }

    public float getDamageDuringFlight() {
        return damageDuringFlight;
    }

    public void setContactDamageMultiplier(double contactDamageMultiplier) {
        this.contactDamageMultiplier = contactDamageMultiplier;
    }

    // inherit spawn cloud
    public void inheritCloud(ApophisSmogWorldEvent cloud, Vec3 centre) {
        this.inheritedCloud = cloud;
        this.inheritedCloudCentre = centre;
        this.flightPending = true;
    }

    public ApophisSmogWorldEvent takeInheritedCloud() {
        ApophisSmogWorldEvent cloud = inheritedCloud;
        inheritedCloud = null;
        return cloud;
    }

    public Vec3 getInheritedCloudCentre() {
        return inheritedCloudCentre;
    }

    public void beginFlight() {
        flightPending = false;
        damageDuringFlight = 0.0f;
        contactDamageMultiplier = 1.0;
        this.entityData.set(DATA_FLYING, true);
        resetTerrainConforming();
    }

    public void endFlight() {
        this.entityData.set(DATA_FLYING, false);
        contactDamageMultiplier = 1.0;
        resetTerrainConforming();
        setChargeStage(STAGE_BALLISTIC); // ballistic stage applies gravity so it falls
        settlePending = true;
    }

    public boolean isSettlePending() {
        return settlePending;
    }

    public void clearSettlePending() {
        settlePending = false;
    }

    // clears every control point so the next ground phase tick builds them fresh
    private void resetTerrainConforming() {
        Arrays.fill(controlPoints, null);
    }

    // ------------------------------ Marionette boilerplate

    // no limbs as the chain is being managed directly
    @Override
    public List<Limb<?>> getLimbs() {
        return List.of();
    }

    @Override
    public PartEntity<?>[] getMarionetteParts() {
        return segments;
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
        chain.tickMultipart();
    }

    /** per-segment render scale, tail first, coiped into render state */
    public float[] getSegmentScales() {
        return segmentScales;
    }

    // girth multipliers head to tail
    private static final float[] TAPER_KEY_T = {0.00f, 0.25f, 0.50f, 0.80f, 1.00f};
    private static final float[] TAPER_KEY_SCALE = {1.50f, 1.00f, 1.00f, 1.20f, 0.25f};

    public static float bodyTaper(int bodyIndex, int bodyCount) {
        if (bodyCount <= 1) return 1.0f;
        float t = bodyIndex / (float) (bodyCount - 1);
        for (int i = 0; i < TAPER_KEY_T.length - 1; i++) {
            if (t <= TAPER_KEY_T[i + 1] || i == TAPER_KEY_T.length - 2) {
                float segT = Mth.clamp((t - TAPER_KEY_T[i]) / (TAPER_KEY_T[i + 1] - TAPER_KEY_T[i]), 0.0f, 1.0f);
                float eased = segT * segT * (3.0f - 2.0f * segT);
                return Mth.lerp(eased, TAPER_KEY_SCALE[i], TAPER_KEY_SCALE[i + 1]);
            }
        }
        return TAPER_KEY_SCALE[TAPER_KEY_SCALE.length - 1];
    }

    // ------------------------------------- GOAL INTERFACE

    public ApophisPartEntity head() {
        return segments[SEGMENT_COUNT - 1];
    }

    public Vec3 headPosition() {
        return this.position();
    }

    public Vec3 headDirection() {
        return head().getPartDirection();
    }

    public boolean isBurrowed() {
        return this.level().collidesWithSuffocatingBlock(null, this.getBoundingBox());
    }

    public Vec3 getChaseVelocity() {
        return chaseVelocity;
    }

    public void applyAcceleration(Vec3 acceleration) {
        chaseVelocity = chaseVelocity.add(acceleration);
        if (chaseVelocity.length() > SPEED_SCALE) chaseVelocity = chaseVelocity.normalize().scale(SPEED_SCALE);
    }

    public void setChaseVelocity(Vec3 velocity) {
        this.chaseVelocity = velocity;
    }

    public int getChargeStage() {
        return chargeStage;
    }

    public void setChargeStage(int chargeStage) {
        this.chargeStage = chargeStage;
        if (chargeStage == STAGE_DRIVE) hasStruckTargetThisRun = false;
    }

    public boolean hasStruckTargetThisRun() {
        return hasStruckTargetThisRun;
    }

    // ---------------------------------- MOVEMENT

    // no vanilla travel
    @Override
    public void travel(Vec3 input) {
    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return !isFlying() && super.removeWhenFarAway(distanceSquared);
    }

    @Override
    public void tick() {
        // before super.tick() because goals run in there and the head is not initialized
        if (!chainPrimed) {
            primeChain();
            chainPrimed = true;
        }

        super.tick(); // goals run here

        if (!this.level().isClientSide()) {
            // clear flying in case of abandoned goal
            if (isFlying() && (flightGoal == null || !flightGoal.isFlightActive())) {
                endFlight();
            }

            this.move(MoverType.SELF, chaseVelocity);
            this.setDeltaMovement(chaseVelocity); // for client
            faceTravelDirection();
        }

        solveChain();

        if (!this.level().isClientSide()) {
            lockTimeAtNoon((ServerLevel) this.level());
            hurtEntitiesInBody();
            tickBurrowTelegraph();
        }
    }

    private void lockTimeAtNoon(ServerLevel level) {
        if (lockedNoonTime < 0) lockedNoonTime = nextNoon(level.getOverworldClockTime());
        setOverworldClockTime(level, lockedNoonTime);
    }

    // head faces travel direction
    private void faceTravelDirection() {
        if (chaseVelocity.lengthSqr() < 1.0E-6) return;
        float yaw = (float) (Mth.atan2(-chaseVelocity.x, chaseVelocity.z) * (180.0 / Math.PI));
        float pitch = (float) (-Mth.atan2(chaseVelocity.y, chaseVelocity.horizontalDistance()) * (180.0 / Math.PI));
        this.setYRot(yaw);
        this.setXRot(pitch);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
    }

    // ------------------------------------ CHAIN

    private void solveChain() {
        chain.setRoot(segments[0].getEndPos());
        chain.setFabrikTarget(this.position());
        tickMarionette();
    }

    // initialize chain orientation
    private void primeChain() {
        Vec3 facing = this.getLookAngle();
        if (facing.lengthSqr() < 1.0E-6) facing = new Vec3(1, 0, 0);
        chain.setRoot(this.position().subtract(facing.normalize().scale(totalChainLength)));
        chain.primeMultipart(facing);
        for (ApophisPartEntity segment : segments) segment.tick();
    }

    private Vec3 conformSegmentToTerrain(int index, Vec3 naturalPosition) {
        if (isFlying()) return naturalPosition;

        WormControlPoint controlPoint = controlPoints[index];
        if (controlPoint == null) {
            controlPoint = new WormControlPoint(naturalPosition);
            controlPoints[index] = controlPoint;
        }
        double probeHalfWidth = Math.max(1.0, segments[index].getBbWidth() / 2.0);
        controlPoint.tick(this.level(), naturalPosition, SEGMENT_GRAVITY, SEGMENT_MAX_FALL_SPEED, probeHalfWidth);
        return controlPoint.getPosition();
    }

    // ---------------------------------------- DEAL DAMAGE

    private void hurtEntitiesInBody() {
        AABB chainBox = getMarionetteBoundingBoxForCulling(this);
        List<LivingEntity> candidates = this.level().getEntitiesOfClass(LivingEntity.class, chainBox,
                victim -> victim != this && victim.isAlive() && victim.hurtTime == 0);
        if (candidates.isEmpty()) return;

        ServerLevel serverLevel = (ServerLevel) this.level();
        DamageSource damageSource = this.damageSources().mobAttack(this);
        for (LivingEntity victim : candidates) {
            for (ApophisPartEntity segment : segments) {
                if (!segment.getBoundingBox().intersects(victim.getBoundingBox())) continue;

                double damage = (segment.isHead() ? BODY_DAMAGE * HEAD_DAMAGE_MULTIPLIER : BODY_DAMAGE)
                        * contactDamageMultiplier;
                Vec3 knockback = segment.isHead() ? HEAD_KNOCKBACK : BODY_KNOCKBACK;
                Vec3 away = victim.position().subtract(segment.position()).normalize();

                victim.hurtServer(serverLevel, damageSource, (float) damage);
                victim.push(away.x * knockback.x, away.y * knockback.y, away.z * knockback.z);
                if (victim == this.getTarget()) hasStruckTargetThisRun = true;
                break; // hit one victim once a tick tops regadless of segments
            }
        }
    }

    // -------------------------------------------- BURROWING VFX / SFX

    // digging sound + surface particles above the head
    private void tickBurrowTelegraph() {
        if (!isBurrowed()) return;
        LivingEntity target = this.getTarget();
        if (target == null) return;

        double distance = target.position().distanceTo(this.position());
        float proximity = (float) (1.0 / (1.0 + Math.pow(1.1, distance - DIG_SOUND_REFERENCE_DISTANCE)));
        int interval = Math.round(Mth.lerp(proximity, DIG_SOUND_MAX_INTERVAL_TICKS, DIG_SOUND_MIN_INTERVAL_TICKS));

        if (digSoundTicks < interval) {
            digSoundTicks++;
            return;
        }
        digSoundTicks = 0;

        playDigSound(proximity);
        spawnSurfaceTelegraph(proximity);
    }

    private void playDigSound(float proximity) {
        BlockPos headPos = BlockPos.containing(this.position());
        BlockState headState = this.level().getBlockState(headPos);
        SoundType soundType = headState.isAir() || !headState.getFluidState().isEmpty()
                ? Blocks.DIRT.defaultBlockState().getSoundType()
                : headState.getSoundType();
        float pitch = Mth.lerp(proximity, DIG_SOUND_MIN_PITCH, DIG_SOUND_MAX_PITCH);
        float volume = DIG_SOUND_VOLUME * Mth.lerp(proximity, DIG_SOUND_MIN_VOLUME_SCALE, 1.0f);
        this.level().playSound(null, headPos, soundType.getBreakSound(), SoundSource.HOSTILE, volume*0.5f, pitch);
        this.level().playSound(null, headPos, SoundEvents.WARDEN_STEP,
                SoundSource.HOSTILE, volume * 1.25f, pitch * 0.6f);
    }

    private void spawnSurfaceTelegraph(float proximity) {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        BlockPos headPos = BlockPos.containing(this.position());
        int particleCount = Math.round(Mth.lerp(proximity, SURFACE_TELEGRAPH_PARTICLE_COUNT * 0.5f, SURFACE_TELEGRAPH_PARTICLE_COUNT * 1.5f));
        double spread = Mth.lerp(proximity, 0.35, 0.6);
        double burst = Mth.lerp(proximity, 0.05, 0.25);

        for (int dx = -SURFACE_TELEGRAPH_RADIUS; dx <= SURFACE_TELEGRAPH_RADIUS; dx++) {
            for (int dz = -SURFACE_TELEGRAPH_RADIUS; dz <= SURFACE_TELEGRAPH_RADIUS; dz++) {
                BlockPos surface = findSolidSurfaceAbove(serverLevel, headPos.getX() + dx, headPos.getZ() + dz, headPos.getY());
                if (surface == null) continue;

                BlockState surfaceState = serverLevel.getBlockState(surface);
                serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, surfaceState),
                        surface.getX() + 0.5, surface.getY() + 1.0, surface.getZ() + 0.5,
                        particleCount, spread, 0.2, spread, burst);
                serverLevel.sendParticles(ParticleTypes.POOF,
                        surface.getX() + 0.5, surface.getY() + 1.0, surface.getZ() + 0.5,
                        Math.round(particleCount * 0.3f), spread, 0.1, spread, burst * 0.5);
            }
        }
    }

    private static BlockPos findSolidSurfaceAbove(ServerLevel level, int x, int z, int minY) {
        int top = Math.max(level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), minY);
        for (int y = top; y > level.getMinY(); y--) {
            BlockPos pos = new BlockPos(x, y, z);
            if (level.getBlockState(pos).isSuffocating(level, pos)) return pos;
        }
        return null;
    }

    // -------------------------------------------------- ENTITY BOILERPLATE SETUP

    @Override
    protected void registerGoals() {
        this.flightGoal = new ApophisFlightGoal(this);
        this.goalSelector.addGoal(0, flightGoal);
        this.goalSelector.addGoal(1, new ApophisSettleGoal(this));
        this.goalSelector.addGoal(2, new ApophisDolphinGoal(this));
        this.goalSelector.addGoal(3, new ApophisChargeGoal(this));
        this.goalSelector.addGoal(4, new ApophisIdleGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new ApophisTargetPlayerGoal(this));
        this.targetSelector.addGoal(3, new ApophisTargetMobGoal(this));
    }


    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("charge_stage", chargeStage);
        output.putFloat("damage_toward_flight", damageTowardFlight);
        output.putBoolean("flight_pending", flightPending);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.chargeStage = input.getIntOr("charge_stage", STAGE_DRIVE);
        this.damageTowardFlight = input.getFloatOr("damage_toward_flight", 0.0f);
        this.flightPending = input.getBooleanOr("flight_pending", false);
        this.chainPrimed = false; // reprime body orientation in case of relog / unload
    }

    // ------------------------------------ BOSS BAR

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
