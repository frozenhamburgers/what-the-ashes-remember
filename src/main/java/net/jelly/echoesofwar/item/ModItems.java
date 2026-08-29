package net.jelly.echoesofwar.item;

import net.jelly.echoesofwar.EchoesofWar;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

public class ModItems {
    public static final DeferredItem<Item> KEY_OF_CONQUEST = EchoesofWar.ITEMS.registerSimpleItem(
            "key_of_conquest", p -> p.stacksTo(1));
    public static final DeferredItem<Item> KEY_OF_INDUSTRY = EchoesofWar.ITEMS.registerSimpleItem(
            "key_of_industry", p -> p.stacksTo(1));

    public static final DeferredItem<Item> MISERY_OF_CONQUEST = EchoesofWar.ITEMS.registerSimpleItem("misery_of_conquest");
    public static final DeferredItem<Item> HOPE_OF_CREATION = EchoesofWar.ITEMS.registerSimpleItem("hope_of_creation");
    public static final DeferredItem<Item> MISERY_OF_INDUSTRY = EchoesofWar.ITEMS.registerSimpleItem("misery_of_industry");
    public static final DeferredItem<Item> HOPE_OF_PROGRESS = EchoesofWar.ITEMS.registerSimpleItem("hope_of_progress");
    public static final DeferredItem<Item> MISERY_OF_MAN = EchoesofWar.ITEMS.registerSimpleItem("misery_of_man");

    public static void init() {
    }
}
