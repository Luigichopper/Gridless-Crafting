# Gridless Crafting API Documentation

Welcome to the **Gridless Crafting API** developer guide. This guide explains how to integrate custom crafting stations, query recipes, utilize inventory scanners, and leverage instant smelting math within third-party mods and data packs.

The API is platform-agnostic and runs identically across **Fabric**, **Forge**, and **NeoForge**.

---

## Table of Contents

1. [Architecture & Design](#1-architecture--design)
2. [Adding the API to Your Build](#2-adding-the-api-to-your-build)
3. [Crafting Stations (`com.gridless.api.station`)](#3-crafting-stations-comgridlessapistation)
   - [Station Modes](#station-modes)
   - [Method A: Data Pack Registration (JSON)](#method-a-data-pack-registration-json)
   - [Method B: Programmatic Java Registration](#method-b-programmatic-java-registration)
   - [Querying Stations](#querying-stations)
4. [Recipe Modeling & Indexing (`com.gridless.api.recipe`)](#4-recipe-modeling--indexing-comgridlessapirecipe)
   - [RecipeIndexer](#recipeindexer)
   - [GridlessRecipe](#gridlessrecipe)
   - [CountedIngredient](#countedingredient)
   - [IngredientBag](#ingredientbag)
   - [RecipeCategory](#recipecategory)
5. [Instant Smelting & Fuel Calculations (`com.gridless.api.smelting`)](#5-instant-smelting--fuel-calculations-comgridlessapismelting)
6. [Client UI & Networking Hooks](#6-client-ui--networking-hooks)

---

## 1. Architecture & Design

Gridless Crafting's API is contained within the `com.gridless.api` package in the `common` subproject:

* **Zero Hard UI Dependencies:** The API classes contain no rendering or screen code. They can safely run in headless server environments, data generators, and client instances.
* **Flattened Recipe Abstraction:** Converts vanilla shaped/shapeless grid recipes into flat ingredient pools with exact count requirements ($\sum \text{Ingredient} \times \text{Count}$).
* **Multi-Variant Aggregation:** Recipes producing identical output items are grouped into a single parent recipe with selectable variants (e.g., crafting beds with different wool colors, or recipes with alternate tags).

---

## 2. Adding the API to Your Build

You can compile against the standalone API JAR without bundling or depending on the full client mod.

### Gradle (Groovy DSL)

```groovy
repositories {
    mavenLocal()
    // When published to a Maven repository:
    // maven { url = "https://maven.example.com/" }
}

dependencies {
    // API-only compile dependency
    compileOnly("com.gridless:gridless-crafting:${gridless_version}:api")

    // Or from a local flat directory:
    // compileOnly files("libs/gridless-crafting-${gridless_version}-api.jar")
}
```

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    compileOnly("com.gridless:gridless-crafting:$gridlessVersion:api")
}
```

---

## 3. Crafting Stations (`com.gridless.api.station`)

A **`GridlessStation`** defines a block or context where gridless crafting can occur, which recipe types are allowed, and how the station behaves.

### Station Modes

Defined in `com.gridless.api.station.StationMode`:
* `NORMAL`: Standard crafting station (e.g. Crafting Table, Inventory). Consumes ingredients instantly.
* `SMELTING`: Smelting or cooking station (e.g. Furnace, Blast Furnace, Smoker). Calculates required fuel consumption and supports designated fuel slot highlights.

---

### Method A: Data Pack Registration (JSON)

Stations can be added or modified entirely through data packs without writing code.

Place station JSON files in:
`data/<namespace>/gridless_stations/<station_name>.json`

#### JSON Schema & Example

```json
{
  "id": "mymod:infuser",
  "block_tag": "mymod:infusers",
  "allowed_recipe_types": [
    "mymod:infusing",
    "minecraft:crafting"
  ],
  "supports_quick_craft": true,
  "override_vanilla_gui": true,
  "mode": "NORMAL"
}
```

#### Field Reference

| Field | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `id` | `Identifier` | File path | Unique station identifier (e.g. `mymod:infuser`). |
| `block_tag` | `Identifier` | `null` | Tag ID (e.g. `c:workbench`, `mymod:infusers`) or direct block ID that triggers this station when right-clicked. |
| `allowed_recipe_types` | `Array<Identifier>` | `[]` | List of recipe type IDs permitted at this station (e.g. `["minecraft:crafting"]`). |
| `supports_quick_craft`| `boolean` | `true` | Allows shift-click (craft max) and hold-to-craft mechanics. |
| `override_vanilla_gui`| `boolean` | `true` | When `true`, intercept right-clicking the matching block to open the Gridless UI. |
| `mode` | `String` | `"NORMAL"` | Either `"NORMAL"` or `"SMELTING"`. Auto-detected as `SMELTING` if recipe types include smelting/blasting/smoking. |

---

### Method B: Programmatic Java Registration

You can also register custom stations in Java using `StationRegistry.register(...)`.

```java
import com.gridless.api.station.GridlessStation;
import com.gridless.api.station.StationMode;
import com.gridless.api.station.StationRegistry;
import net.minecraft.resources.Identifier;
import java.util.List;

public class MyModStations {
    public static final Identifier MY_STATION_ID = Identifier.fromNamespaceAndPath("mymod", "advanced_workbench");

    public static void registerStations() {
        GridlessStation myStation = new GridlessStation(
                MY_STATION_ID,
                Identifier.fromNamespaceAndPath("mymod", "advanced_workbenches"), // Block tag or block ID
                List.of(
                        Identifier.fromNamespaceAndPath("minecraft", "crafting"),
                        Identifier.fromNamespaceAndPath("mymod", "advanced_crafting")
                ),
                true,                 // supportsQuickCraft
                true,                 // overrideVanillaGui
                StationMode.NORMAL    // mode
        );

        StationRegistry.register(myStation);
    }
}
```

#### Built-in Station Constants

`StationRegistry` provides pre-registered defaults:
* `StationRegistry.WORKBENCH`: `minecraft:crafting_table`
* `StationRegistry.INVENTORY`: `gridless:inventory`
* `StationRegistry.FURNACE`: `minecraft:furnace`
* `StationRegistry.BLAST_FURNACE`: `minecraft:blast_furnace`
* `StationRegistry.SMOKER`: `minecraft:smoker`

---

### Querying Stations

```java
// Lookup station by identifier
GridlessStation station = StationRegistry.get(Identifier.fromNamespaceAndPath("minecraft", "crafting_table"));

// Lookup station matching a world block state
BlockState state = level.getBlockState(pos);
Optional<GridlessStation> matched = StationRegistry.findStationForBlock(state);

if (matched.isPresent()) {
    GridlessStation activeStation = matched.get();
    List<Identifier> allowedTypes = activeStation.getAllowedRecipeTypeIds();
}
```

---

## 4. Recipe Modeling & Indexing (`com.gridless.api.recipe`)

### RecipeIndexer

The `RecipeIndexer` maintains an indexed, thread-safe cache of all recipes loaded in the current game instance.

#### Querying Recipes

```java
import com.gridless.api.recipe.GridlessRecipe;
import com.gridless.api.recipe.RecipeIndexer;
import net.minecraft.resources.Identifier;
import java.util.List;

// 1. Get a specific indexed recipe by its recipe ID
GridlessRecipe recipe = RecipeIndexer.get(Identifier.fromNamespaceAndPath("minecraft", "stick"));

// 2. Query all recipes for specific recipe type IDs (grouped with output variants)
List<GridlessRecipe> craftingRecipes = RecipeIndexer.getForTypes(
        List.of(Identifier.fromNamespaceAndPath("minecraft", "crafting"))
);

// 3. Get all registered recipes across all types
Collection<GridlessRecipe> allRecipes = RecipeIndexer.getAll();
```

---

### GridlessRecipe

A `GridlessRecipe` represents an output item and its flattened list of `CountedIngredient` inputs.

#### Inspecting Recipe Data

```java
ItemStack output = recipe.getResult();
List<CountedIngredient> inputs = recipe.getInputs();
RecipeCategory category = recipe.getCategory();
boolean needs3x3 = recipe.requires3x3(); // True if recipe cannot fit in a 2x2 grid
int cookTime = recipe.getCookTime();     // For smelting recipes
float experience = recipe.getExperience();
```

#### Crafting Availability & Checks

```java
import com.gridless.api.recipe.IngredientBag;

IngredientBag bag = IngredientBag.fromPlayer(player);

// Check if player has all required ingredients
boolean canCraft = recipe.isCraftable(bag);

// Calculate maximum crafts player can perform with current inventory
int maxCraftableCount = recipe.maxCraftable(bag);

// Get missing ingredients map: CountedIngredient -> Missing Amount
Map<CountedIngredient, Integer> missing = recipe.getMissingIngredients(bag);
```

#### Managing Recipe Variants

When multiple recipes produce the same item (e.g. Different wood types for sticks or chests), they are grouped under a leader `GridlessRecipe`:

```java
int variantCount = recipe.getVariantCount();

if (variantCount > 1) {
    // Cycle to next variant
    recipe.cycleVariant(1);

    // Automatically select the best variant that the player has materials for
    recipe.autoSelectBestVariant(bag);

    // Get the currently selected active variant
    GridlessRecipe activeVariant = recipe.getActiveVariant();
    Identifier currentRecipeId = activeVariant.getId();
}
```

---

### CountedIngredient

Combines a vanilla `Ingredient` with an exact integer requirement count:

```java
CountedIngredient input = recipe.getInputs().get(0);

int amountNeeded = input.getCount();
Ingredient ingredient = input.getIngredient();

// Test if an item stack matches this ingredient requirement
boolean matches = input.test(playerStack);

// Get array of sample ItemStacks for rendering or tooltips
ItemStack[] samples = input.getMatchingStacks();
```

---

### IngredientBag

A lightweight, indexed snapshot of player or container contents designed for rapid recipe requirement checking.

```java
import com.gridless.api.recipe.IngredientBag;

// Create snapshot from player inventory (includes hotbar, main inv, and offhand)
IngredientBag bag = IngredientBag.fromPlayer(player);

// Check count of a specific Item
int ironCount = bag.getCount(Items.IRON_INGOT);

// Check count matching a vanilla Ingredient (matches tags, item lists, etc.)
int woodCount = bag.countMatching(Ingredient.of(ItemTags.PLANKS));

// Check if bag satisfies a CountedIngredient
boolean satisfied = bag.has(countedIngredient);
```

---

### RecipeCategory

Recipes are categorized into intuitive groupings for filtering:

* `ALL`
* `WEAPONS`
* `TOOLS`
* `ARMOR`
* `BUILDING`
* `REDSTONE`
* `CONSUMABLES`
* `MISC`

#### Auto-Classification

You can categorize any `ItemStack` using `RecipeCategory.classify(ItemStack)`:

```java
RecipeCategory cat = RecipeCategory.classify(new ItemStack(Items.DIAMOND_SWORD));
// Returns RecipeCategory.WEAPONS
```

---

## 5. Instant Smelting & Fuel Calculations (`com.gridless.api.smelting`)

`SmeltingCalculator` calculates burn durations and fuel costs for instant smelting stations:

```java
import com.gridless.api.smelting.SmeltingCalculator;

int craftQuantity = 4;
int cookTimePerItem = recipe.getCookTime(); // Typically 200 ticks for vanilla furnace

// 1. Calculate fuel requirement for player
SmeltingCalculator.FuelCost cost = SmeltingCalculator.calculateFuelCost(player, craftQuantity, cookTimePerItem);

boolean hasEnoughFuel = cost.hasEnough;
int itemsNeeded = cost.fuelItemsNeeded;
Item fuelItem = cost.recommendedFuel;
int fuelSlot = cost.primaryFuelSlot; // Slot index in player inventory

// 2. Query maximum items player can smelt with available fuel
int maxSmeltable = SmeltingCalculator.getMaxSmeltableWithFuel(player, cookTimePerItem);

// 3. Inspect fuel burn duration of any stack
int burnTicks = SmeltingCalculator.getBurnDuration(new ItemStack(Items.COAL), level);
// Returns 1600
```

---

## 6. Client UI & Networking Hooks

To open the Gridless Crafting screen programmatically for a player on the client side:

```java
import com.gridless.client.screen.GridlessCraftingScreen;
import com.gridless.menu.GridlessCraftingMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import java.util.Optional;

// Open screen for a specific station at a block position
Minecraft mc = Minecraft.getInstance();
mc.setScreen(new GridlessCraftingScreen<>(
        new GridlessCraftingMenu(0, mc.player.getInventory()),
        mc.player.getInventory(),
        Component.translatable("container.crafting"),
        customStation,
        Optional.of(blockPos),
        false // isInventoryCrafting
));
```

### Crafting Network Payload

Crafting transactions are submitted using the common network packet `C2SCraftGridlessRecipePayload`:

```java
import com.gridless.network.C2SCraftGridlessRecipePayload;

// Send craft request to server (recipe ID, quantity, optional station block pos)
C2SCraftGridlessRecipePayload payload = new C2SCraftGridlessRecipePayload(recipe.getId(), 1, Optional.of(blockPos));
```

The server automatically validates ingredient presence, station proximity, and inventory space before executing the craft transaction atomically.
