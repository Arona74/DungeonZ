# DungeonZ - Bleak Isles Edition <img alt="DungeonZ Mod Icon" src="src/main/resources/assets/dungeonz/icon.png">
**DungeonZ - BLIS** is a fork made by Arona74 from **DungeonZ** mod made by Globox_Z, created for [Conquest of the Bleak Isles Modpack](https://modrinth.com/modpack/bleak-isles) needs.

### What are the differences with original DungeonZ (1.20.1)?
- **Feature: Splitted Mob/Boss modifiers for Health, Damage, Protection and Mouvement Speed, allowing better balancing. DUNGEONS FILES HAVE TO BE EDITED! See example below.**
- **Feature**: Full dungeon structure regeneration, allowing proceduraly generated structure. (Config option to enabled/disabled)
- **Feature**: Improved multiplayer support on Portal GUI (more settings displayed, waiting player list, you can leave the waiting list and been refunded)
- **Feature**: Time limit to finish the dungeon (set in dungeon file)
- **Feature**: Control Mobs loot when killed (set in dungeon file)
- **Feature**: Control Boss loot when killed (set in dungeon file)
- **Feature**: Timers displayed on Portal GUI
- **Feature**: Fame Reward for my forked Factions mod
- **Feature**: Lootr compatibility (config option, default ON)

- **Backport**: LevelZ requirement (originaly in DungeonZ 1.21.1)
- **Backport**: Fix to multiple trigger cost when going through portal block (originaly in DungeonZ 1.21.1)
- **Backport**: Fix to portal block getting broken to flowing water (originaly in DungeonZ 1.21.1)
- **Backport**: Fix to portal block screen in other dimensions (originaly in DungeonZ 1.21.1)
- **Backport**: keepInventory option (originaly in DungeonZ 1.21.1)
- **Backport**: ender pearl option, info button and positive effect change (originaly in DungeonZ 1.21.1)
- **Backport**: fixed hole by LeDok (originaly in DungeonZ 1.21.1)

- **Change**: OP portal screen when manually creating dungeon portal will now scan the area for other portal to prevent placing them too close
- **Change**: Portal block entity data now use an hybrid NBT+file system to avoid NBT oversize that could cause chunk poisoning
- **Change**: Fixed player showed in the dungeon while being outside of it
- **Change**: Avoid crash when dungeonPortalEntity is null due to potential incorrect dungeon file
- **Change**: Add other crash protections and a method for faulty portal to be repeared when opening the portal screen if correct dungeon file is accessible
- **Change**: If a player disconnect or quit, it will have similar behavior as "/dungeon leave" command
- **Change**: Fixed Gates and Spawners blocks
- **Change**: Easier parkour in Dark dungeon, gates in first corridor room as example usage of Gates blocks
- **Change**: Congratulations message when finishing dungeon with a leave link for easy leaving, you can also leave with a right-click on dungeon portal blocks or use the "/dungeon leave" command.
- **Change**: Few fixes on dark dungeon (easier parkour), missing vines in temple dungeon, etc
- **Change**: Fix to avoid fire to break blocks (like vines)
- **Change**: Fix the "flying not allowed" kick on server when teleporting player to dungeon
- **Change**: Fix effects button on dungeon screen
- **Change**: GUI textures build from [Conquest Reforged mod](https://conquestreforged.com/mod) ressources
- **Change**: Fix Compass out of bound exception by listnt
- **Change**: Prevent spectators to interact with dungeon blocks
- **Content**: Temple dungeon made by xeven (originaly in DungeonZ 1.21.1)
- **Content**: French translation made by hirtz-gregoire (originaly in DungeonZ 1.21.1)
- **Content**: [Desert dungeon made by D1scoball](https://modrinth.com/mod/desert-dungeon-dungeonz-addon)

### What next?
**backport features/fixes** made in last dungeonZ version **to 1.20.1**.
**add new features and content in the process**, feel free to participate or just share ideas.

### Support
This fork is given as is, please **don't ask original DungeonZ devs** about issues regarding this fork, **please post in [Issues](https://github.com/Arona74/DungeonZ/issues)**.

### Installation
**download the binaries from [Releases](https://github.com/Arona74/DungeonZ/releases)**.
**download the binaries from [Modrinth](https://modrinth.com/mod/dungeonz-blis)**.

### License
DungeonZ - BLIS is licensed under MIT.

### Datapacks - Slighlty different from original DungeonZ
If you don't know how to create a datapack check out [Data Pack Wiki](https://minecraft.fandom.com/wiki/Data_Pack) website and try to create your first one for the vanilla game.
If you know how to create one, the folder path has to be ```data\dungeonz\dungeon\YOURFILE.json```

```json
{
    "dungeon_type": "dark_dungeon", // unique dungeon id, create a lang file in a resource pack "dungeon.unique_id" to have proper translation
    "difficulty": { // set difficulties here, can be any name but have to get translated with a resource pack if you don't use "easy","normal","hard" or "extreme"
        "easy": {
            "mob_health_modificator": 1.0, // modificator to increase mob base health
            "mob_damage_modificator": 1.0, // modificator to increase mob base attack damage
            "mob_protection_modificator": 1.0, // modificator to increase mob base armor
            "mob_speed_modificator": 1.0, // modificator to increase mob base mouvement speed
            "loot_table_ids": [ // a list of different loot tables chests and barrels will get filled with
                "dungeonz:chests/dark_dungeon_low_tier_chest_loot",
                "dungeonz:chests/dark_dungeon_mid_tier_chest_loot"
            ],
            "boss_health_modificator": 1.0, // modificator to increase boss base health
            "boss_damage_modificator": 1.0, // modificator to increase boss base attack damage
            "boss_protection_modificator": 1.0, // modificator to increase boss base armor
            "boss_speed_modificator": 1.0, // modificator to increase boss base mouvement speed
            "boss_loot_table_id": "dungeonz:chests/dark_dungeon_easy_boss_loot"
        },
        "normal": {
            "mob_health_modificator": 1.5,
            "mob_damage_modificator": 1.5,
            "mob_protection_modificator": 1.5,
            "mob_speed_modificator": 1.15,
            "loot_table_ids": [
                "dungeonz:chests/dark_dungeon_low_tier_chest_loot",
                "dungeonz:chests/dark_dungeon_mid_tier_chest_loot",
                "dungeonz:chests/dark_dungeon_high_tier_chest_loot"
            ],
            "boss_health_modificator": 2.0,
            "boss_damage_modificator": 2.0,
            "boss_protection_modificator": 2.0,
            "boss_speed_modificator": 1.15,
            "boss_loot_table_id": "dungeonz:chests/dark_dungeon_normal_boss_loot"
        }
    },
    "blocks": {
        "minecraft:gold_block": {
            "spawns": [ // a list of entity types which can spawn at the block positions
                "minecraft:skeleton"
            ],
            "chance": { // an object for each difficulty which includes spawn chances at the block positions
                "easy": 0.4,
                "normal": 0.7
            },
            "replace": "minecraft:air"
        },
        "minecraft:iron_block": {
            "spawns": [
                "minecraft:zombie"
            ],
            "chance": {
                "easy": 0.4,
                "normal": 0.7
            },
            "replace": "minecraft:air"
        },
        "minecraft:netherite_block": {
            "boss_entity": "minecraft:warden", // required for one block id! At this block position the boss will spawn
            "data": "", // optional add nbt info to the boss entity
            "replace": "minecraft:air"
        },
        "minecraft:emerald_block": {
            "boss_loot_block": true, // required for one block id! This block will get replaced by a chest filled with the boss loot after completion
            "replace": "minecraft:air"
        },
        "minecraft:quartz_block": {
            "exit_block": true, // required for one block id! Those blocks will get replaced by the dungeon portal to get out from the dungeon after completion
            "replace": "minecraft:stone_bricks"
        }
    },
    "spawner": { // use Dungeon Spawner in your structure build to set the max spawn time for the spawner here before the spawner will automatically break
        "minecraft:zombie": 10,
        "minecraft:skeleton": 5
    },
    "breakable": [], // block ids which can be broken in the dungeon by the player
    "placeable": [ // block ids which can be placed in the dungeon by the player
        "minecraft:torch"
    ],
    "required": { // Items which get consumed after joining the dungeon per difficulty
        "easy": {
            "minecraft:diamond": 1
        },
        "normal": {
            "minecraft:diamond": 5
        },
        "hard": {
            "minecraft:diamond": 10
        }
    },
    "respawn": false, // OPTIONAL: is respawn allowed? true by default
    "elytra": false, // Is Elytra usage allowed? false by default
    "keep_inventory": false, // OPTIONAL, false by default, if true: keepInventory will be on when dying in the dungeon
    "ender_pearl": false, // Is Ender pearl usage allowed? false by default
    "positive_effects": false, // Optional, false by default
    "mobs_loot": true, // Optional, true by default
    "boss_loot": true, // Optional, true by default
    "max_group_size": 5, // Maximum number of player in the dungeon
    "min_group_size": 0, // OPTIONAL, minimum number of player to start the dungeon
    "required_level": 0, // OPTIONAL, levelZ compat
    "cooldown": 108000, // Cooldown after the dungeon is completed or failed in ticks
    "time_limit": 1800, // OPTIONAL, Time to complete the dungeon in seconds, if dungeon isn't completed and players are still inside, they will be teleported out and dungeon will start cooldown
    "background_texture": "", // For custom dungeon portal backgrounds, set your texture path here using proper identifier like this "dungeonz:textures/gui/dark_dungeon_portal.png"
    "dungeon_structure_pool_id": "dungeonz:dark_dungeon/dungeon_spawn" // Structure part which the dungeon generates start of
}
```

Make sure your first structure piece (`"dungeon_structure_pool_id"`) has a jigsaw block named `dungeonz:spawn`. This is the block where the player will teleport to.
To add information about your dungeon add `"dungeon.your_dungeon_type.description.1":"..."` (add up to 9 lines) translations keys at your lang file.

An example part for the overworld structure which leads to the dungeon:

```json
{
    "type": "dungeonz:dimension_structures",
    "start_pool": "dungeonz:overworld_test_start",
    "size": 1,
    "max_distance_from_center": 80,
    "biomes": "#minecraft:is_overworld",
    "step": "surface_structures",
    "start_height": {
        "absolute": 0
    },
    "project_start_to_heightmap": "WORLD_SURFACE_WG",
    "spawn_overrides": {},
    "dungeon_type": "dark_dungeon" // set your unique dungeon id here
}
```

DungeonZ has an extra field called `"dungeon_type"` which has to be one of the defined dungeon types.

### Advancement
DungeonZ provides a advancement criterion trigger called `dungeonz:dungeon_completion`.

```json
    "criteria": {
        "completion_example": {
            "trigger": "dungeonz:dungeon_completion",
            "conditions": {
                "dungeon_type": "dark_dungeon",
                "difficulty": "easy"
            }
        }
    }
```

### Dungeon Spawner
The Dungeon Spawner is a special block you can place in your structure templates.

- Spawns mobs when players enter the room
- Configure max spawn count per entity type in the `"spawner"` section of your dungeon JSON
- Value `0` or no entry means infinite spawns
- Spawner breaks automatically when max count is reached

### Dungeon Gate
The Dungeon Gate is a block that acts as a lockable barrier in your dungeon.

**Placement:**
- Place gates in your structure template at room exits
- Gates use the structure piece bounding box as their monitoring area
- Shift-click in creative to configure: display block, particle effect, and unlock item

**Unlock behavior:**
- **With unlock item:** Player must use the specified item on the gate to unlock
- **Without unlock item:** Gate auto-unlocks when all hostile mobs in the area are killed

**Tips:**
- Place gates at structure piece boundaries for proper area detection
- All connected gate blocks unlock together
- Gates stay unlocked once opened (won't re-lock if new mobs spawn)

### Commands
`/dungeon leave`
- Leave the current dungeon (if unable to finish the dungeon)
****
