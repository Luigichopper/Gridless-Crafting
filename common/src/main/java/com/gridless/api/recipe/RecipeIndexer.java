package com.gridless.api.recipe;

import com.gridless.GridlessMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RecipeIndexer {
    private static final Map<ResourceLocation, GridlessRecipe> INDEXED_RECIPES = new ConcurrentHashMap<>();
    private static final Map<RecipeType<?>, List<GridlessRecipe>> BY_TYPE = new ConcurrentHashMap<>();

    public static void reindex(RecipeAccess recipeAccess, HolderLookup.Provider registries) {
        INDEXED_RECIPES.clear();
        BY_TYPE.clear();

        Collection<RecipeHolder<?>> holders;
        if (recipeAccess instanceof RecipeMap map) {
            holders = map.values();
        } else {
            holders = Collections.emptyList();
        }
        GridlessMod.LOGGER.info("Indexing {} vanilla recipes for Gridless Crafting...", holders.size());

        for (RecipeHolder<?> holder : holders) {
            try {
                GridlessRecipe indexed = indexRecipe(holder, registries);
                if (indexed != null && !indexed.getResult().isEmpty()) {
                    INDEXED_RECIPES.put(indexed.getId(), indexed);
                    BY_TYPE.computeIfAbsent(indexed.getRecipeType(), k -> new ArrayList<>()).add(indexed);
                }
            } catch (Exception e) {
                GridlessMod.LOGGER.debug("Skipping unindexable recipe: {}", holder.id(), e);
            }
        }

        GridlessMod.LOGGER.info("Indexed {} gridless recipes across {} types.", INDEXED_RECIPES.size(), BY_TYPE.size());
    }

    public static GridlessRecipe indexSingle(RecipeHolder<?> holder, HolderLookup.Provider registries) {
        try {
            GridlessRecipe indexed = indexRecipe(holder, registries);
            if (indexed != null && !indexed.getResult().isEmpty()) {
                INDEXED_RECIPES.put(indexed.getId(), indexed);
                BY_TYPE.computeIfAbsent(indexed.getRecipeType(), k -> new ArrayList<>()).add(indexed);
                return indexed;
            }
        } catch (Exception e) {
            GridlessMod.LOGGER.debug("Failed to index single recipe: {}", holder.id(), e);
        }
        return null;
    }

    public static GridlessRecipe get(ResourceLocation id) {
        return INDEXED_RECIPES.get(id);
    }

    public static Collection<GridlessRecipe> getAll() {
        return Collections.unmodifiableCollection(INDEXED_RECIPES.values());
    }

    public static List<GridlessRecipe> getForTypes(List<ResourceLocation> allowedTypeIds) {
        List<GridlessRecipe> result = new ArrayList<>();
        for (Map.Entry<RecipeType<?>, List<GridlessRecipe>> entry : BY_TYPE.entrySet()) {
            ResourceLocation typeId = net.minecraft.core.registries.BuiltInRegistries.RECIPE_TYPE.getKey(entry.getKey());
            if (typeId != null && allowedTypeIds.contains(typeId)) {
                result.addAll(entry.getValue());
            }
        }
        return groupRecipesByOutput(result);
    }

    public static List<GridlessRecipe> groupRecipesByOutput(List<GridlessRecipe> list) {
        List<GridlessRecipe> grouped = new ArrayList<>();
        Map<String, GridlessRecipe> outputMap = new LinkedHashMap<>();

        for (GridlessRecipe r : list) {
            ItemStack result = r.getResult();
            String key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(result.getItem()).toString() + "#" + result.getCount();
            if (outputMap.containsKey(key)) {
                outputMap.get(key).addVariant(r);
            } else {
                GridlessRecipe leader = new GridlessRecipe(
                        r.getId(),
                        r.getResult(),
                        r.getInputs(),
                        r.getRecipeType(),
                        r.getCategory(),
                        r.getCookTime(),
                        r.getExperience(),
                        r.requires3x3()
                );
                outputMap.put(key, leader);
                grouped.add(leader);
            }
        }
        return grouped;
    }

    public static GridlessRecipe indexRecipe(RecipeHolder<?> holder, HolderLookup.Provider registries) {
        Recipe<?> recipe = holder.value();
        ResourceLocation id = holder.id().location();
        RecipeType<?> type = recipe.getType();

        ItemStack output = ItemStack.EMPTY;
        try {
            if (recipe instanceof ShapedRecipe shaped) {
                output = ((com.gridless.mixin.ShapedRecipeAccessor) shaped).getResult();
            } else if (recipe instanceof ShapelessRecipe shapeless) {
                output = ((com.gridless.mixin.ShapelessRecipeAccessor) shapeless).getResult();
            } else if (recipe instanceof SingleItemRecipe single) {
                output = ((com.gridless.mixin.SingleItemRecipeAccessor) single).getResult();
            }
        } catch (Exception e) {
            return null;
        }
        if (output == null || output.isEmpty()) return null;

        boolean requires3x3 = false;
        if (recipe instanceof ShapedRecipe shaped) {
            requires3x3 = shaped.getWidth() > 2 || shaped.getHeight() > 2;
        }

        List<CountedIngredient> inputs;
        List<Ingredient> ingredients = recipe.placementInfo().ingredients();
        inputs = consolidateIngredients(ingredients);

        if (recipe instanceof AbstractCookingRecipe cooking) {
            return new GridlessRecipe(id, output, inputs, type, RecipeCategory.classify(output), cooking.cookingTime(), cooking.experience(), false);
        }

        if (inputs.isEmpty()) return null;

        return new GridlessRecipe(id, output, inputs, type, RecipeCategory.classify(output), 0, 0f, requires3x3);
    }

    private static List<CountedIngredient> consolidateIngredients(List<Ingredient> list) {
        List<CountedIngredient> result = new ArrayList<>();

        for (Ingredient ing : list) {
            if (ing.isEmpty()) continue;

            boolean merged = false;
            for (int i = 0; i < result.size(); i++) {
                CountedIngredient existing = result.get(i);
                if (areIngredientsEqual(existing.getIngredient(), ing)) {
                    result.set(i, new CountedIngredient(existing.getIngredient(), existing.getCount() + 1));
                    merged = true;
                    break;
                }
            }

            if (!merged) {
                result.add(new CountedIngredient(ing, 1));
            }
        }

        return result;
    }

    private static boolean areIngredientsEqual(Ingredient a, Ingredient b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}
