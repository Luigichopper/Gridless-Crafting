package com.gridless.client.screen.widget;

import com.gridless.api.recipe.GridlessRecipe;
import com.gridless.api.recipe.IngredientBag;
import com.gridless.sound.GridlessSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class RecipeRibbonWidget extends AbstractWidget {
    private static final int ITEM_SIZE = 22;
    private final Consumer<GridlessRecipe> onRecipeSelected;
    private List<GridlessRecipe> recipes = new ArrayList<>();
    private IngredientBag currentBag = new IngredientBag();
    private GridlessRecipe selectedRecipe = null;
    private double scrollOffset = 0;
    private boolean isDraggingScrollbar = false;

    public RecipeRibbonWidget(int x, int y, int width, int height, Consumer<GridlessRecipe> onRecipeSelected) {
        super(x, y, width, height, Component.empty());
        this.onRecipeSelected = onRecipeSelected;
    }

    public void setRecipes(List<GridlessRecipe> recipes, IngredientBag bag) {
        this.recipes = recipes;
        this.currentBag = bag;
        this.scrollOffset = Mth.clamp(this.scrollOffset, 0, getMaxScroll());
        if (selectedRecipe != null && !recipes.contains(selectedRecipe)) {
            selectedRecipe = recipes.isEmpty() ? null : recipes.get(0);
            onRecipeSelected.accept(selectedRecipe);
        } else if (selectedRecipe == null && !recipes.isEmpty()) {
            selectedRecipe = recipes.get(0);
            onRecipeSelected.accept(selectedRecipe);
        }
    }

    public GridlessRecipe getSelectedRecipe() {
        return selectedRecipe;
    }

    public void setSelectedRecipe(GridlessRecipe recipe) {
        this.selectedRecipe = recipe;
        onRecipeSelected.accept(recipe);
    }

    private int getMaxScroll() {
        int contentHeight = recipes.size() * ITEM_SIZE;
        return Math.max(0, contentHeight - this.height);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Background panel
        graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0xAA141414);
        graphics.outline(this.getX(), this.getY(), this.width, this.height, 0xFF333333);

        int maxScroll = getMaxScroll();
        int startIndex = (int) (scrollOffset / ITEM_SIZE);
        int visibleCount = (this.height / ITEM_SIZE) + 2;
        int endIndex = Math.min(recipes.size(), startIndex + visibleCount);

        graphics.enableScissor(this.getX(), this.getY(), this.getX() + this.width - 6, this.getY() + this.height);

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        for (int i = startIndex; i < endIndex; i++) {
            GridlessRecipe recipe = recipes.get(i);
            int itemY = (int) (this.getY() + i * ITEM_SIZE - scrollOffset);
            int itemX = this.getX() + 2;
            int itemWidth = this.width - 8;

            boolean isHovered = mouseX >= itemX && mouseX < itemX + itemWidth && mouseY >= itemY && mouseY < itemY + ITEM_SIZE && mouseY >= this.getY() && mouseY < this.getY() + this.height;
            boolean isSelected = recipe == selectedRecipe;
            boolean isCraftable = recipe.isCraftable(currentBag);

            // Item slot background
            int slotBg = isSelected ? 0xFF2E4057 : (isHovered ? 0xFF353535 : 0xFF222222);
            int border = isSelected ? 0xFFF4D06F : (isCraftable ? 0xFF3D5A80 : 0xFF2A2A2A);

            graphics.fill(itemX, itemY, itemX + itemWidth, itemY + ITEM_SIZE - 2, slotBg);
            graphics.outline(itemX, itemY, itemWidth, ITEM_SIZE - 2, border);

            // Render Output ItemStack
            ItemStack resultStack = recipe.getResult();
            graphics.item(resultStack, itemX + 3, itemY + 2);
            graphics.itemDecorations(font, resultStack, itemX + 3, itemY + 2);

            // If not craftable, render a darkened overlay over the item
            if (!isCraftable) {
                graphics.fill(itemX + 2, itemY + 1, itemX + 20, itemY + 19, 0x88000000);
            }

            // Draw item name text
            int maxTextWidth = itemWidth - 26;
            if (recipe.getVariantCount() > 1) {
                maxTextWidth -= 18;
            }
            String itemName = resultStack.getHoverName().getString();
            if (font.width(itemName) > maxTextWidth) {
                itemName = font.plainSubstrByWidth(itemName, maxTextWidth - 6) + "..";
            }
            int textColor = isCraftable ? (isSelected ? 0xFFFFFFFF : 0xFFE0E0E0) : 0xFF777777;
            graphics.text(font, itemName, itemX + 24, itemY + 6, textColor, false);

            if (recipe.getVariantCount() > 1) {
                String varBadge = "(" + recipe.getVariantCount() + ")";
                graphics.text(font, varBadge, itemX + itemWidth - font.width(varBadge) - 2, itemY + 6, 0xFFFFA726, false);
            }
        }

        graphics.disableScissor();

        // Render scrollbar
        if (maxScroll > 0) {
            int scrollbarX = this.getX() + this.width - 5;
            int scrollbarHeight = Math.max(12, (int) ((float) this.height / (recipes.size() * ITEM_SIZE) * this.height));
            int scrollbarY = (int) (this.getY() + (scrollOffset / maxScroll) * (this.height - scrollbarHeight));

            graphics.fill(scrollbarX, this.getY(), scrollbarX + 4, this.getY() + this.height, 0xFF1A1A1A);
            graphics.fill(scrollbarX, scrollbarY, scrollbarX + 4, scrollbarY + scrollbarHeight, isDraggingScrollbar ? 0xFFF4D06F : 0xFF555555);
        }
    }

    public void renderRecipeTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!this.isHovered) return;
        int startIndex = (int) (scrollOffset / ITEM_SIZE);
        int visibleCount = (this.height / ITEM_SIZE) + 2;
        int endIndex = Math.min(recipes.size(), startIndex + visibleCount);

        for (int i = startIndex; i < endIndex; i++) {
            int itemY = (int) (this.getY() + i * ITEM_SIZE - scrollOffset);
            int itemX = this.getX() + 2;
            int itemWidth = this.width - 8;

            if (mouseX >= itemX && mouseX < itemX + itemWidth && mouseY >= itemY && mouseY < itemY + ITEM_SIZE && mouseY >= this.getY() && mouseY < this.getY() + this.height) {
                GridlessRecipe recipe = recipes.get(i);
                graphics.setTooltipForNextFrame(Minecraft.getInstance().font, recipe.getResult(), mouseX, mouseY);
                break;
            }
        }
    }

    public boolean isDraggingScrollbar() {
        return this.isDraggingScrollbar;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!this.isHovered) return false;

        // Check scrollbar click
        int scrollbarX = this.getX() + this.width - 8;
        if (event.x() >= scrollbarX && event.x() <= this.getX() + this.width + 4 && event.y() >= this.getY() && event.y() <= this.getY() + this.height) {
            this.isDraggingScrollbar = true;
            if (getMaxScroll() > 0) {
                double scrollRatio = (event.y() - this.getY()) / (double) this.height;
                this.scrollOffset = Mth.clamp(scrollRatio * getMaxScroll(), 0, getMaxScroll());
            }
            return true;
        }

        // Check recipe slot click
        int clickedY = (int) (event.y() - this.getY() + scrollOffset);
        int index = clickedY / ITEM_SIZE;
        if (index >= 0 && index < recipes.size()) {
            GridlessRecipe clicked = recipes.get(index);
            setSelectedRecipe(clicked);
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(GridlessSounds.RECIPE_SELECT, 1.0f));
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.isDraggingScrollbar = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.isDraggingScrollbar && getMaxScroll() > 0) {
            double scrollRatio = (event.y() - this.getY()) / (double) this.height;
            this.scrollOffset = Mth.clamp(scrollRatio * getMaxScroll(), 0, getMaxScroll());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.isHovered) {
            this.scrollOffset = Mth.clamp(this.scrollOffset - scrollY * ITEM_SIZE, 0, getMaxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }
}
