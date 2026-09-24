package com.gridless.client.screen.widget;

import com.gridless.api.recipe.RecipeCategory;
import com.gridless.sound.GridlessSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class CategoryTabWidget extends AbstractWidget {
    private static final RecipeCategory[] CATEGORIES = RecipeCategory.values();
    private static final int COLS = 4;
    private static final int ROWS = 2;

    private final Consumer<RecipeCategory> onCategoryChanged;
    private RecipeCategory selectedCategory = RecipeCategory.ALL;

    public CategoryTabWidget(int x, int y, int width, int height, Consumer<RecipeCategory> onCategoryChanged) {
        super(x, y, width, height, Component.empty());
        this.onCategoryChanged = onCategoryChanged;
    }

    public RecipeCategory getSelectedCategory() {
        return selectedCategory;
    }

    public void setSelectedCategory(RecipeCategory category) {
        if (this.selectedCategory != category) {
            this.selectedCategory = category;
            this.onCategoryChanged.accept(category);
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(GridlessSounds.RECIPE_SELECT, 1.0f));
        }
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        int tabWidth = this.width / COLS;
        int tabHeight = this.height / ROWS;

        for (int i = 0; i < CATEGORIES.length; i++) {
            RecipeCategory cat = CATEGORIES[i];
            int col = i % COLS;
            int row = i / COLS;

            int tabX = this.getX() + col * tabWidth;
            int tabY = this.getY() + row * tabHeight;
            int currentTabWidth = (col == COLS - 1) ? (this.width - (COLS - 1) * tabWidth) : tabWidth;

            boolean isSelected = cat == selectedCategory;
            boolean isHovered = mouseX >= tabX && mouseX < tabX + currentTabWidth && mouseY >= tabY && mouseY < tabY + tabHeight;

            int bgColor = isSelected ? 0xFF2D4A3E : (isHovered ? 0xFF3E3E3E : 0xFF222222);
            int borderColor = isSelected ? 0xFF55FF55 : (isHovered ? 0xFF666666 : 0xFF333333);

            // Tab background & border
            graphics.fill(tabX, tabY, tabX + currentTabWidth, tabY + tabHeight, bgColor);
            graphics.outline(tabX, tabY, currentTabWidth, tabHeight, borderColor);

            // Tab label
            String label = getCategoryLabel(cat);
            int textX = tabX + (currentTabWidth - font.width(label)) / 2;
            int textY = tabY + (tabHeight - 8) / 2;
            int textColor = isSelected ? 0xFFFFFFFF : (isHovered ? 0xFFDDDDDD : 0xFF888888);
            graphics.text(font, label, textX, textY, textColor, false);
        }
    }

    public void renderTabTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!this.isHovered) return;

        Font font = Minecraft.getInstance().font;
        int tabWidth = this.width / COLS;
        int tabHeight = this.height / ROWS;

        int col = (mouseX - this.getX()) / tabWidth;
        int row = (mouseY - this.getY()) / tabHeight;

        if (col >= 0 && col < COLS && row >= 0 && row < ROWS) {
            int index = row * COLS + col;
            if (index < CATEGORIES.length) {
                Component tooltip = getCategoryTooltip(CATEGORIES[index]);
                graphics.setTooltipForNextFrame(font, tooltip, mouseX, mouseY);
            }
        }
    }

    private String getCategoryLabel(RecipeCategory cat) {
        return switch (cat) {
            case ALL -> Component.translatable("category.gridless.all").getString();
            case WEAPONS -> Component.translatable("category.gridless.weapons").getString();
            case TOOLS -> Component.translatable("category.gridless.tools").getString();
            case ARMOR -> Component.translatable("category.gridless.armor").getString();
            case BUILDING -> Component.translatable("category.gridless.building").getString();
            case REDSTONE -> Component.translatable("category.gridless.redstone").getString();
            case CONSUMABLES -> Component.translatable("category.gridless.consumables").getString();
            case MISC -> Component.translatable("category.gridless.misc").getString();
        };
    }

    private Component getCategoryTooltip(RecipeCategory cat) {
        return switch (cat) {
            case ALL -> Component.translatable("category.gridless.all.desc");
            case WEAPONS -> Component.translatable("category.gridless.weapons.desc");
            case TOOLS -> Component.translatable("category.gridless.tools.desc");
            case ARMOR -> Component.translatable("category.gridless.armor.desc");
            case BUILDING -> Component.translatable("category.gridless.building.desc");
            case REDSTONE -> Component.translatable("category.gridless.redstone.desc");
            case CONSUMABLES -> Component.translatable("category.gridless.consumables.desc");
            case MISC -> Component.translatable("category.gridless.misc.desc");
        };
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && this.isHovered) {
            int tabWidth = this.width / COLS;
            int tabHeight = this.height / ROWS;

            int col = (int) ((event.x() - this.getX()) / tabWidth);
            int row = (int) ((event.y() - this.getY()) / tabHeight);

            if (col >= 0 && col < COLS && row >= 0 && row < ROWS) {
                int index = row * COLS + col;
                if (index < CATEGORIES.length) {
                    setSelectedCategory(CATEGORIES[index]);
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }
}
