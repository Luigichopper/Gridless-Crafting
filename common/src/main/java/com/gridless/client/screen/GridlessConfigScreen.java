package com.gridless.client.screen;

import com.gridless.config.GridlessConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class GridlessConfigScreen extends Screen {
    private final Screen parent;
    private int activeTab = 0; // 0 = General, 1 = Stations, 2 = Audio & Visual

    public GridlessConfigScreen(Screen parent) {
        super(Component.translatable("text.cloth-config.gridless_crafting.title"));
        this.parent = parent;
    }

    public static Screen create(Screen parent) {
        return new GridlessConfigScreen(parent);
    }

    @Override
    protected void init() {
        super.init();
        rebuildWidgets();
    }

    @Override
    protected void rebuildWidgets() {
        this.clearWidgets();

        int centerX = this.width / 2;
        int tabY = 32;
        int tabWidth = 95;

        // Category Tabs
        Button genTab = Button.builder(Component.translatable("text.cloth-config.gridless_crafting.category.general"), btn -> switchTab(0))
                .bounds(centerX - 150, tabY, tabWidth, 20).build();
        Button staTab = Button.builder(Component.translatable("text.cloth-config.gridless_crafting.category.stations"), btn -> switchTab(1))
                .bounds(centerX - 48, tabY, tabWidth, 20).build();
        Button avTab = Button.builder(Component.translatable("text.cloth-config.gridless_crafting.category.audio_visual"), btn -> switchTab(2))
                .bounds(centerX + 54, tabY, tabWidth, 20).build();

        genTab.active = (activeTab != 0);
        staTab.active = (activeTab != 1);
        avTab.active = (activeTab != 2);

        this.addRenderableWidget(genTab);
        this.addRenderableWidget(staTab);
        this.addRenderableWidget(avTab);

        int startY = 64;
        int spacing = 24;
        int btnWidth = 260;

        if (activeTab == 0) { // General
            // Enable Gridless Crafting
            this.addRenderableWidget(Button.builder(
                    getToggleText("text.cloth-config.gridless_crafting.option.enabled", GridlessConfig.general.enabled),
                    btn -> {
                        GridlessConfig.general.enabled = !GridlessConfig.general.enabled;
                        btn.setMessage(getToggleText("text.cloth-config.gridless_crafting.option.enabled", GridlessConfig.general.enabled));
                    }
            ).bounds(centerX - btnWidth / 2, startY, btnWidth, 20).build());

            // Replace Inventory Crafting
            this.addRenderableWidget(Button.builder(
                    getToggleText("text.cloth-config.gridless_crafting.option.replace_inventory_crafting", GridlessConfig.general.replace_inventory_crafting),
                    btn -> {
                        GridlessConfig.general.replace_inventory_crafting = !GridlessConfig.general.replace_inventory_crafting;
                        btn.setMessage(getToggleText("text.cloth-config.gridless_crafting.option.replace_inventory_crafting", GridlessConfig.general.replace_inventory_crafting));
                    }
            ).bounds(centerX - btnWidth / 2, startY + spacing, btnWidth, 20).build());

            // Remove 2x2 Crafting Grid
            this.addRenderableWidget(Button.builder(
                    getToggleText("text.cloth-config.gridless_crafting.option.remove_crafting_inventory", GridlessConfig.general.remove_crafting_inventory),
                    btn -> {
                        GridlessConfig.general.remove_crafting_inventory = !GridlessConfig.general.remove_crafting_inventory;
                        btn.setMessage(getToggleText("text.cloth-config.gridless_crafting.option.remove_crafting_inventory", GridlessConfig.general.remove_crafting_inventory));
                    }
            ).bounds(centerX - btnWidth / 2, startY + spacing * 2, btnWidth, 20).build());

            // Hide Uncraftable Recipes
            this.addRenderableWidget(Button.builder(
                    getToggleText("text.cloth-config.gridless_crafting.option.hide_uncraftable_recipes", GridlessConfig.general.hide_uncraftable_recipes),
                    btn -> {
                        GridlessConfig.general.hide_uncraftable_recipes = !GridlessConfig.general.hide_uncraftable_recipes;
                        btn.setMessage(getToggleText("text.cloth-config.gridless_crafting.option.hide_uncraftable_recipes", GridlessConfig.general.hide_uncraftable_recipes));
                    }
            ).bounds(centerX - btnWidth / 2, startY + spacing * 3, btnWidth, 20).build());
        } else if (activeTab == 1) { // Stations
            // Enable Crafting Table
            this.addRenderableWidget(Button.builder(
                    getToggleText("text.cloth-config.gridless_crafting.option.enable_workbench", GridlessConfig.stations.enable_crafting_table),
                    btn -> {
                        GridlessConfig.stations.enable_crafting_table = !GridlessConfig.stations.enable_crafting_table;
                        btn.setMessage(getToggleText("text.cloth-config.gridless_crafting.option.enable_workbench", GridlessConfig.stations.enable_crafting_table));
                    }
            ).bounds(centerX - btnWidth / 2, startY, btnWidth, 20).build());

            // Furnace Mode
            this.addRenderableWidget(Button.builder(
                    getModeText("text.cloth-config.gridless_crafting.option.furnace_mode", GridlessConfig.stations.furnace_mode),
                    btn -> {
                        GridlessConfig.stations.furnace_mode = "INSTANT_WITH_FUEL".equals(GridlessConfig.stations.furnace_mode) ? "TRADITIONAL" : "INSTANT_WITH_FUEL";
                        btn.setMessage(getModeText("text.cloth-config.gridless_crafting.option.furnace_mode", GridlessConfig.stations.furnace_mode));
                    }
            ).bounds(centerX - btnWidth / 2, startY + spacing, btnWidth, 20).build());

            // Blast Furnace Mode
            this.addRenderableWidget(Button.builder(
                    getModeText("text.cloth-config.gridless_crafting.option.blast_furnace_mode", GridlessConfig.stations.blast_furnace_mode),
                    btn -> {
                        GridlessConfig.stations.blast_furnace_mode = "INSTANT_WITH_FUEL".equals(GridlessConfig.stations.blast_furnace_mode) ? "TRADITIONAL" : "INSTANT_WITH_FUEL";
                        btn.setMessage(getModeText("text.cloth-config.gridless_crafting.option.blast_furnace_mode", GridlessConfig.stations.blast_furnace_mode));
                    }
            ).bounds(centerX - btnWidth / 2, startY + spacing * 2, btnWidth, 20).build());

            // Smoker Mode
            this.addRenderableWidget(Button.builder(
                    getModeText("text.cloth-config.gridless_crafting.option.smoker_mode", GridlessConfig.stations.smoker_mode),
                    btn -> {
                        GridlessConfig.stations.smoker_mode = "INSTANT_WITH_FUEL".equals(GridlessConfig.stations.smoker_mode) ? "TRADITIONAL" : "INSTANT_WITH_FUEL";
                        btn.setMessage(getModeText("text.cloth-config.gridless_crafting.option.smoker_mode", GridlessConfig.stations.smoker_mode));
                    }
            ).bounds(centerX - btnWidth / 2, startY + spacing * 3, btnWidth, 20).build());
        } else if (activeTab == 2) { // Audio & Visual
            // Custom Sounds
            this.addRenderableWidget(Button.builder(
                    getToggleText("text.cloth-config.gridless_crafting.option.enable_custom_sounds", GridlessConfig.audioVisual.enable_custom_sounds),
                    btn -> {
                        GridlessConfig.audioVisual.enable_custom_sounds = !GridlessConfig.audioVisual.enable_custom_sounds;
                        btn.setMessage(getToggleText("text.cloth-config.gridless_crafting.option.enable_custom_sounds", GridlessConfig.audioVisual.enable_custom_sounds));
                    }
            ).bounds(centerX - btnWidth / 2, startY, btnWidth, 20).build());
        }

        // Done / Save Button
        this.addRenderableWidget(Button.builder(
                CommonComponents.GUI_DONE,
                btn -> {
                    GridlessConfig.save();
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(this.parent);
                    }
                }
        ).bounds(centerX - 100, this.height - 28, 200, 20).build());
    }

    private void switchTab(int tab) {
        this.activeTab = tab;
        rebuildWidgets();
    }

    private Component getToggleText(String translationKey, boolean state) {
        return Component.translatable(translationKey)
                .append(": ")
                .append(state ? Component.literal("ON") : Component.literal("OFF"));
    }

    private Component getModeText(String translationKey, String mode) {
        return Component.translatable(translationKey)
                .append(": ")
                .append(Component.literal(mode));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        GridlessConfig.save();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}
