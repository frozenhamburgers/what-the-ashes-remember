package net.jelly.echoesofwar.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class GradientNameBlockItem extends BlockItem {
    private final String displayName;
    private final int colorA;
    private final int colorB;
    private long gameTime;

    public GradientNameBlockItem(Block block, Properties properties, String displayName, int colorA, int colorB) {
        super(block, properties);
        this.displayName = displayName;
        this.colorA = colorA;
        this.colorB = colorB;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        this.gameTime = level.getGameTime();
    }

    @Override
    public Component getName(ItemStack stack) {
        return GradientName.build(displayName, colorA, colorB, gameTime);
    }
}
