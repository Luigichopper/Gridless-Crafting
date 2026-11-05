package com.gridless.neoforge.platform;

import com.gridless.neoforge.network.NeoForgeNetworkHandler;
import com.gridless.network.C2SCraftGridlessRecipePayload;
import com.gridless.platform.services.IPlatformHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

public class NeoForgePlatformHelper implements IPlatformHelper {
    @Override
    public String getPlatformName() {
        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }

    @Override
    public Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public void sendToServer(C2SCraftGridlessRecipePayload payload) {
        NeoForgeNetworkHandler.sendToServer(payload);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, C2SCraftGridlessRecipePayload payload) {
        NeoForgeNetworkHandler.sendToPlayer(player, payload);
    }

    @Override
    public void openMenu(ServerPlayer player, MenuProvider provider) {
        player.openMenu(provider);
    }
}
