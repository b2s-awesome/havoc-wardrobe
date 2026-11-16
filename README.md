# HAVOC Wardrobe

Server-side `/wardrobe` for Minecraft **26.3** (Fabric). Save up to 9 armour sets and swap between them from a chest menu. Players do **not** need the mod installed (works with vanilla clients).

Based on [Easy Wardrobe](https://github.com/KyNarec/Easy-Wardrobe) by **KyNarec** (GPL-3.0). This is a modified version: rewritten in Java for 26.3, with dupe-safe slot handling, HAVOC branding and optional [EconomyCraft](https://github.com/PhilipB06/EconomyCraft) costs.

## Commands

| Command | What it does |
|---|---|
| `/wardrobe` | Opens the wardrobe |
| `/wd` | Same (aliases set in config; `/w` is not used because it is vanilla whisper) |

## Menu

- Rows 1-4: helmet, chestplate, leggings, boots of each set. Click with a piece to store it; click a stored piece to take it; shift-click to send it to your inventory. Only items that fit that slot are accepted.
- Row 5: pink dye = wear this set, lime dye = currently worn (click to take it off into the column), gold ingot = buy the next slot.
- Row 6: info, close, balance.
- The worn set's column shows what you are wearing right now (red names). Nothing is stored twice.
- Curse of Binding armour blocks swapping (unless in creative).

## Config: `config/havoc_wardrobe.json`

| Key | Default | Meaning |
|---|---|---|
| `title` | `&8&lHAVOC &7» &0Wardrobe` | Menu title (`&` colour codes) |
| `prefix` | `&8[&c&lHAVOC&8] &7` | Chat message prefix |
| `aliases` | `["wd"]` | Extra command names |
| `economyEnabled` | `true` | Use EconomyCraft costs if EconomyCraft is installed |
| `freeSlots` | `3` | Slots everyone gets for free |
| `slotPrices` | `[25000, 50000, 100000, 250000, 500000, 1000000]` | Price of slot 4, 5, 6... (last price repeats) |
| `equipCost` | `0` | Money taken each time a set is equipped (0 = free) |

Without EconomyCraft (or with `economyEnabled: false`) all 9 slots are free and nothing costs money. Restart the server after editing the config.

## Data

Saved per player in `<world>/havoc_wardrobe/<uuid>.dat`. A file that cannot be read is copied to `<uuid>.broken-<time>.dat` instead of being overwritten.

## Building

`./gradlew build` (JDK 25). GitHub Actions builds the jar on every push (see the Actions tab, artifact `havoc-wardrobe`).

## Licence

GPL-3.0-or-later, same as Easy Wardrobe. The EconomyCraft API v1 interfaces under `src/main/java/com/reazip/` (GPL-3.0, by PhilipB06) are included only to compile against and are excluded from the built jar.
