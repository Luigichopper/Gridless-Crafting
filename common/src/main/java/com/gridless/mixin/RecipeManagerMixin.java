package com.gridless.mixin;

import com.gridless.api.recipe.RecipeIndexer;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RecipeManager.class)
public class RecipeManagerMixin {

    @Shadow
    @Final
    private HolderLookup.Provider registries;

    @Inject(method = "finalizeRecipeLoading", at = @At("TAIL"))
    private void onFinalizeRecipeLoading(net.minecraft.world.flag.FeatureFlagSet flags, CallbackInfo ci) {
        RecipeManager self = (RecipeManager) (Object) this;
        RecipeIndexer.reindex(self, this.registries);
    }
}
