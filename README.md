# Gridless Crafting (Terraria-Style) Mod & API

A multi-loader Minecraft mod (**Fabric**, **Forge**, & **NeoForge**) for Minecraft 1.21.1 that completely reimagines Minecraft's traditional $3 \times 3$ and $2 \times 2$ grid crafting into a dynamic, scrollable Terraria-style list and picker system.

---

## Features

- **Terraria-Style Recipe Ribbon**: A vertical scrollable carousel displaying all craftable item icons, with full mouse dragging and scroll wheel support.
- **Dynamic Inventory Refresh**: Instantly updates the craftable list as items enter or leave your inventory.
- **Ingredient Breakdown & Highlighting**:
  - Displays required materials with clear `have / need` counters and missing material alerts.
  - Automatically highlights inventory slots containing inputs for the currently selected recipe (Emerald Green outline).
- **Search & Category Filtering**: Live text search across item names and resource identifiers, paired with intuitive category tabs:
  - `All`, `Weapons`, `Tools`, `Armor`, `Building`, `Redstone`, `Consumables`, `Miscellaneous`.
- **Flexible Crafting Actions**:
  - **Left-Click**: Craft 1 item.
  - **Shift-Click / Craft Max**: Craft the maximum possible stack.
  - **Right-Click / Hold**: Continuous rapid crafting.
- **Smelting & Timed Conversion**:
  - **Mode A (Traditional)**: Preserves vanilla furnace screens.
  - **Mode B (Instant with Fuel)**: Gridless smelting with exact fuel calculation $\lceil \frac{\text{Item Count} \times \text{Cook Time}}{\text{Fuel Burn Time}} \rceil$ and automatic fuel deduction.
- **Native 3-Tab Config Screen**: Native GUI configuration split across **General**, **Stations**, and **Audio & Visual** tabs (supported natively across Fabric, Forge, and NeoForge mod menus).
- **Data-Driven Station Model**: Stations are dynamically defined via JSON in data packs (`data/<namespace>/gridless_stations/*.json`). Any block or tag can serve as a station.
- **Robust Anti-Cheat Networking**: Client screens send crafting intent (`C2SCraftGridlessRecipePayload`); the server independently verifies player distance, station block validity, atomically verifies input items, deducts materials, and awards outputs.

---

## Multi-Loader Architecture

```text
Gridless/
├── common/             # Platform-agnostic API, recipe models, networking payloads, UI ribbon logic, data loader
├── fabric/             # Fabric entry points, networking registration, Fabric PlatformHelper & ModMenu integration
├── forge/              # Forge entry points, registry listeners, Forge PlatformHelper & Config screen factory
└── neoforge/           # NeoForge entry points, registry listeners, NeoForge PlatformHelper & Config screen factory
```

- **Target Version**: Minecraft 1.21.1
- **Java**: Java 21 LTS
- **Build System**: Architectury Loom with multi-project Gradle

---

## Developer API & API JAR

Gridless Crafting provides an API for other mod developers to register custom stations, define recipe handlers, or integrate custom crafting mechanics.

### Compiling Against the API

You can compile against the lightweight API JAR (`gridless-crafting-1.0.0+1.21.1-api.jar`) which exposes `com.gridless.api.**`:

```groovy
dependencies {
    compileOnly files("libs/gridless-crafting-1.0.0+1.21.1-api.jar")
}
```

### Adding Custom Stations via JSON

Create a JSON file in your mod's data pack at `data/<your_mod>/gridless_stations/<station_name>.json`:

```json
{
  "id": "mymod:infusion_altar",
  "block_tag": "mymod:altars",
  "allowed_recipe_types": [
    "mymod:infusion",
    "minecraft:crafting"
  ],
  "supports_quick_craft": true,
  "override_vanilla_gui": true,
  "mode": "NORMAL"
}
```

### Registering Stations in Code

```java
import com.gridless.api.station.StationRegistry;
import com.gridless.api.station.GridlessStation;
import com.gridless.api.station.StationMode;

StationRegistry.register(new GridlessStation(
    ResourceLocation.fromNamespaceAndPath("mymod", "altar"),
    ResourceLocation.fromNamespaceAndPath("mymod", "altars"),
    List.of(ResourceLocation.fromNamespaceAndPath("mymod", "infusion")),
    true,
    true,
    StationMode.NORMAL
));
```

---

## Building

To build all mod JARs and the API JAR locally:

```bash
# Build all production mod JARs and the API JAR
./gradlew build
```

Generated outputs:
- **Fabric**: `fabric/build/libs/gridless-crafting-1.0.0+1.21.1.jar`
- **Forge**: `forge/build/libs/gridless-crafting-1.0.0+1.21.1.jar`
- **NeoForge**: `neoforge/build/libs/gridless-crafting-1.0.0+1.21.1.jar`
- **API JAR**: `common/build/libs/gridless-crafting-1.0.0+1.21.1-api.jar`

---

## License

This project is licensed under the [MIT License](LICENSE).
