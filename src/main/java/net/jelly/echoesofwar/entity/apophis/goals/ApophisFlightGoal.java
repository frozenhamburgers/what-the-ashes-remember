package net.jelly.echoesofwar.entity.apophis.goals;

import net.jelly.echoesofwar.entity.apophis.ApophisEntity;
import net.jelly.echoesofwar.entity.apophis.smog.ApophisSmogWorldEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import team.lodestar.lodestone.modules.toolkit.worldevent.WorldEventHandler;

import java.util.EnumSet;

// loop: ASCEND to the cloud, then PROWL until SUBMERGED_TICKS_BEFORE_DIVING, then cycle
// APPROACH -> PASS -> RECOVER -> PROWL. leave from PROWL once FLIGHT_EXIT_DAMAGE lands for proper recovery
public class ApophisFlightGoal extends Goal {

    // swimming rise
    private static final double SWIM_SPEED = 0.6;
    private static final double SWIM_AMPLITUDE = 0.7;
    private static final int SWIM_PERIOD_TICKS = 100;
    private static final double VERTICAL_WEAVE = 0.45;
    private static final double VELOCITY_SMOOTHING = 0.14;

    // climb
    private static final double ARRIVAL_DISTANCE = 10.0;
    private static final int ASCENT_TIMEOUT_TICKS = 900;
    private static final float SMOG_RADIUS = 75.0f;
    private static final double SMOG_START_FRACTION = 0.25;

    // within cloud
    /** fraction of the cloud radius that counts as submerged for healing and retreat */
    private static final double SUBMERGED_FRACTION = 0.55;
    // orbiting/wander
    private static final double ORBIT_EXTENT_FRACTION = 0.38;
    private static final double CONTAINMENT_FRACTION = 0.55;
    private static final double CONTAINMENT_LIMIT_FRACTION = 0.78;
    private static final double HEAL_PER_SECOND = 2.0;
    /** grace period outside the cloud before the submerged counter resets */
    private static final int SUBMERGED_GRACE_TICKS = 15;
    /** submerged time before the first attack */
    private static final int SUBMERGED_TICKS_BEFORE_DIVING = 400;
    /** submerged time between subsequent attacks */
    private static final int DIVE_COOLDOWN_TICKS = 120;

    /** speed boost on entering clouud for coiling */
    private static final double SETTLE_SPEED_MULTIPLIER = 12;
    private static final int SETTLE_DURATION_TICKS = 120;

    // orbiting center and shape
    private static final int ORBIT_SHIFT_TICKS = 160;
    private static final double ORBIT_EASE = 0.02;
    /** smallest and widest the circle gets, fractions of the cloud radius */
    private static final double ORBIT_RADIUS_MIN_FRACTION = 0.24;
    private static final double ORBIT_RADIUS_MAX_FRACTION = 0.46;
    private static final double ORBIT_RADIUS_FLOOR = 18.0;
    /** how far the circle's centre may sit off the cloud's center */
    private static final double ORBIT_CENTER_MAX_FRACTION = 0.20;
    // orbiting forces
    private static final double ORBIT_PULL = 0.045;
    private static final double ORBIT_AXIAL_PULL = 0.018;
    private static final double ORBIT_MAX_TILT_DEGREES = 80.0;
    private static final double COIL_DRIFT_MAX = 0.40;

    // dive attack
    private static final double DIVE_SPEED = 2.6;
    /** speed to the pass entry point, which is outside the cloud so the recovery is angled */
    private static final double APPROACH_SPEED = 2.0;
    /** how close to the entry point counts as lined up */
    private static final double ENTRY_ARRIVAL = 12.0;
    /** how much of the idle weave conforms to attack trajectory */
    private static final double DIVE_WEAVE = 0.15;
    private static final double DIVE_HOMING = 0.085;
    private static final double DIVE_SMOOTHING = 0.30;
    /** contact damage multiplier for dive */
    private static final double DIVE_DAMAGE_MULTIPLIER = 2.5;
    /** how far above the target the descent begins */
    private static final double PASS_ENTRY_HEIGHT = 30.0;
    /** horizontal distance covered while descending onto the target */
    private static final double PASS_DESCENT_RUN = 46.0;
    /** horizontal distance skimmed past the target before pulling up */
    private static final double PASS_EXIT_RUN = 38.0;
    private static final double PASS_ALTITUDE_GAIN = 0.10;
    private static final double PASS_ALTITUDE_CLAMP = 1.1;
    /** cap on lead prediction of target */
    private static final double PASS_LEAD_CAP = 24.0;
    private static final int APPROACH_TIMEOUT_TICKS = 140;
    private static final int PASS_TIMEOUT_TICKS = 140;

