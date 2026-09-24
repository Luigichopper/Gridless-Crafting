package com.gridless.api.station;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.gridless.GridlessMod;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StationRegistry extends SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>> {
    private static final Gson GSON = new Gson();
    public static final String DIRECTORY = "gridless_stations";
    public static final StationRegistry INSTANCE = new StationRegistry();

    private static final Map<ResourceLocation, GridlessStation> STATIONS = new ConcurrentHashMap<>();

    // Built-in default station IDs
    public static final ResourceLocation WORKBENCH = ResourceLocation.fromNamespaceAndPath("minecraft", "crafting_table");
    public static final ResourceLocation INVENTORY = ResourceLocation.fromNamespaceAndPath("gridless", "inventory");
    public static final ResourceLocation FURNACE = ResourceLocation.fromNamespaceAndPath("minecraft", "furnace");
    public static final ResourceLocation BLAST_FURNACE = ResourceLocation.fromNamespaceAndPath("minecraft", "blast_furnace");
    public static final ResourceLocation SMOKER = ResourceLocation.fromNamespaceAndPath("minecraft", "smoker");

    static {
        registerDefaults();
    }

    public StationRegistry() {
    }

    @Override
    protected Map<ResourceLocation, JsonElement> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, JsonElement> map = new HashMap<>();
        FileToIdConverter converter = FileToIdConverter.json(DIRECTORY);
        for (Map.Entry<ResourceLocation, net.minecraft.server.packs.resources.Resource> entry : converter.listMatchingResources(resourceManager).entrySet()) {
            ResourceLocation id = converter.fileToId(entry.getKey());
            try (java.io.Reader reader = entry.getValue().openAsReader()) {
                JsonElement json = com.google.gson.JsonParser.parseReader(reader);
                map.put(id, json);
            } catch (Exception e) {
                GridlessMod.LOGGER.error("Failed to read station JSON: {}", entry.getKey(), e);
            }
        }
        return map;
    }

    public static void registerDefaults() {
        // Workbench
        register(new GridlessStation(
                WORKBENCH,
                ResourceLocation.fromNamespaceAndPath("c", "workbench"),
                List.of(ResourceLocation.fromNamespaceAndPath("minecraft", "crafting")),
                true,
                true,
                StationMode.NORMAL
        ));

        // Player Portable Inventory
        register(new GridlessStation(
                INVENTORY,
                null,
                List.of(ResourceLocation.fromNamespaceAndPath("minecraft", "crafting")),
                true,
                false,
                StationMode.NORMAL
        ));

        // Furnace
        register(new GridlessStation(
                FURNACE,
                ResourceLocation.fromNamespaceAndPath("c", "furnace"),
                List.of(ResourceLocation.fromNamespaceAndPath("minecraft", "smelting")),
                true,
                true,
                StationMode.SMELTING
        ));

        // Blast Furnace
        register(new GridlessStation(
                BLAST_FURNACE,
                ResourceLocation.fromNamespaceAndPath("c", "blast_furnace"),
                List.of(ResourceLocation.fromNamespaceAndPath("minecraft", "blasting")),
                true,
                true,
                StationMode.SMELTING
        ));

        // Smoker
        register(new GridlessStation(
                SMOKER,
                ResourceLocation.fromNamespaceAndPath("c", "smoker"),
                List.of(ResourceLocation.fromNamespaceAndPath("minecraft", "smoking")),
                true,
                true,
                StationMode.SMELTING
        ));
    }

    public static void register(GridlessStation station) {
        STATIONS.put(station.getId(), station);
    }

    public static GridlessStation get(ResourceLocation id) {
        return STATIONS.get(id);
    }

    public static Optional<GridlessStation> findStationForBlock(BlockState state) {
        for (GridlessStation station : STATIONS.values()) {
            if (station.matchesBlock(state)) {
                return Optional.of(station);
            }
        }
        return Optional.empty();
    }

    public static Collection<GridlessStation> getAllStations() {
        return Collections.unmodifiableCollection(STATIONS.values());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager resourceManager, ProfilerFiller profiler) {
        STATIONS.clear();
        registerDefaults();

        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            try {
                JsonObject json = entry.getValue().getAsJsonObject();
                ResourceLocation id = json.has("id") ? ResourceLocation.parse(json.get("id").getAsString()) : fileId;
                ResourceLocation blockTag = json.has("block_tag") ? ResourceLocation.parse(json.get("block_tag").getAsString()) : null;

                List<ResourceLocation> allowedRecipeTypes = new ArrayList<>();
                if (json.has("allowed_recipe_types")) {
                    JsonArray typesArray = json.getAsJsonArray("allowed_recipe_types");
                    for (JsonElement typeElement : typesArray) {
                        allowedRecipeTypes.add(ResourceLocation.parse(typeElement.getAsString()));
                    }
                }

                boolean supportsQuickCraft = !json.has("supports_quick_craft") || json.get("supports_quick_craft").getAsBoolean();
                boolean overrideVanillaGui = !json.has("override_vanilla_gui") || json.get("override_vanilla_gui").getAsBoolean();
                
                StationMode mode = StationMode.NORMAL;
                if (json.has("mode")) {
                    try {
                        mode = StationMode.valueOf(json.get("mode").getAsString().toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException ignored) {}
                } else if (allowedRecipeTypes.stream().anyMatch(t -> t.getPath().contains("smelt") || t.getPath().contains("blast") || t.getPath().contains("smok"))) {
                    mode = StationMode.SMELTING;
                }

                GridlessStation station = new GridlessStation(id, blockTag, allowedRecipeTypes, supportsQuickCraft, overrideVanillaGui, mode);
                register(station);
                GridlessMod.LOGGER.debug("Loaded gridless station: {}", id);
            } catch (Exception e) {
                GridlessMod.LOGGER.error("Failed to parse station JSON: {}", fileId, e);
            }
        }
    }
}
