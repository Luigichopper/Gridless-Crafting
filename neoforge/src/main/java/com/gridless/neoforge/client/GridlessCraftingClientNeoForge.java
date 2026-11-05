package com.gridless.neoforge.client;

import com.gridless.GridlessMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = GridlessMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class GridlessCraftingClientNeoForge {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Client setup
    }
}
