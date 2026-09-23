package com.gridless.network;

import com.gridless.GridlessMod;
import com.gridless.api.recipe.CountedIngredient;
import com.gridless.api.recipe.GridlessRecipe;
import com.gridless.api.recipe.IngredientBag;
import com.gridless.api.recipe.RecipeIndexer;
import com.gridless.api.smelting.SmeltingCalculator;
import com.gridless.api.station.GridlessStation;
import com.gridless.api.station.StationMode;
import com.gridless.api.station.StationRegistry;
import com.gridless.sound.GridlessSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.BlastFurnaceMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.SmokerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class CraftingTransactionHandler {

    public static void handleCraft(ServerPlayer player, C2SCraftGridlessRecipePayload payload) {
        ResourceLocation recipeId = payload.recipeId();
        int requestedAmount = Math.max(1, Math.min(payload.amount(), 6400));
        Optional<BlockPos> stationPos = payload.stationPos();
        boolean isInventory = payload.isInventoryCrafting();

        // 1. Station & Distance Validation
        GridlessStation station = null;
        if (!isInventory) {
            if (player.containerMenu instanceof CraftingMenu) {
                station = StationRegistry.get(StationRegistry.WORKBENCH);
            } else if (player.containerMenu instanceof BlastFurnaceMenu) {
                station = StationRegistry.get(StationRegistry.BLAST_FURNACE);
            } else if (player.containerMenu instanceof SmokerMenu) {
                station = StationRegistry.get(StationRegistry.SMOKER);
            } else if (player.containerMenu instanceof AbstractFurnaceMenu) {
                station = StationRegistry.get(StationRegistry.FURNACE);
            } else if (stationPos.isPresent()) {
                BlockPos pos = stationPos.get();
                Vec3 center = Vec3.atCenterOf(pos);
                if (player.distanceToSqr(center) > 64.0) {
                    GridlessMod.LOGGER.warn("Player {} is too far from station at {}", player.getName().getString(), pos);
                    return;
                }
                BlockState state = player.level().getBlockState(pos);
                Optional<GridlessStation> matched = StationRegistry.findStationForBlock(state);
                if (matched.isEmpty()) {
                    GridlessMod.LOGGER.warn("Player {} attempted crafting at invalid block: {}", player.getName().getString(), state);
                    return;
                }
                station = matched.get();
            } else {
                station = StationRegistry.get(StationRegistry.WORKBENCH);
            }
        } else {
            station = StationRegistry.get(StationRegistry.INVENTORY);
        }

        // 2. Resolve Recipe
        GridlessRecipe recipe = RecipeIndexer.get(recipeId);
        if (recipe == null && player.server != null) {
            Optional<RecipeHolder<?>> holderOpt = player.server.getRecipeManager().byKey(recipeId);
            if (holderOpt.isPresent()) {
                recipe = RecipeIndexer.indexSingle(holderOpt.get(), player.server.registryAccess());
            }
        }
        if (recipe == null) {
            GridlessMod.LOGGER.warn("Unknown recipe ID requested: {}", recipeId);
            return;
        }

        // Check if recipe is allowed for this station
        if (station != null && !station.isAllowedRecipeType(recipe.getRecipeType())) {
            GridlessMod.LOGGER.warn("Recipe type {} not allowed at station {}", recipe.getRecipeType(), station.getId());
            return;
        }

        // Check if recipe requires 3x3 table and player is using portable inventory
        if (isInventory && recipe.requires3x3()) {
            GridlessMod.LOGGER.warn("Recipe {} requires crafting table, cannot craft in inventory", recipeId);
            return;
        }

        // 3. Validate Inventory & Calculate Actual Craftable Amount
        IngredientBag bag = IngredientBag.fromPlayer(player);
        int maxCraftable = recipe.maxCraftable(bag);
        int actualCraftAmount = Math.min(requestedAmount, maxCraftable);

        if (actualCraftAmount <= 0) {
            return;
        }

        // If station is smelting, check fuel and cap craftable amount by fuel
        if (station != null && station.getMode() == StationMode.SMELTING) {
            int cookTime = recipe.getCookTime() > 0 ? recipe.getCookTime() : 200;
            int maxWithFuel = SmeltingCalculator.getMaxSmeltableWithFuel(player, cookTime);
            actualCraftAmount = Math.min(actualCraftAmount, maxWithFuel);
            if (actualCraftAmount <= 0) {
                return;
            }
            int totalBurnNeeded = actualCraftAmount * cookTime;
            if (!SmeltingCalculator.deductFuel(player, totalBurnNeeded)) {
                return;
            }
        }

        // 4. Atomically Deduct Input Items
        Inventory inv = player.getInventory();
        for (CountedIngredient counted : recipe.getInputs()) {
            int remainingNeeded = counted.getCount() * actualCraftAmount;

            for (int i = 0; i < 36 && remainingNeeded > 0; i++) {
                ItemStack stack = inv.getItem(i);
                if (!stack.isEmpty() && counted.test(stack)) {
                    int toTake = Math.min(stack.getCount(), remainingNeeded);
                    stack.shrink(toTake);
                    remainingNeeded -= toTake;
                    if (stack.isEmpty()) {
                        inv.setItem(i, ItemStack.EMPTY);
                    }
                }
            }

            // Check offhand if needed
            if (remainingNeeded > 0) {
                ItemStack offhand = inv.getItem(40);
                if (!offhand.isEmpty() && counted.test(offhand)) {
                    int toTake = Math.min(offhand.getCount(), remainingNeeded);
                    offhand.shrink(toTake);
                    remainingNeeded -= toTake;
                    if (offhand.isEmpty()) {
                        inv.setItem(40, ItemStack.EMPTY);
                    }
                }
            }
        }

        // 5. Grant Output Items to Player
        ItemStack resultTemplate = recipe.getResult();
        int totalOutputCount = resultTemplate.getCount() * actualCraftAmount;
        int maxStackSize = resultTemplate.getMaxStackSize();

        while (totalOutputCount > 0) {
            int stackCount = Math.min(totalOutputCount, maxStackSize);
            ItemStack resultStack = resultTemplate.copyWithCount(stackCount);
            totalOutputCount -= stackCount;

            if (!inv.add(resultStack)) {
                player.drop(resultStack, false);
            }
        }

        // 6. Experience (for smelting)
        if (recipe.getExperience() > 0) {
            int xp = (int) (recipe.getExperience() * actualCraftAmount);
            if (xp > 0) {
                player.giveExperiencePoints(xp);
            }
        }

        // 7. Feedback & Audio
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                GridlessSounds.CRAFT_CLICK, SoundSource.PLAYERS, 0.8f, 1.0f);

        player.containerMenu.broadcastChanges();
    }
}
