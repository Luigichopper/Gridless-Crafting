package com.gridless.api.recipe;

import com.gridless.GridlessMod;
import com.gridless.mixin.ShapedRecipeAccessor;
import com.gridless.mixin.ShapelessRecipeAccessor;
import com.gridless.mixin.SingleItemRecipeAccessor;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.RecipeDisplay;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RecipeIndexer {
    private static final Map<Identifier, GridlessRecipe> INDEXED_RECIPES = new ConcurrentHashMap<>();
    private static final Map<RecipeType<?>, List<GridlessRecipe>> BY_TYPE = new ConcurrentHashMap<>();

    public static void reindex(RecipeMap recipeMap, HolderLookup.Provider registries) {
        reindex(recipeMap.values(), registries);
    }

    public static void reindex(RecipeManager recipeManager, HolderLookup.Provider registries) {
        reindex(recipeManager.getRecipes(), registries);
    }

    public static void reindex(Iterable<RecipeHolder<?>> holders, HolderLookup.Provider registries) {
        INDEXED_RECIPES.clear();
        BY_TYPE.clear();

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

    public static GridlessRecipe get(Identifier id) {
        return INDEXED_RECIPES.get(id);
    }

    public static Collection<GridlessRecipe> getAll() {
        return Collections.unmodifiableCollection(INDEXED_RECIPES.values());
    }

    public static List<GridlessRecipe> getForTypes(List<Identifier> allowedTypeIds) {
        List<GridlessRecipe> result = new ArrayList<>();
        for (Map.Entry<RecipeType<?>, List<GridlessRecipe>> entry : BY_TYPE.entrySet()) {
            Identifier typeId = net.minecraft.core.registries.BuiltInRegistries.RECIPE_TYPE.getKey(entry.getKey());
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
        Identifier id = holder.id().identifier();
        RecipeType<?> type = recipe.getType();

        ItemStack output = null;
        boolean requires3x3 = false;
        List<CountedIngredient> inputs = null;
        int cookTime = 0;
        float experience = 0f;

        if (recipe instanceof ShapedRecipe shaped) {
            output = ((ShapedRecipeAccessor) shaped).getResult().create();
            requires3x3 = shaped.getWidth() > 2 || shaped.getHeight() > 2;
            List<Ingredient> list = shaped.getIngredients().stream()
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();
            inputs = consolidateIngredients(list);
        } else if (recipe instanceof ShapelessRecipe shapeless) {
            output = ((ShapelessRecipeAccessor) shapeless).getResult().create();
            requires3x3 = false;
            inputs = consolidateIngredients(((ShapelessRecipeAccessor) shapeless).getIngredients());
        } else if (recipe instanceof AbstractCookingRecipe cooking) {
            output = ((SingleItemRecipeAccessor) cooking).getResult().create();
            requires3x3 = false;
            inputs = List.of(new CountedIngredient(cooking.input(), 1));
            cookTime = cooking.cookingTime();
            experience = cooking.experience();
        } else if (recipe instanceof SingleItemRecipe singleItem) {
            output = ((SingleItemRecipeAccessor) singleItem).getResult().create();
            requires3x3 = false;
            inputs = List.of(new CountedIngredient(singleItem.input(), 1));
        } else {
            try {
                List<RecipeDisplay> displays = recipe.display();
                if (!displays.isEmpty()) {
                    RecipeDisplay disp = displays.get(0);
                    net.minecraft.util.context.ContextMap context = registries != null
                            ? new net.minecraft.util.context.ContextMap.Builder().withParameter(net.minecraft.world.item.crafting.display.SlotDisplayContext.REGISTRIES, registries).create(net.minecraft.world.item.crafting.display.SlotDisplayContext.CONTEXT)
                            : new net.minecraft.util.context.ContextMap.Builder().create(net.minecraft.world.item.crafting.display.SlotDisplayContext.CONTEXT);
                    output = disp.result().resolveForFirstStack(context);
                }
            } catch (Exception ignored) {}
        }

        if (output == null || output.isEmpty() || inputs == null || inputs.isEmpty()) {
            return null;
        }

        return new GridlessRecipe(id, output, inputs, type, RecipeCategory.classify(output), cookTime, experience, requires3x3);
    }

    public static List<CountedIngredient> consolidateIngredients(List<Ingredient> list) {
        List<CountedIngredient> result = new ArrayList<>();
        if (list == null) return result;

        for (Ingredient ing : list) {
            if (ing == null || ing.isEmpty()) continue;

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
