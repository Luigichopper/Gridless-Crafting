package com.gridless.client.gui;

import com.gridless.config.GridlessConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class GridlessConfigScreen extends Screen {
    public enum Tab {
        GENERAL("text.cloth-config.gridless_crafting.category.general", "General behavior and inventory screen overhaul"),
        STATIONS("text.cloth-config.gridless_crafting.category.stations", "Station overrides and instant smelting modes"),
        AUDIO_VISUAL("text.cloth-config.gridless_crafting.category.audio_visual", "Audio effects and visual appearance");

        private final String key;
        private final String description;

        Tab(String key, String description) {
            this.key = key;
            this.description = description;
        }

        public Component getTitle() {
            return Component.translatable(key);
        }

        public String getDescription() {
            return description;
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
        int tabY = 28;
        int tabWidth = 110;
        int tabHeight = 20;

        // --- Create Category Tab Buttons ---
        Tab[] tabs = Tab.values();
        int totalTabsWidth = tabs.length * tabWidth + (tabs.length - 1) * 6;
        int startTabX = centerX - totalTabsWidth / 2;

        for (int i = 0; i < tabs.length; i++) {
            Tab tab = tabs[i];
            int tabX = startTabX + i * (tabWidth + 6);
            Button btn = Button.builder(tab.getTitle(), button -> selectTab(tab))
                    .bounds(tabX, tabY, tabWidth, tabHeight)
                    .build();
            tabButtons.add(btn);
            this.addRenderableWidget(btn);
        }

        int contentStartY = 76;
        int buttonWidth = 280;
        int buttonHeight = 20;
        int spacing = 24;

        // --- 1. General Category Widgets ---
        int y = contentStartY;
        generalWidgets.add(this.addRenderableWidget(CycleButton.onOffBuilder(GridlessConfig.general.enabled)
                .withTooltip(val -> Tooltip.create(Component.translatable("text.cloth-config.gridless_crafting.option.enabled.tooltip")))
                .create(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight,
                        Component.translatable("text.cloth-config.gridless_crafting.option.enabled"),
                        (button, value) -> GridlessConfig.general.enabled = value)));
        y += spacing;

        generalWidgets.add(this.addRenderableWidget(CycleButton.onOffBuilder(GridlessConfig.general.replace_inventory_crafting)
                .withTooltip(val -> Tooltip.create(Component.translatable("text.cloth-config.gridless_crafting.option.replace_inventory_crafting.tooltip")))
                .create(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight,
                        Component.translatable("text.cloth-config.gridless_crafting.option.replace_inventory_crafting"),
                        (button, value) -> GridlessConfig.general.replace_inventory_crafting = value)));
        y += spacing;

        generalWidgets.add(this.addRenderableWidget(CycleButton.onOffBuilder(GridlessConfig.general.remove_crafting_inventory)
                .withTooltip(val -> Tooltip.create(Component.translatable("text.cloth-config.gridless_crafting.option.remove_crafting_inventory.tooltip")))
                .create(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight,
                        Component.translatable("text.cloth-config.gridless_crafting.option.remove_crafting_inventory"),
                        (button, value) -> GridlessConfig.general.remove_crafting_inventory = value)));
        y += spacing;

        generalWidgets.add(this.addRenderableWidget(CycleButton.onOffBuilder(GridlessConfig.general.hide_uncraftable_recipes)
                .withTooltip(val -> Tooltip.create(Component.translatable("text.cloth-config.gridless_crafting.option.hide_uncraftable_recipes.tooltip")))
                .create(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight,
                        Component.translatable("text.cloth-config.gridless_crafting.option.hide_uncraftable_recipes"),
                        (button, value) -> GridlessConfig.general.hide_uncraftable_recipes = value)));

        // --- 2. Stations Category Widgets ---
        y = contentStartY;
        stationsWidgets.add(this.addRenderableWidget(CycleButton.onOffBuilder(GridlessConfig.stations.enable_crafting_table)
                .withTooltip(val -> Tooltip.create(Component.translatable("text.cloth-config.gridless_crafting.option.enable_workbench.tooltip")))
                .create(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight,
                        Component.translatable("text.cloth-config.gridless_crafting.option.enable_workbench"),
                        (button, value) -> GridlessConfig.stations.enable_crafting_table = value)));
        y += spacing;

        stationsWidgets.add(this.addRenderableWidget(CycleButton.<String>builder(GridlessConfigScreen::getModeDisplay)
                .withValues("INSTANT_WITH_FUEL", "TRADITIONAL")
                .withInitialValue(GridlessConfig.stations.furnace_mode)
                .withTooltip(val -> Tooltip.create(Component.translatable("text.cloth-config.gridless_crafting.option.furnace_mode.tooltip")))
                .create(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight,
                        Component.translatable("text.cloth-config.gridless_crafting.option.furnace_mode"),
                        (button, value) -> GridlessConfig.stations.furnace_mode = value)));
        y += spacing;

        stationsWidgets.add(this.addRenderableWidget(CycleButton.<String>builder(GridlessConfigScreen::getModeDisplay)
                .withValues("INSTANT_WITH_FUEL", "TRADITIONAL")
                .withInitialValue(GridlessConfig.stations.smoker_mode)
                .withTooltip(val -> Tooltip.create(Component.translatable("text.cloth-config.gridless_crafting.option.smoker_mode.tooltip")))
                .create(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight,
                        Component.translatable("text.cloth-config.gridless_crafting.option.smoker_mode"),
                        (button, value) -> GridlessConfig.stations.smoker_mode = value)));
        y += spacing;

        stationsWidgets.add(this.addRenderableWidget(CycleButton.<String>builder(GridlessConfigScreen::getModeDisplay)
                .withValues("INSTANT_WITH_FUEL", "TRADITIONAL")
                .withInitialValue(GridlessConfig.stations.blast_furnace_mode)
                .withTooltip(val -> Tooltip.create(Component.translatable("text.cloth-config.gridless_crafting.option.blast_furnace_mode.tooltip")))
                .create(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight,
                        Component.translatable("text.cloth-config.gridless_crafting.option.blast_furnace_mode"),
                        (button, value) -> GridlessConfig.stations.blast_furnace_mode = value)));

        // --- 3. Audio & Visual Category Widgets ---
        y = contentStartY;
        audioVisualWidgets.add(this.addRenderableWidget(CycleButton.onOffBuilder(GridlessConfig.audioVisual.enable_custom_sounds)
                .withTooltip(val -> Tooltip.create(Component.translatable("text.cloth-config.gridless_crafting.option.enable_custom_sounds.tooltip")))
                .create(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight,
                        Component.translatable("text.cloth-config.gridless_crafting.option.enable_custom_sounds"),
                        (button, value) -> GridlessConfig.audioVisual.enable_custom_sounds = value)));
        y += spacing;

        audioVisualWidgets.add(this.addRenderableWidget(CycleButton.<Integer>builder(val -> Component.literal(val + " Rows"))
                .withValues(4, 5, 6, 7, 8, 9, 10)
                .withInitialValue(GridlessConfig.audioVisual.visible_recipe_rows)
                .withTooltip(val -> Tooltip.create(Component.translatable("text.cloth-config.gridless_crafting.option.visible_recipe_rows.tooltip")))
                .create(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight,
                        Component.translatable("text.cloth-config.gridless_crafting.option.visible_recipe_rows"),
                        (button, value) -> GridlessConfig.audioVisual.visible_recipe_rows = value)));

        // --- Bottom Action Buttons ---
        int bottomButtonWidth = 135;
        this.addRenderableWidget(Button.builder(Component.translatable("controls.reset"), button -> resetToDefaults())
                .bounds(centerX - bottomButtonWidth - 5, this.height - 28, bottomButtonWidth, 20)
                .tooltip(Tooltip.create(Component.literal("Reset all Gridless settings to their defaults.")))
                .build());

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(centerX + 5, this.height - 28, bottomButtonWidth, 20)
                .build());

        selectTab(activeTab);
    }

    private static Component getModeDisplay(String mode) {
        if ("INSTANT_WITH_FUEL".equalsIgnoreCase(mode)) {
            return Component.literal("Instant (Fuel)").withStyle(ChatFormatting.GREEN);
        }
        return Component.literal("Vanilla (Slow)").withStyle(ChatFormatting.GRAY);
    }

    private void resetToDefaults() {
        GridlessConfig.general = new GridlessConfig.General();
        GridlessConfig.stations = new GridlessConfig.Stations();
        GridlessConfig.audioVisual = new GridlessConfig.AudioVisual();
        GridlessConfig.save();
        this.rebuildWidgets();
    }

    private void selectTab(Tab tab) {
        this.activeTab = tab;

        for (int i = 0; i < Tab.values().length; i++) {
            if (i < tabButtons.size()) {
                Tab t = Tab.values()[i];
                Button b = tabButtons.get(i);
                boolean selected = (t == tab);
                b.setMessage(selected ? Component.literal("▶ ").append(t.getTitle()).withStyle(ChatFormatting.YELLOW) : t.getTitle());
            }
        }

        for (AbstractWidget w : generalWidgets) {
            w.visible = (tab == Tab.GENERAL);
            w.active = (tab == Tab.GENERAL);
        }
        for (AbstractWidget w : stationsWidgets) {
            w.visible = (tab == Tab.STATIONS);
            w.active = (tab == Tab.STATIONS);
        }
        for (AbstractWidget w : audioVisualWidgets) {
            w.visible = (tab == Tab.AUDIO_VISUAL);
            w.active = (tab == Tab.AUDIO_VISUAL);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFFFF);
        guiGraphics.drawCenteredString(this.font, Component.literal(this.activeTab.getDescription()).withStyle(ChatFormatting.GRAY), this.width / 2, 54, 0xFFA0A0A0);
    }

    @Override
    public void onClose() {
        GridlessConfig.save();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.lastScreen);
        }
    }
}
