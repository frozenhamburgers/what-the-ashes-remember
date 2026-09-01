package net.jelly.echoesofwar.entity.trinity;

import net.jelly.echoesofwar.entity.trinity.attack.AttackHitDetection;
import net.jelly.echoesofwar.entity.trinity.attack.AttackSlot;
import net.jelly.echoesofwar.entity.trinity.attack.BulletField;
import net.jelly.echoesofwar.entity.trinity.attack.BulletPatternAttack;
import net.jelly.echoesofwar.entity.trinity.attack.TrinityAttack;
import net.jelly.echoesofwar.entity.trinity.fx.TrinityFx;
import net.jelly.echoesofwar.entity.trinity.fx.TrinityPostProcessor;
import net.jelly.echoesofwar.sound.ModMusicManager;
import net.jelly.echoesofwar.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import team.lodestar.lodestone.modules.toolkit.worldevent.WorldEventInstance;
import team.lodestar.lodestone.registry.common.LodestoneAttachmentTypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static net.jelly.echoesofwar.entity.trinity.TrinityTuning.*;

/**
 * Trinity, the final boss. No need for entity boilerplate, so represented as a world event.
 * client/server split: both sides tick same counters, and server pushes a packet when something
 * discrete and non-deterministic happens that the client cannot infer.
 */
public class TrinityWorldEvent extends WorldEventInstance {

    // slots for attack geometry, Must equal TRINITY_ATTACK_SLOTS in instance.glsl.
    public static final int ATTACK_SLOTS = 80;

    // ---------------------------------------------------------- state

    private Vec3 centre = Vec3.ZERO;
    private float seed;

    private float health; // criticality
    private int criticalityDegree; // phase, # of meltdowns
    private int nextThreshold;

    private int spawnTicks = 400;
    private int criticalTicks = 200;

    // SERVER ONLY
    private int emptyTicks; // consecutive ticks with no one in active range
    private boolean defeated;
    private boolean rewardsGiven;
    private final TrinityDemolition demolition = new TrinityDemolition();

    // not just server only
    private TrinityPhase phase = TrinityPhase.SPAWNING;
    private int phaseTicks; // ticks in current phase
    private int totalTicks;

    // detonation sub-state for DETONATING, REFORMING, DYING
    private int detonationTicks;
    private int detonationLifetimeTicks = OPENING_DETONATION_TICKS;

    public final AttackSlot[] slots = new AttackSlot[ATTACK_SLOTS];

    // generated projectile field
    public final BulletField bulletField = new BulletField();

    // attack patterns currently rurnning, never down to just one
    private final List<Track> tracks = new ArrayList<>();

    private int desiredAttacks = ATTACKS_MIN; // how many patterns scheduler aims for (2 or 3)
    private int lastTrackCount; // size of attack track last tick

    // server only, tick sbetween persistent attacks restart
    private final int[] persistentCooldown = new int[TrinityAttacks.count()];

    /**
     * One running attack pattern + the slots it owns.
     */
    private static final class Track {
        final int index;              // TrinityAttacks factory index, synced so the client rebuilds
        final TrinityAttack attack;
        final int[] owned;
        int age;
        int cutAge = -1; // dissolve when -1

        Track(int index, TrinityAttack attack, int[] owned) {
            this.index = index;
            this.attack = attack;
            this.owned = owned;
        }
    }

    private int serverTicks; // total ticks lived for cooldowns
    private final Map<UUID, Integer> hitCooldowns = new HashMap<>(); // server only, last tick player was hit

    // CLIENT ONLY
    @Nullable private TrinityFx fx;
    private boolean clientRetired;
    private boolean musicHeld;

    // SERVER ONLY
    @Nullable private ServerBossEvent bossEvent;

    public TrinityWorldEvent() {
        super(TrinityWorldEvents.TRINITY.get());
        for (int i = 0; i < slots.length; i++) slots[i] = new AttackSlot();
    }

    public TrinityWorldEvent setup(Vec3 centre, float seed) { // read once here and synced from then on
        this.centre = centre;
        this.seed = seed;
        this.spawnTicks = spawnCountdownTicks();
        this.criticalTicks = meltdownTicks();
        return this;
    }

