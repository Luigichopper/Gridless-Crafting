package com.gridless.api.recipe;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.*;

public enum RecipeCategory {
    ALL("category.gridless.all"),
    WEAPONS("category.gridless.weapons"),
    TOOLS("category.gridless.tools"),
    ARMOR("category.gridless.armor"),
    BUILDING("category.gridless.building"),
    REDSTONE("category.gridless.redstone"),
    CONSUMABLES("category.gridless.consumables"),
    MISC("category.gridless.misc");

    private final String translationKey;

    RecipeCategory(String translationKey) {
        this.translationKey = translationKey;
    }

    public Component getDisplayName() {
        return Component.translatable(translationKey);
    }

    public static RecipeCategory classify(ItemStack stack) {
        if (stack.isEmpty()) return MISC;
        Item item = stack.getItem();

        if (stack.is(ItemTags.SWORDS) || stack.is(ItemTags.WEAPON_ENCHANTABLE) || item instanceof ProjectileWeaponItem || item instanceof TridentItem || item instanceof MaceItem) {
            return WEAPONS;
        }
        if (stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.AXES) || stack.is(ItemTags.SHOVELS) || stack.is(ItemTags.HOES) || item instanceof FishingRodItem || item instanceof ShearsItem || item instanceof FlintAndSteelItem) {
            return TOOLS;
        }
        if (stack.is(ItemTags.ARMOR_ENCHANTABLE) || stack.is(ItemTags.HEAD_ARMOR) || stack.is(ItemTags.CHEST_ARMOR) || stack.is(ItemTags.LEG_ARMOR) || stack.is(ItemTags.FOOT_ARMOR) || item instanceof ShieldItem) {
            return ARMOR;
        }
        if (stack.has(DataComponents.FOOD) || stack.has(DataComponents.POTION_CONTENTS)) {
            return CONSUMABLES;
        }
        if (item instanceof BlockItem) {
            // Check redstone vs building
            String path = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getPath();
            if (path.contains("redstone") || path.contains("piston") || path.contains("repeater") ||
                    path.contains("comparator") || path.contains("observer") || path.contains("dispenser") ||
                    path.contains("dropper") || path.contains("hopper") || path.contains("lever") ||
                    path.contains("button") || path.contains("pressure_plate") || path.contains("wire") ||
                    path.contains("target") || path.contains("tripwire") || path.contains("daylight")) {
                return REDSTONE;
            }
            return BUILDING;
        }

        String path = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getPath();
        if (path.contains("redstone") || path.contains("rail")) {
            return REDSTONE;
        }

        return MISC;
    }
}
