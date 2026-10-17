package dev.cursedarts;

import dev.cursedarts.item.BlackFlashKnucklesItem;
import dev.cursedarts.item.CursedTalismanItem;
import dev.cursedarts.item.Technique;
import dev.cursedarts.item.TechniqueItem;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Rarity;

public final class ModItems {
    public static final Item RED = technique("red", Technique.RED, Rarity.RARE);
    public static final Item BLUE = technique("blue", Technique.BLUE, Rarity.RARE);
    public static final Item HOLLOW_PURPLE = technique("hollow_purple", Technique.HOLLOW_PURPLE, Rarity.EPIC);
    public static final Item DISMANTLE = technique("dismantle", Technique.DISMANTLE, Rarity.RARE);
    public static final Item REVERSE_CURSED_TECHNIQUE = technique("reverse_cursed_technique", Technique.REVERSE_CURSED_TECHNIQUE, Rarity.UNCOMMON);
    public static final Item DOMAIN_EXPANSION = technique("domain_expansion", Technique.DOMAIN_EXPANSION, Rarity.EPIC);

    public static final Item BLACK_FLASH_KNUCKLES = register("black_flash_knuckles",
            new BlackFlashKnucklesItem(ToolMaterials.NETHERITE, new Item.Settings()
                    .rarity(Rarity.EPIC)
                    .fireproof()
                    .attributeModifiers(SwordItem.createAttributeModifiers(ToolMaterials.NETHERITE, 2, -1.6f))));

    public static final Item CURSED_TALISMAN = register("cursed_talisman",
            new CursedTalismanItem(new Item.Settings().maxCount(16).rarity(Rarity.UNCOMMON)));

    public static final ItemGroup GROUP = Registry.register(Registries.ITEM_GROUP, CursedArts.id("main"),
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(HOLLOW_PURPLE))
                    .displayName(Text.translatable("itemGroup.cursedarts"))
                    .entries((ctx, entries) -> {
                        entries.add(RED);
                        entries.add(BLUE);
                        entries.add(HOLLOW_PURPLE);
                        entries.add(DISMANTLE);
                        entries.add(REVERSE_CURSED_TECHNIQUE);
                        entries.add(DOMAIN_EXPANSION);
                        entries.add(BLACK_FLASH_KNUCKLES);
                        entries.add(CURSED_TALISMAN);
                    })
                    .build());

    private ModItems() {}

    public static void init() {}

    private static Item technique(String name, Technique t, Rarity rarity) {
        return register(name, new TechniqueItem(new Item.Settings().maxCount(1).rarity(rarity), t));
    }

    private static Item register(String name, Item item) {
        return Registry.register(Registries.ITEM, CursedArts.id(name), item);
    }
}