    @Override
    public boolean shouldSave() { // don't save!
        return false;
    }

    // ----------------------------------------------------- public API

    // if there is live instance in this dimension
    public static @Nullable TrinityWorldEvent find(Level level) {
        var data = level.getData(LodestoneAttachmentTypes.WORLD_EVENT_DATA);
        for (WorldEventInstance instance : data.activeWorldEvents) {
            if (instance instanceof TrinityWorldEvent trinity && !trinity.discarded) return trinity;
        }
        for (WorldEventInstance instance : data.inboundWorldEvents) {
            if (instance instanceof TrinityWorldEvent trinity && !trinity.discarded) return trinity;
        }
        return null;
    }

    public Vec3 centre() {
        return centre;
    }

    public float criticality() {
        return health;
    }

    public int criticalityDegree() {
        return criticalityDegree;
    }

    public TrinityPhase phase() {
        return phase;
    }

    public float difficulty() {
        return difficultyForDegree(criticalityDegree);
    }

    public void addCriticality(float amount) {
        if (amount <= 0f) return;
        if (phase != TrinityPhase.FIGHTING) return;
        health = Mth.clamp(health + amount, 0f, MAX_HEALTH);
    }

    public void forceRetire() {
        clearAttacks();
        demolition.cancel();
        // Explicitly NOT a defeat, so the reward hook does not fire: this is an operator
        // removing Trinity, not a player beating it. `defeated` is only ever set by the final
        // detonation actually starting.
        defeated = false;
        phase = TrinityPhase.DYING;
        detonationTicks = Math.max(detonationLifetimeTicks - 1, 0);
        removeBossBar();
        setDirty();
    }

    // -------------------------------------------------------- tick

    @Override
    public void tick(Level level) {
        phaseTicks++;
        totalTicks++;

        if (level.isClientSide()) {
            tickShared(level);
            if (!discarded) tickClient();  // tickShared can retire Trinity and ticking client right after will cause it to be rebuilt.
            return;
        }
        tickShared(level);
        tickServer((ServerLevel) level);
    }

    /** Motion both sides must agree on, driven purely by counters. */
    // Both sides must agree on, driven by counters and timers
    private void tickShared(Level level) {
        if (phase == TrinityPhase.DETONATING || phase == TrinityPhase.DYING
                || phase == TrinityPhase.REFORMING) {
            detonationTicks++;
        }
        tickAttacks(level);

        // b/c discard is not networked
        boolean blastDone = phase == TrinityPhase.DYING && detonationTicks >= detonationLifetimeTicks;
        boolean faded = phase == TrinityPhase.DESPAWNING && phaseTicks >= DESPAWN_TICKS;
        if (blastDone || faded) {
            if (level.isClientSide()) {
                removeFx();
            } else {
                // one place to do stuff after defeat
                if (blastDone) dropRewards((ServerLevel) level);
                removeBossBar();
            }
            discarded = true;
        }
    }

