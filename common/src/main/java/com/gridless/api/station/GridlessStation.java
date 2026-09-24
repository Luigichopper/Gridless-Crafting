package com.gridless.api.station;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GridlessStation {
    private final Identifier id;
    private final Identifier blockTagId;
    private final List<Identifier> allowedRecipeTypeIds;
    private final boolean supportsQuickCraft;
    private final boolean overrideVanillaGui;
    private final StationMode mode;

    public GridlessStation(Identifier id,
                           Identifier blockTagId,
                           List<Identifier> allowedRecipeTypeIds,
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

    public Identifier getId() {
        return id;
    }

    public Identifier getBlockTagId() {
        return blockTagId;
    }

    public List<Identifier> getAllowedRecipeTypeIds() {
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
        Identifier typeId = BuiltInRegistries.RECIPE_TYPE.getKey(type);
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
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return blockId != null && blockId.equals(blockTagId);
    }
}
