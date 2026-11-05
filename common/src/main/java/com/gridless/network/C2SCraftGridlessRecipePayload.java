package com.gridless.network;

import com.gridless.GridlessMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record C2SCraftGridlessRecipePayload(
        ResourceLocation recipeId,
        int amount,
        Optional<BlockPos> stationPos,
        boolean isInventoryCrafting
) {
    public static final ResourceLocation ID = new ResourceLocation(GridlessMod.MOD_ID, "craft_gridless_recipe");

    public C2SCraftGridlessRecipePayload(FriendlyByteBuf buf) {
        this(
                buf.readResourceLocation(),
                buf.readVarInt(),
                buf.readBoolean() ? Optional.of(buf.readBlockPos()) : Optional.empty(),
                buf.readBoolean()
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(recipeId);
        buf.writeVarInt(amount);
        if (stationPos.isPresent()) {
            buf.writeBoolean(true);
            buf.writeBlockPos(stationPos.get());
        } else {
            buf.writeBoolean(false);
        }
        buf.writeBoolean(isInventoryCrafting);
    }
}
