package net.jelly.echoesofwar.entity.trinity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class TrinityRewards {
    private TrinityRewards() {}

    public static void onTrinityDefeated(ServerLevel level, Vec3 centre, Vec3 detonationBase) {
        level.playSound(null, BlockPos.containing(centre), SoundEvents.END_PORTAL_SPAWN,
                SoundSource.HOSTILE, 6.0F, 0.7F);

        // TODO: Trinity's drop table
    }

    private static void dropAt(ServerLevel level, Vec3 at, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemEntity item = new ItemEntity(level, at.x, at.y, at.z, stack);
        item.setDeltaMovement(Vec3.ZERO);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
    }
}