    private void tickServer(ServerLevel level) {
        serverTicks++;
        updateBossBar(level);
        demolition.tick(level);

        // this should probably be tuned down, its way too long right now for both
        if (phase == TrinityPhase.DETONATING || phase == TrinityPhase.DYING) {
            DetonationBlast.apply(level, detonationBase().add(0,4,0));
        }

        if (tickAbandonment(level)) return;

        // hit detection & do damage
        if ((phase == TrinityPhase.FIGHTING || phase == TrinityPhase.MELTDOWN) && !tracks.isEmpty()) {
            AttackHitDetection.apply(level, centre, BODY_RADIUS, slots, bulletField,
                    hitCooldowns, serverTicks);
        }

        switch (phase) {
            case SPAWNING -> {
                if (phaseTicks == spawnTicks - 1) {
                    demolition.schedule(BlockPos.containing(detonationBase()));
                }
                tickCountdownWarning(level, spawnTicks); // introduce fight, signal player of spawn detonation
                if (phaseTicks >= spawnTicks) {
                    beginDetonation(level, OPENING_DETONATION_TICKS, false);
                }
            }

            case DETONATING -> {
                // switch to reform, freeze detonation age since reformation will fade it
                if (detonationTicks >= reformHandoffTick()) {
                    setPhase(TrinityPhase.REFORMING);
                }
            }

            case REFORMING -> {
                if (phaseTicks == 1) {
                    level.playSound(null, BlockPos.containing(centre), SoundEvents.BEACON_DEACTIVATE,
                            SoundSource.HOSTILE, 8.0F, 0.35F);
                }
                if (phaseTicks >= REFORM_TICKS) {
                    // increment criticality
                    if (nextThreshold > 0) criticalityDegree++;
                    setPhase(TrinityPhase.FIGHTING);
                }
            }

            case FIGHTING -> {
                addCriticality(CRITICALITY_PER_SECOND / 20f);
                if (health >= currentThreshold()) {
                    setPhase(TrinityPhase.MELTDOWN);
                    level.playSound(null, BlockPos.containing(centre), SoundEvents.WARDEN_SONIC_CHARGE,
                            SoundSource.HOSTILE, 6.0F, 0.4F);
                } else {
                    // persistent first since attack selection can depend on it
                    tickPersistent(level);
                    tickAttackSelection(level);
                }
            }

            case MELTDOWN -> {
                tickCountdownWarning(level, criticalTicks);
                if (phaseTicks >= criticalTicks) {
                    boolean last = nextThreshold >= CRITICALITY_THRESHOLDS.length - 1;
                    nextThreshold++;
                    beginDetonation(level, last ? FINAL_DETONATION_TICKS : MELTDOWN_DETONATION_TICKS, last);
                }
            }

            // DYING & DESPAWNING are handled in shared
            case DYING, DESPAWNING -> { }
        }
    }

    // -------------------------------------------- abandonment

    private boolean tickAbandonment(ServerLevel level) {
        if (phase == TrinityPhase.DYING || phase == TrinityPhase.DESPAWNING) return false;

        if (anyPlayerNear(level)) {
            emptyTicks = 0;
            return false;
        }

        if (++emptyTicks < abandonTimeoutTicks()) return false;

        if (phase != TrinityPhase.FIGHTING && phase != TrinityPhase.SPAWNING
                && phase != TrinityPhase.MELTDOWN) {
            return false;
        }

        clearAttacks();
        setPhase(TrinityPhase.DESPAWNING);
        return true;
    }

