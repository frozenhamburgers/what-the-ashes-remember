package net.jelly.echoesofwar.entity.trinity;


public enum TrinityPhase {
    SPAWNING, // spawn countdown, fully formed, visible, critical, transitions to opening detonation
    DETONATING, // detonating, dont render body
    REFORMING, // reform after detonation
    FIGHTING, // normal attack pattern
    MELTDOWN, // critical, phase switch
    DYING, // final detonation, let animation play fully out.
    DESPAWNING;

    private static final TrinityPhase[] VALUES = values();

    public static TrinityPhase byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : SPAWNING;
    }

    public boolean isDetonating() {
        return this == DETONATING || this == DYING;
    }

    public boolean isFighting() {
        return this == FIGHTING;
    }
}
