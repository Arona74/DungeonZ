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
                nbt.putIntArray("BlockPos" + blockCount + "" + i, List.of(pos.getX(), pos.getY(), pos.getZ()));
            }
            blockCount++;
        }

        // Chest list
        nbt.putInt("ChestListSize", this.chestPosList.size());
        for (int i = 0; i < this.chestPosList.size(); i++) {
            BlockPos pos = this.chestPosList.get(i);
            nbt.putIntArray("ChestPos" + i, List.of(pos.getX(), pos.getY(), pos.getZ()));
        }

        // Exit list
        nbt.putInt("ExitListSize", this.exitPosList.size());
        for (int i = 0; i < this.exitPosList.size(); i++) {
            BlockPos pos = this.exitPosList.get(i);
            nbt.putIntArray("ExitPos" + i, List.of(pos.getX(), pos.getY(), pos.getZ()));
        }

        // Gate list
        nbt.putInt("GateListSize", this.gatePosList.size());
        for (int i = 0; i < this.gatePosList.size(); i++) {
            BlockPos pos = this.gatePosList.get(i);
            nbt.putIntArray("GatePos" + i, List.of(pos.getX(), pos.getY(), pos.getZ()));
        }

        // Moving block map
        nbt.putInt("MovingPosSize", this.movingBlockMap.size());
        int movingCount = 0;
        for (Map.Entry<BlockPos, Integer> entry : this.movingBlockMap.entrySet()) {
            BlockPos pos = entry.getKey();
            nbt.putIntArray("MovingPos" + movingCount, List.of(pos.getX(), pos.getY(), pos.getZ(), entry.getValue()));
            movingCount++;
        }

        // Powered block map
        nbt.putInt("PoweredPosSize", this.poweredBlockMap.size());
        int poweredCount = 0;
        for (Map.Entry<BlockPos, DungeonPortalEntity.Powered> entry : this.poweredBlockMap.entrySet()) {
            DungeonPortalEntity.Powered p = entry.getValue();
            BlockPos pos = entry.getKey();
            nbt.putIntArray("PoweredPos" + poweredCount, List.of(
                pos.getX(), pos.getY(), pos.getZ(),
                p.getBlockId(),
                p.getPowered() ? 1 : 0,
                p.getFacing(),
                p.getBlockFacing()
            ));
            poweredCount++;
        }

        // Spawner map
        nbt.putInt("SpawnerMapSize", this.spawnerPosEntityIdMap.size());
        int spawnerCount = 0;
        for (Map.Entry<BlockPos, String> entry : this.spawnerPosEntityIdMap.entrySet()) {
            BlockPos pos = entry.getKey();
            nbt.putIntArray("SpawnerPos" + spawnerCount, List.of(pos.getX(), pos.getY(), pos.getZ()));
            nbt.putString("SpawnerEntityId" + spawnerCount, entry.getValue());
            spawnerCount++;
        }

        // Replace map
        nbt.putInt("ReplacePosSize", this.replacePosBlockIdMap.size());
        int replaceCount = 0;
        for (Map.Entry<BlockPos, Integer> entry : this.replacePosBlockIdMap.entrySet()) {
            BlockPos pos = entry.getKey();
            nbt.putIntArray("ReplacePos" + replaceCount, List.of(pos.getX(), pos.getY(), pos.getZ(), entry.getValue()));
            replaceCount++;
        }

        // Dungeon edge list
        nbt.putInt("DungeonEdgeSize", this.dungeonEdgeList.size());
        for (int i = 0; i < this.dungeonEdgeList.size() / 3; i++) {
            nbt.putIntArray("DungeonEdge" + i, List.of(this.dungeonEdgeList.get(3 * i), this.dungeonEdgeList.get(1 + 3 * i), this.dungeonEdgeList.get(2 + 3 * i)));
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
                    int[] blockPos = nbt.getIntArray("BlockPos" + i + "" + u);
                    posList.add(new BlockPos(blockPos[0], blockPos[1], blockPos[2]));
                }
                data.blockBlockPosMap.put(nbt.getInt("BlockId" + i), posList);
            }
        }

        // Chest list
        if (nbt.contains("ChestListSize", NbtElement.INT_TYPE)) {
            for (int i = 0; i < nbt.getInt("ChestListSize"); i++) {
                int[] chestPos = nbt.getIntArray("ChestPos" + i);
                data.chestPosList.add(new BlockPos(chestPos[0], chestPos[1], chestPos[2]));
            }
        }

        // Exit list
        if (nbt.contains("ExitListSize", NbtElement.INT_TYPE)) {
            for (int i = 0; i < nbt.getInt("ExitListSize"); i++) {
                int[] exitPos = nbt.getIntArray("ExitPos" + i);
                data.exitPosList.add(new BlockPos(exitPos[0], exitPos[1], exitPos[2]));
            }
        }

        // Gate list
        if (nbt.contains("GateListSize", NbtElement.INT_TYPE)) {
            for (int i = 0; i < nbt.getInt("GateListSize"); i++) {
                int[] gatePos = nbt.getIntArray("GatePos" + i);
                data.gatePosList.add(new BlockPos(gatePos[0], gatePos[1], gatePos[2]));
            }
        }

        // Moving block map
        if (nbt.contains("MovingPosSize", NbtElement.INT_TYPE)) {
            for (int i = 0; i < nbt.getInt("MovingPosSize"); i++) {
                int[] movingPos = nbt.getIntArray("MovingPos" + i);
                data.movingBlockMap.put(new BlockPos(movingPos[0], movingPos[1], movingPos[2]), movingPos[3]);
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
                int[] spawnerPos = nbt.getIntArray("SpawnerPos" + i);
                data.spawnerPosEntityIdMap.put(
                    new BlockPos(spawnerPos[0], spawnerPos[1], spawnerPos[2]),
                    nbt.getString("SpawnerEntityId" + i)
                );
            }
        }

        // Replace map
        if (nbt.contains("ReplacePosSize", NbtElement.INT_TYPE)) {
            for (int i = 0; i < nbt.getInt("ReplacePosSize"); i++) {
                int[] replacePos = nbt.getIntArray("ReplacePos" + i);
                data.replacePosBlockIdMap.put(
                    new BlockPos(replacePos[0], replacePos[1], replacePos[2]),
                    replacePos[3]
                );
            }
        }

        // Dungeon edge list
        if (nbt.contains("DungeonEdgeSize", NbtElement.INT_TYPE)) {
            for (int i = 0; i < nbt.getInt("DungeonEdgeSize") / 3; i++) {
                int[] dungeonEdgePos = nbt.getIntArray("DungeonEdge" + i);
                data.dungeonEdgeList.add(dungeonEdgePos[0]);
                data.dungeonEdgeList.add(dungeonEdgePos[1]);
                data.dungeonEdgeList.add(dungeonEdgePos[2]);
            }
        }

        return data;
    }
}
