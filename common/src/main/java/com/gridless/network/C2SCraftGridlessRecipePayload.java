package com.gridless.network;

import com.gridless.GridlessMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record C2SCraftGridlessRecipePayload(ResourceLocation recipeId,
                                            int amount,
                                            Optional<BlockPos> stationPos,
                                            boolean isInventoryCrafting) implements CustomPacketPayload {

    public static final Type<C2SCraftGridlessRecipePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(GridlessMod.MOD_ID, "craft_gridless_recipe"));

    public static final StreamCodec<FriendlyByteBuf, C2SCraftGridlessRecipePayload> STREAM_CODEC = CustomPacketPayload.codec(
            C2SCraftGridlessRecipePayload::write,
            C2SCraftGridlessRecipePayload::new
    );

    public C2SCraftGridlessRecipePayload(FriendlyByteBuf buf) {
        this(
                buf.readResourceLocation(),
                buf.readVarInt(),
                buf.readOptional(b -> b.readBlockPos()),
                buf.readBoolean()
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(recipeId);
        buf.writeVarInt(amount);
        buf.writeOptional(stationPos, (b, pos) -> b.writeBlockPos(pos));
        buf.writeBoolean(isInventoryCrafting);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
