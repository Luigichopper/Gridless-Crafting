package com.gridless.mixin;

import com.gridless.config.GridlessConfig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryMenu.class)
public class InventoryMenuMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        if (GridlessConfig.general.enabled && GridlessConfig.general.remove_crafting_inventory) {
            InventoryMenu self = (InventoryMenu) (Object) this;
            for (int i = 0; i <= 4 && i < self.slots.size(); i++) {
                Slot original = self.slots.get(i);
                Slot customSlot = new Slot(original.container, original.getContainerSlot(), -9999, -9999) {
                    @Override
                    public boolean isActive() {
                        return false;
                    }

                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }

                    @Override
                    public boolean mayPickup(Player player) {
                        return false;
                    }
                };
                customSlot.index = i;
                self.slots.set(i, customSlot);
            }
        }
    }
}