    private boolean anyPlayerNear(ServerLevel level) {
        double r2 = targetRange() * targetRange();
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || !p.isAlive()) continue;
            if (p.position().distanceToSqr(centre) <= r2) return true;
        }
        return false;
    }

    // -------------------------------------------------------- defaet/rewards

    private void dropRewards(ServerLevel level) {
        if (!defeated || rewardsGiven) return;
        rewardsGiven = true;
        TrinityRewards.onTrinityDefeated(level, centre, detonationBase());
    }

    private int reformHandoffTick() {
        return Math.round(detonationLifetimeTicks * REFORM_START_FRAC);
    }

    private float currentThreshold() {
        return CRITICALITY_THRESHOLDS[Math.min(nextThreshold, CRITICALITY_THRESHOLDS.length - 1)];
    }

    private void beginDetonation(ServerLevel level, int lifetimeTicks, boolean fatal) { // ONLY set defeated here
        if (fatal) defeated = true;
        clearAttacks();
        detonationTicks = 0;
        detonationLifetimeTicks = lifetimeTicks;
        setPhase(fatal ? TrinityPhase.DYING : TrinityPhase.DETONATING);
        playDetonation(level, fatal);
    }

    // TODO: custom detonation sound effects
    private void playDetonation(ServerLevel level, boolean fatal) {
        BlockPos at = BlockPos.containing(detonationBase());
        level.playSound(null, at, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 10.0F, 0.35F);
        level.playSound(null, at, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 10.0F, 0.5F);
        if (fatal) {
            level.playSound(null, at, SoundEvents.WITHER_DEATH, SoundSource.HOSTILE, 10.0F, 0.4F);
        }
    }

    // TODO: countdown sound effects, for both spawning detonation and phase changes
    private void tickCountdownWarning(ServerLevel level, int lengthTicks) {
        float progress = Mth.clamp(phaseTicks / (float) Math.max(lengthTicks, 1), 0f, 1f);
        int interval = Math.max(1, Math.round(Mth.lerp(progress, 10f, 1f)));
        if (phaseTicks % interval != 0) return;
        level.playSound(null, BlockPos.containing(centre), SoundEvents.NOTE_BLOCK_BIT.value(),
                SoundSource.HOSTILE, 3.0F, 0.6F + progress * 1.2F);
    }

    private void setPhase(TrinityPhase next) {
        phase = next;
        phaseTicks = 0;
        setDirty();
    }

    // ------------------------------------------------------- attacks

    // advances every running pattern from both sides
    // outside fighting everything is faded out and frozen
    private void tickAttacks(Level level) {
        if (tracks.isEmpty()) return;
        TrinityAttack.Context ctx = attackContext(level);
        boolean cutting = phase != TrinityPhase.FIGHTING;

        boolean released = false;
        Iterator<Track> it = tracks.iterator();
        while (it.hasNext()) {
            Track track = it.next();
            track.age++;
            track.attack.tick(ctx, track.age);

            if (cutting) {
                track.cutAge++;
                float fade = Mth.clamp(track.cutAge / (float) ATTACK_CUT_TICKS, 0f, 1f);
                for (int index : track.owned) {
                    AttackSlot slot = slots[index];
                    if (slot.isActive()) slot.fade = Math.max(slot.fade, fade);
                }
                if (track.attack instanceof BulletPatternAttack) {
                    bulletField.cut = Math.max(bulletField.cut, fade); // manually cut projectile field
                }
                if (track.cutAge >= ATTACK_CUT_TICKS) {
                    release(track);
                    it.remove();
                    released = true;
                    continue;
                }
            }

            if (track.attack.isFinished()) {
                release(track);
                it.remove();
                released = true;
            }
        }

        if (released && !level.isClientSide()) setDirty();
    }

    /**
     * Keeps the three persistent systems running. Server side, FIGHTING only.
     * <p>
     * These are not attacks the scheduler picks - they are started once and regenerate their own
     * patterns for the whole of normal combat. Two of them (the projectile field and the
     * wandering beams) never finish at all and are only ever ended by a meltdown cutting them;
     * the spike volley does finish, and is simply started again, which is what "continuously
     * cycle through its 2-6 spike sequences" means in practice.
     * <p>
     * Because a persistent track sits in {@code tracks} like any other, it occupies its family,
     * and the ordinary scheduler is left with the laser and the two containment lattices without
     * needing to know that persistence exists at all.
     */
    private void tickPersistent(ServerLevel level) {
        for (int index : TrinityAttacks.PERSISTENT) {
            if (persistentCooldown[index] > 0) persistentCooldown[index]--;

            boolean running = false;
            for (Track track : tracks) {
                if (track.index == index) {
                    running = true;
                    break;
                }
            }
            if (running) continue;

            // A short beat between cycles, so one sequence is seen to end before the next
            // begins. Charged only after a cycle actually ended - the first start is immediate.
            if (persistentCooldown[index] > 0) continue;
            if (!startAttack(level, index)) continue;
            persistentCooldown[index] = PERSISTENT_RECYCLE_TICKS;
        }
    }

    // keeps one or two scheduled attack patterns running on the persistent ones
    // serverside, FIGHTING only.
    private void tickAttackSelection(ServerLevel level) {
        for (Track track : tracks) {
            if (track.attack.isDirty()) setDirty();
        }

        int scheduled = scheduledTrackCount(); // counted only aver scheduled tracks, no need to track omnipresent persistent attacks

        if (scheduled < lastTrackCount || scheduled == 0) {
            desiredAttacks = level.getRandom().nextFloat() < extraAttackChance(difficulty())
                    ? ATTACKS_MAX : ATTACKS_MIN;
        }

        int want = Mth.clamp(desiredAttacks, ATTACKS_MIN, ATTACKS_MAX);
        while (scheduled < want) {
            if (!startAttack(level, -1)) break;   // try again next tick
            scheduled++;
        }
        lastTrackCount = scheduled;
    }

    private int scheduledTrackCount() {
        int n = 0;
        for (Track track : tracks) {
            if (TrinityAttacks.isScheduled(track.index)) n++;
        }
        return n;
    }

    // starts one more attack pattern alongside anything, returns false is nothing is eligible
    private boolean startAttack(ServerLevel level, int forcedIndex) {
        int free = freeSlotCount();

        TrinityAttack.Context ctx = attackContext(level);
        int chosenIndex;
        if (forcedIndex >= 0) {
            TrinityAttack probe = TrinityAttacks.byIndex(forcedIndex);
            if (probe == null || probe.slotsNeeded(ctx) > free) return false;
            chosenIndex = forcedIndex;
        } else {
            chosenIndex = TrinityAttacks.pick(level.getRandom(), candidate -> {
                int family = TrinityAttacks.family(candidate);
                for (Track track : tracks) {
                    if (TrinityAttacks.family(track.index) == family) return false;
                }
                TrinityAttack probe = TrinityAttacks.byIndex(candidate);
                return probe != null && probe.slotsNeeded(ctx) <= free;
            });
        }
        if (chosenIndex < 0) return false;

        TrinityAttack chosen = TrinityAttacks.byIndex(chosenIndex);
        if (chosen == null) return false;

        int need = chosen.slotsNeeded(ctx);
        if (need > free) return false;   // never bind a pattern to fewer slots than it needs
        int[] owned = allocateSlots(need);
        AttackSlot[] view = new AttackSlot[owned.length];
        for (int i = 0; i < owned.length; i++) view[i] = slots[owned[i]];

        chosen.bind(view);
        chosen.start(ctx);
        tracks.add(new Track(chosenIndex, chosen, owned));
        setDirty();
        return true;
    }

    private boolean[] slotsInUse() {
        boolean[] used = new boolean[ATTACK_SLOTS];
        for (Track track : tracks) {
            for (int index : track.owned) used[index] = true;
        }
        return used;
    }

    private int freeSlotCount() {
        int free = 0;
        for (boolean used : slotsInUse()) if (!used) free++;
        return free;
    }

    private int[] allocateSlots(int count) {
        boolean[] used = slotsInUse();
        int[] owned = new int[count];
        int n = 0;
        for (int i = 0; i < ATTACK_SLOTS && n < count; i++) {
            if (used[i]) continue;
            slots[i].clear();   // clear previous occupant's geometry
            owned[n++] = i;
        }
        return owned;
    }

    // frees a finished pattern's slots, attack object is dropped
    private void release(Track track) {
        for (int index : track.owned) slots[index].clear();
        if (track.attack instanceof BulletPatternAttack) bulletField.clear();
    }

    // drops every pattern at once
    private void clearAttacks() {
        tracks.clear();
        for (AttackSlot slot : slots) slot.clear();
        bulletField.clear();
        lastTrackCount = 0;
        setDirty();
    }

    private TrinityAttack.Context attackContext(Level level) {
        return new TrinityAttack.Context() {
            @Override public Vec3 centre() { return centre; }
            @Override public float bodyRadius() { return BODY_RADIUS; }
            @Override public float difficulty() { return TrinityWorldEvent.this.difficulty(); }
            @Override public RandomSource random() { return level.getRandom(); }
            @Override public @Nullable Player pickTarget() { return TrinityWorldEvent.this.pickTarget(level); }
            @Override public @Nullable Player playerById(int id) {
                return level.getEntity(id) instanceof Player p ? p : null;
            }
            @Override public BulletField bulletField() { return bulletField; }
        };
    }

    private @Nullable Player pickTarget(Level level) {
        List<Player> candidates = new ArrayList<>();
        double r2 = targetRange() * targetRange();
        for (Player p : level.players()) {
            if (p.isSpectator() || !p.isAlive()) continue;
            if (p.position().distanceToSqr(centre) <= r2) candidates.add(p);
        }
        if (candidates.isEmpty()) return null;
        return candidates.get(level.getRandom().nextInt(candidates.size())); // random player
    }

    // ---------------------------------------------- boss bar

    private void updateBossBar(ServerLevel level) {
        if (bossEvent == null) {
            bossEvent = new ServerBossEvent(Mth.createInsecureUUID(level.getRandom()),
                    Component.translatable("bossbar.echoesofwar.trinity"),
                    BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_20);
            bossEvent.setDarkenScreen(true);
        }
        bossEvent.setProgress(Mth.clamp(health / MAX_HEALTH, 0f, 1f));

        if (serverTicks % 20 != 0) return;
        double r2 = targetRange() * targetRange();
        for (ServerPlayer p : level.players()) {
            boolean near = p.position().distanceToSqr(centre) <= r2;
            if (near) bossEvent.addPlayer(p);
            else bossEvent.removePlayer(p);
        }
    }

    private void removeBossBar() {
        if (bossEvent == null) return;
        bossEvent.removeAllPlayers();
        bossEvent = null;
    }

    @Override
    public void end(Level level) {
        removeBossBar();
        removeFx();
        super.end(level);
    }

    // ---------------------------------------------------- client

    private void tickClient() {
        if (clientRetired) return;
        if (fx == null) {
            fx = new TrinityFx();
            if (TrinityPostProcessor.INSTANCE.addFxInstance(fx) == null) {
                fx = null;
                return;
            }
            TrinityPostProcessor.INSTANCE.setActive(true);
        }
        fx.write(this);

        ModMusicManager.requestTrack(ModSounds.TRINITY_SOUNDTRACK.get());
        musicHeld = true;
    }

    // tears down everything owned by clientside and marks it as such
    // safe to call anywhere from any state
    private void removeFx() {
        clientRetired = true;
        if (musicHeld) {
            ModMusicManager.release();
            musicHeld = false;
        }
        if (fx == null) return;
        fx.remove();
        fx = null;
    }

    // ------------------------------------------- SHADER PASSED VALUES

    public Vec3 shaderCentre() { return centre; }
    public Vec3 detonationBase() { return centre.subtract(0.0, SPAWN_HEIGHT, 0.0); }
    public float shaderSeed() { return seed; }

    public float meltdownProgress() {
        return switch (phase) {
            case MELTDOWN -> Mth.clamp(phaseTicks / (float) Math.max(criticalTicks, 1), 0f, 1f);
            case SPAWNING -> Mth.clamp(phaseTicks / (float) Math.max(spawnTicks, 1), 0f, 1f);
            default -> 0f;
        };
    }

    public float reformProgress() { // 0..1
        return phase == TrinityPhase.REFORMING ? Mth.clamp(phaseTicks / (float) REFORM_TICKS, 0f, 1f) : 0f;
    }

    // ho wmuch of body is present, zero during detonation
    public float bodyScale() {
        return switch (phase) {
            case DETONATING, DYING -> 0f;
            case REFORMING -> bodyGrowth();
            case DESPAWNING -> 1f - Mth.clamp(phaseTicks / (float) DESPAWN_TICKS, 0f, 1f);
            default -> 1f;
        };
    }

    // how far body has gathered during reformation
    private float bodyGrowth() {
        float rf = reformProgress();
        float t = Mth.clamp((rf - REFORM_BODY_START) / (1f - REFORM_BODY_START), 0f, 1f);
        return t * t;
    }

    public boolean detonationActive() {
        // stays true throughout REFORMING since reformation shader IS the detonation shader
        return phase == TrinityPhase.DETONATING || phase == TrinityPhase.DYING
                || phase == TrinityPhase.REFORMING;
    }

    public float clockSeconds() { return totalTicks / 20f; }

    public float detonationAge() { return detonationTicks / 20f; }
    public float detonationLifetime() { return detonationLifetimeTicks / 20f; }
    public int phaseTicks() { return phaseTicks; }

    // -------------------------------------------------------- persistence/sync

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putDouble("x", centre.x);
        tag.putDouble("y", centre.y);
        tag.putDouble("z", centre.z);
        tag.putFloat("seed", seed);
        tag.putFloat("health", health);
        tag.putInt("degree", criticalityDegree);
        tag.putInt("nextThreshold", nextThreshold);
        tag.putInt("phase", phase.ordinal());
        tag.putInt("phaseTicks", phaseTicks);
        tag.putInt("totalTicks", totalTicks);
        tag.putInt("detTicks", detonationTicks);
        tag.putInt("detLife", detonationLifetimeTicks);
        tag.putInt("desired", desiredAttacks);
        tag.putInt("spawnTicks", spawnTicks);
        tag.putInt("criticalTicks", criticalTicks);

        ListTag list = new ListTag();
        for (AttackSlot slot : slots) {
            CompoundTag s = new CompoundTag();
            slot.save(s);
            list.add(s);
        }
        tag.put("slots", list);

        // Every running pattern, with the slots it owns and how far into it we are, so the
        // client can rebuild each attack object and keep animating between packets rather than
        // freezing on the last synced geometry.
        ListTag running = new ListTag();
        for (Track track : tracks) {
            CompoundTag t = new CompoundTag();
            t.putInt("index", track.index);
            t.putInt("age", track.age);
            t.putInt("cut", track.cutAge);
            t.putIntArray("owned", track.owned);
            CompoundTag data = new CompoundTag();
            track.attack.save(data);
            t.put("data", data);
            running.add(t);
        }
        tag.put("tracks", running);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        centre = new Vec3(tag.getDoubleOr("x", 0), tag.getDoubleOr("y", 0), tag.getDoubleOr("z", 0));
        seed = tag.getFloatOr("seed", 0f);
        health = tag.getFloatOr("health", 0f);
        criticalityDegree = tag.getIntOr("degree", 0);
        nextThreshold = tag.getIntOr("nextThreshold", 0);
        phase = TrinityPhase.byId(tag.getIntOr("phase", 0));
        phaseTicks = tag.getIntOr("phaseTicks", 0);
        totalTicks = tag.getIntOr("totalTicks", 0);
        detonationTicks = tag.getIntOr("detTicks", 0);
        detonationLifetimeTicks = tag.getIntOr("detLife", OPENING_DETONATION_TICKS);
        desiredAttacks = tag.getIntOr("desired", ATTACKS_MIN);
        spawnTicks = Math.max(1, tag.getIntOr("spawnTicks", 400));
        criticalTicks = Math.max(1, tag.getIntOr("criticalTicks", 200));

        tag.getList("slots").ifPresent(list -> {
            for (int i = 0; i < Math.min(list.size(), slots.length); i++) {
                list.getCompound(i).ifPresent(slots[i]::load);
            }
        });

        // rebuilding running set of attacks
        tracks.clear();
        tag.getList("tracks").ifPresent(list -> {
            for (int i = 0; i < list.size(); i++) {
                final int at = i;
                list.getCompound(at).ifPresent(t -> {
                    int index = t.getIntOr("index", -1);
                    TrinityAttack rebuilt = TrinityAttacks.byIndex(index);
                    if (rebuilt == null) return;

                    int[] owned = t.getIntArray("owned").orElse(new int[0]);
                    AttackSlot[] view = new AttackSlot[owned.length];
                    for (int k = 0; k < owned.length; k++) {
                        int slot = owned[k];
                        if (slot < 0 || slot >= slots.length) return;
                        view[k] = slots[slot];
                    }

                    rebuilt.bind(view);
                    t.getCompound("data").ifPresent(rebuilt::load);
                    Track track = new Track(index, rebuilt, owned);
                    track.age = t.getIntOr("age", 0);
                    track.cutAge = t.getIntOr("cut", -1);
                    tracks.add(track);
                });
            }
        });
        lastTrackCount = tracks.size();
    }
}
