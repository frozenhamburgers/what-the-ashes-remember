package net.jelly.echoesofwar.entity.trinity;

import net.jelly.echoesofwar.entity.trinity.attack.*;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * Trinity's attack pool
 * PERSISTENT attacks are not selected or ended, they just start and run continuously
 * other events are selected, and are also sorted as families such that two in the same family
 * cannot be chosen at the same time.
 * kinda not too relevant other than the containment beams after normal spikes were removed
 * but kept so adding attacks later is easier
 * FACTORIES is synced
 */
public final class TrinityAttacks {
    private TrinityAttacks() {}

    private static final List<Supplier<TrinityAttack>> FACTORIES = List.of(
            PredictiveSpikeAttack::new,                // 0 - cone, leads the player [persistent]
            SmokeLaserAttack::new,                     // 1 - contaiment cylinders
            () -> new ContainmentBeamAttack(false),    // 2 - lattice, one direction
            () -> new ContainmentBeamAttack(true),     // 3 - lattice, counter-rotating bands
            WanderingLaserAttack::new,                 // 4 - cylinder, random drift [persistent]
            BulletPatternAttack::new                   // 5 - 3D projectile field [persistent]
    );

    public static final int SPIKES = 0;
    public static final int LASER = 1;
    public static final int CONTAINMENT = 2;
    public static final int CONTAINMENT_ALT = 3;
    public static final int WANDERING = 4;
    public static final int BULLETS = 5;

    public static final int[] PERSISTENT = { SPIKES, WANDERING, BULLETS };

    public static final int FAMILY_SPIKES = 0;
    public static final int FAMILY_LASER = 1;
    public static final int FAMILY_CONTAINMENT = 2;
    public static final int FAMILY_WANDERING = 3;
    public static final int FAMILY_BULLETS = 4;

    // familes per index, parallel to FACTORIES
    private static final int[] FAMILIES = {
            FAMILY_SPIKES,       // 0 predictive spikes
            FAMILY_LASER,        // 1 smoke laser
            FAMILY_CONTAINMENT,  // 2 containment
            FAMILY_CONTAINMENT,  // 3 alternating containment
            FAMILY_WANDERING,    // 4 wandering lasers
            FAMILY_BULLETS       // 5 projectile field
    };

    public static int count() {
        return FACTORIES.size();
    }

    public static int family(int index) {
        return index < 0 || index >= FAMILIES.length ? -1 : FAMILIES[index];
    }

    public static boolean isScheduled(int index) {
        for (int persistent : PERSISTENT) {
            if (persistent == index) return false;
        }
        return index >= 0 && index < FACTORIES.size();
    }

    public static @Nullable TrinityAttack byIndex(int index) {
        if (index < 0 || index >= FACTORIES.size()) return null;
        return FACTORIES.get(index).get();
    }

    // pick schedulable attack whose index passes allowed, -1 if nothing does
    public static int pick(RandomSource random, java.util.function.IntPredicate allowed) {
        int n = FACTORIES.size();
        int legal = 0;
        for (int i = 0; i < n; i++) if (isScheduled(i) && allowed.test(i)) legal++;
        if (legal == 0) return -1;

        int wanted = random.nextInt(legal);
        for (int i = 0; i < n; i++) {
            if (!isScheduled(i) || !allowed.test(i)) continue;
            if (wanted-- == 0) return i;
        }
        return -1;
    }
}
