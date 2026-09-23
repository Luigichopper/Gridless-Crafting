package com.gridless.api.recipe;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.HashMap;
import java.util.Map;

public class IngredientBag {
    private final Map<Item, Integer> itemCounts = new HashMap<>();

    public IngredientBag() {}

    public static IngredientBag fromPlayer(Player player) {
        IngredientBag bag = new IngredientBag();
        Inventory inv = player.getInventory();
        // Check main inventory (slots 0 to 35) + offhand (slot 40)
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                bag.add(stack.getItem(), stack.getCount());
            }
        }
        ItemStack offhand = inv.getItem(40);
        if (!offhand.isEmpty()) {
            bag.add(offhand.getItem(), offhand.getCount());
        }
        return bag;
    }

    public void add(Item item, int count) {
        itemCounts.merge(item, count, Integer::sum);
    }

    public int getCount(Item item) {
        return itemCounts.getOrDefault(item, 0);
    }

    public int countMatching(Ingredient ingredient) {
        if (ingredient.isEmpty()) return 0;
        int total = 0;
        for (Map.Entry<Item, Integer> entry : itemCounts.entrySet()) {
            ItemStack sampleStack = new ItemStack(entry.getKey());
            if (ingredient.test(sampleStack)) {
                total += entry.getValue();
            }
        }
        return total;
    }

    public boolean has(CountedIngredient counted) {
        return countMatching(counted.getIngredient()) >= counted.getCount();
    }

    public IngredientBag copy() {
        IngredientBag copy = new IngredientBag();
        copy.itemCounts.putAll(this.itemCounts);
        return copy;
    }

    public Map<Item, Integer> getItemCounts() {
        return itemCounts;
    }
}
