package com.gridless.forge.network;

import com.gridless.GridlessMod;
import com.gridless.network.C2SCraftGridlessRecipePayload;
import com.gridless.network.CraftingTransactionHandler;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;

public class ForgeNetworkHandler {
    private static Channel<CustomPacketPayload> CHANNEL;

    public static void init() {
        CHANNEL = ChannelBuilder.named(ResourceLocation.fromNamespaceAndPath(GridlessMod.MOD_ID, "main"))
                .networkProtocolVersion(1)
                .acceptedVersions(Channel.VersionTest.exact(1))
                .payloadChannel()
                .play()
                .serverbound()
                .add(C2SCraftGridlessRecipePayload.TYPE, C2SCraftGridlessRecipePayload.STREAM_CODEC.cast(), (payload, ctx) -> {
                    ctx.enqueueWork(() -> {
                        ServerPlayer player = ctx.getSender();
                        if (player != null) {
                            CraftingTransactionHandler.handleCraft(player, payload);
                        }
                    });
                    ctx.setPacketHandled(true);
                })
                .build();
    }

    public static void sendToServer(CustomPacketPayload payload) {
        if (CHANNEL != null) {
            CHANNEL.send(payload, PacketDistributor.SERVER.noArg());
        }
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (CHANNEL != null) {
            CHANNEL.send(payload, PacketDistributor.PLAYER.with(player));
        }
    }
}
