package net.jelly.echoesofwar.entity.apophis;

import net.jelly.marionette_lib.utility.MarionettePart;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;

// one body segment of ApophisEntity
public class ApophisPartEntity extends MarionettePart<ApophisEntity> {
    private final boolean head;
    private final double damageTakenMultiplier;

    public ApophisPartEntity(ApophisEntity parent, float sizeXZ, float sizeY, float length, boolean head, double damageTakenMultiplier) {
        super(parent, sizeXZ, sizeY, length);
        this.head = head;
        this.damageTakenMultiplier = damageTakenMultiplier;
    }

    public boolean isHead() {
        return head;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return super.hurtServer(level, source, (float) (amount * damageTakenMultiplier));
    }
}
