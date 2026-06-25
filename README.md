# NBT to Data API 🛡️📦

A lightweight, developer-friendly Kotlin DSL and SNBT translation library for Minecraft mod developers. 

This API provides a unified way to edit ItemStack data, automatically handling the architectural gap between legacy NBT-tag versions and the modern Data Component system.

---

## ⚠️ Project Status: Alpha
This project is currently in its **Alpha** stage. The core APIs are stable and optimized, but breaking changes may occur as we continue to refine platform compatibility. Use with care in production environments.

---

## 🗺️ Supported Versions

* **Branch `1.20`**: Supports Minecraft **1.20 to 1.20.4** (Fabric & Forge).
* **Branch `1.21`**: Supports Minecraft **1.21 to 1.21.1** (Fabric & NeoForge).

---

## 📦 How to include in your project

Add the JitPack repository to your `build.gradle` (or `settings.gradle`):

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}
```

Then, add the API dependency to your common or loader-specific projects:

```gradle
dependencies {
    // Replace 'VERSION' with the latest release tag or commit hash
    modImplementation "com.github.aleksti21:NBTtoDataAPI:VERSION"
    
    // If you are using Architectury, you can also bundle/include it:
    // include "com.github.aleksti21:NBTtoDataAPI:VERSION"
}
```

---

## 🚀 Features

### 1. `editData` — Unified Kotlin DSL Builder
Provides a clean, declarative Kotlin DSL to edit item stack properties. The builder syntax remains **identical** across all supported Minecraft versions, translating your code to raw NBT tags on 1.20.x, and to Data Components on 1.21.x under the hood.

Here is a comprehensive example demonstrating every available builder tag in action:

```kotlin
item.editData {
    // Basic Properties
    damage = 100
    customModelData = 12345
    unbreakable = true
    mapId = 42
    fireworkFlight = 3

    // Tooltip Hide Flags
    hideEnchantments = true
    hideAttributes = true
    hideUnbreakable = true
    hideAdditionalTooltip = true

    // Unified Enchantments (automatically maps to StoredEnchantments on books!)
    enchantments {
        "minecraft:sharpness"(5)
        "minecraft:fire_aspect"(2)
        -"minecraft:fire_aspect" // Easily remove any enchantment by ID
    }

    // Display Name and Lore
    display {
        setName("Doom Artifact", color = "dark_red", italic = true)
        +"A mysterious sword forged in the nether." // Add lore line via "+"
        +"It glows with unholy energy..."
    }

    // Player Skull Properties
    skull {
        name = "aleksti21"
        textureBase64 = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2I5M2Zl..."
    }

    // Custom Food Properties
    food {
        nutrition = 6
        saturation = 0.8f
        canAlwaysEat = true
        eatSeconds = 2.0f
        convertsTo = "minecraft:bowl" // Item left in hand after eating
        effects.add(FoodEffectData("minecraft:regeneration", 100, 1, 1.0f))
    }

    // Banner Pattern Layers
    banner {
        "stripe_top"(DyeColor.RED)
        "base"(DyeColor.BLUE)
    }

    // Written / Writable Book Data (pages are automatically mapped)
    book {
        title = "The Ancient Scroll"
        author = "Alex"
        generation = 0
        resolved = true
        +"Page 1: The journey begins..."
        +"Page 2: The end of NBT is near."
    }

    // Attribute Modifiers
    attributes {
        "minecraft:generic.attack_damage"(10.0, modifierName = "artifact_boost")
    }

    // Adventure Predicates (Accepts varargs of block IDs)
    canDestroy("minecraft:stone", "minecraft:iron_ore")
    canPlaceOn("minecraft:obsidian")

    // Custom Potions
    potion {
        id = "minecraft:strong_healing"
        customColor = 16711680 // Custom RGB color
    }

    // Container Inventory (Slot operators supported!)
    inventory {
        0(ItemStack(Items.DIAMOND, 64)) // Place 64 diamonds in slot 0
        1(ItemStack(Items.GOLD_INGOT, 32))
        !1 // Remove items from slot 1 using "!"
    }

    // Direct Block Entity Tag Editing
    blockEntityData {
        putString("CustomSpawnerTag", "MyValue")
    }

    // Direct Custom NBT Data Editing
    customData {
        putBoolean("MySecretModFlag", true)
    }
}
```

### 2. `applySNBT` — Legacy SNBT Translation
Allows you to feed legacy SNBT (String NBT) strings directly into an ItemStack. The API parses, cleans, and translates the data. It maps it directly to raw NBT on 1.20.x, and translates it into modern Data Components on 1.21.x.

```kotlin
val legacyNbtString = """
{
  Unbreakable: 1b,
  HideFlags: 1,
  display: {
    Name: '{"text": "Apple of God", "color": "yellow"}',
    Lore: ['{"text": "Grants divine powers..."}']
  }
}
""".trimIndent()

// Parses, cleans, and applies the legacy structure to the item stack
item.applySNBT(legacyNbtString, registries)
```

---

## ⚖️ License

This project is licensed under the **ZLA License** (ZLib Aleksti edition).

You are free to use, modify, and redistribute the source code. You may commercialize compiled binary versions of this mod, but you must keep the source code open-source and compilable for free by anyone. Refer to the `LICENSE.md` file for full details.