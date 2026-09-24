package com.gridless.mixin;

import com.gridless.api.station.GridlessStation;
import com.gridless.api.station.StationRegistry;
import com.gridless.client.screen.GridlessCraftingScreen;
import com.gridless.config.GridlessConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.BlastFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.gui.screens.inventory.SmokerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(AbstractFurnaceScreen.class)
public class AbstractFurnaceScreenMixin {

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void onInitFurnace(CallbackInfo ci) {
        if (!GridlessConfig.general.enabled) return;

        AbstractFurnaceScreen<?> self = (AbstractFurnaceScreen<?>) (Object) this;
        GridlessStation station = null;

        if (self instanceof BlastFurnaceScreen && GridlessConfig.stations.isBlastFurnaceInstant()) {
            station = StationRegistry.get(StationRegistry.BLAST_FURNACE);
        } else if (self instanceof SmokerScreen && GridlessConfig.stations.isSmokerInstant()) {
            station = StationRegistry.get(StationRegistry.SMOKER);
        } else if (self instanceof FurnaceScreen && GridlessConfig.stations.isFurnaceInstant()) {
            station = StationRegistry.get(StationRegistry.FURNACE);
        }

        if (station != null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                AbstractFurnaceMenu menu = self.getMenu();
                ci.cancel();
                GridlessStation finalStation = station;
                mc.execute(() -> mc.setScreen(new GridlessCraftingScreen<>(
                        menu,
                        mc.player.getInventory(),
                        Component.translatable("container." + finalStation.getId().getPath()),
                        finalStation,
                        Optional.empty(),
                        false
                )));
            }
        }
    }
}
