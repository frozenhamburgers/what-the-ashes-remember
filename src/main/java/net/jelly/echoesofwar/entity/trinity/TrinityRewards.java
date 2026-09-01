package net.jelly.echoesofwar.entity.trinity;

import net.jelly.echoesofwar.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public final class TrinityRewards {
    private static final double DROP_RADIUS = 10.0;

    private TrinityRewards() {}

    public static void onTrinityDefeated(ServerLevel level, Vec3 centre, Vec3 detonationBase) {
        level.playSound(null, BlockPos.containing(centre), SoundEvents.END_PORTAL_SPAWN,
                SoundSource.HOSTILE, 6.0F, 0.7F);

        dropAt(level, randomSurfacePos(level, centre, detonationBase),
                new ItemStack(ModBlocks.DESTROYER_OF_WORLDS_ITEM.get()));
    }

    private static Vec3 randomSurfacePos(ServerLevel level, Vec3 centre, Vec3 detonationBase) {
        RandomSource random = level.getRandom();
        double angle = random.nextDouble() * Math.PI * 2.0;
        double distance = Math.sqrt(random.nextDouble()) * DROP_RADIUS;
        double x = centre.x + Math.cos(angle) * distance;
        double z = centre.z + Math.sin(angle) * distance;

        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mth.floor(x), Mth.floor(z));
        double y = Math.max(surface, detonationBase.y) + 1.0;
        return new Vec3(x, y, z);
    }

    private static void dropAt(ServerLevel level, Vec3 at, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemEntity item = new ItemEntity(level, at.x, at.y, at.z, stack);
        item.setDeltaMovement(Vec3.ZERO);
        item.setDefaultPickUpDelay();
        item.setGlowingTag(true);
        level.addFreshEntity(item);
    }
}
