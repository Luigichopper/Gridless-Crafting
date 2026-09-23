package com.gridless.api.station;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GridlessStation {
    private final ResourceLocation id;
    private final ResourceLocation blockTagId;
    private final List<ResourceLocation> allowedRecipeTypeIds;
    private final boolean supportsQuickCraft;
    private final boolean overrideVanillaGui;
    private final StationMode mode;

    public GridlessStation(ResourceLocation id,
                           ResourceLocation blockTagId,
                           List<ResourceLocation> allowedRecipeTypeIds,
                           boolean supportsQuickCraft,
                           boolean overrideVanillaGui,
                           StationMode mode) {
        this.id = id;
        this.blockTagId = blockTagId;
        this.allowedRecipeTypeIds = allowedRecipeTypeIds != null ? allowedRecipeTypeIds : Collections.emptyList();
        this.supportsQuickCraft = supportsQuickCraft;
        this.overrideVanillaGui = overrideVanillaGui;
        this.mode = mode != null ? mode : StationMode.NORMAL;
    }

    public ResourceLocation getId() {
        return id;
    }

    public ResourceLocation getBlockTagId() {
        return blockTagId;
    }

    public List<ResourceLocation> getAllowedRecipeTypeIds() {
        return allowedRecipeTypeIds;
    }

    public boolean supportsQuickCraft() {
        return supportsQuickCraft;
    }

    public boolean overrideVanillaGui() {
        return overrideVanillaGui;
    }

    public StationMode getMode() {
        return mode;
    }

    public boolean isAllowedRecipeType(RecipeType<?> type) {
        ResourceLocation typeId = BuiltInRegistries.RECIPE_TYPE.getKey(type);
        if (typeId == null) return false;
        return allowedRecipeTypeIds.contains(typeId);
    }

    public boolean matchesBlock(BlockState state) {
        if (blockTagId == null) return false;
        TagKey<Block> tag = TagKey.create(Registries.BLOCK, blockTagId);
        if (state.is(tag)) {
            return true;
        }
        // Direct block ID fallback
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return blockId != null && blockId.equals(blockTagId);
    }
}
