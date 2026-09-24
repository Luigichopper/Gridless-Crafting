package com.gridless.mixin;

import com.gridless.api.recipe.RecipeIndexer;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(method = "handleUpdateTags", at = @At("TAIL"))
    private void onUpdateTags(ClientboundUpdateTagsPacket packet, CallbackInfo ci) {
        ClientPacketListener self = (ClientPacketListener) (Object) this;
        RecipeIndexer.reindex(self.recipes(), self.registryAccess());
    }

    @Inject(method = "handleUpdateRecipes", at = @At("TAIL"))
    private void onUpdateRecipes(ClientboundUpdateRecipesPacket packet, CallbackInfo ci) {
        ClientPacketListener self = (ClientPacketListener) (Object) this;
        RecipeIndexer.reindex(self.recipes(), self.registryAccess());
    }
}

