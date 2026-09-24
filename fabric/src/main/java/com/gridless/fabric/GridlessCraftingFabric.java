package com.gridless.fabric;

import com.gridless.GridlessMod;
import com.gridless.api.station.StationRegistry;
import com.gridless.network.C2SCraftGridlessRecipePayload;
import com.gridless.network.CraftingTransactionHandler;
import com.gridless.sound.GridlessSounds;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class GridlessCraftingFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        GridlessMod.init();
        GridlessSounds.init();

        // Register custom networking payloads (1.21.1)
        PayloadTypeRegistry.playC2S().register(C2SCraftGridlessRecipePayload.TYPE, C2SCraftGridlessRecipePayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(C2SCraftGridlessRecipePayload.TYPE, (payload, context) -> {
            context.server().execute(() -> CraftingTransactionHandler.handleCraft(context.player(), payload));
        });

        // Register Data Pack Reload Listener for Gridless Stations
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new IdentifiableResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return ResourceLocation.fromNamespaceAndPath(GridlessMod.MOD_ID, "gridless_stations");
            }

            @Override
            public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager resourceManager, Executor bgExecutor, Executor gameExecutor) {
                return StationRegistry.INSTANCE.reload(barrier, resourceManager, bgExecutor, gameExecutor);
            }
        });
    }
}
