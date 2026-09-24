package com.gridless.forge;

import com.gridless.GridlessMod;
import com.gridless.api.station.StationRegistry;
import com.gridless.forge.network.ForgeNetworkHandler;
import com.gridless.sound.GridlessSounds;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.registries.RegisterEvent;

@Mod(GridlessMod.MOD_ID)
public class GridlessCraftingForge {
    public GridlessCraftingForge(FMLJavaModLoadingContext context) {
        GridlessMod.init();
        ForgeNetworkHandler.init();

        RegisterEvent.getBus(context.getModBusGroup()).addListener(this::registerSounds);
        AddReloadListenerEvent.BUS.addListener(this::addReloadListeners);
        net.minecraftforge.event.server.ServerStartingEvent.BUS.addListener(event -> ensureLootModifierManagerInitialized(null));

        ensureLootModifierManagerInitialized(null);

        if (FMLLoader.getDist().isClient()) {
            com.gridless.forge.client.GridlessCraftingClientForge.registerConfigScreen(context);
        }
    }

    private void registerSounds(RegisterEvent event) {
        event.register(Registries.SOUND_EVENT, helper -> GridlessSounds.register(helper::register));
    }

    private void addReloadListeners(AddReloadListenerEvent event) {
        ensureLootModifierManagerInitialized(event.getRegistries());
        event.addListener(StationRegistry.INSTANCE);
    }

    private static void ensureLootModifierManagerInitialized(net.minecraft.core.HolderLookup.Provider registries) {
        try {
            java.lang.reflect.Field field = Class.forName("net.minecraftforge.common.ForgeInternalHandler").getDeclaredField("INSTANCE");
            field.setAccessible(true);
            if (field.get(null) == null) {
                net.minecraft.core.HolderLookup.Provider provider = registries != null ? registries : net.minecraft.data.registries.VanillaRegistries.createLookup();
                field.set(null, new net.minecraftforge.common.loot.LootModifierManager(provider));
                GridlessMod.LOGGER.info("Initialized fallback LootModifierManager to prevent crash before resource reload.");
            }
        } catch (Throwable t) {
            GridlessMod.LOGGER.debug("Could not ensure LootModifierManager initialization: {}", t.getMessage());
        }
    }
}