    // post dive retreat
    /** how long the bank from the skim onto the climb takes */
    private static final int RECOVER_TURN_TICKS = 20;
    // recovery should be quick bc body very vulnerable here
    private static final double RECOVER_SPEED = 2.2;
    private static final int RECOVER_TIMEOUT_TICKS = 300;

    // with no target, end flight after time
    private static final int NO_TARGET_EXIT_TICKS = 200;

    private enum Phase { ASCEND, PROWL, APPROACH, PASS, RECOVER }

    private final ApophisEntity apophis;

    private Phase phase;
    private boolean finished;
    private Vec3 destination;
    private double startDistance;
    private int phaseTicks;
    private int swimTicks;
    /** ticks spent inside the cloud during the current prowl */
    private int submergedTicks;
    /** consecutive ticks spent outside it, against SUBMERGED_GRACE_TICKS */
    private int outsideTicks;
    /** false until the first attack */
    private boolean divingBegun;
    /** consecutive ticks spent prowling with nothing to fight */
    private int noTargetTicks;

    // live orbit, and the targets it is easing toward
    private Vec3 orbitCenter = Vec3.ZERO;
    private Vec3 orbitNormal = new Vec3(0, 1, 0);
    private double orbitRadius;
    private double coilDrift;
    private Vec3 targetOrbitCenter = Vec3.ZERO;
    private Vec3 targetOrbitNormal = new Vec3(0, 1, 0);
    private double targetOrbitRadius;
    private double targetCoilDrift;
    private int orbitShiftTicks;

    // the pass: a horizontal bearing fixed at plan time, and a strike point that keeps some of its
    // freedom to chase the target - see DIVE_HOMING
    private Vec3 passEntry = Vec3.ZERO;
    private Vec3 passStrike = Vec3.ZERO;
    private Vec3 passBearing = new Vec3(1, 0, 0);
    /** heading the skim finished on, so RECOVER can smoothly drive away from it */
    private Vec3 recoverHeading = new Vec3(0, 1, 0);

    private ApophisSmogWorldEvent smog;

    public ApophisFlightGoal(ApophisEntity apophis) {
        this.apophis = apophis;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return apophis.isFlightPending();
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
        this.finished = false;
        this.phaseTicks = 0;
        this.swimTicks = 0;
        this.submergedTicks = 0;
        this.outsideTicks = 0;
        this.divingBegun = false;
        this.noTargetTicks = 0;

        // a summoned Apophis arrives in a cloud already poured for it - starts prowling in place
        // rather than climbing to lay its own
        ApophisSmogWorldEvent inherited = apophis.takeInheritedCloud();
        if (inherited != null) {
            this.smog = inherited;
            this.destination = apophis.getInheritedCloudCentre();
            this.startDistance = 1.0;
            this.smog.bindToFlight(apophis.getId());
            this.smog.stopEmitting();
            this.phase = Phase.PROWL;
        } else {
            this.smog = null;
            this.destination = flightDestination();
            this.startDistance = Math.max(apophis.headPosition().distanceTo(destination), 1.0);
            this.phase = Phase.ASCEND;
        }

        initOrbit();
        apophis.beginFlight();
    }

    @Override
    public void stop() {
        this.phase = null;
        // the cloud outlives the phase -> hand it over to fade on its own schedule gracefully
        if (smog != null) {
            smog.beginFade();
            smog = null;
        }
        apophis.endFlight();
    }

    @Override
    public void tick() {
        phaseTicks++;
        swimTicks++;

        if (isSubmerged()) {
            apophis.heal((float) (HEAL_PER_SECOND / 20.0));
            submergedTicks++;
            outsideTicks = 0;
        } else if (++outsideTicks >= SUBMERGED_GRACE_TICKS) {
            submergedTicks = 0;
        }

        switch (phase) {
            case ASCEND -> tickAscend();
            case PROWL -> tickProwl();
            case APPROACH -> tickApproach();
            case PASS -> tickPass();
            case RECOVER -> tickRecover();
        }
    }

