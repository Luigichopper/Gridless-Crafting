package com.gridless.fabric.platform;

import com.gridless.network.C2SCraftGridlessRecipePayload;
import com.gridless.platform.services.IPlatformHelper;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;

import java.nio.file.Path;

public class FabricPlatformHelper implements IPlatformHelper {
    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public void sendToServer(C2SCraftGridlessRecipePayload payload) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        payload.write(buf);
        ClientPlayNetworking.send(C2SCraftGridlessRecipePayload.ID, buf);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, C2SCraftGridlessRecipePayload payload) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        payload.write(buf);
        ServerPlayNetworking.send(player, C2SCraftGridlessRecipePayload.ID, buf);
    }

    @Override
    public void openMenu(ServerPlayer player, MenuProvider provider) {
        player.openMenu(provider);
    }
}
