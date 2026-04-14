package net.dungeonz.data;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import org.jetbrains.annotations.Nullable;

import net.dungeonz.DungeonzMain;
import net.dungeonz.dungeon.Dungeon;
import net.dungeonz.init.ConfigInit;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.Registries;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

public class DungeonLoader implements SimpleSynchronousResourceReloadListener {

    @Override
    public Identifier getFabricId() {
        return new Identifier("dungeonz", "dungeon_loader");
    }

    @Override
    public void reload(ResourceManager manager) {
        manager.findResources("dungeon", id -> id.getPath().endsWith(".json")).forEach((id, resourceRef) -> {
            try {
                InputStream stream = resourceRef.getInputStream();
                JsonObject data = JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject();

                String dungeonTypeId = data.get("dungeon_type").getAsString();
                if (!ConfigInit.CONFIG.defaultDungeons && (dungeonTypeId.equals("dark_dungeon") || dungeonTypeId.equals("temple_dungeon"))) {
                    return;
                }
                int maxGroupSize = data.get("max_group_size").getAsInt();
                int minGroupSize = data.has("min_group_size") ? data.get("min_group_size").getAsInt() : 0;
                int requiredLevel = data.has("required_level") ? data.get("required_level").getAsInt() : 0;
                int cooldown = data.get("cooldown").getAsInt();
                int timeLimit = data.get("time_limit").getAsInt();
                boolean allowElytra = data.has("elytra") ? data.get("elytra").getAsBoolean() : false;
                boolean allowRespawn = data.has("respawn") ? data.get("respawn").getAsBoolean() : true;
                boolean keepInventory = data.has("keep_inventory") ? data.get("keep_inventory").getAsBoolean() : false;
                boolean allowEnderPearl = data.has("ender_pearl") ? data.get("ender_pearl").getAsBoolean() : false;
                boolean allowPositiveEffects = data.has("positive_effects") ? data.get("positive_effects").getAsBoolean() : true;
                boolean allowMobsLoot = data.has("mobs_loot") ? data.get("mobs_loot").getAsBoolean() : true;
                boolean allowBossLoot = data.has("boss_loot") ? data.get("boss_loot").getAsBoolean() : true;

                Identifier dungeonBackgroundId = data.has("background_texture") && !data.get("background_texture").getAsString().trim().isEmpty() 
                    ? new Identifier(data.get("background_texture").getAsString()) 
                    : null;
                Identifier dungeonStructurePoolId = new Identifier(data.get("dungeon_structure_pool_id").getAsString());
                boolean randomRotation = data.has("random_rotation") ? data.get("random_rotation").getAsBoolean() : false;
                boolean hidePossibleLoot = data.has("hide_possible_loot") ? data.get("hide_possible_loot").getAsBoolean() : false;

                List<String> difficulties = new ArrayList<String>();
                JsonObject difficultyObject = data.get("difficulty").getAsJsonObject();
                Iterator<String> difficultyIterator = difficultyObject.keySet().iterator();

                HashMap<String, Float> difficultyMobHealthModificator = new HashMap<String, Float>();
                HashMap<String, Float> difficultyMobDamageModificator = new HashMap<String, Float>();
                HashMap<String, Float> difficultyMobProtectionModificator = new HashMap<String, Float>();
                HashMap<String, Float> difficultyMobSpeedModificator = new HashMap<String, Float>();
                HashMap<String, List<String>> difficultyLootTableIds = new HashMap<String, List<String>>();
                HashMap<String, Float> difficultyBossHealthModificator = new HashMap<String, Float>();
                HashMap<String, Float> difficultyBossDamageModificator = new HashMap<String, Float>();
                HashMap<String, Float> difficultyBossProtectionModificator = new HashMap<String, Float>();
                HashMap<String, Float> difficultyBossSpeedModificator = new HashMap<String, Float>();
                HashMap<String, String> difficultyBossLootTable = new HashMap<String, String>();
                HashMap<String, Integer> difficultyFameReward = new HashMap<String, Integer>();

                while (difficultyIterator.hasNext()) {
                    String difficulty = difficultyIterator.next();
                    difficulties.add(difficulty);
                    JsonObject specificDifficultyObject = difficultyObject.get(difficulty).getAsJsonObject();

                    difficultyMobHealthModificator.put(difficulty, specificDifficultyObject.get("mob_health_modificator").getAsFloat());
                    difficultyMobDamageModificator.put(difficulty, specificDifficultyObject.get("mob_damage_modificator").getAsFloat());
                    difficultyMobProtectionModificator.put(difficulty, specificDifficultyObject.get("mob_protection_modificator").getAsFloat());
                    difficultyMobSpeedModificator.put(difficulty, specificDifficultyObject.get("mob_speed_modificator").getAsFloat());
                    List<String> lootTableIds = new ArrayList<String>();
                    for (int i = 0; i < specificDifficultyObject.get("loot_table_ids").getAsJsonArray().size(); i++) {
                        lootTableIds.add(specificDifficultyObject.get("loot_table_ids").getAsJsonArray().get(i).getAsString());
                    }
                    difficultyLootTableIds.put(difficulty, lootTableIds);
                    difficultyBossHealthModificator.put(difficulty, specificDifficultyObject.get("boss_health_modificator").getAsFloat());
                    difficultyBossDamageModificator.put(difficulty, specificDifficultyObject.get("boss_damage_modificator").getAsFloat());
                    difficultyBossProtectionModificator.put(difficulty, specificDifficultyObject.get("boss_protection_modificator").getAsFloat());
                    difficultyBossSpeedModificator.put(difficulty, specificDifficultyObject.get("boss_speed_modificator").getAsFloat());
                    difficultyBossLootTable.put(difficulty, specificDifficultyObject.get("boss_loot_table_id").getAsString());

                    // Parse fame_reward (optional field, defaults to 0)
                    if (specificDifficultyObject.has("fame_reward")) {
                        difficultyFameReward.put(difficulty, specificDifficultyObject.get("fame_reward").getAsInt());
                    } else {
                        difficultyFameReward.put(difficulty, 0);
                    }
                }

                JsonObject blockObject = data.get("blocks").getAsJsonObject();
                Iterator<String> blockIterator = blockObject.keySet().iterator();

                HashMap<Integer, List<EntityType<?>>> blockIdEntityMap = new HashMap<Integer, List<EntityType<?>>>();
                HashMap<Integer, HashMap<String, Float>> blockIdEntitySpawnChance = new HashMap<Integer, HashMap<String, Float>>();
                HashMap<Integer, Integer> blockIdBlockReplacement = new HashMap<Integer, Integer>();
                int bossBlockId = -1;
                int bossLootBlockId = -1;
                int exitBlockId = -1;
                EntityType<?> bossEntityType = null;
                NbtCompound bossNbtCompound = null;

                while (blockIterator.hasNext()) {
                    String block = blockIterator.next();
                    if (Registries.BLOCK.get(new Identifier(block)).toString().equals("Block{minecraft:air}")) {
                        DungeonzMain.LOGGER.warn("{} is not a valid block identifier", block);
                        continue;
                    }
                    int rawBlockId = Registries.BLOCK.getRawId(Registries.BLOCK.get(new Identifier(block)));

                    JsonObject specificBlockObject = blockObject.get(block).getAsJsonObject();

                    if (specificBlockObject.has("spawns")) {
                        List<EntityType<?>> entityTypes = new ArrayList<EntityType<?>>();
                        for (int i = 0; i < specificBlockObject.get("spawns").getAsJsonArray().size(); i++) {
                            if (!Registries.ENTITY_TYPE.containsId(new Identifier(specificBlockObject.get("spawns").getAsJsonArray().get(i).getAsString()))) {
                                DungeonzMain.LOGGER.warn("{} is not a valid entity identifier", specificBlockObject.get("spawns").getAsJsonArray().get(i).getAsString());
                                continue;
                            }
                            entityTypes.add(Registries.ENTITY_TYPE.get(new Identifier(specificBlockObject.get("spawns").getAsJsonArray().get(i).getAsString())));
                        }
                        blockIdEntityMap.put(rawBlockId, entityTypes);

                        HashMap<String, Float> difficultyChance = new HashMap<String, Float>();
                        for (String difficulty : difficulties) {
                            difficultyChance.put(difficulty, specificBlockObject.get("chance").getAsJsonObject().get(difficulty).getAsFloat());
                        }
                        blockIdEntitySpawnChance.put(rawBlockId, difficultyChance);

                    } else if (specificBlockObject.has("boss_entity")) {
                        if (!Registries.ENTITY_TYPE.containsId(new Identifier(specificBlockObject.get("boss_entity").getAsString()))) {
                            DungeonzMain.LOGGER.warn("{} is not a valid entity identifier", specificBlockObject.get("boss_entity").getAsString());
                        }
                        bossEntityType = Registries.ENTITY_TYPE.get(new Identifier(specificBlockObject.get("boss_entity").getAsString()));
                        bossNbtCompound = tryReadNbtData(specificBlockObject);
                        bossBlockId = rawBlockId;
                    } else if (specificBlockObject.has("exit_block") && specificBlockObject.get("exit_block").getAsBoolean()) {
                        exitBlockId = rawBlockId;
                    } else if (specificBlockObject.has("boss_loot_block") && specificBlockObject.get("boss_loot_block").getAsBoolean()) {
                        bossLootBlockId = rawBlockId;
                    } else {
                        DungeonzMain.LOGGER.warn("{} has no set spawns nor exit block nor boss loot block nor boss entity", blockIterator);
                    }

                    if (!specificBlockObject.get("replace").isJsonNull()) {
                        Identifier blockIdentifier = new Identifier(specificBlockObject.get("replace").getAsString());
                        if (!blockIdentifier.toString().equals("minecraft:air") && Registries.BLOCK.get(blockIdentifier).toString().equals("Block{minecraft:air}")) {
                            DungeonzMain.LOGGER.warn("{} is not a valid block identifier", specificBlockObject.get("replace").getAsString());
                            continue;
                        }
                        blockIdBlockReplacement.put(rawBlockId, Registries.BLOCK.getRawId(Registries.BLOCK.get(blockIdentifier)));
                    } else {
                        blockIdBlockReplacement.put(rawBlockId, -1);
                    }
                }

                JsonObject spawnerObject = data.get("spawner").getAsJsonObject();
                Iterator<String> spawnerIterator = spawnerObject.keySet().iterator();

                HashMap<String, Integer> spawnerEntityIdCountMap = new HashMap<String, Integer>();

                while (spawnerIterator.hasNext()) {
                    String entityString = spawnerIterator.next();
                    Identifier entityIdentifier = new Identifier(entityString);

                    if (Registries.ENTITY_TYPE.get(entityIdentifier).toString().equals("entity.minecraft.pig")) {
                        DungeonzMain.LOGGER.warn("{} is not a valid entity identifier", entityString);
                        continue;
                    }
                    spawnerEntityIdCountMap.put(entityIdentifier.toString(), spawnerObject.get(entityString).getAsInt());
                }
                List<Integer> breakableBlockIds = new ArrayList<Integer>();
                if (data.has("breakable")) {
                    for (int i = 0; i < data.get("breakable").getAsJsonArray().size(); i++) {
                        Identifier blockIdentifier = new Identifier(data.get("breakable").getAsJsonArray().get(i).getAsString());
                        if (Registries.BLOCK.get(blockIdentifier).toString().equals("Block{minecraft:air}")) {
                            DungeonzMain.LOGGER.warn("{} is not a valid block identifier", data.get("breakable").getAsJsonArray().get(i).getAsString());
                            continue;
                        }
                        breakableBlockIds.add(Registries.BLOCK.getRawId(Registries.BLOCK.get(blockIdentifier)));
                    }
                }
                List<Integer> placeableBlockIds = new ArrayList<Integer>();
                if (data.has("placeable")) {
                    for (int i = 0; i < data.get("placeable").getAsJsonArray().size(); i++) {
                        Identifier blockIdentifier = new Identifier(data.get("placeable").getAsJsonArray().get(i).getAsString());
                        if (Registries.BLOCK.get(blockIdentifier).toString().equals("Block{minecraft:air}")) {
                            DungeonzMain.LOGGER.warn("{} is not a valid block identifier", data.get("placeable").getAsJsonArray().get(i).getAsString());
                            continue;
                        }
                        placeableBlockIds.add(Registries.BLOCK.getRawId(Registries.BLOCK.get(blockIdentifier)));
                    }
                }

                JsonObject requiredObject = data.get("required").getAsJsonObject();
                Iterator<String> requiredIterator = requiredObject.keySet().iterator();

                HashMap<String, HashMap<Integer, Integer>> difficultyRequiredItemCountMap = new HashMap<>();

                while (requiredIterator.hasNext()) {
                    String difficulty = requiredIterator.next();

                    if (!difficulties.contains(difficulty)) {
                        DungeonzMain.LOGGER.warn("{} is not a valid difficulty at the required list", difficulty);
                        continue;
                    }

                    HashMap<Integer, Integer> requiredItemCountMap = new HashMap<>();

                    for (String itemString : requiredObject.get(difficulty).getAsJsonObject().keySet()) {
                        Identifier itemIdentifier = new Identifier(itemString);
                        if (Registries.ITEM.get(itemIdentifier).toString().equals("air")) {
                            DungeonzMain.LOGGER.warn("{} is not a valid item identifier", itemString);
                            continue;
                        }
                        requiredItemCountMap.put(Registries.ITEM.getRawId(Registries.ITEM.get(itemIdentifier)), requiredObject.get(difficulty).getAsJsonObject().get(itemString).getAsInt());
                    }
                    difficultyRequiredItemCountMap.put(difficulty, requiredItemCountMap);
                }
                for (String difficulty : difficulties) {
                    if (!difficultyRequiredItemCountMap.containsKey(difficulty)) {
                        difficultyRequiredItemCountMap.put(difficulty, new HashMap<>());
                    }
                }

                if (bossEntityType == null) {
                    DungeonzMain.LOGGER.error("{} has no set boss", data);
                    return;
                }

                Dungeon.addDungeon(new Dungeon(dungeonTypeId, blockIdEntityMap, blockIdEntitySpawnChance, blockIdBlockReplacement, spawnerEntityIdCountMap, difficultyRequiredItemCountMap, breakableBlockIds,
                        placeableBlockIds, difficultyMobHealthModificator, difficultyMobDamageModificator, difficultyMobProtectionModificator, difficultyMobSpeedModificator, difficultyLootTableIds, difficultyBossHealthModificator,
                        difficultyBossDamageModificator, difficultyBossProtectionModificator, difficultyBossSpeedModificator, difficultyBossLootTable, difficultyFameReward, bossEntityType, bossNbtCompound, bossBlockId,
                        bossLootBlockId, exitBlockId, allowRespawn, allowElytra, keepInventory, allowEnderPearl, allowPositiveEffects, allowMobsLoot, allowBossLoot, maxGroupSize, minGroupSize, requiredLevel, cooldown, timeLimit, dungeonBackgroundId, dungeonStructurePoolId, randomRotation, hidePossibleLoot));
            } catch (Exception e) {
                DungeonzMain.LOGGER.error("Error occurred while loading resource {}. {}", id.toString(), e.toString());
            }
        });
    }

    @Nullable
    private static NbtCompound tryReadNbtData(JsonObject json) {
        if (json.has("data") && json.get("data") != null && !json.get("data").getAsString().equals("")) {
            try {
                return new StringNbtReader(new StringReader(json.get("data").getAsString())).parseCompound();
            } catch (CommandSyntaxException e) {
                e.printStackTrace();
                throw new JsonParseException("Failed to load nbt data of json object " + json);
            }
        }
        return null;
    }

}
