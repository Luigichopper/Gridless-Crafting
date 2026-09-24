# Gridless Crafting

![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen?style=flat-square)
![Mod Loaders](https://img.shields.io/badge/Loaders-Fabric%20%7C%20Forge%20%7C%20NeoForge-blue?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-green?style=flat-square)
![Java Version](https://img.shields.io/badge/Java-21-orange?style=flat-square)

Gridless Crafting replaces Minecraft's traditional $3 \times 3$ grid crafting screens with a dynamic, scrollable list system inspired by Terraria. View all craftable items at a glance, search recipes instantly, and craft without manual ingredient grid placement.

---

### Features

- **Scrollable Recipe Ribbon**: Vertical carousel displaying all available recipes with full mouse drag and scroll wheel support.
- **Dynamic Inventory Refresh**: Recipe availability updates automatically as items enter or leave your inventory.
- **Ingredient Highlighting**: Outlines inventory slots containing required materials for the selected recipe.
- **Search & Category Filters**: Search by item name or ID, with filtering tabs for Weapons, Tools, Armor, Building, Redstone, Consumables, and Miscellaneous.
- **Flexible Crafting Actions**: Left-click to craft 1, Shift-click to craft max, or hold right-click for continuous rapid crafting.
- **Gridless Smelting**: Optional mode for instant smelting using exact fuel consumption calculations.
- **Config Screen**: Native 3-tab configuration GUI for General, Stations, and Audio & Visual settings.
- **Data-Driven Stations**: Custom stations can be registered via data packs (`data/<namespace>/gridless_stations/*.json`) or Java API.

---

### Developer API

Third-party mod developers can compile against the API JAR:

```groovy
dependencies {
    compileOnly files("libs/gridless-crafting-1.0.0+1.21.1-api.jar")
}
```

---

### Building

```bash
./gradlew build
```

Compiled JARs will be generated in `fabric/build/libs`, `forge/build/libs`, `neoforge/build/libs`, and `common/build/libs`.

