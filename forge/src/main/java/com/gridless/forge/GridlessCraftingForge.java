package com.gridless.forge;

import com.gridless.GridlessMod;
import com.gridless.api.station.StationRegistry;
import com.gridless.client.gui.GridlessConfigScreen;
import com.gridless.forge.network.ForgeNetworkHandler;
import com.gridless.sound.GridlessSounds;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
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

        IEventBus modEventBus = context.getModEventBus();
        modEventBus.addListener(this::registerSounds);

        MinecraftForge.EVENT_BUS.addListener(this::addReloadListeners);

        if (FMLLoader.getDist().isClient()) {
            ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new GridlessConfigScreen(parent)));
        }
    }

    private void registerSounds(RegisterEvent event) {
        event.register(Registries.SOUND_EVENT, helper -> GridlessSounds.register(helper::register));
    }

    private void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(StationRegistry.INSTANCE);
    }
}
