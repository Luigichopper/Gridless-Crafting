package com.gridless.api.smelting;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

import java.util.Map;

public class SmeltingCalculator {
    public static class FuelCost {
        public final int totalBurnTimeNeeded;
        public final int fuelItemsNeeded;
        public final Item recommendedFuel;
        public final int fuelBurnDuration;
        public final int primaryFuelSlot;
        public final boolean hasEnough;

        public FuelCost(int totalBurnTimeNeeded, int fuelItemsNeeded, Item recommendedFuel, int fuelBurnDuration, int primaryFuelSlot, boolean hasEnough) {
            this.totalBurnTimeNeeded = totalBurnTimeNeeded;
            this.fuelItemsNeeded = fuelItemsNeeded;
            this.recommendedFuel = recommendedFuel;
            this.fuelBurnDuration = fuelBurnDuration;
            this.primaryFuelSlot = primaryFuelSlot;
            this.hasEnough = hasEnough;
        }
    }

    public static int getBurnDuration(ItemStack stack, Level level) {
        if (stack == null || stack.isEmpty()) return 0;
        try {
            Map<Item, Integer> fuelMap = AbstractFurnaceBlockEntity.getFuel();
            if (fuelMap.containsKey(stack.getItem())) {
                return fuelMap.get(stack.getItem());
            }
        } catch (Throwable ignored) {}
        // Fallback standard fuel values
        Item item = stack.getItem();
        if (item == Items.LAVA_BUCKET) return 20000;
        if (item == Items.COAL_BLOCK) return 16000;
        if (item == Items.BLAZE_ROD) return 2400;
        if (item == Items.COAL || item == Items.CHARCOAL) return 1600;
        if (item.toString().contains("LOG") || item.toString().contains("WOOD")) return 300;
        if (item.toString().contains("PLANKS")) return 300;
        if (item == Items.STICK) return 100;
        return 0;
    }

    /**
     * Finds the primary fuel slot index (0..35), checking hotbar (0..8) first, then main inventory (9..35).
     * Returns -1 if no fuel is found.
     */
    public static int findPrimaryFuelSlot(Player player) {
        if (player == null) return -1;
        Inventory inv = player.getInventory();
        // 1. Hotbar first (slots 0 to 8)
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getItem(i);
            if (getBurnDuration(stack, player.level()) > 0) {
                return i;
            }
        }
        // 2. Main inventory next (slots 9 to 35)
        for (int i = 9; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            if (getBurnDuration(stack, player.level()) > 0) {
                return i;
            }
        }
        return -1;
    }

    public static int getTotalBurnAvailable(Player player) {
        if (player == null) return 0;
        int total = 0;
        Inventory inv = player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            int burn = getBurnDuration(stack, player.level());
            if (burn > 0) {
                total += burn * stack.getCount();
            }
        }
        return total;
    }

    public static int getMaxSmeltableWithFuel(Player player, int cookTimePerItem) {
        if (player == null) return 0;
        int effectiveCookTime = cookTimePerItem > 0 ? cookTimePerItem : 200;
        int totalBurn = getTotalBurnAvailable(player);
        return totalBurn / effectiveCookTime;
    }

    public static FuelCost calculateFuelCost(Player player, int itemCount, int cookTimePerItem) {
        int effectiveCookTime = cookTimePerItem > 0 ? cookTimePerItem : 200;
        int totalBurnTimeNeeded = Math.max(1, itemCount) * effectiveCookTime;

        if (player == null) {
            return new FuelCost(totalBurnTimeNeeded, (int) Math.ceil((double) totalBurnTimeNeeded / 1600), Items.COAL, 1600, -1, false);
        }

        int primarySlot = findPrimaryFuelSlot(player);
        Item primaryFuel = Items.COAL;
        int burnDuration = 1600;

        if (primarySlot >= 0) {
            ItemStack stack = player.getInventory().getItem(primarySlot);
            primaryFuel = stack.getItem();
            burnDuration = Math.max(1, getBurnDuration(stack, player.level()));
        }

        int fuelCountNeeded = (int) Math.ceil((double) totalBurnTimeNeeded / burnDuration);
        boolean hasEnough = getTotalBurnAvailable(player) >= totalBurnTimeNeeded;

        return new FuelCost(totalBurnTimeNeeded, Math.max(1, fuelCountNeeded), primaryFuel, burnDuration, primarySlot, hasEnough);
    }

    public static boolean hasEnoughFuel(Player player, int totalBurnTimeNeeded) {
        return getTotalBurnAvailable(player) >= totalBurnTimeNeeded;
    }

    /**
     * Deducts fuel strictly starting from the hotbar (0..8) then main inventory (9..35).
     */
    public static boolean deductFuel(Player player, int totalBurnTimeNeeded) {
        if (!hasEnoughFuel(player, totalBurnTimeNeeded)) {
            return false;
        }

        int remainingBurn = totalBurnTimeNeeded;
        Inventory inv = player.getInventory();

        // Check hotbar first (0..8), then main inventory (9..35)
        int[] searchOrder = new int[36];
        for (int i = 0; i < 9; i++) searchOrder[i] = i;
        for (int i = 9; i < 36; i++) searchOrder[i] = i;

        for (int slot : searchOrder) {
            ItemStack stack = inv.getItem(slot);
            int burnPerItem = getBurnDuration(stack, player.level());
            if (burnPerItem <= 0) continue;

            while (!stack.isEmpty() && remainingBurn > 0) {
                remainingBurn -= burnPerItem;
                if (stack.getItem() == Items.LAVA_BUCKET) {
                    stack.shrink(1);
                    player.getInventory().add(new ItemStack(Items.BUCKET));
                } else {
                    stack.shrink(1);
                }
                if (stack.isEmpty()) {
                    inv.setItem(slot, ItemStack.EMPTY);
                }
            }

            if (remainingBurn <= 0) {
                break;
            }
        }

        return remainingBurn <= 0;
    }
}
