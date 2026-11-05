package com.gridless.neoforge.network;

import com.gridless.GridlessMod;
import com.gridless.network.C2SCraftGridlessRecipePayload;
import com.gridless.network.CraftingTransactionHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class NeoForgeNetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(GridlessMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void init() {
        int id = 0;
        CHANNEL.registerMessage(
                id++,
                C2SCraftGridlessRecipePayload.class,
                C2SCraftGridlessRecipePayload::write,
                C2SCraftGridlessRecipePayload::new,
                (msg, ctxSupplier) -> {
                    NetworkEvent.Context ctx = ctxSupplier.get();
                    ctx.enqueueWork(() -> {
                        ServerPlayer player = ctx.getSender();
                        if (player != null) {
                            CraftingTransactionHandler.handleCraft(player, msg);
                        }
                    });
                    ctx.setPacketHandled(true);
                }
        );
    }

    public static void sendToServer(C2SCraftGridlessRecipePayload payload) {
        CHANNEL.sendToServer(payload);
    }

    public static void sendToPlayer(ServerPlayer player, C2SCraftGridlessRecipePayload payload) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
    }
}
