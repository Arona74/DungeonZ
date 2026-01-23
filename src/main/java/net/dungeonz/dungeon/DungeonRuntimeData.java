package net.dungeonz.dungeon;

import net.dungeonz.block.entity.DungeonPortalEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds large runtime dungeon data that should NOT be stored in block entity NBT.
 * This data is stored in separate server-side files to avoid the 2MB chunk NBT limit.
 */
public class DungeonRuntimeData {

    private HashMap<Integer, ArrayList<BlockPos>> blockBlockPosMap = new HashMap<>();
    private List<BlockPos> chestPosList = new ArrayList<>();
    private List<BlockPos> exitPosList = new ArrayList<>();
    private List<BlockPos> gatePosList = new ArrayList<>();
    private Map<BlockPos, Integer> movingBlockMap = new HashMap<>();
    private Map<BlockPos, DungeonPortalEntity.Powered> poweredBlockMap = new HashMap<>();
    private HashMap<BlockPos, String> spawnerPosEntityIdMap = new HashMap<>();
    private HashMap<BlockPos, Integer> replacePosBlockIdMap = new HashMap<>();
    private List<Integer> dungeonEdgeList = new ArrayList<>();

    public DungeonRuntimeData() {
    }

    // Getters and setters
    public HashMap<Integer, ArrayList<BlockPos>> getBlockBlockPosMap() {
        return blockBlockPosMap;
    }

    public void setBlockBlockPosMap(HashMap<Integer, ArrayList<BlockPos>> blockBlockPosMap) {
        this.blockBlockPosMap = blockBlockPosMap;
    }

    public List<BlockPos> getChestPosList() {
        return chestPosList;
    }

    public void setChestPosList(List<BlockPos> chestPosList) {
        this.chestPosList = chestPosList;
    }

    public List<BlockPos> getExitPosList() {
        return exitPosList;
    }

    public void setExitPosList(List<BlockPos> exitPosList) {
        this.exitPosList = exitPosList;
    }

    public List<BlockPos> getGatePosList() {
        return gatePosList;
    }

    public void setGatePosList(List<BlockPos> gatePosList) {
        this.gatePosList = gatePosList;
    }

    public Map<BlockPos, Integer> getMovingBlockMap() {
        return movingBlockMap;
    }

    public void setMovingBlockMap(Map<BlockPos, Integer> movingBlockMap) {
        this.movingBlockMap = movingBlockMap;
    }

    public Map<BlockPos, DungeonPortalEntity.Powered> getPoweredBlockMap() {
        return poweredBlockMap;
    }

    public void setPoweredBlockMap(Map<BlockPos, DungeonPortalEntity.Powered> poweredBlockMap) {
        this.poweredBlockMap = poweredBlockMap;
    }

    public HashMap<BlockPos, String> getSpawnerPosEntityIdMap() {
        return spawnerPosEntityIdMap;
    }

    public void setSpawnerPosEntityIdMap(HashMap<BlockPos, String> spawnerPosEntityIdMap) {
        this.spawnerPosEntityIdMap = spawnerPosEntityIdMap;
    }

    public HashMap<BlockPos, Integer> getReplacePosBlockIdMap() {
        return replacePosBlockIdMap;
    }

    public void setReplacePosBlockIdMap(HashMap<BlockPos, Integer> replacePosBlockIdMap) {
        this.replacePosBlockIdMap = replacePosBlockIdMap;
    }

    public List<Integer> getDungeonEdgeList() {
        return dungeonEdgeList;
    }

    public void setDungeonEdgeList(List<Integer> dungeonEdgeList) {
        this.dungeonEdgeList = dungeonEdgeList;
    }

    public void addDungeonEdge(int x, int y, int z) {
        this.dungeonEdgeList.add(x);
        this.dungeonEdgeList.add(y);
        this.dungeonEdgeList.add(z);
    }

    public void addReplaceBlockId(BlockPos pos, int blockId) {
        this.replacePosBlockIdMap.put(pos, blockId);
    }

