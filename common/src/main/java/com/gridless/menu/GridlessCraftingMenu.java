package com.gridless.menu;

import com.gridless.api.station.GridlessStation;
import com.gridless.api.station.StationRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class GridlessCraftingMenu extends AbstractContainerMenu {
    public static MenuType<GridlessCraftingMenu> TYPE;

    private final ContainerLevelAccess access;
    private final Player player;
    private final GridlessStation station;
    private final Optional<BlockPos> stationPos;
    private final boolean isInventoryCrafting;

    public static final int INV_X = 188;
    public static final int INV_Y = 148;
    public static final int HOTBAR_Y = 206;

    public GridlessCraftingMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL, StationRegistry.get(StationRegistry.INVENTORY), Optional.empty(), true);
    }

    public GridlessCraftingMenu(int containerId,
                                Inventory playerInventory,
                                ContainerLevelAccess access,
                                GridlessStation station,
                                Optional<BlockPos> stationPos,
                                boolean isInventoryCrafting) {
        super(TYPE, containerId);
        this.access = access;
        this.player = playerInventory.player;
        this.station = station != null ? station : StationRegistry.get(StationRegistry.INVENTORY);
        this.stationPos = stationPos;
        this.isInventoryCrafting = isInventoryCrafting;

        // Player Inventory (3 rows x 9 columns) - positioned cleanly in the bottom right
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
            }
        }

        // Hotbar (9 columns)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, INV_X + col * 18, HOTBAR_Y));
        }
    }

    public GridlessStation getStation() {
        return station;
    }

    public Optional<BlockPos> getStationPos() {
        return stationPos;
    }

    public boolean isInventoryCrafting() {
        return isInventoryCrafting;
    }

    @Override
    public boolean stillValid(Player player) {
        if (isInventoryCrafting) {
            return true;
        }
        if (stationPos.isEmpty()) {
            return true;
        }
        return stillValid(this.access, player, player.level().getBlockState(stationPos.get()).getBlock());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            itemstack = current.copy();

            if (slotIndex < 27) { // In main inventory -> move to hotbar
                if (!this.moveItemStackTo(current, 27, 36, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotIndex < 36) { // In hotbar -> move to main inventory
                if (!this.moveItemStackTo(current, 0, 27, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (current.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (current.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, current);
        }

        return itemstack;
    }
}
