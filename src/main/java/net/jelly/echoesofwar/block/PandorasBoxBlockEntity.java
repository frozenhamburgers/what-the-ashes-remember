package net.jelly.echoesofwar.block;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.List;

public class PandorasBoxBlockEntity extends BlockEntity implements GeoBlockEntity {
    public static final String OPEN_ANIMATION = "open";
    public static final String CLOSE_ANIMATION = "close";

    // key open sequence timing (open animation is 55 ticks)
    private static final int PARTICLE_TRIGGER_TICK = 30;
    private static final int PARTICLE_BURST_DURATION_TICKS = 30;
    private static final int EJECT_DELAY_TICKS = 30; // ticks to eject items after particle begins

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);

    // guard closing before spawn
    private boolean bossSeen = false;
    private int keyOpeningTicks = -1;

    public PandorasBoxBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected Class<? extends LivingEntity> bossClass() {
        throw new UnsupportedOperationException("bossClass() not implemented for " + getClass());
    }

    protected double bossSearchRadius() {
        return 96.0;
    }

    protected Item keyItem() {
        throw new UnsupportedOperationException("keyItem() not implemented for " + getClass());
    }

    protected List<ItemStack> rewardItems() {
        throw new UnsupportedOperationException("rewardItems() not implemented for " + getClass());
    }

    public boolean isRunningKeyOpeningSequence() {
        return keyOpeningTicks >= 0;
    }

    void onBossSummoned() {
        bossSeen = false;
        setChanged();
    }

    void beginKeyOpeningSequence() {
        keyOpeningTicks = 0;
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PandorasBoxBlockEntity box) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        if (box.keyOpeningTicks >= 0) {
            box.tickKeyOpeningSequence(serverLevel, pos);
            return;
        }

        if (!state.getValue(PandorasBoxBlock.OPEN)) return;

        boolean bossPresent = !serverLevel.getEntitiesOfClass(box.bossClass(), new AABB(pos).inflate(box.bossSearchRadius()),
                LivingEntity::isAlive).isEmpty();

        if (bossPresent) {
            box.bossSeen = true;
            box.setChanged();
        } else if (box.bossSeen) {
            serverLevel.setBlock(pos, state.setValue(PandorasBoxBlock.OPEN, false), Block.UPDATE_ALL);
            box.bossSeen = false;
            box.setChanged();
        }
    }

    private void tickKeyOpeningSequence(ServerLevel level, BlockPos pos) {
        keyOpeningTicks++;

        int sinceParticlesBegan = keyOpeningTicks - PARTICLE_TRIGGER_TICK;

        if (sinceParticlesBegan == 0) {
            playOpeningSounds(level, pos);
        }
        if (sinceParticlesBegan >= 0 && sinceParticlesBegan < PARTICLE_BURST_DURATION_TICKS) {
            spawnPortalBurstParticles(level, pos);
        }
        if (sinceParticlesBegan == EJECT_DELAY_TICKS) {
            ejectRewards(level, pos);
            level.removeBlock(pos, false);
        }

        setChanged();
    }

    private void playOpeningSounds(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.WEATHER_END_FLASH, SoundSource.BLOCKS, 4.0F, 1.0F);
        level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 4.0F, 0.6F);
    }

    private void spawnPortalBurstParticles(ServerLevel level, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        RandomSource random = level.getRandom();

        for (int i = 0; i < 6; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double speed = 0.6 + random.nextDouble() * 0.8;
            double vx = Math.cos(angle) * speed;
            double vz = Math.sin(angle) * speed;
            double vy = 0.4 + random.nextDouble() * 0.6;

            level.sendParticles(ParticleTypes.PORTAL, center.x, center.y + 0.3, center.z, 0, vx, vy, vz, 1.0);
        }
    }

    private void ejectRewards(ServerLevel level, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        RandomSource random = level.getRandom();

        for (ItemStack stack : rewardItems()) {
            if (stack.isEmpty()) continue;

            ItemEntity itemEntity = new ItemEntity(level, center.x, center.y + 0.5, center.z, stack);
            double hx = (random.nextDouble() - 0.5) * 0.4;
            double hz = (random.nextDouble() - 0.5) * 0.4;
            itemEntity.setDeltaMovement(hx, 0.2 + random.nextDouble() * 0.05, hz);
            itemEntity.setNoGravity(true);
            itemEntity.setGlowingTag(true);
            level.addFreshEntity(itemEntity);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("open_state", 0, test -> {
            boolean open = getBlockState().hasProperty(PandorasBoxBlock.OPEN) && getBlockState().getValue(PandorasBoxBlock.OPEN);

            return test.setAndContinue(RawAnimation.begin().thenPlayAndHold(open ? OPEN_ANIMATION : CLOSE_ANIMATION));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableCache;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("boss_seen", bossSeen);
        output.putInt("key_opening_ticks", keyOpeningTicks);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        bossSeen = input.getBooleanOr("boss_seen", false);
        keyOpeningTicks = input.getIntOr("key_opening_ticks", -1);
    }
}
