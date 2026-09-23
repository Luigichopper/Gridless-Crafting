package com.gridless.fabric.client;

import com.gridless.config.GridlessConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class GridlessClothConfigScreen {
    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("text.cloth-config.gridless_crafting.title"));

        builder.setSavingRunnable(GridlessConfig::save);

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // 1. General Category
        ConfigCategory generalCat = builder.getOrCreateCategory(Component.translatable("text.cloth-config.gridless_crafting.category.general"));

        generalCat.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("text.cloth-config.gridless_crafting.option.enabled"),
                GridlessConfig.general.enabled)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("text.cloth-config.gridless_crafting.option.enabled.tooltip"))
                .setSaveConsumer(val -> GridlessConfig.general.enabled = val)
                .build());

        generalCat.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("text.cloth-config.gridless_crafting.option.replace_inventory_crafting"),
                GridlessConfig.general.replace_inventory_crafting)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("text.cloth-config.gridless_crafting.option.replace_inventory_crafting.tooltip"))
                .setSaveConsumer(val -> GridlessConfig.general.replace_inventory_crafting = val)
                .build());

        generalCat.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("text.cloth-config.gridless_crafting.option.remove_crafting_inventory"),
                GridlessConfig.general.remove_crafting_inventory)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("text.cloth-config.gridless_crafting.option.remove_crafting_inventory.tooltip"))
                .setSaveConsumer(val -> GridlessConfig.general.remove_crafting_inventory = val)
                .build());

        generalCat.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("text.cloth-config.gridless_crafting.option.hide_uncraftable_recipes"),
                GridlessConfig.general.hide_uncraftable_recipes)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("text.cloth-config.gridless_crafting.option.hide_uncraftable_recipes.tooltip"))
                .setSaveConsumer(val -> GridlessConfig.general.hide_uncraftable_recipes = val)
                .build());

        // 2. Stations Category
        ConfigCategory stationsCat = builder.getOrCreateCategory(Component.translatable("text.cloth-config.gridless_crafting.category.stations"));

        stationsCat.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("text.cloth-config.gridless_crafting.option.enable_workbench"),
                GridlessConfig.stations.enable_crafting_table)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("text.cloth-config.gridless_crafting.option.enable_workbench.tooltip"))
                .setSaveConsumer(val -> GridlessConfig.stations.enable_crafting_table = val)
                .build());

        stationsCat.addEntry(entryBuilder.startSelector(
                Component.translatable("text.cloth-config.gridless_crafting.option.furnace_mode"),
                new String[]{"INSTANT_WITH_FUEL", "TRADITIONAL"},
                GridlessConfig.stations.furnace_mode)
                .setDefaultValue("INSTANT_WITH_FUEL")
                .setTooltip(Component.translatable("text.cloth-config.gridless_crafting.option.furnace_mode.tooltip"))
                .setSaveConsumer(val -> GridlessConfig.stations.furnace_mode = val)
                .build());

        stationsCat.addEntry(entryBuilder.startSelector(
                Component.translatable("text.cloth-config.gridless_crafting.option.blast_furnace_mode"),
                new String[]{"INSTANT_WITH_FUEL", "TRADITIONAL"},
                GridlessConfig.stations.blast_furnace_mode)
                .setDefaultValue("INSTANT_WITH_FUEL")
                .setTooltip(Component.translatable("text.cloth-config.gridless_crafting.option.blast_furnace_mode.tooltip"))
                .setSaveConsumer(val -> GridlessConfig.stations.blast_furnace_mode = val)
                .build());

        stationsCat.addEntry(entryBuilder.startSelector(
                Component.translatable("text.cloth-config.gridless_crafting.option.smoker_mode"),
                new String[]{"INSTANT_WITH_FUEL", "TRADITIONAL"},
                GridlessConfig.stations.smoker_mode)
                .setDefaultValue("INSTANT_WITH_FUEL")
                .setTooltip(Component.translatable("text.cloth-config.gridless_crafting.option.smoker_mode.tooltip"))
                .setSaveConsumer(val -> GridlessConfig.stations.smoker_mode = val)
                .build());

        // 3. Audio & Visual Category
        ConfigCategory audioVisualCat = builder.getOrCreateCategory(Component.translatable("text.cloth-config.gridless_crafting.category.audio_visual"));

        audioVisualCat.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("text.cloth-config.gridless_crafting.option.enable_custom_sounds"),
                GridlessConfig.audioVisual.enable_custom_sounds)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("text.cloth-config.gridless_crafting.option.enable_custom_sounds.tooltip"))
                .setSaveConsumer(val -> GridlessConfig.audioVisual.enable_custom_sounds = val)
                .build());

        audioVisualCat.addEntry(entryBuilder.startIntSlider(
                Component.translatable("text.cloth-config.gridless_crafting.option.visible_recipe_rows"),
                GridlessConfig.audioVisual.visible_recipe_rows, 4, 12)
                .setDefaultValue(6)
                .setTooltip(Component.translatable("text.cloth-config.gridless_crafting.option.visible_recipe_rows.tooltip"))
                .setSaveConsumer(val -> GridlessConfig.audioVisual.visible_recipe_rows = val)
                .build());

        return builder.build();
    }
}
