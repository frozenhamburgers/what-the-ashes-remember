package net.jelly.echoesofwar.entity.trinity.attack;

import net.jelly.echoesofwar.entity.trinity.TrinityTuning;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;

/**
 * Server side only hit detection for Trinity's drawn geom
 * Reads the same AttackSlot data array the shader marches
 *
 */
public final class AttackHitDetection {
    private AttackHitDetection() {}

    // radius exponents
    private static final double CONE_POW = 1.55;
    private static final double CYL_TAPER = 0.18;


    public static void apply(ServerLevel level, Vec3 centre, float bodyRadius,
                             AttackSlot[] slots, BulletField field,
                             Map<UUID, Integer> cooldowns, int now) {
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isCreative() || !player.isAlive()) continue;

            Integer last = cooldowns.get(player.getUUID());
            if (last != null && now - last < TrinityTuning.HIT_COOLDOWN_TICKS) continue;

            // midpoint instead of feet
            Vec3 at = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);

            // projectiles
            Vec3 push = bulletHit(field, centre, player);
            if (push != null) {
                strike(level, player, push, TrinityTuning.BULLET_DAMAGE, cooldowns, now);
                continue;
            }

            for (AttackSlot slot : slots) {
                if (!slot.isActive()) continue;
                // don't apply damageo n telegraphs
                if (slot.extend <= 0.001f || slot.fade >= 1f) continue;

                Vec3 hitAxis = hitAxis(centre, bodyRadius, slot, at);
                if (hitAxis == null) continue;

                boolean cone = slot.type == AttackSlot.TYPE_PREDICTIVE;
                strike(level, player, hitAxis,
                        cone ? TrinityTuning.CONE_DAMAGE : TrinityTuning.BEAM_DAMAGE,
                        cooldowns, now);
                break; // one hit per tick tops
            }
        }
    }

    // heights up the payer, as fractions of their hitbox, that the projectile field is tested against
    private static final double[] BODY_SAMPLES = {0.15, 0.5, 0.9};

    private static Vec3 bulletHit(BulletField field, Vec3 centre, ServerPlayer player) {
        double height = player.getBbHeight();
        for (double f : BODY_SAMPLES) {
            Vec3 at = player.position().add(0.0, height * f, 0.0);
            Vec3 push = BulletFieldMath.hit(field, centre, at,
                    TrinityTuning.BULLET_HIT_RADIUS_FRAC);
            if (push != null) return push;
        }
        return null;
    }

    private static void strike(ServerLevel level, ServerPlayer player, Vec3 push, float damage,
                               Map<UUID, Integer> cooldowns, int now) {
        if (!player.hurtServer(level, level.damageSources().explosion(null, null), damage)) return;

        player.push(push.x * TrinityTuning.HIT_KNOCKBACK,
                push.y * TrinityTuning.HIT_KNOCKBACK,
                push.z * TrinityTuning.HIT_KNOCKBACK);
        player.hurtMarked = true;
        cooldowns.put(player.getUUID(), now);
    }

    // if inside volume, return unit vector pointing away from its axis to push
    private static Vec3 hitAxis(Vec3 centre, float bodyRadius, AttackSlot slot, Vec3 at) {
        Vec3 root = centre.add(slot.dir.scale(bodyRadius));
        double reach = slot.length * slot.extend;
        if (reach <= 0.01) return null;

        Vec3 v = at.subtract(root);
        double along = v.dot(slot.dir);
        if (along < 0.0 || along > reach) return null;

        Vec3 perp = v.subtract(slot.dir.scale(along));
        double off = perp.length();

        double u = along / reach;
        boolean cone = slot.type == AttackSlot.TYPE_PREDICTIVE;
        double prof = cone ? Math.pow(1.0 - u, CONE_POW) : (1.0 - CYL_TAPER * u);
        double radius = slot.radius * prof * TrinityTuning.HIT_RADIUS_FRAC;
        if (off > radius) return null;

        // perpendicular fallback
        if (off < 1.0e-4) {
            Vec3 pick = Math.abs(slot.dir.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
            return slot.dir.cross(pick).normalize();
        }
        return perp.scale(1.0 / off);
    }
}
