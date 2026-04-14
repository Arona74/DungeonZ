package net.dungeonz.block.screen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.dungeonz.block.entity.DungeonPortalEntity;
import net.dungeonz.init.BlockInit;
import net.dungeonz.util.DungeonHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class DungeonPortalScreenHandler extends ScreenHandler {

    private World world;
    private ScreenHandlerContext context;
    private DungeonPortalEntity dungeonPortalEntity;
    private BlockPos pos;

    private List<String> difficulties = new ArrayList<String>();
    private Map<String, List<ItemStack>> possibleLootDifficultyItemStackMap = new HashMap<String, List<ItemStack>>();
    private Map<String, List<ItemStack>> requiredItemStacks = new HashMap<String, List<ItemStack>>();
    private int waitingGroupSize = 0;

    private int requiredLevel = 0;
    private boolean allowRespawn = false;
    private boolean keepInventory = false;
    private boolean allowPositiveEffects = false;
    private boolean allowEnderPearl = false;
    private boolean allowElytra = false;
    private boolean allowMobsLoot = true;
    private boolean allowBossLoot = true;

    @Nullable
    private Identifier backgroundId = null;

    private boolean dungeonTimerActive = false;
    private int dungeonTimeRemaining = 0;
    private Map<String, Integer> fameRewards = new HashMap<>();

    public DungeonPortalScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        super(BlockInit.PORTAL, syncId);
    
        // CRITICAL: Immediately reject spectators to prevent any data reading
        if (playerInventory.player.isSpectator()) {
            // Initialize with absolute minimum to prevent any errors
            this.world = playerInventory.player.getWorld();
            this.context = ScreenHandlerContext.EMPTY;
            this.pos = BlockPos.ORIGIN;
            this.dungeonPortalEntity = null;
            
            // Set all fields to safe empty values
            this.difficulties = new ArrayList<>();
            this.possibleLootDifficultyItemStackMap = new HashMap<>();
            this.requiredItemStacks = new HashMap<>();
            this.waitingGroupSize = 0;
            this.requiredLevel = 0;
            this.allowRespawn = false;
            this.keepInventory = false;
            this.allowPositiveEffects = false;
            this.allowEnderPearl = false;
            this.allowElytra = false;
            this.allowMobsLoot = true;
            this.allowBossLoot = true;
            this.backgroundId = null;
            this.dungeonTimerActive = false;
            this.dungeonTimeRemaining = 0;
            
            // Don't read anything from the buffer - just return
            return;
        }

        // Read exactly what your packet writes, in the same order
        
        // 1. BlockPos
        BlockPos blockPos = buf.readBlockPos();
        
        // Initialize fields
        this.world = playerInventory.player.getWorld();
        this.context = ScreenHandlerContext.EMPTY;
        this.dungeonPortalEntity = new DungeonPortalEntity(blockPos, this.world.getBlockState(blockPos));
        this.pos = blockPos;
        
        // 2. playerUuids
        int dungeonPlayerCount = buf.readInt();
        List<UUID> dungeonPlayerUUIDs = new ArrayList<UUID>();
        for (int i = 0; i < dungeonPlayerCount; i++) {
            dungeonPlayerUUIDs.add(buf.readUuid());
        }
        
        // 3. deadPlayerUuids
        int deadDungeonPlayerCount = buf.readInt();
        List<UUID> deadDungeonPlayerUUIDs = new ArrayList<UUID>();
        for (int i = 0; i < deadDungeonPlayerCount; i++) {
            deadDungeonPlayerUUIDs.add(buf.readUuid());
        }
        
        // 4. waitingPlayerUuids
        int waitingPlayerCount = buf.readInt();
        List<UUID> waitingUUIDs = new ArrayList<UUID>();
        for (int i = 0; i < waitingPlayerCount; i++) {
            waitingUUIDs.add(buf.readUuid());
        }
        
        // 5. difficulties
        int difficultyCount = buf.readInt();
        List<String> difficulties = new ArrayList<String>();
        for (int i = 0; i < difficultyCount; i++) {
            difficulties.add(buf.readString(32767));
        }
        
        // 6. possibleLoot
        int possibleLootCount = buf.readInt();
        Map<String, List<ItemStack>> possibleLootDifficultyItemStackMap = new HashMap<String, List<ItemStack>>();
        for (int i = 0; i < possibleLootCount; i++) {
            String difficulty = buf.readString(32767);
            int lootCount = buf.readInt();
            List<ItemStack> itemStacks = new ArrayList<ItemStack>();
            for (int u = 0; u < lootCount; u++) {
                itemStacks.add(buf.readItemStack());
            }
            possibleLootDifficultyItemStackMap.put(difficulty, itemStacks);
        }

        // 7. requiredItemStacks
        int requiredItemCount = buf.readInt();
        Map<String, List<ItemStack>> requiredItemStacksMap = new HashMap<String, List<ItemStack>>();
        for (int i = 0; i < requiredItemCount; i++) {
            String difficulty = buf.readString(32767);
            int reqCount = buf.readInt();
            List<ItemStack> itemStacks = new ArrayList<ItemStack>();
            for (int u = 0; u < reqCount; u++) {
                itemStacks.add(buf.readItemStack());
            }
            requiredItemStacksMap.put(difficulty, itemStacks);
        }
        
        // 8. Five integers
        int maxGroupSize = buf.readInt();
        int minGroupSize = buf.readInt();
        int waitingGroupSize = buf.readInt();
        int requiredLevel = buf.readInt();
        int cooldownTime = buf.readInt();
        
        // 9. difficulty string
        String difficulty = buf.readString(32767);
        
        // 10. Five booleans
        boolean allowEnderPearl = buf.readBoolean();
        boolean allowPositiveEffects = buf.readBoolean();
        boolean allowElytra = buf.readBoolean();
        boolean allowRespawn = buf.readBoolean();
        boolean allowMobsLoot = buf.readBoolean();
        boolean allowBossLoot = buf.readBoolean();
        boolean keepInventory = buf.readBoolean();
        boolean privateGroup = buf.readBoolean();
        
        // 11. Optional backgroundId
        Identifier backgroundId = null;
        if (buf.readBoolean()) {
            backgroundId = buf.readIdentifier();
        }
        
        // 12. timestamp
        long timestamp = buf.readLong();

        // 13. Fame rewards (at the end)
        int fameRewardCount = buf.readInt();
        Map<String, Integer> fameRewardsMap = new HashMap<>();
        for (int i = 0; i < fameRewardCount; i++) {
            fameRewardsMap.put(buf.readString(32767), buf.readInt());
        }

        // Set all values
        this.setDifficulties(difficulties);
        this.setPossibleLootItemStacks(possibleLootDifficultyItemStackMap);
        this.setRequiredItemStacks(requiredItemStacksMap);
        this.setWaitingGroupSize(waitingGroupSize);
        this.setDungeonTimerActive(false);
        this.setDungeonTimeRemaining(0);

        this.getDungeonPortalEntity().setDungeonPlayerUuids(dungeonPlayerUUIDs);
        this.getDungeonPortalEntity().setDeadDungeonPlayerUuids(deadDungeonPlayerUUIDs);
        this.getDungeonPortalEntity().setMaxGroupSize(maxGroupSize);
        this.getDungeonPortalEntity().setMinGroupSize(minGroupSize);
        this.getDungeonPortalEntity().setWaitingUuids(waitingUUIDs);
        this.getDungeonPortalEntity().setCooldownTime(cooldownTime);
        this.getDungeonPortalEntity().setDifficulty(difficulty);
        this.getDungeonPortalEntity().setPrivateGroup(privateGroup);

        this.requiredLevel = requiredLevel;
        this.allowRespawn = allowRespawn;
        this.keepInventory = keepInventory;
        this.allowPositiveEffects = allowPositiveEffects;
        this.allowEnderPearl = allowEnderPearl;
        this.allowElytra = allowElytra;
        this.allowMobsLoot = allowMobsLoot;
        this.allowBossLoot = allowBossLoot;
        this.backgroundId = backgroundId;
        this.fameRewards = fameRewardsMap;
    }

    public DungeonPortalScreenHandler(int syncId, PlayerInventory playerInventory, DungeonPortalEntity dungeonPortalEntity, ScreenHandlerContext context) {
        super(BlockInit.PORTAL, syncId);
        this.context = context;
        this.world = playerInventory.player.getWorld();
        this.dungeonPortalEntity = dungeonPortalEntity;
        this.pos = dungeonPortalEntity.getPos();

        if (!this.world.isClient()) {
            setDifficulties(this.dungeonPortalEntity.getDungeon().getDifficultyList());
            setRequiredItemStacks(DungeonHelper.getRequiredItemStackList(this.dungeonPortalEntity.getDungeon()));
            setPossibleLootItemStacks(this.dungeonPortalEntity.getDungeon().isHidePossibleLoot() ? new java.util.HashMap<>() : DungeonHelper.getPossibleLootItemStackMap(this.dungeonPortalEntity.getDungeon(), this.world.getServer()));

            if (this.dungeonPortalEntity.isDungeonTimerActive()) {
                setDungeonTimerActive(true);
                setDungeonTimeRemaining(this.dungeonPortalEntity.getDungeonTimeRemaining());
            }
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity var1, int var2) {
        return null;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        // CRITICAL: Spectators should NEVER be able to use this screen
        if (player.isSpectator() || this.dungeonPortalEntity == null) {
            return false;
        }
        return this.context.get((world, pos) -> {
            if (!this.world.getBlockState(pos).isOf(BlockInit.DUNGEON_PORTAL)) {
                return false;
            }
            return player.squaredDistanceTo((double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5) <= 64.0;
        }, true);
    }

    @Nullable
    public Identifier getBackgroundId() {
        return this.backgroundId;
    }

    public DungeonPortalEntity getDungeonPortalEntity() {
        return this.dungeonPortalEntity;
    }

    public List<String> getDifficulties() {
        return this.difficulties;
    }

    public void setDifficulties(List<String> difficulties) {
        this.difficulties = difficulties;
    }

    public Map<String, List<ItemStack>> getPossibleLootDifficultyItemStackMap() {
        return this.possibleLootDifficultyItemStackMap;
    }

    public void setPossibleLootItemStacks(Map<String, List<ItemStack>> possibleLootDifficultyItemStackMap) {
        this.possibleLootDifficultyItemStackMap = possibleLootDifficultyItemStackMap;
    }

    public Map<String, List<ItemStack>> getRequiredItemStacks() {
        return this.requiredItemStacks;
    }

    public void setRequiredItemStacks(Map<String, List<ItemStack>> requiredItemStacks) {
        this.requiredItemStacks = requiredItemStacks;
    }

    public int getWaitingGroupSize() {
        return this.waitingGroupSize;
    }

    public void setWaitingGroupSize(int waitingGroupSize) {
        this.waitingGroupSize = waitingGroupSize;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public int getRequiredLevel() {
        return this.requiredLevel;
    }

    public boolean isAllowRespawn() {
        return allowRespawn;
    }

    public boolean isKeepInventory() {
        return keepInventory;
    }

    public boolean isAllowPositiveEffects() {
        return allowPositiveEffects;
    }

    public boolean isAllowEnderPearl() {
        return allowEnderPearl;
    }

    public boolean isAllowElytra() {
        return allowElytra;
    }

    public boolean isAllowMobsLoot() {
        return allowMobsLoot;
    }

    public boolean isAllowBossLoot() {
        return allowBossLoot;
    }

    public boolean isDungeonTimerActive() {
        return dungeonTimerActive;
    }

    public void setDungeonTimerActive(boolean active) {
        this.dungeonTimerActive = active;
    }

    public int getDungeonTimeRemaining() {
        return this.dungeonTimeRemaining;
    }

    public void setDungeonTimeRemaining(int timeRemaining) {
        this.dungeonTimeRemaining = timeRemaining;
    }

    public Map<String, Integer> getFameRewards() {
        return this.fameRewards;
    }

    public int getFameRewardForDifficulty(String difficulty) {
        return this.fameRewards.getOrDefault(difficulty, 0);
    }
}
