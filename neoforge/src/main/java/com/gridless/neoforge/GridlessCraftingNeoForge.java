package com.gridless.neoforge;

import com.gridless.GridlessMod;
import com.gridless.api.station.StationRegistry;
import com.gridless.client.gui.GridlessConfigScreen;
import com.gridless.network.C2SCraftGridlessRecipePayload;
import com.gridless.network.CraftingTransactionHandler;
import com.gridless.sound.GridlessSounds;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(GridlessMod.MOD_ID)
public class GridlessCraftingNeoForge {
    public GridlessCraftingNeoForge(IEventBus modEventBus) {
        GridlessMod.init();

        modEventBus.addListener(this::registerSounds);
        modEventBus.addListener(this::registerPayloads);
        NeoForge.EVENT_BUS.addListener(this::addReloadListeners);

        if (FMLLoader.getDist().isClient()) {
            ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class,
                    () -> (container, parent) -> new GridlessConfigScreen(parent));
        }
    }

    private void registerSounds(RegisterEvent event) {
        event.register(Registries.SOUND_EVENT, helper -> GridlessSounds.register(helper::register));
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(GridlessMod.MOD_ID);
        registrar.playToServer(
                C2SCraftGridlessRecipePayload.TYPE,
                C2SCraftGridlessRecipePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        CraftingTransactionHandler.handleCraft(serverPlayer, payload);
                    }
                })
        );
    }

    private void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(StationRegistry.INSTANCE);
    }
}
