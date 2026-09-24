package com.gridless.client.screen;

import com.gridless.api.recipe.CountedIngredient;
import com.gridless.api.recipe.GridlessRecipe;
import com.gridless.api.recipe.IngredientBag;
import com.gridless.api.recipe.RecipeCategory;
import com.gridless.api.recipe.RecipeIndexer;
import com.gridless.api.smelting.SmeltingCalculator;
import com.gridless.api.station.GridlessStation;
import com.gridless.api.station.StationMode;
import com.gridless.api.station.StationRegistry;
import com.gridless.client.screen.widget.CategoryTabWidget;
import com.gridless.client.screen.widget.IngredientCardWidget;
import com.gridless.client.screen.widget.RecipeRibbonWidget;
import com.gridless.config.GridlessConfig;
import com.gridless.menu.GridlessCraftingMenu;
import com.gridless.mixin.SlotAccessor;
import com.gridless.network.C2SCraftGridlessRecipePayload;
import com.gridless.platform.Services;
import com.gridless.sound.GridlessSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class GridlessCraftingScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    private final GridlessStation station;
    private final Optional<BlockPos> stationPos;
    private final boolean isInventoryCrafting;
    private final Map<Slot, int[]> originalSlotPositions = new HashMap<>();

    private EditBox searchBox;
    private Button filterCraftableBtn;
    private Button backBtn;
    private CategoryTabWidget categoryTabs;
    private RecipeRibbonWidget recipeRibbon;
    private IngredientCardWidget ingredientCard;

    private boolean showOnlyCraftable;
    private List<GridlessRecipe> allStationRecipes = new ArrayList<>();
    private String currentSearchText = "";
    private RecipeCategory currentCategory = RecipeCategory.ALL;

    public GridlessCraftingScreen(T menu, Inventory playerInventory, Component title, GridlessStation station, Optional<BlockPos> stationPos, boolean isInventoryCrafting) {
        super(menu, playerInventory, title, 368, 240);
        this.station = station != null ? station : StationRegistry.get(StationRegistry.INVENTORY);
        this.stationPos = stationPos != null ? stationPos : Optional.empty();
        this.isInventoryCrafting = isInventoryCrafting;
    }

    public GridlessCraftingScreen(T menu, Inventory playerInventory, Component title) {
        this(menu, playerInventory, title, StationRegistry.get(StationRegistry.INVENTORY), Optional.empty(), true);
    }

    @Override
    protected void init() {
        super.init();
        int left = this.leftPos;
        int top = this.topPos;

        // 1. Record original slot positions and arrange inventory cleanly, moving grids off-screen
        if (this.minecraft != null && this.minecraft.player != null) {
            Inventory playerInv = this.minecraft.player.getInventory();
            if (originalSlotPositions.isEmpty()) {
                for (Slot slot : this.menu.slots) {
                    originalSlotPositions.put(slot, new int[]{slot.x, slot.y});
                }
            }

            for (Slot slot : this.menu.slots) {
                SlotAccessor acc = (SlotAccessor) slot;
                if (slot.container == playerInv) {
                    int containerSlot = slot.getContainerSlot();
                    if (containerSlot >= 9 && containerSlot < 36) { // 27 main inventory slots
                        int row = (containerSlot - 9) / 9;
                        int col = (containerSlot - 9) % 9;
                        acc.setX(GridlessCraftingMenu.INV_X + col * 18);
                        acc.setY(GridlessCraftingMenu.INV_Y + row * 18);
                    } else if (containerSlot >= 0 && containerSlot < 9) { // 9 hotbar slots
                        int col = containerSlot;
                        acc.setX(GridlessCraftingMenu.INV_X + col * 18);
                        acc.setY(GridlessCraftingMenu.HOTBAR_Y);
                    } else {
                        // Armor / offhand slots
                        acc.setX(-9999);
                        acc.setY(-9999);
                    }
                } else {
                    // Non-inventory slots (3x3 craft grid, 2x2 craft grid, result slots, etc.)
                    acc.setX(-9999);
                    acc.setY(-9999);
                }
            }
        }

        this.showOnlyCraftable = GridlessConfig.general.hide_uncraftable_recipes;

        // 2. Back Button (only when inventory crafting)
        if (this.isInventoryCrafting) {
            this.backBtn = Button.builder(Component.literal("←"), btn -> {
                if (this.minecraft != null && this.minecraft.player != null) {
                    this.minecraft.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(this.minecraft.player));
                }
            }).bounds(left + 8, top + 8, 18, 16)
              .tooltip(Tooltip.create(Component.translatable("gui.gridless.back_tooltip")))
              .build();
            this.addRenderableWidget(this.backBtn);

            this.searchBox = new EditBox(this.font, left + 28, top + 8, 106, 16, Component.literal("Search"));
        } else {
            this.searchBox = new EditBox(this.font, left + 8, top + 8, 126, 16, Component.literal("Search"));
        }

        this.searchBox.setHint(Component.translatable("gui.gridless.search_hint"));
        this.searchBox.setResponder(this::onSearchChanged);
        this.addRenderableWidget(this.searchBox);

        // 3. Craftable Only Filter Toggle Button
        this.filterCraftableBtn = Button.builder(getFilterButtonText(), btn -> toggleCraftableFilter())
                .bounds(left + 138, top + 8, 30, 16)
                .tooltip(getFilterButtonTooltip())
                .build();
        this.addRenderableWidget(this.filterCraftableBtn);

        // 4. Category Tabs (2 rows of 4 tabs each, cleanly sized)
        this.categoryTabs = new CategoryTabWidget(left + 8, top + 28, 160, 30, this::onCategoryChanged);
        this.addRenderableWidget(this.categoryTabs);

        // 5. Recipe Ribbon (Scrollable Picker)
        this.recipeRibbon = new RecipeRibbonWidget(left + 8, top + 62, 160, 170, this::onRecipeSelected);
        this.addRenderableWidget(this.recipeRibbon);

        // 6. Ingredient Card (Right/Top Panel)
        this.ingredientCard = new IngredientCardWidget(left + 176, top + 8, 184, 122, this::onCraftRequested);
        this.addRenderableWidget(this.ingredientCard);

        // Load station recipes
        refreshStationRecipes();
        updateFilteredRecipes();
    }

    private void restoreSlotPositions() {
        for (Map.Entry<Slot, int[]> entry : originalSlotPositions.entrySet()) {
            Slot slot = entry.getKey();
            int[] pos = entry.getValue();
            SlotAccessor acc = (SlotAccessor) slot;
            acc.setX(pos[0]);
            acc.setY(pos[1]);
        }
        originalSlotPositions.clear();
    }

    @Override
    public void removed() {
        restoreSlotPositions();
        super.removed();
    }

    @Override
    public void onClose() {
        restoreSlotPositions();
        super.onClose();
    }

    private void toggleCraftableFilter() {
        this.showOnlyCraftable = !this.showOnlyCraftable;
        this.filterCraftableBtn.setMessage(getFilterButtonText());
        this.filterCraftableBtn.setTooltip(getFilterButtonTooltip());
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(GridlessSounds.RECIPE_SELECT, 1.0f));
        updateFilteredRecipes();
    }

    private Component getFilterButtonText() {
        return this.showOnlyCraftable ? Component.literal("🔨✔") : Component.literal("🔨");
    }

    private Tooltip getFilterButtonTooltip() {
        return Tooltip.create(
                this.showOnlyCraftable
                        ? Component.translatable("gui.gridless.filter_craftable")
                        : Component.translatable("gui.gridless.filter_all")
        );
    }

    private void refreshStationRecipes() {
        if (RecipeIndexer.getAll().isEmpty() && this.minecraft != null && this.minecraft.hasSingleplayerServer() && this.minecraft.getSingleplayerServer() != null && this.minecraft.level != null) {
            RecipeIndexer.reindex(this.minecraft.getSingleplayerServer().getRecipeManager(), this.minecraft.level.registryAccess());
        }

        if (this.isInventoryCrafting || (this.station != null && this.station.getId().equals(StationRegistry.INVENTORY))) {
            allStationRecipes = new ArrayList<>(RecipeIndexer.getForTypes(List.of(Identifier.fromNamespaceAndPath("minecraft", "crafting"))));
            allStationRecipes.removeIf(GridlessRecipe::requires3x3);
            for (GridlessRecipe r : allStationRecipes) {
                r.remove3x3Variants();
            }
        } else if (this.station != null) {
            allStationRecipes = new ArrayList<>(RecipeIndexer.getForTypes(this.station.getAllowedRecipeTypeIds()));
        } else {
            allStationRecipes = new ArrayList<>(RecipeIndexer.getAll());
        }
    }

    private void updateFilteredRecipes() {
        if (this.minecraft == null || this.minecraft.player == null) return;
        IngredientBag bag = IngredientBag.fromPlayer(this.minecraft.player);

        List<GridlessRecipe> filtered = new ArrayList<>();
        String query = currentSearchText.toLowerCase(Locale.ROOT).trim();

        for (GridlessRecipe recipe : allStationRecipes) {
            recipe.autoSelectBestVariant(bag);
            // Craftable filter
            if (this.showOnlyCraftable && !recipe.isCraftable(bag)) {
                continue;
            }
            // Category check
            if (currentCategory != RecipeCategory.ALL && recipe.getCategory() != currentCategory) {
                continue;
            }
            // Search text check
            if (!query.isEmpty()) {
                String name = recipe.getResult().getHoverName().getString().toLowerCase(Locale.ROOT);
                String id = recipe.getId().toString().toLowerCase(Locale.ROOT);
                if (!name.contains(query) && !id.contains(query)) {
                    continue;
                }
            }
            filtered.add(recipe);
        }

        // Sort: Craftable first, then alphabetically by item name
        filtered.sort(Comparator
                .comparing((GridlessRecipe r) -> !r.isCraftable(bag))
                .thenComparing(r -> r.getResult().getHoverName().getString())
        );

        this.recipeRibbon.setRecipes(filtered, bag);
        GridlessRecipe selected = this.recipeRibbon.getSelectedRecipe();
        StationMode mode = this.station != null ? this.station.getMode() : StationMode.NORMAL;
        this.ingredientCard.setRecipe(selected, bag, mode);
    }

    private void onSearchChanged(String text) {
        this.currentSearchText = text;
        updateFilteredRecipes();
    }

    private void onCategoryChanged(RecipeCategory category) {
        this.currentCategory = category;
        updateFilteredRecipes();
    }

    private void onRecipeSelected(GridlessRecipe recipe) {
        if (this.minecraft != null && this.minecraft.player != null) {
            IngredientBag bag = IngredientBag.fromPlayer(this.minecraft.player);
            StationMode mode = this.station != null ? this.station.getMode() : StationMode.NORMAL;
            this.ingredientCard.setRecipe(recipe, bag, mode);
        }
    }

    private void onCraftRequested(GridlessRecipe recipe, int amount) {
        if (recipe == null || amount <= 0) return;
        C2SCraftGridlessRecipePayload payload = new C2SCraftGridlessRecipePayload(
                recipe.getId(),
                amount,
                this.stationPos,
                this.isInventoryCrafting
        );
        Services.PLATFORM.sendToServer(payload);
    }

    private int lastInventoryHash = -1;

    private int getInventoryHash() {
        if (this.minecraft == null || this.minecraft.player == null) return 0;
        Inventory inv = this.minecraft.player.getInventory();
        int hash = inv.getTimesChanged();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                hash = 31 * hash + stack.getItem().hashCode();
                hash = 31 * hash + stack.getCount();
            }
        }
        return hash;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.ingredientCard != null) {
            this.ingredientCard.tick();
        }
        // Update recipe list and ingredient card dynamically when inventory changes
        if (this.minecraft != null && this.minecraft.player != null && this.recipeRibbon != null) {
            int currentInvHash = getInventoryHash();
            if (currentInvHash != lastInventoryHash) {
                lastInventoryHash = currentInvHash;
                updateFilteredRecipes();
            } else {
                IngredientBag bag = IngredientBag.fromPlayer(this.minecraft.player);
                GridlessRecipe selected = this.recipeRibbon.getSelectedRecipe();
                StationMode mode = this.station != null ? this.station.getMode() : StationMode.NORMAL;
                this.ingredientCard.setRecipe(selected, bag, mode);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.searchBox != null) {
            boolean insideSearch = event.x() >= this.searchBox.getX() && event.x() < this.searchBox.getX() + this.searchBox.getWidth()
                    && event.y() >= this.searchBox.getY() && event.y() < this.searchBox.getY() + this.searchBox.getHeight();
            if (insideSearch) {
                if (event.button() == 1) { // Right-click clears search
                    this.searchBox.setValue("");
                }
                this.searchBox.setFocused(true);
                this.setFocused(this.searchBox);
            } else if (event.button() == 0) {
                this.searchBox.setFocused(false);
                if (this.getFocused() == this.searchBox) {
                    this.setFocused(null);
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.recipeRibbon != null && (this.recipeRibbon.isDraggingScrollbar() || this.recipeRibbon.isMouseOver(event.x(), event.y()))) {
            if (this.recipeRibbon.mouseDragged(event, dragX, dragY)) {
                return true;
            }
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.recipeRibbon != null) {
            this.recipeRibbon.mouseReleased(event);
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.searchBox != null && this.searchBox.isFocused() && this.searchBox.isVisible()) {
            if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                this.searchBox.setFocused(false);
                if (this.getFocused() == this.searchBox) {
                    this.setFocused(null);
                }
                return super.keyPressed(event);
            }
            if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
                this.searchBox.setFocused(false);
                if (this.getFocused() == this.searchBox) {
                    this.setFocused(null);
                }
                return true;
            }
            if (this.searchBox.keyPressed(event)) {
                return true;
            }
            // Consume all other key presses while search box is focused so inventory/gameplay keybinds ('E', 'Q', 1-9) do not trigger
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (this.searchBox != null && this.searchBox.isFocused() && this.searchBox.isVisible()) {
            if (this.searchBox.charTyped(event)) {
                return true;
            }
        }
        return super.charTyped(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        // Tooltips for tabs, recipe ribbon items, and ingredient items
        if (this.categoryTabs != null) {
            this.categoryTabs.renderTabTooltips(graphics, mouseX, mouseY);
        }
        if (this.recipeRibbon != null) {
            this.recipeRibbon.renderRecipeTooltip(graphics, mouseX, mouseY);
        }
        if (this.ingredientCard != null) {
            this.ingredientCard.renderIngredientTooltips(graphics, mouseX, mouseY);
        }

        // Tooltip for fuel tab
        StationMode mode = this.station != null ? this.station.getMode() : StationMode.NORMAL;
        if (mode == StationMode.SMELTING) {
            int fuelSlotX = this.leftPos + GridlessCraftingMenu.INV_X - 1;
            int fuelSlotY = this.topPos + GridlessCraftingMenu.HOTBAR_Y - 1;
            if (mouseX >= fuelSlotX && mouseX < fuelSlotX + 18 && mouseY >= fuelSlotY - 9 && mouseY < fuelSlotY) {
                graphics.setTooltipForNextFrame(this.font, Component.translatable("gui.gridless.fuel_tooltip"), mouseX, mouseY);
            }
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int left = this.leftPos;
        int top = this.topPos;

        // Main window backdrop
        graphics.fill(left, top, left + this.imageWidth, top + this.imageHeight, 0xEE121212);
        graphics.outline(left, top, this.imageWidth, this.imageHeight, 0xFF4A4A4A);

        // Player Inventory Section Backdrop (bottom right)
        int invBoxX = left + 176;
        int invBoxY = top + 134;
        int invBoxWidth = 184;
        int invBoxHeight = 98;
        graphics.fill(invBoxX, invBoxY, invBoxX + invBoxWidth, invBoxY + invBoxHeight, 0xCC1E1E1E);
        graphics.outline(invBoxX, invBoxY, invBoxWidth, invBoxHeight, 0xFF3A3A3A);

        graphics.text(this.font, Component.translatable("gui.gridless.inventory").getString(), invBoxX + 8, invBoxY + 4, 0x888888, false);

        // Slot background boxes (18x18 each) - Main inventory 3 rows x 9 cols
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotX = left + GridlessCraftingMenu.INV_X + col * 18 - 1;
                int slotY = top + GridlessCraftingMenu.INV_Y + row * 18 - 1;
                graphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF141414);
                graphics.outline(slotX, slotY, 18, 18, 0xFF353535);
            }
        }

        // Slot background boxes (18x18 each) - Hotbar
        for (int col = 0; col < 9; col++) {
            int slotX = left + GridlessCraftingMenu.INV_X + col * 18 - 1;
            int slotY = top + GridlessCraftingMenu.HOTBAR_Y - 1;
            graphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF141414);
            graphics.outline(slotX, slotY, 18, 18, 0xFF353535);
        }

        // Highlight inventory slots containing required ingredients for the currently selected recipe
        GridlessRecipe selectedRecipe = this.recipeRibbon != null ? this.recipeRibbon.getSelectedRecipe() : null;
        if (selectedRecipe != null && this.minecraft != null && this.minecraft.player != null) {
            List<CountedIngredient> reqs = selectedRecipe.getInputs();
            if (reqs != null && !reqs.isEmpty()) {
                Inventory playerInv = this.minecraft.player.getInventory();
                for (Slot slot : this.menu.slots) {
                    if (slot.container == playerInv && slot.hasItem()) {
                        ItemStack stack = slot.getItem();
                        boolean isIngredient = false;
                        for (CountedIngredient req : reqs) {
                            if (req.test(stack)) {
                                isIngredient = true;
                                break;
                            }
                        }
                        if (isIngredient) {
                            int sX = left + slot.x - 1;
                            int sY = top + slot.y - 1;
                            // Emerald Green highlight overlay & outline around inventory slots with required ingredients
                            graphics.fill(sX, sY, sX + 18, sY + 18, 0x3300E676);
                            graphics.outline(sX, sY, 18, 18, 0xFF00E676);
                        }
                    }
                }
            }
        }

        // Orange fuel tab & slot highlight for furnace/smelter/blast furnace
        StationMode mode = this.station != null ? this.station.getMode() : StationMode.NORMAL;
        if (mode == StationMode.SMELTING) {
            int fuelSlotX = left + GridlessCraftingMenu.INV_X - 1;
            int fuelSlotY = top + GridlessCraftingMenu.HOTBAR_Y - 1;

            // Orange tab on top of the first hotbar slot (slot 0)
            int tabY = fuelSlotY - 9;
            graphics.fill(fuelSlotX, tabY, fuelSlotX + 18, fuelSlotY, 0xFFFF6F00); // Amber orange
            graphics.outline(fuelSlotX, tabY, 18, 10, 0xFFFFB300);
            graphics.text(this.font, "🔥", fuelSlotX + 5, tabY + 1, 0xFFFFFF, false);

            // Orange highlight around designated fuel slot (slot 0)
            graphics.outline(fuelSlotX, fuelSlotY, 18, 18, 0xFFFF8C00);

            // If another slot is currently the active primary fuel, also highlight it
            if (this.minecraft != null && this.minecraft.player != null) {
                int activeSlot = SmeltingCalculator.findPrimaryFuelSlot(this.minecraft.player);
                if (activeSlot >= 0 && activeSlot != 0) {
                    int aSlotX, aSlotY;
                    if (activeSlot < 9) { // Hotbar
                        aSlotX = left + GridlessCraftingMenu.INV_X + activeSlot * 18 - 1;
                        aSlotY = top + GridlessCraftingMenu.HOTBAR_Y - 1;
                    } else { // Main inventory
                        int r = (activeSlot - 9) / 9;
                        int c = (activeSlot - 9) % 9;
                        aSlotX = left + GridlessCraftingMenu.INV_X + c * 18 - 1;
                        aSlotY = top + GridlessCraftingMenu.INV_Y + r * 18 - 1;
                    }
                    graphics.outline(aSlotX, aSlotY, 18, 18, 0xFFFF8C00);
                }
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.ingredientCard != null && this.ingredientCard.isMouseOver(mouseX, mouseY)) {
            if (this.ingredientCard.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
                return true;
            }
        }
        if (this.recipeRibbon != null && this.recipeRibbon.isMouseOver(mouseX, mouseY)) {
            if (this.recipeRibbon.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // Suppress standard vanilla label rendering since custom widgets handle title and inventory
    }
}
