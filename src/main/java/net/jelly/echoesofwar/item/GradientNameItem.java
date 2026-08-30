package net.jelly.echoesofwar.item;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class GradientNameItem extends Item {
    private static final float TICKS_PER_CYCLE = 60.0F;

    private final String displayName;
    private final int colorA;
    private final int colorB;
    private long gameTime;

    public GradientNameItem(Properties properties, String displayName, int colorA, int colorB) {
        super(properties);
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
        int len = displayName.length();
        MutableComponent result = Component.empty();
        for (int i = 0; i < len; i++) {
            float phase = (float) i / Math.max(1, len - 1) + (float) gameTime / TICKS_PER_CYCLE;
            phase -= (float) Math.floor(phase);
            float blend = phase < 0.5F ? phase * 2.0F : (1.0F - phase) * 2.0F;
            int color = lerpColor(colorA, colorB, blend);
            result.append(Component.literal(String.valueOf(displayName.charAt(i)))
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color))));
        }
        return result;
    }

    private static int lerpColor(int c1, int c2, float t) {
        int r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
        int r = Math.round(r1 + (r2 - r1) * t);
        int g = Math.round(g1 + (g2 - g1) * t);
        int b = Math.round(b1 + (b2 - b1) * t);
        return (r << 16) | (g << 8) | b;
    }
}
