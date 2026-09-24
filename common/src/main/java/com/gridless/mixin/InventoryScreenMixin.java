package com.gridless.mixin;

import com.gridless.api.station.StationRegistry;
import com.gridless.client.screen.GridlessCraftingScreen;
import com.gridless.config.GridlessConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(InventoryScreen.class)
public class InventoryScreenMixin {

    private static final ResourceLocation CLEAN_INVENTORY_LOCATION = ResourceLocation.fromNamespaceAndPath("gridless", "textures/gui/container/inventory.png");

    @Inject(method = "renderBg", at = @At("HEAD"), cancellable = true)
    private void onRenderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
        if (GridlessConfig.general.enabled && GridlessConfig.general.remove_crafting_inventory) {
            ci.cancel();
            AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) (Object) this;
            int x = accessor.getLeftPos();
            int y = accessor.getTopPos();
            graphics.blit(net.minecraft.client.renderer.RenderType::guiTextured, CLEAN_INVENTORY_LOCATION, x, y, 0.0F, 0.0F, 176, 166, 256, 256);
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, x + 26, y + 8, x + 75, y + 78, 30, 0.0625F, (float) mouseX, (float) mouseY, mc.player);
            }
        }
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInitInventory(CallbackInfo ci) {
        if (!GridlessConfig.general.enabled) return;

        InventoryScreen self = (InventoryScreen) (Object) this;
        InventoryMenu menu = self.getMenu();
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) self;
        Minecraft mc = Minecraft.getInstance();

        if (GridlessConfig.general.remove_crafting_inventory) {
            try {
                Class<?> cls = self.getClass();
                while (cls != null && cls != Object.class) {
                    for (java.lang.reflect.Field field : cls.getDeclaredFields()) {
                        if (RecipeBookComponent.class.isAssignableFrom(field.getType())) {
                            field.setAccessible(true);
                            RecipeBookComponent rbc = (RecipeBookComponent) field.get(self);
                            if (rbc != null && rbc.isVisible()) {
                                rbc.toggleVisibility();
                            }
                            break;
                        }
                    }
                    cls = cls.getSuperclass();
                }
            } catch (Throwable ignored) {}
            accessor.setLeftPos((self.width - accessor.getImageWidth()) / 2);



            List<? extends GuiEventListener> currentChildren = new ArrayList<>(self.children());
            for (GuiEventListener child : currentChildren) {
                if (child instanceof ImageButton) {
                    ((ScreenAccessor) self).invokeRemoveWidget(child);
                }
            }

            if (mc.player != null) {
                for (Slot slot : menu.slots) {
                    SlotAccessor acc = (SlotAccessor) slot;
                    if (slot.container == mc.player.getInventory()) {
                        int cs = slot.getContainerSlot();
                        if (cs >= 9 && cs < 36) { // Main 27 inventory
                            int row = (cs - 9) / 9;
                            int col = (cs - 9) % 9;
                            acc.setX(8 + col * 18);
                            acc.setY(84 + row * 18);
                        } else if (cs >= 0 && cs < 9) { // 9 hotbar
                            acc.setX(8 + cs * 18);
                            acc.setY(142);
                        } else if (cs >= 36 && cs <= 39) { // Armor
                            int armorIdx = 39 - cs;
                            acc.setX(8);
                            acc.setY(8 + armorIdx * 18);
                        } else if (cs == 40) { // Offhand
                            acc.setX(77);
                            acc.setY(62);
                        }
                    } else {
                        // Crafting slots (0..4) -> hide off-screen
                        acc.setX(-9999);
                        acc.setY(-9999);
                    }
                }
            }
        }

        // 2. Add Gridless Crafting launcher button in top right of GUI where it was
        if (GridlessConfig.general.replace_inventory_crafting) {
            int left = accessor.getLeftPos();
            int top = accessor.getTopPos();

            Button gridlessBtn = Button.builder(Component.literal("⚒"), btn -> {
                if (mc.player != null) {
                    mc.setScreen(new GridlessCraftingScreen<>(
                            menu,
                            mc.player.getInventory(),
                            Component.translatable("container.crafting"),
                            StationRegistry.get(StationRegistry.INVENTORY),
                            Optional.empty(),
                            true
                    ));
                }
            }).bounds(left + 152, top + 6, 18, 18)
              .tooltip(Tooltip.create(Component.translatable("gui.gridless.launcher_tooltip")))
              .build();

            ((ScreenAccessor) self).invokeAddRenderableWidget(gridlessBtn);
        }
    }

    @Inject(method = "renderLabels", at = @At("HEAD"), cancellable = true)
    private void onRenderLabels(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (GridlessConfig.general.enabled && GridlessConfig.general.remove_crafting_inventory) {
            // Cancel drawing the vanilla "Crafting" label that was printed above the 2x2 grid
            ci.cancel();
        }
    }
}