    // Serialization methods
    public NbtCompound writeToNbt() {
        NbtCompound nbt = new NbtCompound();

        // Block map
        nbt.putInt("BlockMapSize", this.blockBlockPosMap.size());
        int blockCount = 0;
        for (Map.Entry<Integer, ArrayList<BlockPos>> entry : this.blockBlockPosMap.entrySet()) {
            nbt.putInt("BlockId" + blockCount, entry.getKey());
            nbt.putInt("BlockListSize" + blockCount, entry.getValue().size());
            for (int i = 0; i < entry.getValue().size(); i++) {
                BlockPos pos = entry.getValue().get(i);
                nbt.putInt("BlockPosX" + blockCount + "_" + i, pos.getX());
                nbt.putInt("BlockPosY" + blockCount + "_" + i, pos.getY());
                nbt.putInt("BlockPosZ" + blockCount + "_" + i, pos.getZ());
            }
            blockCount++;
        }

        // Chest list
        nbt.putInt("ChestListSize", this.chestPosList.size());
        for (int i = 0; i < this.chestPosList.size(); i++) {
            BlockPos pos = this.chestPosList.get(i);
            nbt.putInt("ChestPosX" + i, pos.getX());
            nbt.putInt("ChestPosY" + i, pos.getY());
            nbt.putInt("ChestPosZ" + i, pos.getZ());
        }

        // Exit list
        nbt.putInt("ExitListSize", this.exitPosList.size());
        for (int i = 0; i < this.exitPosList.size(); i++) {
            BlockPos pos = this.exitPosList.get(i);
            nbt.putInt("ExitPosX" + i, pos.getX());
            nbt.putInt("ExitPosY" + i, pos.getY());
            nbt.putInt("ExitPosZ" + i, pos.getZ());
        }

        // Gate list
        nbt.putInt("GateListSize", this.gatePosList.size());
        for (int i = 0; i < this.gatePosList.size(); i++) {
            BlockPos pos = this.gatePosList.get(i);
            nbt.putInt("GatePosX" + i, pos.getX());
            nbt.putInt("GatePosY" + i, pos.getY());
            nbt.putInt("GatePosZ" + i, pos.getZ());
        }

        // Moving block map
        nbt.putInt("MovingPosSize", this.movingBlockMap.size());
        int movingCount = 0;
        for (Map.Entry<BlockPos, Integer> entry : this.movingBlockMap.entrySet()) {
            BlockPos pos = entry.getKey();
            nbt.putInt("MovingPosX" + movingCount, pos.getX());
            nbt.putInt("MovingPosY" + movingCount, pos.getY());
            nbt.putInt("MovingPosZ" + movingCount, pos.getZ());
            nbt.putInt("MovingBlockId" + movingCount, entry.getValue());
            movingCount++;
        }

        // Powered block map
        nbt.putInt("PoweredPosSize", this.poweredBlockMap.size());
        int poweredCount = 0;
        for (Map.Entry<BlockPos, DungeonPortalEntity.Powered> entry : this.poweredBlockMap.entrySet()) {
            DungeonPortalEntity.Powered p = entry.getValue();
            BlockPos pos = entry.getKey();
            nbt.putIntArray("PoweredPos" + poweredCount, new int[] {
                pos.getX(), pos.getY(), pos.getZ(),
                p.getBlockId(),
                p.getPowered() ? 1 : 0,
                p.getFacing(),
                p.getBlockFacing()
            });
            poweredCount++;
        }

        // Spawner map
        nbt.putInt("SpawnerMapSize", this.spawnerPosEntityIdMap.size());
        int spawnerCount = 0;
        for (Map.Entry<BlockPos, String> entry : this.spawnerPosEntityIdMap.entrySet()) {
            BlockPos pos = entry.getKey();
            nbt.putInt("SpawnerPosX" + spawnerCount, pos.getX());
            nbt.putInt("SpawnerPosY" + spawnerCount, pos.getY());
            nbt.putInt("SpawnerPosZ" + spawnerCount, pos.getZ());
            nbt.putString("SpawnerEntityId" + spawnerCount, entry.getValue());
            spawnerCount++;
        }

        // Replace map
        nbt.putInt("ReplacePosSize", this.replacePosBlockIdMap.size());
        int replaceCount = 0;
        for (Map.Entry<BlockPos, Integer> entry : this.replacePosBlockIdMap.entrySet()) {
            BlockPos pos = entry.getKey();
            nbt.putInt("ReplacePosX" + replaceCount, pos.getX());
            nbt.putInt("ReplacePosY" + replaceCount, pos.getY());
            nbt.putInt("ReplacePosZ" + replaceCount, pos.getZ());
            nbt.putInt("ReplaceBlockId" + replaceCount, entry.getValue());
            replaceCount++;
        }

        // Dungeon edge list
        nbt.putInt("DungeonEdgeSize", this.dungeonEdgeList.size());
        for (int i = 0; i < this.dungeonEdgeList.size() / 3; i++) {
            nbt.putInt("DungeonEdgeX" + i, this.dungeonEdgeList.get(i * 3));
            nbt.putInt("DungeonEdgeY" + i, this.dungeonEdgeList.get(i * 3 + 1));
            nbt.putInt("DungeonEdgeZ" + i, this.dungeonEdgeList.get(i * 3 + 2));
        }

        return nbt;
    }

