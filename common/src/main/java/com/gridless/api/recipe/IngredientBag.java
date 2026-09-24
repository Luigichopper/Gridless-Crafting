package com.gridless.api.recipe;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IngredientBag {
    private final Map<Item, Integer> itemCounts = new HashMap<>();
    private final List<ItemStack> inventoryStacks = new ArrayList<>();

    public IngredientBag() {}

    public static IngredientBag fromPlayer(Player player) {
        IngredientBag bag = new IngredientBag();
        if (player == null) return bag;
        Inventory inv = player.getInventory();
        for (ItemStack stack : inv.items) {
            if (!stack.isEmpty()) {
                bag.addStack(stack);
            }
        }
        for (ItemStack stack : inv.offhand) {
            if (!stack.isEmpty()) {
                bag.addStack(stack);
            }
        }
        return bag;
    }

    public void addStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        inventoryStacks.add(stack.copy());
        itemCounts.merge(stack.getItem(), stack.getCount(), Integer::sum);
    }

    public void add(Item item, int count) {
        if (item == null || count <= 0) return;
        itemCounts.merge(item, count, Integer::sum);
        inventoryStacks.add(new ItemStack(item, count));
    }

    public int getCount(Item item) {
        return itemCounts.getOrDefault(item, 0);
    }

    public int countMatching(Ingredient ingredient) {
        if (ingredient == null) return 0;
        try {
            if (ingredient.isEmpty()) return 0;
        } catch (Throwable ignored) {}

        int total = 0;
        for (ItemStack stack : inventoryStacks) {
            if (!stack.isEmpty() && isMatch(ingredient, stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public static boolean isMatch(Ingredient ingredient, ItemStack stack) {
        if (ingredient == null || stack == null || stack.isEmpty()) return false;
        try {
            if (ingredient.test(stack)) return true;
        } catch (Throwable ignored) {}
        try {
            return ingredient.items().anyMatch(h -> h.value() == stack.getItem());
        } catch (Throwable ignored) {}
        return false;
    }

    public boolean has(CountedIngredient counted) {
        return countMatching(counted.getIngredient()) >= counted.getCount();
    }

    public IngredientBag copy() {
        IngredientBag copy = new IngredientBag();
        copy.itemCounts.putAll(this.itemCounts);
        for (ItemStack stack : this.inventoryStacks) {
            copy.inventoryStacks.add(stack.copy());
        }
        return copy;
    }

    public Map<Item, Integer> getItemCounts() {
        return itemCounts;
    }

    public List<ItemStack> getInventoryStacks() {
        return inventoryStacks;
    }
}

