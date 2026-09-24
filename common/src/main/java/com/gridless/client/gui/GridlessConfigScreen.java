package com.gridless.client.gui;

import com.gridless.config.GridlessConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class GridlessConfigScreen extends Screen {
    public enum Tab {
        GENERAL("text.cloth-config.gridless_crafting.category.general"),
        STATIONS("text.cloth-config.gridless_crafting.category.stations"),
        AUDIO_VISUAL("text.cloth-config.gridless_crafting.category.audio_visual");

        private final String key;

        Tab(String key) {
            this.key = key;
        }

        public Component getTitle() {
            return Component.translatable(key);
        }
    }

    private final Screen lastScreen;
    private Tab activeTab = Tab.GENERAL;

    private final List<AbstractWidget> generalWidgets = new ArrayList<>();
    private final List<AbstractWidget> stationsWidgets = new ArrayList<>();
    private final List<AbstractWidget> audioVisualWidgets = new ArrayList<>();
    private final List<Button> tabButtons = new ArrayList<>();

    public GridlessConfigScreen(Screen lastScreen) {
        super(Component.translatable("text.cloth-config.gridless_crafting.title"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        generalWidgets.clear();
        stationsWidgets.clear();
        audioVisualWidgets.clear();
        tabButtons.clear();

        int centerX = this.width / 2;
        int tabY = 32;
        int tabWidth = 105;
        int tabHeight = 20;

        // --- Create Tab Buttons ---
        Tab[] tabs = Tab.values();
        int totalTabsWidth = tabs.length * tabWidth + (tabs.length - 1) * 4;
        int startTabX = centerX - totalTabsWidth / 2;

        for (int i = 0; i < tabs.length; i++) {
            Tab tab = tabs[i];
            int tabX = startTabX + i * (tabWidth + 4);
            Button btn = Button.builder(tab.getTitle(), button -> selectTab(tab))
                    .bounds(tabX, tabY, tabWidth, tabHeight)
                    .build();
            tabButtons.add(btn);
            this.addRenderableWidget(btn);
        }

        int contentStartY = 64;
        int buttonWidth = 240;
        int buttonHeight = 20;
        int spacing = 24;

        // --- 1. General Category Widgets ---
        int y = contentStartY;
        generalWidgets.add(this.addRenderableWidget(CycleButton.onOffBuilder(GridlessConfig.general.enabled)
                .create(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight,
                        Component.translatable("text.cloth-config.gridless_crafting.option.enabled"),
                        (button, value) -> GridlessConfig.general.enabled = value)));
        y += spacing;

        generalWidgets.add(this.addRenderableWidget(CycleButton.onOffBuilder(GridlessConfig.general.replace_inventory_crafting)
                .create(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight,
                        Component.translatable("text.cloth-config.gridless_crafting.option.replace_inventory_crafting"),
                        (button, value) -> GridlessConfig.general.replace_inventory_crafting = value)));
        y += spacing;

        generalWidgets.add(this.addRenderableWidget(CycleButton.onOffBuilder(GridlessConfig.general.remove_crafting_inventory)
                .create(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight,
                        Component.translatable("text.cloth-config.gridless_crafting.option.remove_crafting_inventory"),
                        (button, value) -> GridlessConfig.general.remove_crafting_inventory = value)));
        y += spacing;

        generalWidgets.add(this.addRenderableWidget(CycleButton.onOffBuilder(GridlessConfig.general.hide_uncraftable_recipes)
                .create(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight,
                        Component.translatable("text.cloth-config.gridless_crafting.option.hide_uncraftable_recipes"),
                        (button, value) -> GridlessConfig.general.hide_uncraftable_recipes = value)));


        // --- 2. Stations Category Widgets ---
        y = contentStartY;
        stationsWidgets.add(this.addRenderableWidget(CycleButton.onOffBuilder(GridlessConfig.stations.enable_crafting_table)
                .create(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight,
                        Component.translatable("text.cloth-config.gridless_crafting.option.enable_workbench"),
                        (button, value) -> GridlessConfig.stations.enable_crafting_table = value)));
        y += spacing;

        stationsWidgets.add(this.addRenderableWidget(CycleButton.<String>builder(Component::literal, GridlessConfig.stations.furnace_mode)
                .withValues("INSTANT_WITH_FUEL", "TRADITIONAL")
                .create(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight,
                        Component.translatable("text.cloth-config.gridless_crafting.option.furnace_mode"),
                        (button, value) -> GridlessConfig.stations.furnace_mode = value)));
        y += spacing;

        stationsWidgets.add(this.addRenderableWidget(CycleButton.<String>builder(Component::literal, GridlessConfig.stations.smoker_mode)
                .withValues("INSTANT_WITH_FUEL", "TRADITIONAL")
                .create(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight,
                        Component.translatable("text.cloth-config.gridless_crafting.option.smoker_mode"),
                        (button, value) -> GridlessConfig.stations.smoker_mode = value)));
        y += spacing;

        stationsWidgets.add(this.addRenderableWidget(CycleButton.<String>builder(Component::literal, GridlessConfig.stations.blast_furnace_mode)
                .withValues("INSTANT_WITH_FUEL", "TRADITIONAL")
                .create(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight,
                        Component.translatable("text.cloth-config.gridless_crafting.option.blast_furnace_mode"),
                        (button, value) -> GridlessConfig.stations.blast_furnace_mode = value)));


        // --- 3. Audio & Visual Category Widgets ---
        y = contentStartY;
        audioVisualWidgets.add(this.addRenderableWidget(CycleButton.onOffBuilder(GridlessConfig.audioVisual.enable_custom_sounds)
                .create(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight,
                        Component.translatable("text.cloth-config.gridless_crafting.option.enable_custom_sounds"),
                        (button, value) -> GridlessConfig.audioVisual.enable_custom_sounds = value)));
        y += spacing;

        // --- Done Button ---
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> {
            GridlessConfig.save();
            if (this.minecraft != null) {
                this.minecraft.setScreen(this.lastScreen);
            }
        }).bounds(centerX - 100, this.height - 28, 200, 20).build());

        selectTab(activeTab);
    }

    private void selectTab(Tab tab) {
        this.activeTab = tab;

        for (int i = 0; i < Tab.values().length; i++) {
            if (i < tabButtons.size()) {
                tabButtons.get(i).active = (Tab.values()[i] != tab);
            }
        }

        for (AbstractWidget w : generalWidgets) {
            w.visible = (tab == Tab.GENERAL);
        }
        for (AbstractWidget w : stationsWidgets) {
            w.visible = (tab == Tab.STATIONS);
        }
        for (AbstractWidget w : audioVisualWidgets) {
            w.visible = (tab == Tab.AUDIO_VISUAL);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        GridlessConfig.save();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.lastScreen);
        }
    }
}