    public static DungeonRuntimeData readFromNbt(NbtCompound nbt) {
        DungeonRuntimeData data = new DungeonRuntimeData();

        // Block map
        if (nbt.contains("BlockMapSize", NbtElement.INT_TYPE)) {
            for (int i = 0; i < nbt.getInt("BlockMapSize"); i++) {
                ArrayList<BlockPos> posList = new ArrayList<>();
                for (int u = 0; u < nbt.getInt("BlockListSize" + i); u++) {
                    posList.add(new BlockPos(
                        nbt.getInt("BlockPosX" + i + "_" + u),
                        nbt.getInt("BlockPosY" + i + "_" + u),
                        nbt.getInt("BlockPosZ" + i + "_" + u)
                    ));
                }
                data.blockBlockPosMap.put(nbt.getInt("BlockId" + i), posList);
            }
        }

        // Chest list
        if (nbt.contains("ChestListSize", NbtElement.INT_TYPE)) {
            for (int i = 0; i < nbt.getInt("ChestListSize"); i++) {
                data.chestPosList.add(new BlockPos(
                    nbt.getInt("ChestPosX" + i),
                    nbt.getInt("ChestPosY" + i),
                    nbt.getInt("ChestPosZ" + i)
                ));
            }
        }

        // Exit list
        if (nbt.contains("ExitListSize", NbtElement.INT_TYPE)) {
            for (int i = 0; i < nbt.getInt("ExitListSize"); i++) {
                data.exitPosList.add(new BlockPos(
                    nbt.getInt("ExitPosX" + i),
                    nbt.getInt("ExitPosY" + i),
                    nbt.getInt("ExitPosZ" + i)
                ));
            }
        }

        // Gate list
        if (nbt.contains("GateListSize", NbtElement.INT_TYPE)) {
            for (int i = 0; i < nbt.getInt("GateListSize"); i++) {
                data.gatePosList.add(new BlockPos(
                    nbt.getInt("GatePosX" + i),
                    nbt.getInt("GatePosY" + i),
                    nbt.getInt("GatePosZ" + i)
                ));
            }
        }

        // Moving block map
        if (nbt.contains("MovingPosSize", NbtElement.INT_TYPE)) {
            for (int i = 0; i < nbt.getInt("MovingPosSize"); i++) {
                data.movingBlockMap.put(
                    new BlockPos(
                        nbt.getInt("MovingPosX" + i),
                        nbt.getInt("MovingPosY" + i),
                        nbt.getInt("MovingPosZ" + i)
                    ),
                    nbt.getInt("MovingBlockId" + i)
                );
            }
        }

        // Powered block map
        if (nbt.contains("PoweredPosSize", NbtElement.INT_TYPE)) {
            for (int i = 0; i < nbt.getInt("PoweredPosSize"); i++) {
                int[] poweredPos = nbt.getIntArray("PoweredPos" + i);
                if (poweredPos.length >= 7) {
                    boolean isPowered = poweredPos[4] == 1;
                    data.poweredBlockMap.put(
                        new BlockPos(poweredPos[0], poweredPos[1], poweredPos[2]),
                        new DungeonPortalEntity.Powered(poweredPos[3], isPowered, poweredPos[5], poweredPos[6])
                    );
                }
            }
        }

        // Spawner map
        if (nbt.contains("SpawnerMapSize", NbtElement.INT_TYPE)) {
            for (int i = 0; i < nbt.getInt("SpawnerMapSize"); i++) {
                data.spawnerPosEntityIdMap.put(
                    new BlockPos(
                        nbt.getInt("SpawnerPosX" + i),
                        nbt.getInt("SpawnerPosY" + i),
                        nbt.getInt("SpawnerPosZ" + i)
                    ),
                    nbt.getString("SpawnerEntityId" + i)
                );
            }
        }

        // Replace map
        if (nbt.contains("ReplacePosSize", NbtElement.INT_TYPE)) {
            for (int i = 0; i < nbt.getInt("ReplacePosSize"); i++) {
                data.replacePosBlockIdMap.put(
                    new BlockPos(
                        nbt.getInt("ReplacePosX" + i),
                        nbt.getInt("ReplacePosY" + i),
                        nbt.getInt("ReplacePosZ" + i)
                    ),
                    nbt.getInt("ReplaceBlockId" + i)
                );
            }
        }

        // Dungeon edge list
        if (nbt.contains("DungeonEdgeSize", NbtElement.INT_TYPE)) {
            for (int i = 0; i < nbt.getInt("DungeonEdgeSize") / 3; i++) {
                data.dungeonEdgeList.add(nbt.getInt("DungeonEdgeX" + i));
                data.dungeonEdgeList.add(nbt.getInt("DungeonEdgeY" + i));
                data.dungeonEdgeList.add(nbt.getInt("DungeonEdgeZ" + i));
            }
        }

        return data;
    }
}
