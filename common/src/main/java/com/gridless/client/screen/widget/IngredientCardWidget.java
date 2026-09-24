package com.gridless.client.screen.widget;

import com.gridless.api.recipe.CountedIngredient;
import com.gridless.api.recipe.GridlessRecipe;
import com.gridless.api.recipe.IngredientBag;
import com.gridless.api.smelting.SmeltingCalculator;
import com.gridless.api.station.StationMode;
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

import java.util.Map;
import java.util.function.BiConsumer;

public class IngredientCardWidget extends AbstractWidget {
    private static final int ROW_HEIGHT = 15;
    private final BiConsumer<GridlessRecipe, Integer> onCraftRequested;
    private GridlessRecipe recipe = null;
    private IngredientBag bag = new IngredientBag();
    private StationMode mode = StationMode.NORMAL;

    // Scrolling state
    private double scrollOffset = 0;
    private boolean isDraggingScrollbar = false;

    // Variant auto-cycle
    private int variantCycleTicks = 0;

    // Rapid craft timer
    private boolean isHoldingCraft = false;
    private int holdTicks = 0;

    public IngredientCardWidget(int x, int y, int width, int height, BiConsumer<GridlessRecipe, Integer> onCraftRequested) {
        super(x, y, width, height, Component.empty());
        this.onCraftRequested = onCraftRequested;
    }

    public void setRecipe(GridlessRecipe recipe, IngredientBag bag, StationMode mode) {
        if (this.recipe != recipe) {
            this.scrollOffset = 0;
            this.variantCycleTicks = 0;
        }
        this.recipe = recipe;
        this.bag = bag;
        this.mode = mode;
        if (this.recipe != null) {
            this.recipe.autoSelectBestVariant(bag);
        }
    }

    public void tick() {
        if (recipe != null && recipe.getVariantCount() > 1) {
            variantCycleTicks++;
            // Auto cycle every 60 ticks (3 seconds) if not hovered
            if (variantCycleTicks >= 60 && !this.isHovered) {
                recipe.cycleVariant(1);
                variantCycleTicks = 0;
            }
        }

        if (isHoldingCraft && recipe != null && recipe.isCraftable(bag)) {
            holdTicks++;
            if (holdTicks > 10 && holdTicks % 4 == 0) {
                onCraftRequested.accept(recipe, 1);
            }
        } else {
            holdTicks = 0;
        }
    }

    private int getViewportY() {
        return this.getY() + 37;
    }

    private int getViewportHeight() {
        return 62;
    }

    private int getTotalRows() {
        if (recipe == null) return 0;
        return recipe.getInputs().size() + (mode == StationMode.SMELTING ? 1 : 0);
    }

    private int getMaxScroll() {
        int contentHeight = getTotalRows() * ROW_HEIGHT;
        return Math.max(0, contentHeight - getViewportHeight());
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Panel background
        graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0xCC181818);
        graphics.outline(this.getX(), this.getY(), this.width, this.height, 0xFF3E3E3E);

