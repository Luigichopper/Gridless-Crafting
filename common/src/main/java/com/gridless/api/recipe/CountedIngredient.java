package com.gridless.api.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public class CountedIngredient {
    private final Ingredient ingredient;
    private final int count;

    public CountedIngredient(Ingredient ingredient, int count) {
        this.ingredient = ingredient;
        this.count = Math.max(1, count);
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public int getCount() {
        return count;
    }

    public boolean test(ItemStack stack) {
        return IngredientBag.isMatch(ingredient, stack);
    }

    public ItemStack[] getMatchingStacks() {
        try {
            return ingredient.items()
                    .map(net.minecraft.core.Holder::value)
                    .map(ItemStack::new)
                    .toArray(ItemStack[]::new);
        } catch (Throwable t) {
            return new ItemStack[0];
        }
    }
}

