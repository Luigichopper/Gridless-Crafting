package com.gridless.api.station;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gridless.GridlessMod;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StationRegistry extends SimplePreparableReloadListener<Map<Identifier, JsonElement>> {
    private static final Gson GSON = new Gson();
    public static final String DIRECTORY = "gridless_stations";
    public static final StationRegistry INSTANCE = new StationRegistry();

    private static final Map<Identifier, GridlessStation> STATIONS = new ConcurrentHashMap<>();

    // Built-in default station IDs
    public static final Identifier WORKBENCH = Identifier.fromNamespaceAndPath("minecraft", "crafting_table");
    public static final Identifier INVENTORY = Identifier.fromNamespaceAndPath("gridless", "inventory");
    public static final Identifier FURNACE = Identifier.fromNamespaceAndPath("minecraft", "furnace");
    public static final Identifier BLAST_FURNACE = Identifier.fromNamespaceAndPath("minecraft", "blast_furnace");
    public static final Identifier SMOKER = Identifier.fromNamespaceAndPath("minecraft", "smoker");

    static {
        registerDefaults();
    }

    public StationRegistry() {
    }

    public static void registerDefaults() {
        // Workbench
        register(new GridlessStation(
                WORKBENCH,
                Identifier.fromNamespaceAndPath("c", "workbench"),
                List.of(Identifier.fromNamespaceAndPath("minecraft", "crafting")),
                true,
                true,
                StationMode.NORMAL
        ));

        // Player Portable Inventory
        register(new GridlessStation(
                INVENTORY,
                null,
                List.of(Identifier.fromNamespaceAndPath("minecraft", "crafting")),
                true,
                false,
                StationMode.NORMAL
        ));

        // Furnace
        register(new GridlessStation(
                FURNACE,
                Identifier.fromNamespaceAndPath("c", "furnace"),
                List.of(Identifier.fromNamespaceAndPath("minecraft", "smelting")),
                true,
                true,
                StationMode.SMELTING
        ));

        // Blast Furnace
        register(new GridlessStation(
                BLAST_FURNACE,
                Identifier.fromNamespaceAndPath("c", "blast_furnace"),
                List.of(Identifier.fromNamespaceAndPath("minecraft", "blasting")),
                true,
                true,
                StationMode.SMELTING
        ));

        // Smoker
        register(new GridlessStation(
                SMOKER,
                Identifier.fromNamespaceAndPath("c", "smoker"),
                List.of(Identifier.fromNamespaceAndPath("minecraft", "smoking")),
                true,
                true,
                StationMode.SMELTING
        ));
    }

    public static void register(GridlessStation station) {
        STATIONS.put(station.getId(), station);
    }

    public static GridlessStation get(Identifier id) {
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
    protected Map<Identifier, JsonElement> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Identifier, JsonElement> map = new HashMap<>();
        FileToIdConverter lister = FileToIdConverter.json(DIRECTORY);
        for (Map.Entry<Identifier, net.minecraft.server.packs.resources.Resource> entry : lister.listMatchingResources(resourceManager).entrySet()) {
            Identifier file = entry.getKey();
            Identifier id = lister.fileToId(file);
            try (java.io.Reader reader = entry.getValue().openAsReader()) {
                JsonElement json = JsonParser.parseReader(reader);
                map.put(id, json);
            } catch (Exception e) {
                GridlessMod.LOGGER.error("Couldn't parse data file {} from {}", id, file, e);
            }
        }
        return map;
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> resources, ResourceManager resourceManager, ProfilerFiller profiler) {
        STATIONS.clear();
        registerDefaults();

        for (Map.Entry<Identifier, JsonElement> entry : resources.entrySet()) {
            Identifier fileId = entry.getKey();
            try {
                JsonObject json = entry.getValue().getAsJsonObject();
                Identifier id = json.has("id") ? Identifier.parse(json.get("id").getAsString()) : fileId;
                Identifier blockTag = json.has("block_tag") ? Identifier.parse(json.get("block_tag").getAsString()) : null;

                List<Identifier> allowedRecipeTypes = new ArrayList<>();
                if (json.has("allowed_recipe_types")) {
                    JsonArray typesArray = json.getAsJsonArray("allowed_recipe_types");
                    for (JsonElement typeElement : typesArray) {
                        allowedRecipeTypes.add(Identifier.parse(typeElement.getAsString()));
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
