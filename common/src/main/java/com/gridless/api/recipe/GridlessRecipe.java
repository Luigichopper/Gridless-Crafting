package com.gridless.api.recipe;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.*;

public class GridlessRecipe {
    public enum CraftState {
        CRAFTABLE,
        MISSING_INGREDIENTS,
        LOCKED
    }

    private final Identifier id;
    private final ItemStack result;
    private final List<CountedIngredient> inputs;
    private final RecipeType<?> recipeType;
    private final RecipeCategory category;
    private final int cookTime;
    private final float experience;
    private final boolean requires3x3;

    private final List<GridlessRecipe> variants = new ArrayList<>();
    private int activeVariantIndex = 0;

    public GridlessRecipe(Identifier id,
                          ItemStack result,
                          List<CountedIngredient> inputs,
                          RecipeType<?> recipeType,
                          RecipeCategory category,
                          int cookTime,
                          float experience,
                          boolean requires3x3) {
        this.id = id;
        this.result = result;
        this.inputs = inputs != null ? inputs : Collections.emptyList();
        this.recipeType = recipeType;
        this.category = category != null ? category : RecipeCategory.classify(result);
        this.cookTime = cookTime;
        this.experience = experience;
        this.requires3x3 = requires3x3;
        this.variants.add(this);
    }

    public void addVariant(GridlessRecipe variant) {
        if (variant != null && !variants.contains(variant)) {
            variants.add(variant);
        }
    }

    public List<GridlessRecipe> getVariants() {
        return Collections.unmodifiableList(variants);
    }

    public int getVariantCount() {
        return variants.size();
    }

    public int getActiveVariantIndex() {
        return activeVariantIndex;
    }

    public void setActiveVariantIndex(int index) {
        if (!variants.isEmpty()) {
            this.activeVariantIndex = Math.floorMod(index, variants.size());
        }
    }

    public void cycleVariant(int direction) {
        if (variants.size() > 1) {
            setActiveVariantIndex(this.activeVariantIndex + direction);
        }
    }

    public GridlessRecipe getActiveVariant() {
        if (variants.isEmpty()) return this;
        return variants.get(Math.floorMod(activeVariantIndex, variants.size()));
    }

    public void autoSelectBestVariant(IngredientBag bag) {
        if (variants.size() <= 1) return;
        // If current variant is craftable, keep it
        if (getActiveVariant().isCraftableSingle(bag)) return;
        // Otherwise, find the first variant that is craftable
        for (int i = 0; i < variants.size(); i++) {
            if (variants.get(i).isCraftableSingle(bag)) {
                this.activeVariantIndex = i;
                return;
            }
        }
    }

    public Identifier getId() {
        return getActiveVariant().id;
    }

    public ItemStack getResult() {
        return result.copy();
    }

    public List<CountedIngredient> getInputs() {
        return getActiveVariant().inputs;
    }

    public RecipeType<?> getRecipeType() {
        return getActiveVariant().recipeType;
    }

    public RecipeCategory getCategory() {
        return category;
    }

    public int getCookTime() {
        return getActiveVariant().cookTime;
    }

    public float getExperience() {
        return getActiveVariant().experience;
    }

    public boolean requires3x3() {
        if (variants.size() > 1) {
            for (GridlessRecipe v : variants) {
                if (!v.requires3x3) return false;
            }
            return true;
        }
        return requires3x3;
    }

    public void remove3x3Variants() {
        if (variants.size() > 1) {
            variants.removeIf(v -> v.requires3x3);
            if (variants.isEmpty()) {
                variants.add(this);
            }
            this.activeVariantIndex = 0;
        }
    }

    private boolean isCraftableSingle(IngredientBag bag) {
        for (CountedIngredient counted : inputs) {
            if (!bag.has(counted)) {
                return false;
            }
        }
        return true;
    }

    public boolean isCraftable(IngredientBag bag) {
        if (variants.size() > 1) {
            for (GridlessRecipe v : variants) {
                if (v.isCraftableSingle(bag)) {
                    return true;
                }
            }
            return false;
        }
        return isCraftableSingle(bag);
    }

    public int maxCraftable(IngredientBag bag) {
        GridlessRecipe active = getActiveVariant();
        if (active.inputs.isEmpty()) return 0;
        int max = Integer.MAX_VALUE;
        for (CountedIngredient counted : active.inputs) {
            int available = bag.countMatching(counted.getIngredient());
            int canCraft = available / counted.getCount();
            if (canCraft < max) {
                max = canCraft;
            }
        }
        return max == Integer.MAX_VALUE ? 0 : max;
    }

    public Map<CountedIngredient, Integer> getMissingIngredients(IngredientBag bag) {
        Map<CountedIngredient, Integer> missing = new LinkedHashMap<>();
        for (CountedIngredient counted : getInputs()) {
            int available = bag.countMatching(counted.getIngredient());
            int needed = counted.getCount();
            if (available < needed) {
                missing.put(counted, needed - available);
            }
        }
        return missing;
    }

    public CraftState getState(IngredientBag bag) {
        if (isCraftable(bag)) {
            return CraftState.CRAFTABLE;
        }
        return CraftState.MISSING_INGREDIENTS;
    }
}
