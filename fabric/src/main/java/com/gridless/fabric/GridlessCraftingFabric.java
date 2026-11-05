package com.gridless.fabric;

import com.gridless.GridlessMod;
import com.gridless.api.station.StationRegistry;
import com.gridless.network.C2SCraftGridlessRecipePayload;
import com.gridless.network.CraftingTransactionHandler;
import com.gridless.sound.GridlessSounds;
import net.fabricmc.api.ModInitializer;
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

        ServerPlayNetworking.registerGlobalReceiver(C2SCraftGridlessRecipePayload.ID, (server, player, handler, buf, responseSender) -> {
            C2SCraftGridlessRecipePayload payload = new C2SCraftGridlessRecipePayload(buf);
            server.execute(() -> CraftingTransactionHandler.handleCraft(player, payload));
        });

        // Register Data Pack Reload Listener for Gridless Stations
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new IdentifiableResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return new ResourceLocation(GridlessMod.MOD_ID, "gridless_stations");
            }

            @Override
            public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager resourceManager, ProfilerFiller prepProfiler, ProfilerFiller reloadProfiler, Executor bgExecutor, Executor gameExecutor) {
                return StationRegistry.INSTANCE.reload(barrier, resourceManager, prepProfiler, reloadProfiler, bgExecutor, gameExecutor);
            }
        });
    }
}
