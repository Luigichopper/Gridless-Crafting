package com.gridless.mixin;

import com.gridless.api.station.StationRegistry;
import com.gridless.client.screen.GridlessCraftingScreen;
import com.gridless.config.GridlessConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.CraftingMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(CraftingScreen.class)
public class CraftingScreenMixin {

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void onInit(CallbackInfo ci) {
        if (GridlessConfig.general.enabled && GridlessConfig.stations.enable_crafting_table) {
            CraftingScreen self = (CraftingScreen) (Object) this;
            CraftingMenu menu = self.getMenu();
            Minecraft mc = Minecraft.getInstance();

            if (mc.player != null) {
                ci.cancel();
                mc.execute(() -> mc.setScreen(new GridlessCraftingScreen<>(
                        menu,
                        mc.player.getInventory(),
                        Component.translatable("container.crafting"),
                        StationRegistry.get(StationRegistry.WORKBENCH),
                        Optional.empty(),
                        false
                )));
            }
        }
    }
}