    private boolean isSubmerged() {
        return withinCloud(SUBMERGED_FRACTION);
    }

    private boolean withinCloud(double fraction) {
        if (smog == null || smog.isFading()) return false;
        double radius = cloudRadius() * fraction;
        return apophis.headPosition().distanceToSqr(destination) <= radius * radius;
    }

    private double cloudRadius() {
        return smog != null ? smog.currentRadius() : 0.0;
    }

    /** FLIGHT_HEIGHT over whoever it is fighting, or straight up if it has nobody */
    private Vec3 flightDestination() {
        LivingEntity target = apophis.getTarget();
        Vec3 below = target != null ? target.position() : apophis.headPosition();
        return new Vec3(below.x, below.y + ApophisEntity.FLIGHT_HEIGHT, below.z);
    }

    // ------------------------------------------------------------------ PHASES

    private void tickAscend() {
        Vec3 headPos = apophis.headPosition();
        double remaining = headPos.distanceTo(destination);

        // start spewing partway up, so the jet and the cloud both have the rest of the climb to
        // build rather than appearing finished
        if (smog == null && remaining <= startDistance * (1.0 - SMOG_START_FRACTION)) {
            spawnSmog();
        }

        if (remaining <= ARRIVAL_DISTANCE || phaseTicks >= ASCENT_TIMEOUT_TICKS) {
            if (smog != null) smog.stopEmitting();
            enterPhase(Phase.PROWL);
            return;
        }

        swimToward(destination, SWIM_SPEED, 1.0, VELOCITY_SMOOTHING);
    }

    // circling inside the cloud, healing - the only phase the flight is allowed to end from, so
    // damage landing mid-attack finishes the attack first
    private void tickProwl() {
        if (divingBegun && apophis.getDamageDuringFlight() >= ApophisEntity.FLIGHT_EXIT_DAMAGE) {
            finished = true;
            return;
        }

        // no cloud means no cover/healing/submerged timer, so the attack cycle (and thus the exit
        // condition) could never trigger - leave outright instead
        if (smog == null || smog.isFading()) {
            finished = true;
            return;
        }

        if (apophis.getTarget() == null) {
            if (++noTargetTicks >= NO_TARGET_EXIT_TICKS) {
                finished = true;
                return;
            }
        } else {
            noTargetTicks = 0;
            int required = divingBegun ? DIVE_COOLDOWN_TICKS : SUBMERGED_TICKS_BEFORE_DIVING;
            if (submergedTicks >= required && planPass(true)) {
                divingBegun = true;
                apophis.setContactDamageMultiplier(DIVE_DAMAGE_MULTIPLIER);
                enterPhase(Phase.APPROACH);
                return;
            }
        }

        advanceOrbit();
        steer(prowlDirection(), prowlSpeed(), 1.0, VELOCITY_SMOOTHING);
    }

    private double prowlSpeed() {
        double settle = Math.max(0.0, 1.0 - phaseTicks / (double) SETTLE_DURATION_TICKS);
        return SWIM_SPEED * (1.0 + (SETTLE_SPEED_MULTIPLIER - 1.0) * settle);
    }

    // run-in to the head of the pass line. arrival is a distance test rather than a plane test:
    // Apophis usually starts directly over its prey, already past the entry plane (which sits
    // up-and-back along a downward line), so a plane test would end the approach immediately
    private void tickApproach() {
        Vec3 headPos = apophis.headPosition();

        planPass(false); // still tracking freely; nothing committed until the descent starts

        if (headPos.distanceToSqr(passEntry) <= ENTRY_ARRIVAL * ENTRY_ARRIVAL
                || phaseTicks >= APPROACH_TIMEOUT_TICKS) {
            enterPhase(Phase.PASS);
            return;
        }
        swimToward(passEntry, APPROACH_SPEED, DIVE_WEAVE, DIVE_SMOOTHING);
    }

