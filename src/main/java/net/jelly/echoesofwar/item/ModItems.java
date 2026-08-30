package net.jelly.echoesofwar.item;

import net.jelly.echoesofwar.EchoesofWar;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

public class ModItems {
    public static final DeferredItem<Item> KEY_OF_CONQUEST = EchoesofWar.ITEMS.registerSimpleItem(
            "key_of_conquest", p -> p.stacksTo(1));
    public static final DeferredItem<Item> KEY_OF_INDUSTRY = EchoesofWar.ITEMS.registerSimpleItem(
            "key_of_industry", p -> p.stacksTo(1));

    public static final DeferredItem<Item> MISERY_OF_CONQUEST = EchoesofWar.ITEMS.registerItem("misery_of_conquest",
            p -> new GradientNameItem(p, "Misery of Conquest", 0xFFD700, 0xFFFFFF));
    public static final DeferredItem<Item> HOPE_OF_CREATION = EchoesofWar.ITEMS.registerSimpleItem("hope_of_creation");
    public static final DeferredItem<Item> MISERY_OF_INDUSTRY = EchoesofWar.ITEMS.registerItem("misery_of_industry",
            p -> new GradientNameItem(p, "Misery of Industry", 0xE10600, 0x000000));
    public static final DeferredItem<Item> HOPE_OF_PROGRESS = EchoesofWar.ITEMS.registerSimpleItem("hope_of_progress");
    public static final DeferredItem<Item> MISERY_OF_MAN = EchoesofWar.ITEMS.registerItem("misery_of_man",
            p -> new GradientNameItem(p, "Misery of Man", 0xF0E5A0, 0x8FB6A0));

    public static void init() {
    }
}
