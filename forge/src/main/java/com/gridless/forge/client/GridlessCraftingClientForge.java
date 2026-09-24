package com.gridless.forge.client;

import com.gridless.client.gui.GridlessConfigScreen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class GridlessCraftingClientForge {
    public static void registerConfigScreen(FMLJavaModLoadingContext context) {
        context.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new GridlessConfigScreen(parent)));
    }
}