    // the strike itself: flat out along the frozen line, no correction - whether it connects was
    // settled when planPass() last ran, which is what makes it dodgeable
    private void tickPass() {
        Vec3 headPos = apophis.headPosition();
        double along = passBearing.dot(headPos.subtract(passStrike));

        if (along >= PASS_EXIT_RUN || phaseTicks >= PASS_TIMEOUT_TICKS) {
            recoverHeading = travelDirection();
            enterPhase(Phase.RECOVER);
            return;
        }

        homeStrikePoint();
        steer(skimHeading(headPos, along), DIVE_SPEED, DIVE_WEAVE, DIVE_SMOOTHING);
    }

    // altitude follows a quadratic that bottoms out level at the strike point (rather than a straight
    // line descending through it), so the exit is already level and recovery is a pull-up
    private Vec3 skimHeading(Vec3 headPos, double along) {
        double desiredY;
        if (along < 0.0) {
            double remaining = Math.min(-along, PASS_DESCENT_RUN) / PASS_DESCENT_RUN;
            desiredY = passStrike.y + PASS_ENTRY_HEIGHT * remaining * remaining;
        } else {
            desiredY = passStrike.y;
        }

        // steers at the strike point on the way in, then holds the planned bearing once through it
        // so the exit carries straight on instead of curling back around
        Vec3 flat = passStrike.subtract(headPos).multiply(1.0, 0.0, 1.0);
        Vec3 horizontal = along < 0.0 && flat.lengthSqr() > 9.0 ? flat.normalize() : passBearing;

        double climb = Mth.clamp((desiredY - headPos.y) * PASS_ALTITUDE_GAIN,
                -PASS_ALTITUDE_CLAMP, PASS_ALTITUDE_CLAMP);
        return horizontal.add(0.0, climb, 0.0).normalize();
    }

    // homing for dive
    private void homeStrikePoint() {
        LivingEntity target = apophis.getTarget();
        if (target == null) return;
        passStrike = passStrike.add(target.position().subtract(passStrike).scale(DIVE_HOMING));
    }

    // eases from the pass's exit heading round to the cloud over RECOVER_TURN_TICKS
    private void tickRecover() {
        if (isSubmerged() || phaseTicks >= RECOVER_TIMEOUT_TICKS) {
            enterPhase(Phase.PROWL);
            return;
        }

        Vec3 headPos = apophis.headPosition();
        Vec3 home = destination.subtract(headPos);
        home = home.lengthSqr() > 1.0E-4 ? home.normalize() : new Vec3(0, 1, 0);

        double turn = Math.min(phaseTicks / (double) RECOVER_TURN_TICKS, 1.0);
        Vec3 heading = lerpDirection(recoverHeading, home, turn);
        steer(heading, RECOVER_SPEED, 1.0, DIVE_SMOOTHING);
    }

    // ------------------------------------------------------------------ PROWL/ORBIT

    private void initOrbit() {
        orbitCenter = destination;
        targetOrbitCenter = destination;
        orbitNormal = randomUnitVector();
        targetOrbitNormal = orbitNormal;
        orbitRadius = targetOrbitRadius = 20.0;
        coilDrift = targetCoilDrift = 0.0;
        orbitShiftTicks = 0;
    }

    private void advanceOrbit() {
        if (--orbitShiftTicks <= 0) {
            rollOrbitTargets();
            orbitShiftTicks = ORBIT_SHIFT_TICKS;
        }

        orbitCenter = orbitCenter.add(targetOrbitCenter.subtract(orbitCenter).scale(ORBIT_EASE));
        orbitNormal = lerpDirection(orbitNormal, targetOrbitNormal, ORBIT_EASE);
        orbitRadius += (targetOrbitRadius - orbitRadius) * ORBIT_EASE;
        coilDrift += (targetCoilDrift - coilDrift) * ORBIT_EASE;
    }

