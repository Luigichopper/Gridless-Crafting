package com.gridless.sound;

import com.gridless.GridlessMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.util.function.BiConsumer;

public class GridlessSounds {
    public static final SoundEvent CRAFT_CLICK = create("craft_click");
    public static final SoundEvent RECIPE_SELECT = create("recipe_select");
    public static final SoundEvent CRAFT_FAIL = create("craft_fail");

    private static SoundEvent create(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(GridlessMod.MOD_ID, name);
        return SoundEvent.createVariableRangeEvent(id);
    }

    public static void register(BiConsumer<ResourceLocation, SoundEvent> consumer) {
        consumer.accept(CRAFT_CLICK.location(), CRAFT_CLICK);
        consumer.accept(RECIPE_SELECT.location(), RECIPE_SELECT);
        consumer.accept(CRAFT_FAIL.location(), CRAFT_FAIL);
    }

    public static void init() {
        register((id, sound) -> Registry.register(BuiltInRegistries.SOUND_EVENT, id, sound));
    }
}
