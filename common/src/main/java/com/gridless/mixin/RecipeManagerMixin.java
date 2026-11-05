package com.gridless.mixin;

import com.google.gson.JsonElement;
import com.gridless.api.recipe.RecipeIndexer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(RecipeManager.class)
public class RecipeManagerMixin {

    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At("TAIL"))
    private void onApply(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo ci) {
        RecipeManager self = (RecipeManager) (Object) this;
        RecipeIndexer.reindex(self, null);
    }

    @Inject(method = "replaceRecipes", at = @At("TAIL"))
    private void onReplaceRecipes(Iterable<Recipe<?>> recipes, CallbackInfo ci) {
        RecipeManager self = (RecipeManager) (Object) this;
        RecipeIndexer.reindex(self, null);
    }
}