    private void rollOrbitTargets() {
        double cloudR = cloudRadius();
        double extent = cloudR * ORBIT_EXTENT_FRACTION;
        double wanted = cloudR * Mth.lerp(apophis.getRandom().nextDouble(),
                ORBIT_RADIUS_MIN_FRACTION, ORBIT_RADIUS_MAX_FRACTION);
        targetOrbitRadius = Math.min(Math.max(wanted, ORBIT_RADIUS_FLOOR), Math.max(extent, 1.0));

        // keep the whole circle inside the extent rather than just its center, else a wide orbit
        // offset to one side would spend half of every lap out past it
        double maxOffset = Math.max(extent - targetOrbitRadius, 0.0);
        double offset = Math.min(cloudR * ORBIT_CENTER_MAX_FRACTION, maxOffset)
                * Math.cbrt(apophis.getRandom().nextDouble());
        targetOrbitCenter = destination.add(randomUnitVector().scale(offset));

        targetOrbitNormal = tiltedNormal(targetOrbitNormal, Math.toRadians(ORBIT_MAX_TILT_DEGREES));
        targetCoilDrift = (apophis.getRandom().nextDouble() * 2.0 - 1.0) * COIL_DRIFT_MAX;
    }

    // orbit tangent + pull-back onto it + axial coil drift, blended toward the cloud center if it
    // has strayed near the edge
    private Vec3 prowlDirection() {
        Vec3 headPos = apophis.headPosition();
        Vec3 relative = headPos.subtract(orbitCenter);
        double axial = relative.dot(orbitNormal);
        Vec3 radial = relative.subtract(orbitNormal.scale(axial));
        double radialLength = radial.length();

        Vec3 radialAxis = radialLength > 1.0E-4
                ? radial.scale(1.0 / radialLength)
                : anyPerpendicular(orbitNormal);
        Vec3 tangent = orbitNormal.cross(radialAxis).normalize();

        Vec3 heading = tangent
                .add(radialAxis.scale((orbitRadius - radialLength) * ORBIT_PULL))
                .add(orbitNormal.scale(coilDrift - axial * ORBIT_AXIAL_PULL));
        heading = heading.lengthSqr() > 1.0E-6 ? heading.normalize() : tangent;

        // containment is a blend rather than a hard override, to avoid snapping into a corner
        double cloudR = cloudRadius();
        double inner = cloudR * CONTAINMENT_FRACTION;
        double outer = cloudR * CONTAINMENT_LIMIT_FRACTION;
        double distance = headPos.distanceTo(destination);
        double homeWeight = Mth.clamp((distance - inner) / Math.max(outer - inner, 1.0E-3), 0.0, 1.0);
        if (homeWeight <= 0.0) return heading;

        Vec3 home = destination.subtract(headPos);
        if (home.lengthSqr() < 1.0E-4) return heading;
        return lerpDirection(heading, home.normalize(), homeWeight);
    }

    // ------------------------------------------------------------------ DIVE/PASS ATTACK

    // plans  out the attack: where the target will be, and the bearing the skim runs along. called
    // once when the attack begins and then every approach tick to keep aim live
    // until the descent starts. Returns false if there's nothing to aim at
    private boolean planPass(boolean chooseBearing) {
        LivingEntity target = apophis.getTarget();
        if (target == null) return false;

        Vec3 headPos = apophis.headPosition();
        Vec3 targetPos = target.position();

        // lead by the expected flight time (run-in + descent)
        double runIn = Math.max(headPos.distanceTo(targetPos) - PASS_DESCENT_RUN, 0.0) / APPROACH_SPEED;
        double flightTicks = runIn + PASS_DESCENT_RUN / DIVE_SPEED;
        Vec3 lead = target.getDeltaMovement().scale(flightTicks);
        if (lead.length() > PASS_LEAD_CAP) lead = lead.normalize().scale(PASS_LEAD_CAP);
        passStrike = targetPos.add(lead.x, 0.0, lead.z);

        if (chooseBearing) {
            Vec3 flat = passStrike.subtract(headPos).multiply(1.0, 0.0, 1.0);
            passBearing = flat.lengthSqr() > 4.0 ? flat.normalize() : randomHorizontal();
        }

        passEntry = passStrike.subtract(passBearing.scale(PASS_DESCENT_RUN)).add(0.0, PASS_ENTRY_HEIGHT, 0.0);
        return true;
    }

    /** current direction of travel, for handing the recovery a heading to bank away from */
    private Vec3 travelDirection() {
        Vec3 velocity = apophis.getChaseVelocity();
        return velocity.lengthSqr() > 1.0E-4 ? velocity.normalize() : passBearing;
    }

    /** the size of cloud a flight lays, so a summoning box can create correctlys ized one */
    public static float smogRadius() {
        return SMOG_RADIUS;
    }

