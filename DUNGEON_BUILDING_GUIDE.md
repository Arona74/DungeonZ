# Dungeon Building Guide

This guide walks you through the process of creating a custom dungeon for **DungeonZ - BLIS**.
For the full technical reference (all JSON fields with comments), see the [README](README.md).

---

## Table of Contents

1. [How It All Fits Together](#1-how-it-all-fits-together)
2. [Mandatory Files Checklist](#2-mandatory-files-checklist)
3. [Step 1 – Build Your Structure](#3-step-1--build-your-structure)
4. [Step 2 – Set Up the Dungeon Config](#4-step-2--set-up-the-dungeon-config)
5. [Special Blocks Reference](#5-special-blocks-reference)
6. [Step 3 – Create Loot Tables](#6-step-3--create-loot-tables)
7. [Step 4 – Register the Overworld Structure](#7-step-4--register-the-overworld-structure)
8. [Step 5 – Add Translations](#8-step-5--add-translations)
9. [Tips & Common Mistakes](#9-tips--common-mistakes)

---

## 1. How It All Fits Together

A dungeon is made of three main parts that work together:

```
Overworld Portal Block
        │  (player interacts → teleports in)
        ▼
Dungeon Structure (NBT rooms assembled by Jigsaw)
        │  (special blocks replaced at runtime)
        ▼
 Mob/Boss spawns ── Gates ── Chests ── Exit portal
```

- **The overworld portal** is a `dungeonz:dungeon_portal` block placed/generated in the world. Players interact with it to enter.
- **The dungeon instance** lives in a custom `dungeonz:dungeon` dimension. Each dungeon is procedurally assembled from your NBT room files using Minecraft's Jigsaw system.
- **The dungeon config JSON** ties everything together: it defines difficulties, which blocks become mobs/bosses/chests, loot tables, cooldowns, and more.

---

## 2. Mandatory Files Checklist

Every dungeon needs at least these files, all delivered via a **datapack** (or the mod's own `src/main/resources`):

| # | File | Path |
|---|------|------|
| 1 | **Dungeon config** | `data/dungeonz/dungeon/YOUR_DUNGEON.json` |
| 2 | **Structure NBT(s)** | `data/dungeonz/structures/YOUR_ROOM.nbt` (one or more) |
| 3 | **Template pool(s)** | `data/dungeonz/worldgen/template_pool/YOUR_DUNGEON/...json` |
| 4 | **Loot tables** | `data/dungeonz/loot_tables/chests/YOUR_DUNGEON_*.json` |
| 5 | **Worldgen structure** | `data/dungeonz/worldgen/structure/YOUR_DUNGEON_structure.json` |
| 6 | **Structure set** | `data/dungeonz/worldgen/structure_set/YOUR_DUNGEON_structure.json` |
| 7 | **Biome tag** | `data/dungeonz/tags/worldgen/biome/has_structure/YOUR_DUNGEON.json` |
| 8 | **Lang entry** | In a resource pack `assets/YOUR_NAMESPACE/lang/en_us.json` |

> **Tip:** Copy an existing dungeon folder (e.g. `dark_dungeon`) and rename/modify. That's the fastest way to start.

---

## 3. Step 1 – Build Your Structure

### NBT Files

Build your rooms in-game using **structure blocks** (`/give @s minecraft:structure_block`), then save each room as a `.nbt` file and export it to `data/dungeonz/structures/`.

#### The Spawn Room (mandatory)

One of your rooms **must** contain a **Jigsaw block** named `dungeonz:spawn`. This is the exact block position where players will teleport when they enter the dungeon. Place it at a safe, open spot.

#### Connecting Rooms with Jigsaw

Use standard Minecraft Jigsaw blocks to connect rooms together. The Jigsaw system picks rooms randomly from your template pools, so having many room variants makes the dungeon feel different each run.

### Template Pools

Template pools tell the Jigsaw system which NBT pieces can connect to which. You need at least two pools:

**The spawn pool** (`data/dungeonz/worldgen/template_pool/YOUR_DUNGEON/dungeon_spawn.json`):
```json
{
    "fallback": "minecraft:empty",
    "elements": [
        {
            "weight": 1,
            "element": {
                "element_type": "minecraft:single_pool_element",
                "location": "dungeonz:your_spawn_room",
                "projection": "rigid",
                "processors": "minecraft:empty"
            }
        }
    ]
}
```

**Room pools** for each connector type follow the same structure — just add more entries with different `"location"` values and adjust `"weight"` to control how often each room appears.

---

## 4. Step 2 – Set Up the Dungeon Config

This is the heart of your dungeon. Create `data/dungeonz/dungeon/YOUR_DUNGEON.json`.

```json
{
    "dungeon_type": "your_dungeon",

    "dungeon_structure_pool_id": "dungeonz:your_dungeon/dungeon_spawn",

    "difficulty": {
        "easy": {
            "mob_health_modificator": 1.0,
            "mob_damage_modificator": 1.0,
            "mob_protection_modificator": 1.0,
            "mob_speed_modificator": 1.0,
            "loot_table_ids": [
                "dungeonz:chests/your_dungeon_low_tier"
            ],
            "boss_health_modificator": 1.0,
            "boss_damage_modificator": 1.0,
            "boss_protection_modificator": 1.0,
            "boss_speed_modificator": 1.0,
            "boss_loot_table_id": "dungeonz:chests/your_dungeon_easy_boss"
        },
        "normal": { "..." : "..." },
        "hard":   { "..." : "..." }
    },

    "blocks": {
        "minecraft:gold_block":      { "spawns": ["minecraft:skeleton"], "chance": {"easy": 0.3, "normal": 0.5, "hard": 0.7}, "replace": "minecraft:air" },
        "minecraft:iron_block":      { "spawns": ["minecraft:zombie"],   "chance": {"easy": 0.3, "normal": 0.5, "hard": 0.7}, "replace": "minecraft:air" },
        "minecraft:netherite_block": { "boss_entity": "minecraft:evoker", "data": "", "replace": "minecraft:air" },
        "minecraft:emerald_block":   { "boss_loot_block": true, "replace": "minecraft:air" },
        "minecraft:quartz_block":    { "exit_block": true,      "replace": "minecraft:stone_bricks" }
    },

    "spawner": {
        "minecraft:zombie": 10
    },

    "breakable": [],
    "placeable": ["minecraft:torch"],

    "required": {
        "easy":   { "minecraft:gold_ingot": 3 },
        "normal": { "minecraft:gold_ingot": 8 },
        "hard":   { "minecraft:gold_ingot": 16 }
    },

    "respawn": true,
    "keep_inventory": false,
    "elytra": false,
    "ender_pearl": false,
    "positive_effects": false,
    "mobs_loot": true,
    "boss_loot": false,

    "max_group_size": 5,
    "min_group_size": 0,
    "required_level": 0,

    "cooldown": 72000,
    "time_limit": 3600,

    "background_texture": ""
}
```

### Key Fields Explained

| Field | What it does |
|-------|-------------|
| `dungeon_type` | Unique ID — must match your lang key and worldgen `dungeon_type` field |
| `dungeon_structure_pool_id` | Points to the template pool containing your spawn room |
| `difficulty` | Define as many difficulty levels as you want. Names beyond `easy/normal/hard/extreme` need a translation |
| `blocks` | Maps placeholder blocks in your structure to game events (see [Special Blocks](#5-special-blocks-reference)) |
| `spawner` | Maximum times each mob type can spawn from Dungeon Spawner blocks (`0` = infinite) |
| `required` | Items consumed per player on entry |
| `cooldown` | Ticks before dungeon can be re-entered after completion or timeout |
| `time_limit` | Seconds to complete; players are kicked out when it expires |

---

## 5. Special Blocks Reference

DungeonZ uses **placeholder blocks** in your NBT structure to mark where special things happen. When the dungeon generates, these blocks are processed by the mod and replaced.

### Mob Spawn Markers

Place a distinctive ore block (e.g. `minecraft:gold_block`) anywhere in your rooms where mobs should appear.

```json
"minecraft:gold_block": {
    "spawns": ["minecraft:skeleton", "minecraft:stray"],
    "chance": {
        "easy": 0.3,
        "normal": 0.5,
        "hard": 0.7
    },
    "replace": "minecraft:air"
}
```

- `spawns` — list of entity IDs; one is picked at random per marker
- `chance` — probability per difficulty that a mob actually spawns (0.0–1.0)
- `replace` — what block the marker becomes after processing (usually `air`)

> Use **different block types** for different mob types (gold = skeleton, iron = zombie, etc.). Each type can have its own spawn list and chance.

### Boss Marker (exactly one required)

Place exactly **one** block of your chosen marker ID in the boss arena.

```json
"minecraft:netherite_block": {
    "boss_entity": "minecraft:evoker",
    "data": "",
    "replace": "minecraft:air"
}
```

- `boss_entity` — any valid entity ID
- `data` — optional NBT string for the entity (e.g. custom name, equipment)

### Boss Loot Block (exactly one required)

This block is replaced by a **chest filled with boss loot** when the boss is defeated.

```json
"minecraft:emerald_block": {
    "boss_loot_block": true,
    "replace": "minecraft:air"
}
```

Place it near or in the boss arena. Only one block ID can have this role.

### Exit Block (exactly one role required)

After the boss dies, blocks of this type are replaced by exit portals.

```json
"minecraft:quartz_block": {
    "exit_block": true,
    "replace": "minecraft:stone_bricks"
}
```

- `replace` — what the block becomes if it *doesn't* get turned into a portal (e.g. stone bricks as a fallback texture)

Place several of these around the boss arena or at logical exit points.

---

### Dungeon Spawner Block (`dungeonz:dungeon_spawner`)

The **Dungeon Spawner** is a physical block you can place directly in your structure. It works like a vanilla spawner but is aware of the dungeon system:

- Spawns mobs when players are nearby
- Has a configurable max spawn count defined in the `"spawner"` section of your config (value `0` or no entry = infinite)
- Automatically breaks and drops XP when the maximum is reached

Use this for ambient/room spawns rather than encounter-driven spawns. For encounter spawns, use the marker block system above.

---

### Dungeon Gate Block (`dungeonz:dungeon_gate`)

The **Dungeon Gate** is an invisible barrier block used to lock rooms.

- **Auto-unlock:** By default, the gate unlocks when **all hostile mobs** in the structure piece's bounding box are dead
- **Key unlock:** Shift-click the gate in creative mode to set an unlock item — players must right-click the gate with that item
- Multiple gate blocks placed together unlock as a group
- Once unlocked, a gate **stays open permanently** (won't re-lock)
- In creative mode, shift-click also lets you set a **display block** (the gate will visually look like that block) and a **particle effect**

> **Important:** Gates use the **structure piece bounding box** to detect mobs. Place gates at the boundary between two structure pieces so the monitored area is the room the player just came from.

---

## 6. Step 3 – Create Loot Tables

Loot tables use standard Minecraft format. Place them at `data/dungeonz/loot_tables/chests/`.

You need at least:
- One **chest loot table** per difficulty tier (referenced in `loot_table_ids`)
- One **boss loot table** per difficulty (referenced in `boss_loot_table_id`)

### Simple Example

```json
{
    "type": "minecraft:chest",
    "pools": [
        {
            "rolls": { "type": "minecraft:uniform", "min": 2.0, "max": 5.0 },
            "bonus_rolls": 0.0,
            "entries": [
                {
                    "type": "minecraft:item",
                    "name": "minecraft:diamond",
                    "weight": 3,
                    "functions": [
                        {
                            "function": "minecraft:set_count",
                            "count": { "type": "minecraft:uniform", "min": 1.0, "max": 3.0 }
                        }
                    ]
                },
                {
                    "type": "minecraft:item",
                    "name": "minecraft:iron_ingot",
                    "weight": 15,
                    "functions": [
                        {
                            "function": "minecraft:set_count",
                            "count": { "type": "minecraft:uniform", "min": 4.0, "max": 12.0 }
                        }
                    ]
                }
            ]
        }
    ]
}
```

### How Chest Loot Works

- Each chest or barrel in the dungeon gets a random loot table picked from the **`loot_table_ids` list** of the current difficulty.
- The boss loot chest (the block marked with `boss_loot_block: true`) always uses `boss_loot_table_id`.
- Multiple pools in a single loot table all roll independently — use this to guarantee certain items (e.g. a guaranteed boss trophy in pool 1) alongside random loot (pool 2, pool 3...).

### Loot Table Tips

- Use higher `weight` values for common items and lower for rare ones.
- `"enchant_with_levels"` with `"treasure": true` gives treasure enchantments like Mending.
- `"set_damage"` with min/max lets you give partially damaged tools/weapons.
- Create multiple tier files (`low_tier`, `mid_tier`, `high_tier`, `end_tier`) and stack them in `loot_table_ids` for harder difficulties.

---

## 7. Step 4 – Register the Overworld Structure

This controls **where the dungeon portal spawns** in the world.

### Worldgen Structure (`data/dungeonz/worldgen/structure/YOUR_DUNGEON_structure.json`)

```json
{
    "type": "dungeonz:dimension_structures",
    "start_pool": "dungeonz:overworld_your_dungeon",
    "size": 1,
    "max_distance_from_center": 80,
    "biomes": "#dungeonz:has_structure/your_dungeon",
    "step": "surface_structures",
    "start_height": { "absolute": 0 },
    "project_start_to_heightmap": "WORLD_SURFACE_WG",
    "spawn_overrides": {},
    "dungeon_type": "your_dungeon"
}
```

> The `"dungeon_type"` field here **must exactly match** the `"dungeon_type"` in your dungeon config JSON.

### Structure Set (`data/dungeonz/worldgen/structure_set/YOUR_DUNGEON_structure.json`)

Controls **spacing** between portals in the world:

```json
{
    "structures": [
        { "structure": "dungeonz:your_dungeon_structure", "weight": 1 }
    ],
    "placement": {
        "type": "minecraft:random_spread",
        "salt": 987654321,
        "spacing": 128,
        "separation": 96
    }
}
```

- `spacing` — minimum chunks between portal attempts
- `separation` — minimum chunks between placed portals
- `salt` — a unique random seed; **change this** for each dungeon or portals may overlap with others

### Biome Tag (`data/dungeonz/tags/worldgen/biome/has_structure/YOUR_DUNGEON.json`)

```json
{
    "replace": false,
    "values": [
        "minecraft:plains",
        "minecraft:forest",
        "minecraft:taiga"
    ]
}
```

List every biome where the portal should be able to generate. You can also reference biome tags with `#minecraft:is_forest` etc.

### Overworld Template Pool

You also need a template pool for the overworld structure piece (the portal building itself). Create `data/dungeonz/worldgen/template_pool/overworld_your_dungeon.json` pointing to the NBT file that contains the portal block structure.

---

## 8. Step 5 – Add Translations

In a **resource pack** (not a datapack), add to `assets/YOUR_NAMESPACE/lang/en_us.json`:

```json
{
    "dungeon.your_dungeon": "Your Dungeon Name",
    "dungeon.your_dungeon.description.1": "First line of description shown in portal GUI.",
    "dungeon.your_dungeon.description.2": "Second line (optional, up to 9 lines total)."
}
```

Custom difficulty names also need translations if they aren't `easy`, `normal`, `hard`, or `extreme`:

```json
{
    "dungeon.difficulty.mythic": "Mythic"
}
```

---

## 9. Tips & Common Mistakes

### Structure

- **Always verify the `dungeonz:spawn` jigsaw block exists** in your spawn room. Without it, players will teleport to a wrong or null position and likely crash/fall.
- Keep rooms reasonably small. Large rooms can cause performance issues when many mobs are active.
- Make sure rooms connect logically — test with a Jigsaw preview in creative mode.

### Dungeon Config

- `dungeon_type` must be **globally unique** across all loaded dungeons.
- Every difficulty in `blocks.*.chance` must have a key. If you add a `hard` difficulty but forget to add `"hard": 0.5` to a spawn chance block, that mob will never spawn on hard.
- You need **exactly one** `boss_entity` block, **exactly one** `boss_loot_block`, and **exactly one** `exit_block` role defined in `blocks`. Having none or multiples will break completion logic.
- Marker blocks must be **visually distinct** from your normal structure palette so you don't accidentally use them as decorative blocks.

### Gates

- If mobs spawn **outside** the structure piece bounding box where the gate is placed, the gate will never auto-unlock. Design rooms so that all mobs for a gate are within the same structure piece.
- Test gates in creative with `/gamemode creative` — you can shift-click them to reconfigure without breaking the structure.

### Loot Tables

- The `loot_table_ids` list is picked **randomly per chest** — a chest doesn't get items from all tables, just one randomly chosen one. Put guaranteed items in separate pools within a single table, not in separate tables.
- Naming convention used by existing dungeons: `YOUR_DUNGEON_low_tier_chest_loot`, `_mid_tier_`, `_high_tier_`, `_end_tier_`, `_easy_boss_loot`, `_normal_boss_loot`, `_hard_boss_loot`.

### Testing

1. Generate a new world with your datapack enabled.
2. Use `/locate structure dungeonz:your_dungeon_structure` to find the portal.
3. Check logs for `[DungeonLoader]` errors on startup — the loader will tell you exactly which fields are wrong.
4. Use `/dungeon leave` to exit if you get stuck during testing.