        if (recipe == null) {
            Font font = Minecraft.getInstance().font;
            String text = Component.translatable("gui.gridless.select_recipe").getString();
            graphics.text(font, text, this.getX() + (this.width - font.width(text)) / 2, this.getY() + this.height / 2 - 4, 0xFF777777, false);
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        ItemStack output = recipe.getResult();

        // 1. Output Header Banner
        int headerY = this.getY() + 4;
        int headerHeight = 22;
        graphics.fill(this.getX() + 4, headerY, this.getX() + this.width - 4, headerY + headerHeight, 0xFF2A2A2A);
        graphics.outline(this.getX() + 4, headerY, this.width - 8, headerHeight, 0xFF4A4A4A);

        graphics.item(output, this.getX() + 7, headerY + 3);
        graphics.itemDecorations(font, output, this.getX() + 7, headerY + 3);

        int variantCount = recipe.getVariantCount();
        int maxTitleWidth = this.width - (variantCount > 1 ? 86 : 38);
        String title = output.getHoverName().getString() + " (x" + output.getCount() + ")";
        if (font.width(title) > maxTitleWidth) {
            title = font.plainSubstrByWidth(title, maxTitleWidth - 6) + "..";
        }
        graphics.text(font, title, this.getX() + 27, headerY + 7, 0xFFFFFFFF, false);

        // Variant selector (< 1/3 >) if recipe has multiple variants
        if (variantCount > 1) {
            int currentVariant = recipe.getActiveVariantIndex() + 1;
            int btnLeftX = this.getX() + this.width - 56;
            int btnRightX = this.getX() + this.width - 18;
            int btnArrowY = headerY + 4;

            boolean hoverLeft = mouseX >= btnLeftX && mouseX < btnLeftX + 11 && mouseY >= btnArrowY && mouseY < btnArrowY + 14;
            boolean hoverRight = mouseX >= btnRightX && mouseX < btnRightX + 11 && mouseY >= btnArrowY && mouseY < btnArrowY + 14;

            graphics.fill(btnLeftX, btnArrowY, btnLeftX + 11, btnArrowY + 14, hoverLeft ? 0xFF444444 : 0xFF333333);
            graphics.outline(btnLeftX, btnArrowY, 11, 14, hoverLeft ? 0xFFFFA726 : 0xFF555555);
            graphics.text(font, "<", btnLeftX + 3, btnArrowY + 3, hoverLeft ? 0xFFFFA726 : 0xFFAAAAAA, false);

            String varStr = currentVariant + "/" + variantCount;
            graphics.text(font, varStr, btnLeftX + 13 + (24 - font.width(varStr)) / 2, btnArrowY + 3, 0xFFFFA726, false);

            graphics.fill(btnRightX, btnArrowY, btnRightX + 11, btnArrowY + 14, hoverRight ? 0xFF444444 : 0xFF333333);
            graphics.outline(btnRightX, btnArrowY, 11, 14, hoverRight ? 0xFFFFA726 : 0xFF555555);
            graphics.text(font, ">", btnRightX + 3, btnArrowY + 3, hoverRight ? 0xFFFFA726 : 0xFFAAAAAA, false);
        }

        // 2. Ingredients Section Header
        int listHeaderY = this.getY() + 27;
        graphics.text(font, Component.translatable("gui.gridless.required_materials").getString(), this.getX() + 6, listHeaderY, 0xFFAAAAAA, false);

        // 3. Scrollable Ingredients Viewport
        int viewportY = getViewportY();
        int viewportHeight = getViewportHeight();
        int maxScroll = getMaxScroll();
        this.scrollOffset = Mth.clamp(this.scrollOffset, 0, maxScroll);

        int rightClip = maxScroll > 0 ? this.width - 9 : this.width - 5;
        graphics.enableScissor(this.getX() + 4, viewportY, this.getX() + rightClip, viewportY + viewportHeight);

        Map<CountedIngredient, Integer> missingMap = recipe.getMissingIngredients(bag);
        int rowIndex = 0;

        for (CountedIngredient input : recipe.getInputs()) {
            int rowY = (int) (viewportY + rowIndex * ROW_HEIGHT - scrollOffset);
            rowIndex++;

            if (rowY + ROW_HEIGHT < viewportY || rowY > viewportY + viewportHeight) {
                continue;
            }

            ItemStack[] matching = input.getMatchingStacks();
            ItemStack sample = matching.length > 0 ? matching[(int) ((System.currentTimeMillis() / 1000) % matching.length)] : ItemStack.EMPTY;

            int have = bag.countMatching(input.getIngredient());
            int need = input.getCount();
            boolean hasEnough = have >= need;

            ItemStack displayStack = sample.copy();
            displayStack.setCount(need);
            graphics.item(displayStack, this.getX() + 6, rowY - 1);
            if (need > 1) {
                graphics.itemDecorations(font, displayStack, this.getX() + 6, rowY - 1);
            }

            String ingName = sample.isEmpty() ? "Unknown" : sample.getHoverName().getString();
            if (need > 1) {
                ingName += " x" + need;
            }
            int maxNameWidth = this.width - (maxScroll > 0 ? 90 : 80);
            if (font.width(ingName) > maxNameWidth) {
                ingName = font.plainSubstrByWidth(ingName, maxNameWidth - 6) + "..";
            }

            graphics.text(font, ingName, this.getX() + 24, rowY + 3, hasEnough ? 0xFFDDDDDD : 0xFFAAAAAA, false);

            String countStr;
            int countColor;
            if (hasEnough) {
                countStr = have + "/" + need;
                countColor = 0xFF55FF55; // Green
            } else {
                int missing = missingMap.getOrDefault(input, need - have);
                countStr = have + "/" + need + " (-" + missing + ")";
                countColor = 0xFFFF5555; // Red
            }

            int countWidth = font.width(countStr);
            graphics.text(font, countStr, this.getX() + rightClip - countWidth, rowY + 3, countColor, false);
        }

        // Smelting Fuel Row inside the viewport
        if (mode == StationMode.SMELTING && mc.player != null) {
            int rowY = (int) (viewportY + rowIndex * ROW_HEIGHT - scrollOffset);
            if (rowY + ROW_HEIGHT >= viewportY && rowY <= viewportY + viewportHeight) {
                SmeltingCalculator.FuelCost fuelCost = SmeltingCalculator.calculateFuelCost(mc.player, 1, recipe.getCookTime());
                ItemStack fuelStack = new ItemStack(fuelCost.recommendedFuel);
                graphics.item(fuelStack, this.getX() + 6, rowY - 1);

                String fuelName = Component.translatable("gui.gridless.fuel", fuelCost.recommendedFuel.getName(fuelStack).getString(), fuelCost.fuelItemsNeeded).getString();
                int maxNameWidth = this.width - (maxScroll > 0 ? 86 : 76);
                if (font.width(fuelName) > maxNameWidth) {
                    fuelName = font.plainSubstrByWidth(fuelName, maxNameWidth - 6) + "..";
                }
                graphics.text(font, fuelName, this.getX() + 24, rowY + 3, fuelCost.hasEnough ? 0xFFFFA726 : 0xFFE57373, false);

                int haveFuelCount = bag.getCount(fuelCost.recommendedFuel);
                String fuelCountStr = haveFuelCount + "/" + fuelCost.fuelItemsNeeded;
                int fuelCountWidth = font.width(fuelCountStr);
                graphics.text(font, fuelCountStr, this.getX() + rightClip - fuelCountWidth, rowY + 3, fuelCost.hasEnough ? 0xFFFFA726 : 0xFFE57373, false);
            }
        }

        graphics.disableScissor();

        // 4. Scrollbar (if rows exceed 4)
        if (maxScroll > 0) {
            int scrollbarX = this.getX() + this.width - 6;
            int scrollbarHeight = Math.max(10, (int) ((float) viewportHeight / (getTotalRows() * ROW_HEIGHT) * viewportHeight));
            int scrollbarY = (int) (viewportY + (scrollOffset / maxScroll) * (viewportHeight - scrollbarHeight));

            graphics.fill(scrollbarX, viewportY, scrollbarX + 3, viewportY + viewportHeight, 0xFF141414);
            graphics.fill(scrollbarX, scrollbarY, scrollbarX + 3, scrollbarY + scrollbarHeight, isDraggingScrollbar ? 0xFFFFA726 : 0xFF555555);
        }

        // 5. Action Buttons (Craft 1 / Craft Max)
        int maxCraft = recipe.maxCraftable(bag);
        boolean canCraft = recipe.isCraftable(bag);
        if (mode == StationMode.SMELTING && mc.player != null) {
            int maxWithFuel = SmeltingCalculator.getMaxSmeltableWithFuel(mc.player, recipe.getCookTime());
            maxCraft = Math.min(maxCraft, maxWithFuel);
            if (maxCraft <= 0 || !SmeltingCalculator.hasEnoughFuel(mc.player, recipe.getCookTime())) {
                canCraft = false;
            }
        }

        int btnWidth = (this.width - 16) / 2;
        int btnHeight = 17;
        int btnY = this.getY() + this.height - btnHeight - 4;

        // Button 1: Craft 1
        int btn1X = this.getX() + 5;
        boolean btn1Hovered = mouseX >= btn1X && mouseX < btn1X + btnWidth && mouseY >= btnY && mouseY < btnY + btnHeight;
        int btn1Color = canCraft ? (btn1Hovered ? 0xFF388E3C : 0xFF2E7D32) : 0xFF333333;
        graphics.fill(btn1X, btnY, btn1X + btnWidth, btnY + btnHeight, btn1Color);
        graphics.outline(btn1X, btnY, btnWidth, btnHeight, canCraft ? 0xFF81C784 : 0xFF444444);
        String btn1Text = Component.translatable("gui.gridless.craft_x1").getString();
        graphics.text(font, btn1Text, btn1X + (btnWidth - font.width(btn1Text)) / 2, btnY + 5, canCraft ? 0xFFFFFFFF : 0xFF777777, false);

        // Button 2: Craft Max
        int btn2X = btn1X + btnWidth + 6;
        boolean btn2Hovered = mouseX >= btn2X && mouseX < btn2X + btnWidth && mouseY >= btnY && mouseY < btnY + btnHeight;
        int btn2Color = canCraft && maxCraft > 0 ? (btn2Hovered ? 0xFF1976D2 : 0xFF1565C0) : 0xFF333333;
        graphics.fill(btn2X, btnY, btn2X + btnWidth, btnY + btnHeight, btn2Color);
        graphics.outline(btn2X, btnY, btnWidth, btnHeight, canCraft && maxCraft > 0 ? 0xFF64B5F6 : 0xFF444444);
        String btn2Text = Component.translatable("gui.gridless.craft_max", maxCraft).getString();
        graphics.text(font, btn2Text, btn2X + (btnWidth - font.width(btn2Text)) / 2, btnY + 5, canCraft && maxCraft > 0 ? 0xFFFFFFFF : 0xFF777777, false);
    }