    /** whether an attack or the prowl is still running */
    public boolean isFlightActive() {
        return phase != null;
    }

    // ------------------------------------------------------------------ HELPERS

    private void enterPhase(Phase next) {
        phase = next;
        phaseTicks = 0;
        // cleared on exit rather than on entry, so timeouts and cut-short attacks can't leave it stuck on
        if (next != Phase.APPROACH && next != Phase.PASS) {
            apophis.setContactDamageMultiplier(1.0);
        }
    }

    private void spawnSmog() {
        Vec3 headPos = apophis.headPosition();
        float seed = (float) ((headPos.x * 12.9898 + headPos.z * 78.233) % 1000.0);
        smog = new ApophisSmogWorldEvent().setupForFlight(apophis.getId(), destination, SMOG_RADIUS, seed);
        WorldEventHandler.addWorldEvent(apophis.level(), smog);
    }

    /** normalised interpolation between two directions */
    private static Vec3 lerpDirection(Vec3 from, Vec3 to, double amount) {
        Vec3 blended = from.add(to.subtract(from).scale(amount));
        return blended.lengthSqr() > 1.0E-6 ? blended.normalize() : from;
    }

    private static Vec3 anyPerpendicular(Vec3 axis) {
        Vec3 candidate = Math.abs(axis.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 perpendicular = axis.cross(candidate);
        return perpendicular.lengthSqr() > 1.0E-6 ? perpendicular.normalize() : new Vec3(1, 0, 0);
    }

    /** a unit vector at most maxRadians away from in a random direction */
    private Vec3 tiltedNormal(Vec3 from, double maxRadians) {
        Vec3 random = randomUnitVector();
        Vec3 perpendicular = random.subtract(from.scale(random.dot(from)));
        if (perpendicular.lengthSqr() < 1.0E-6) perpendicular = anyPerpendicular(from);
        else perpendicular = perpendicular.normalize();

        // floored off zero
        double angle = maxRadians * (0.4 + 0.6 * apophis.getRandom().nextDouble());
        return from.scale(Math.cos(angle)).add(perpendicular.scale(Math.sin(angle))).normalize();
    }

    private Vec3 randomUnitVector() {
        double y = apophis.getRandom().nextDouble() * 2.0 - 1.0;
        double yaw = apophis.getRandom().nextDouble() * Math.PI * 2.0;
        double ring = Math.sqrt(Math.max(1.0 - y * y, 0.0));
        return new Vec3(Math.cos(yaw) * ring, y, Math.sin(yaw) * ring);
    }

    private Vec3 randomHorizontal() {
        double yaw = apophis.getRandom().nextDouble() * Math.PI * 2.0;
        return new Vec3(Math.cos(yaw), 0.0, Math.sin(yaw));
    }

    private void swimToward(Vec3 goal, double speed, double weaveScale, double smoothing) {
        Vec3 toGoal = goal.subtract(apophis.headPosition());
        steer(toGoal.lengthSqr() > 1.0E-4 ? toGoal.normalize() : apophis.headDirection(),
                speed, weaveScale, smoothing);
    }

    // one tick of serpentine swimming along direction
    private void steer(Vec3 direction, double speed, double weaveScale, double smoothing) {
        Vec3 forward = direction.lengthSqr() > 1.0E-4 ? direction.normalize() : new Vec3(1, 0, 0);

        // orthonormal frame around the direction of travel. world up is the default, but doesn't work
        // when swimming nearnvertically so fall back to a horizontal axis there, where any perpendicular is good
        Vec3 reference = Math.abs(forward.y) > 0.98 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 side = forward.cross(reference).normalize();
        Vec3 up = side.cross(forward).normalize();

        double amplitude = SWIM_AMPLITUDE * weaveScale;
        double phase = swimTicks * (2.0 * Math.PI / SWIM_PERIOD_TICKS);
        Vec3 weave = side.scale(Math.sin(phase) * amplitude)
                .add(up.scale(Math.cos(phase) * amplitude * VERTICAL_WEAVE));

        Vec3 desired = forward.scale(speed).add(weave);
        Vec3 current = apophis.getChaseVelocity();
        apophis.setChaseVelocity(current.add(desired.subtract(current).scale(smoothing)));
    }
}
