package com.gridless.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.gridless.GridlessMod;
import com.gridless.platform.Services;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class GridlessConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "gridless_crafting.json";

    public static General general = new General();
    public static Stations stations = new Stations();
    public static AudioVisual audioVisual = new AudioVisual();

    public static class General {
        public boolean enabled = true;
        public boolean remove_crafting_inventory = true;
        public boolean replace_inventory_crafting = true;
        public boolean hide_uncraftable_recipes = false;
    }

    public static class Stations {
        public boolean enable_crafting_table = true;
        public String furnace_mode = "INSTANT_WITH_FUEL"; // "TRADITIONAL" or "INSTANT_WITH_FUEL"
        public String smoker_mode = "INSTANT_WITH_FUEL";
        public String blast_furnace_mode = "INSTANT_WITH_FUEL";

        public boolean isFurnaceInstant() {
            return "INSTANT_WITH_FUEL".equalsIgnoreCase(furnace_mode);
        }

        public boolean isSmokerInstant() {
            return "INSTANT_WITH_FUEL".equalsIgnoreCase(smoker_mode);
        }

        public boolean isBlastFurnaceInstant() {
            return "INSTANT_WITH_FUEL".equalsIgnoreCase(blast_furnace_mode);
        }
    }

    public static class AudioVisual {
        public boolean enable_custom_sounds = true;
        public int visible_recipe_rows = 6;
    }

    private static class ConfigData {
        General general = new General();
        Stations stations = new Stations();
        AudioVisual audio_visual = new AudioVisual();
    }

    public static void load() {
        try {
            Path configDir = Services.PLATFORM.getConfigDirectory();
            File configFile = configDir.resolve(FILE_NAME).toFile();

            if (configFile.exists()) {
                try (FileReader reader = new FileReader(configFile)) {
                    ConfigData data = GSON.fromJson(reader, ConfigData.class);
                    if (data != null) {
                        if (data.general != null) general = data.general;
                        if (data.stations != null) stations = data.stations;
                        if (data.audio_visual != null) audioVisual = data.audio_visual;
                    }
                }
            } else {
                save();
            }
        } catch (Exception e) {
            GridlessMod.LOGGER.error("Failed to load Gridless Crafting configuration, using defaults", e);
        }
    }

    public static void save() {
        try {
            Path configDir = Services.PLATFORM.getConfigDirectory();
            File dir = configDir.toFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File configFile = configDir.resolve(FILE_NAME).toFile();
            ConfigData data = new ConfigData();
            data.general = general;
            data.stations = stations;
            data.audio_visual = audioVisual;

            try (FileWriter writer = new FileWriter(configFile)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            GridlessMod.LOGGER.error("Failed to save Gridless Crafting configuration", e);
        }
    }
}
