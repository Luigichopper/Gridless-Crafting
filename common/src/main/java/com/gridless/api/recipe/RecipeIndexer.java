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

    public static void reindex(Object recipeAccess, HolderLookup.Provider registries) {
        Collection<RecipeHolder<?>> holders = getHolders(recipeAccess);
        if (holders.isEmpty()) {
            GridlessMod.LOGGER.warn("Attempted recipe reindex with 0 holders from {}, retaining {} existing recipes.", recipeAccess, INDEXED_RECIPES.size());
            return;
        }

        INDEXED_RECIPES.clear();
        BY_TYPE.clear();

        GridlessMod.LOGGER.info("Indexing {} vanilla recipes for Gridless Crafting...", holders.size());

        for (RecipeHolder<?> holder : holders) {
            try {
                GridlessRecipe indexed = indexRecipe(holder, registries);
                if (indexed != null && !indexed.getResult().isEmpty()) {
                    INDEXED_RECIPES.put(indexed.getId(), indexed);
                    BY_TYPE.computeIfAbsent(indexed.getRecipeType(), k -> new ArrayList<>()).add(indexed);
                }
            } catch (Throwable t) {
                GridlessMod.LOGGER.debug("Skipping unindexable recipe: {}", holder.id(), t);
            }
        }

        GridlessMod.LOGGER.info("Indexed {} gridless recipes across {} types.", INDEXED_RECIPES.size(), BY_TYPE.size());
    }

    private static Collection<RecipeHolder<?>> getHolders(Object recipeAccess) {
        if (recipeAccess == null) return Collections.emptyList();
        if (recipeAccess instanceof RecipeMap map) {
            return map.values();
        }
        if (recipeAccess instanceof net.minecraft.world.item.crafting.RecipeManager manager) {
            try {
                return ((com.gridless.mixin.RecipeManagerAccessor) manager).getRecipes().values();
            } catch (Exception ignored) {}
        }
        if (recipeAccess instanceof Collection<?> col) {
            List<RecipeHolder<?>> holders = new ArrayList<>();
            for (Object obj : col) {
                if (obj instanceof RecipeHolder<?> holder) {
                    holders.add(holder);
                }
            }
            return holders;
        }

        // Reflection fallback for ClientRecipeContainer or custom containers
        try {
            for (java.lang.reflect.Method m : recipeAccess.getClass().getMethods()) {
                if (m.getParameterCount() == 0) {
                    Class<?> ret = m.getReturnType();
                    if (RecipeMap.class.isAssignableFrom(ret)) {
                        RecipeMap map = (RecipeMap) m.invoke(recipeAccess);
                        if (map != null && !map.values().isEmpty()) return map.values();
                    } else if (Collection.class.isAssignableFrom(ret)) {
                        Collection<?> col = (Collection<?>) m.invoke(recipeAccess);
                        if (col != null && !col.isEmpty()) {
                            List<RecipeHolder<?>> holders = new ArrayList<>();
                            for (Object obj : col) {
                                if (obj instanceof RecipeHolder<?> holder) {
                                    holders.add(holder);
                                }
                            }
                            if (!holders.isEmpty()) return holders;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        return Collections.emptyList();
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
            RecipeType<?> type = entry.getKey();
            ResourceLocation typeId = net.minecraft.core.registries.BuiltInRegistries.RECIPE_TYPE.getKey(type);
            boolean match = false;
            if (typeId != null && allowedTypeIds.contains(typeId)) {
                match = true;
            } else if (type == RecipeType.CRAFTING && allowedTypeIds.contains(ResourceLocation.fromNamespaceAndPath("minecraft", "crafting"))) {
                match = true;
            } else if (type == RecipeType.SMELTING && allowedTypeIds.contains(ResourceLocation.fromNamespaceAndPath("minecraft", "smelting"))) {
                match = true;
            } else if (type == RecipeType.BLASTING && allowedTypeIds.contains(ResourceLocation.fromNamespaceAndPath("minecraft", "blasting"))) {
                match = true;
            } else if (type == RecipeType.SMOKING && allowedTypeIds.contains(ResourceLocation.fromNamespaceAndPath("minecraft", "smoking"))) {
                match = true;
            }
            if (match) {
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
        } catch (Throwable ignored) {
            return null;
        }
        if (output == null || output.isEmpty()) return null;

        boolean requires3x3 = false;
        if (recipe instanceof ShapedRecipe shaped) {
            requires3x3 = shaped.getWidth() > 2 || shaped.getHeight() > 2;
        }

        List<CountedIngredient> inputs;
        List<Ingredient> ingredients = extractIngredients(recipe);
        inputs = consolidateIngredients(ingredients);

        if (recipe instanceof AbstractCookingRecipe cooking) {
            return new GridlessRecipe(id, output, inputs, type, RecipeCategory.classify(output), cooking.cookingTime(), cooking.experience(), false);
        }

        if (inputs.isEmpty()) return null;

        return new GridlessRecipe(id, output, inputs, type, RecipeCategory.classify(output), 0, 0f, requires3x3);
    }

    private static List<Ingredient> extractIngredients(Recipe<?> recipe) {
        if (recipe instanceof ShapedRecipe shaped) {
            List<Ingredient> list = new ArrayList<>();
            for (Optional<Ingredient> opt : shaped.getIngredients()) {
                opt.ifPresent(list::add);
            }
            if (!list.isEmpty()) {
                return list;
            }
        } else if (recipe instanceof ShapelessRecipe shapeless) {
            try {
                List<Ingredient> ings = ((com.gridless.mixin.ShapelessRecipeAccessor) shapeless).getIngredients();
                if (ings != null && !ings.isEmpty()) {
                    return ings;
                }
            } catch (Throwable ignored) {}
        } else if (recipe instanceof SingleItemRecipe single) {
            try {
                Ingredient ing = single.input();
                if (ing != null) {
                    return List.of(ing);
                }
            } catch (Throwable ignored) {}
        }

        try {
            return recipe.placementInfo().ingredients();
        } catch (Throwable ignored) {
            return Collections.emptyList();
        }
    }



    private static List<CountedIngredient> consolidateIngredients(List<Ingredient> list) {
        List<CountedIngredient> result = new ArrayList<>();
        if (list == null) return result;

        for (Ingredient ing : list) {
            if (ing == null) continue;

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
        if (a.equals(b)) return true;
        Set<net.minecraft.world.item.Item> itemsA = getIngredientItems(a);
        Set<net.minecraft.world.item.Item> itemsB = getIngredientItems(b);
        return !itemsA.isEmpty() && itemsA.equals(itemsB);
    }

    private static Set<net.minecraft.world.item.Item> getIngredientItems(Ingredient ing) {
        try {
            return ing.items().map(net.minecraft.core.Holder::value).collect(java.util.stream.Collectors.toSet());
        } catch (Throwable e) {
            return Collections.emptySet();
        }
    }
}

