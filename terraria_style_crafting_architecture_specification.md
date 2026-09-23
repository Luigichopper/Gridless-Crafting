# Gridless Crafting (Terraria-Style) Mod & API Specification

## 1. Project Overview & Multi-Platform Strategy

Building a mod that replaces Minecraft's traditional $3 \times 3$ / $2 \times 2$ grid crafting with a Terraria-style list/picker system across **Fabric**, **Forge**, and **NeoForge** (targeting major modern versions up to modern release cycles) requires a clean architectural separation.

### 1.1 The Multi-Loader Architecture
Attempting to write individual mod codebases for each loader creates severe code duplication. The recommended industry standard approach is a **multiloader workspace** using Gradle:

*   **Build System:** Gradle multi-project setup powered by tools like **Stonecutter** (for handling multi-version AST differences across Minecraft versions) combined with **Architectury Loom** or **Fabric Loom + NeoForge Loom**.
*   **Workspace Structure:**
    ```text
    ├── common/          # Platform-agnostic API, recipe models, networking payloads, UI rendering logic
    ├── fabric/          # Fabric mod entry points, Fabric Networking, Fabric Resource Conditions
    ├── neoforge/        # NeoForge mod entry point, NeoForge Registries, Data Attachments
    └── forge/           # Legacy Forge entry points (if targeting older versions where Forge still dominated)
    ```

### 1.2 Multi-Version Strategy (Stonecutter)
Minecraft versions often refactor internal UI classes, networking codecs, and item stack storage:
*   Use **Stonecutter** to maintain version-conditional branches (e.g., handling version transitions where `Component` serialization or `StreamCodec` was introduced).
*   Keep the **Common** module as lean as possible, delegating platform-specific calls (such as opening custom screen handlers or reading config files) to a service interface pattern (`PlatformHelper` via Java `ServiceLoader`).

---

## 2. API vs. Mod Separation

To allow third-party developers to easily integrate custom crafting stations without depending on the entire overhaul mod, split the code into two distinct layers:

1.  **Gridless Crafting API (`gridless-crafting-api`)**:
    *   Zero UI rendering logic.
    *   Defines the recipe abstraction: stations, ingredients, unlock conditions, and consumption rules.
    *   Exposes extension hooks for third-party mods to register custom crafting types/stations.
2.  **Gridless Crafting Engine / Mod (`gridless-crafting`)**:
    *   Depends on the API.
    *   Overrides existing vanilla crafting screens (Crafting Table, Inventory, Furnace, etc.).
    *   Implements the Terraria-style scrollable picker GUI, search bar, category tabs, and crafting animations/sounds.
    *   Includes the configuration system.

---

## 3. Core Engine Mechanics

Unlike pure Terraria (which uses spatial proximity to nearby tiles), this design activates when opening a specific station's GUI, presenting a dynamic, gridless list of craftable items.

### 3.1 Data-Driven Station Model
Define crafting stations dynamically via JSON / Data Pack definitions so any block (or tag) can serve as a station.

#### Example: `data/<namespace>/gridless_stations/workbench.json`
```json
{
  "id": "minecraft:crafting_table",
  "block_tag": "c:workbench",
  "allowed_recipe_types": [
    "minecraft:crafting"
  ],
  "supports_quick_craft": true,
  "override_vanilla_gui": true
}
```

### 3.2 Recipe Indexing & Inventory Scanning
1.  **Recipe Indexing (Startup/Data-Reload):**
    *   On data pack sync, parse all vanilla recipes (`RecipeHolder<?>`).
    *   Flatten shaped and shapeless recipes into simple ingredient count bags:
        $$\text{Recipe} = \{ \text{Output}: \text{ItemStack}, \text{Inputs}: \sum (\text{Ingredient} \times \text{Count}) \}$$
2.  **Inventory Matching (Client-Side for UI, Server-Side for Validation):**
    *   When the screen opens, collect all item counts in the player's inventory.
    *   Partition recipes into three UI states:
        *   **Craftable:** All required items are present in sufficient quantities.
        *   **Missing Ingredients:** Part of the recipe is missing (grayed out or sorted lower).
        *   **Locked / Hidden:** Recipes requiring unknown advancements (configurable).

---

