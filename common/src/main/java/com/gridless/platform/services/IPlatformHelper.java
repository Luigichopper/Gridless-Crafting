package com.gridless.platform.services;

import com.gridless.network.C2SCraftGridlessRecipePayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;

import java.nio.file.Path;

public interface IPlatformHelper {
    /**
     * Gets the name of the current platform (e.g. "Fabric" or "NeoForge")
     */
    String getPlatformName();

    /**
     * Checks if a mod with the given ID is loaded.
     */
    boolean isModLoaded(String modId);

    /**
     * Checks if the game is running in a physical development environment.
     */
    boolean isDevelopmentEnvironment();

    /**
     * Gets the game's config directory.
     */
    Path getConfigDirectory();

    /**
     * Sends a custom packet payload to the server.
     */
    void sendToServer(C2SCraftGridlessRecipePayload payload);

    /**
     * Sends a custom packet payload to a specific player on the server.
     */
    void sendToPlayer(ServerPlayer player, C2SCraftGridlessRecipePayload payload);

    /**
     * Opens a menu on the server side with optional extra packet data.
     */
    void openMenu(ServerPlayer player, MenuProvider provider);
}