    public void renderIngredientTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!this.isHovered || recipe == null) return;
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        // Check output item tooltip
        int headerY = this.getY() + 4;
        if (mouseX >= this.getX() + 7 && mouseX < this.getX() + 25 && mouseY >= headerY + 3 && mouseY < headerY + 21) {
            graphics.setTooltipForNextFrame(font, recipe.getResult(), mouseX, mouseY);
            return;
        }

        // Check variant switch tooltip
        if (recipe.getVariantCount() > 1) {
            int btnLeftX = this.getX() + this.width - 56;
            if (mouseX >= btnLeftX && mouseX < this.getX() + this.width - 7 && mouseY >= headerY + 4 && mouseY < headerY + 18) {
                graphics.setTooltipForNextFrame(font, Component.translatable("gui.gridless.variant_tooltip", recipe.getActiveVariantIndex() + 1, recipe.getVariantCount()), mouseX, mouseY);
                return;
            }
        }

        // Check ingredient items tooltip inside viewport
        int viewportY = getViewportY();
        int viewportHeight = getViewportHeight();
        if (mouseY < viewportY || mouseY >= viewportY + viewportHeight) {
            return;
        }

        int rowIndex = 0;
        for (CountedIngredient input : recipe.getInputs()) {
            int rowY = (int) (viewportY + rowIndex * ROW_HEIGHT - scrollOffset);
            rowIndex++;

            if (rowY + ROW_HEIGHT < viewportY || rowY > viewportY + viewportHeight) {
                continue;
            }

            ItemStack[] matching = input.getMatchingStacks();
            ItemStack sample = matching.length > 0 ? matching[(int) ((System.currentTimeMillis() / 1000) % matching.length)] : ItemStack.EMPTY;
            if (!sample.isEmpty() && mouseX >= this.getX() + 6 && mouseX < this.getX() + 22 && mouseY >= rowY && mouseY < rowY + 16) {
                ItemStack tipStack = sample.copy();
                tipStack.setCount(input.getCount());
                graphics.setTooltipForNextFrame(font, tipStack, mouseX, mouseY);
                return;
            }
        }

        // Smelting fuel tooltip
        if (mode == StationMode.SMELTING && mc.player != null) {
            int rowY = (int) (viewportY + rowIndex * ROW_HEIGHT - scrollOffset);
            if (rowY + ROW_HEIGHT >= viewportY && rowY <= viewportY + viewportHeight) {
                SmeltingCalculator.FuelCost fuelCost = SmeltingCalculator.calculateFuelCost(mc.player, 1, recipe.getCookTime());
                ItemStack fuelStack = new ItemStack(fuelCost.recommendedFuel);
                if (mouseX >= this.getX() + 6 && mouseX < this.getX() + 22 && mouseY >= rowY && mouseY < rowY + 16) {
                    graphics.setTooltipForNextFrame(font, fuelStack, mouseX, mouseY);
                }
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.isHovered || recipe == null) return false;

        int headerY = this.getY() + 4;
        // If scrolling over header banner with multiple variants, cycle variants!
        if (mouseY >= headerY && mouseY < headerY + 22 && recipe.getVariantCount() > 1) {
            recipe.cycleVariant(scrollY > 0 ? -1 : 1);
            variantCycleTicks = 0;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(GridlessSounds.RECIPE_SELECT, 1.0f));
            return true;
        }

        // Scroll ingredients viewport
        int maxScroll = getMaxScroll();
        if (maxScroll > 0) {
            this.scrollOffset = Mth.clamp(this.scrollOffset - scrollY * 15, 0, maxScroll);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!this.isHovered || recipe == null) return false;

        int headerY = this.getY() + 4;
        // Variant switch buttons
        if (recipe.getVariantCount() > 1 && event.button() == 0) {
            int btnLeftX = this.getX() + this.width - 56;
            int btnRightX = this.getX() + this.width - 18;
            int btnArrowY = headerY + 4;

            if (event.x() >= btnLeftX && event.x() < btnLeftX + 11 && event.y() >= btnArrowY && event.y() < btnArrowY + 14) {
                recipe.cycleVariant(-1);
                variantCycleTicks = 0;
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(GridlessSounds.RECIPE_SELECT, 1.0f));
                return true;
            }
            if (event.x() >= btnRightX && event.x() < btnRightX + 11 && event.y() >= btnArrowY && event.y() < btnArrowY + 14) {
                recipe.cycleVariant(1);
                variantCycleTicks = 0;
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(GridlessSounds.RECIPE_SELECT, 1.0f));
                return true;
            }
        }

        // Check scrollbar drag start
        int maxScroll = getMaxScroll();
        int viewportY = getViewportY();
        int viewportHeight = getViewportHeight();
        if (maxScroll > 0 && event.x() >= this.getX() + this.width - 8 && event.x() <= this.getX() + this.width && event.y() >= viewportY && event.y() <= viewportY + viewportHeight) {
            isDraggingScrollbar = true;
            return true;
        }

        // Check Action Buttons
        int maxCraft = recipe.maxCraftable(bag);
        boolean canCraft = recipe.isCraftable(bag);
        Minecraft mc = Minecraft.getInstance();
        if (mode == StationMode.SMELTING && mc.player != null) {
            int maxWithFuel = SmeltingCalculator.getMaxSmeltableWithFuel(mc.player, recipe.getCookTime());
            maxCraft = Math.min(maxCraft, maxWithFuel);
            if (maxCraft <= 0 || !SmeltingCalculator.hasEnoughFuel(mc.player, recipe.getCookTime())) {
                canCraft = false;
            }
        }

        int btnWidth = (this.width - 16) / 2;
        int btnHeight = 17;
        int btnY = this.getY() + this.height - btnHeight - 4;

        int btn1X = this.getX() + 5;
        int btn2X = btn1X + btnWidth + 6;

        // Check Craft 1
        if (event.x() >= btn1X && event.x() < btn1X + btnWidth && event.y() >= btnY && event.y() < btnY + btnHeight) {
            if (canCraft) {
                if (event.button() == 0) {
                    onCraftRequested.accept(recipe, 1);
                } else if (event.button() == 1) { // Rapid craft hold
                    isHoldingCraft = true;
                    onCraftRequested.accept(recipe, 1);
                }
            } else {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(GridlessSounds.CRAFT_FAIL, 1.0f));
            }
            return true;
        }

        // Check Craft Max
        if (event.x() >= btn2X && event.x() < btn2X + btnWidth && event.y() >= btnY && event.y() < btnY + btnHeight) {
            if (canCraft && maxCraft > 0) {
                onCraftRequested.accept(recipe, maxCraft);
            } else {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(GridlessSounds.CRAFT_FAIL, 1.0f));
            }
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (isDraggingScrollbar) {
            int maxScroll = getMaxScroll();
            int viewportHeight = getViewportHeight();
            int scrollbarHeight = Math.max(10, (int) ((float) viewportHeight / (getTotalRows() * ROW_HEIGHT) * viewportHeight));
            double trackLength = viewportHeight - scrollbarHeight;
            if (trackLength > 0) {
                double delta = (dragY / trackLength) * maxScroll;
                this.scrollOffset = Mth.clamp(this.scrollOffset + delta, 0, maxScroll);
            }
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        isDraggingScrollbar = false;
        isHoldingCraft = false;
        holdTicks = 0;
        return super.mouseReleased(event);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }
}