## 4. Smelting & Timed Crafting Conversion

For furnaces, smokers, and blast furnaces, the mod supports two configurable modes:

### Mode A: Traditional GUI (Bypass)
The mod leaves the vanilla furnace container untouched if disabled in configuration.

### Mode B: Instant Fuel-Cost Conversion (Gridless Mode)
*   **Fuel Calculation:**
    Vanilla smelting consumes 1 unit of fuel (e.g., 1 coal = 8 items $\approx$ 1600 ticks of burn time).
    $$\text{Fuel Required} = \left\lceil \frac{\text{Item Count} \times \text{Cook Time per Item}}{\text{Fuel Burn Time}} \right\rceil$$
*   **Fuel Fractional Tracking:**
    To avoid wasting fuel when crafting odd numbers of items (e.g., smelting 1 iron ore with a coal piece that has 8 charges), the API allows a persistent player/station capability to track "Stored Smelting Energy" or round to the nearest whole fuel item.

---

## 5. UI & UX Overhaul Design

Rather than the standard $3 \times 3$ grid:
1.  **Recipe Ribbon (Left / Center):**
    *   Vertical or horizontal scrolling carousel of craftable item icons, directly mirroring Terraria's inventory sidebar.
    *   Active/hovered recipe expands to display the detailed requirement card.
2.  **Ingredient Breakdown Card:**
    *   Shows required materials with indicators: `Iron Ingot: 3/5 (Missing 2)`.
    *   Left-clicking crafts 1; Shift-clicking crafts the maximum possible stack; Right-clicking/Holding rapidly crafts.
3.  **Search & Filtering:**
    *   Text-based filter box.
    *   Category tabs: Weapons, Tools, Armor, Building, Redstone, Miscellaneous.

---

## 6. Networking & Anti-Cheat Validation

Because recipe selection happens client-side via custom UI interaction, **never trust client execution**:

```
[ Client Screen ]
       │  Sends Packet: C2SCraftGridlessRecipePacket(recipeId, amount)
       ▼
[ Server Network Handler ]
       │  1. Verifies player is within interact range of target station block
       │  2. Resolves recipeId from Server RecipeManager
       │  3. Validates player inventory has sufficient input items
       │  4. Deducts items atomically
       │  5. Adds output items to player inventory (or drops if full)
       ▼
[ S2C Sync Response / Inventory Update ]
```

---

## 7. Configuration Specification

Implement a unified configuration format (TOML or JSON5):

```toml
[general]
# Enable or disable gridless crafting globally
enabled = true

# Replace player 2x2 inventory crafting with portable gridless picker
replace_inventory_crafting = true

[stations]
# Enable gridless interface for standard crafting tables
enable_crafting_table = true

# Smelting handling: "TRADITIONAL" or "INSTANT_WITH_FUEL"
furnace_mode = "INSTANT_WITH_FUEL"
smoker_mode = "INSTANT_WITH_FUEL"
blast_furnace_mode = "INSTANT_WITH_FUEL"

[audio_visual]
# Play Terraria-style item drop/craft click sounds
enable_custom_sounds = true
# Number of visible items in recipe picker scroll view
visible_recipe_rows = 6
```

---

## 8. Step-by-Step Implementation Roadmap

1.  **Phase 1: Multi-Loader Scaffolding**
    *   Initialize Gradle with Stonecutter and Architectury Loom.
    *   Set up `common`, `fabric`, and `neoforge` modules.
2.  **Phase 2: Recipe Abstraction & Inventory Bag**
    *   Implement an `IngredientBag` helper that counts items across all inventory slots.
    *   Build the validation algorithm to quickly test a list of recipes against the bag.
3.  **Phase 3: Screen & ScreenHandler Replacement**
    *   Mixin into `CraftingScreen` or redirect `MenuType.CRAFTING` open calls to the custom `GridlessCraftingScreenHandler`.
    *   Render the Terraria scrollable selector using standard Minecraft GUI primitives.
4.  **Phase 4: Server Networking & Transaction Handling**
    *   Create `C2SCraftGridlessRecipePacket` with robust server-side item deduction.
5.  **Phase 5: API Finalization & Documentation**
    *   Publish `gridless-crafting-api` to a Maven repository so add-on developers can declare custom station behaviors.