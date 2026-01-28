package net.dungeonz.block.entity;

import net.dungeonz.block.screen.DungeonPortalScreenHandler;
import net.dungeonz.compat.LootrCompat;
import net.dungeonz.dungeon.Dungeon;
import net.dungeonz.dungeon.DungeonPlacementHandler;
import net.dungeonz.init.*;
import net.dungeonz.network.DungeonServerPacket;
import net.dungeonz.util.DungeonHelper;
import net.dungeonz.util.FameHelper;
import net.dungeonz.util.InventoryHelper;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.EndPortalBlockEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.Map.Entry;

public class DungeonPortalEntity extends EndPortalBlockEntity implements ExtendedScreenHandlerFactory {

    private Text title = Text.translatable("container.dungeon_portal");
    private String dungeonType = "";
    private String difficulty = "";
    private boolean dungeonStructureGenerated = false;
    private List<UUID> dungeonPlayerUuids = new ArrayList<UUID>();
    private List<UUID> deadDungeonPlayerUuids = new ArrayList<UUID>();
    private int maxGroupSize = 0;
    private int minGroupSize = 0;
    private List<UUID> waitingUuids = new ArrayList<UUID>();
    private int cooldownTime = 0;
    private int dungeonStartTime = 0;
    private boolean dungeonTimerActive = false;
    private int autoKickTime = 0;
    private boolean privateGroup = false;
    // Large runtime data (blockBlockPosMap, movingBlockMap, etc.) is now stored via DungeonDataManager
    // to avoid NBT size limits. See DungeonDataManager and DungeonRuntimeData classes.
    private BlockPos bossBlockPos = new BlockPos(0, 0, 0);
    private BlockPos bossLootBlockPos = new BlockPos(0, 0, 0);
    private int dungeonTeleportCountdown = 0;
    private boolean needsMigration = false; // Flag for deferred migration from old NBT format
    private NbtCompound pendingMigrationData = null; // Stores old NBT data for deferred migration
    private boolean validationChecked = false;

    public DungeonPortalEntity(BlockPos pos, BlockState state) {
        super(BlockInit.DUNGEON_PORTAL_ENTITY, pos, state);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.dungeonType = nbt.getString("DungeonType");
        this.difficulty = nbt.getString("Difficulty");
        this.dungeonStructureGenerated = nbt.getBoolean("DungeonStructureGenerated");
        this.dungeonPlayerUuids.clear();
        for (int i = 0; i < nbt.getInt("DungeonPlayerCount"); i++) {
            this.dungeonPlayerUuids.add(nbt.getUuid("PlayerUUID" + i));
        }
        this.deadDungeonPlayerUuids.clear();
        for (int i = 0; i < nbt.getInt("DeadDungeonPlayerCount"); i++) {
            this.deadDungeonPlayerUuids.add(nbt.getUuid("DeadPlayerUUID" + i));
        }
        this.maxGroupSize = nbt.getInt("MaxGroupSize");
        this.minGroupSize = nbt.getInt("MinGroupSize");
        this.cooldownTime = nbt.getInt("CooldownTime");
        this.dungeonStartTime = nbt.getInt("DungeonStartTime");
        this.dungeonTimerActive = nbt.getBoolean("DungeonTimerActive");
        this.autoKickTime = nbt.getInt("AutoKickTime");
        this.privateGroup = nbt.getBoolean("PrivateGroup");

        this.bossBlockPos = new BlockPos(nbt.getInt("BossPosX"), nbt.getInt("BossPosY"), nbt.getInt("BossPosZ"));
        this.bossLootBlockPos = new BlockPos(nbt.getInt("BossLootPosX"), nbt.getInt("BossLootPosY"), nbt.getInt("BossLootPosZ"));

        // MIGRATION: Check if this is old format (has large runtime data in NBT)
        // Store data for deferred migration since world may be null during initial load
        boolean isOldFormat = nbt.contains("BlockMapSize") || nbt.contains("MovingPosSize") ||
                              nbt.contains("ChestListSize") || nbt.contains("DungeonEdgeSize");

        if (isOldFormat) {
            // World is null during initial load, so defer migration to first server tick
            this.needsMigration = true;
            this.pendingMigrationData = nbt.copy(); // Store copy for later migration
            LOGGER.info("Detected old NBT format for dungeon portal at {}. Migration will occur on first tick.", this.pos);
        }
    }

    private static final Logger LOGGER = LogManager.getLogger("DungeonPortal");

    /**
     * Performs migration from old NBT format to new DungeonDataManager system.
     * Called during first server tick when old format is detected.
     */
    private void performMigration(ServerWorld world) {
        NbtCompound nbt = this.pendingMigrationData;
        if (nbt == null) {
            return;
        }

        LOGGER.info("Performing deferred migration for dungeon portal at {}...", this.pos);

        // Read old format data into temporary runtime data object
        net.dungeonz.dungeon.DungeonRuntimeData runtimeData = new net.dungeonz.dungeon.DungeonRuntimeData();

        // Migrate blockBlockPosMap
        if (nbt.getInt("BlockMapSize") > 0) {
            HashMap<Integer, ArrayList<BlockPos>> tempBlockMap = new HashMap<>();
            for (int i = 0; i < nbt.getInt("BlockMapSize"); i++) {
                ArrayList<BlockPos> posList = new ArrayList<>();
                for (int u = 0; u < nbt.getInt("BlockListSize" + i); u++) {
                    posList.add(new BlockPos(nbt.getInt("BlockPosX" + i + "_" + u), nbt.getInt("BlockPosY" + i + "_" + u), nbt.getInt("BlockPosZ" + i + "_" + u)));
                }
                tempBlockMap.put(nbt.getInt("BlockId" + i), posList);
            }
            runtimeData.setBlockBlockPosMap(tempBlockMap);
        }

        // Migrate chest list
        if (nbt.getInt("ChestListSize") > 0) {
            List<BlockPos> tempChestList = new ArrayList<>();
            for (int i = 0; i < nbt.getInt("ChestListSize"); i++) {
                tempChestList.add(new BlockPos(nbt.getInt("ChestPosX" + i), nbt.getInt("ChestPosY" + i), nbt.getInt("ChestPosZ" + i)));
            }
            runtimeData.setChestPosList(tempChestList);
        }

        // Migrate exit list
        if (nbt.getInt("ExitListSize") > 0) {
            List<BlockPos> tempExitList = new ArrayList<>();
            for (int i = 0; i < nbt.getInt("ExitListSize"); i++) {
                tempExitList.add(new BlockPos(nbt.getInt("ExitPosX" + i), nbt.getInt("ExitPosY" + i), nbt.getInt("ExitPosZ" + i)));
            }
            runtimeData.setExitPosList(tempExitList);
        }

        // Migrate gate list
        if (nbt.getInt("GateListSize") > 0) {
            List<BlockPos> tempGateList = new ArrayList<>();
            for (int i = 0; i < nbt.getInt("GateListSize"); i++) {
                tempGateList.add(new BlockPos(nbt.getInt("GatePosX" + i), nbt.getInt("GatePosY" + i), nbt.getInt("GatePosZ" + i)));
            }
            runtimeData.setGatePosList(tempGateList);
        }

        // Migrate spawner map
        if (nbt.getInt("SpawnerMapSize") > 0) {
            HashMap<BlockPos, String> tempSpawnerMap = new HashMap<>();
            for (int i = 0; i < nbt.getInt("SpawnerMapSize"); i++) {
                tempSpawnerMap.put(new BlockPos(nbt.getInt("SpawnerPosX" + i), nbt.getInt("SpawnerPosY" + i), nbt.getInt("SpawnerPosZ" + i)), nbt.getString("SpawnerEntityId" + i));
            }
            runtimeData.setSpawnerPosEntityIdMap(tempSpawnerMap);
        }

        // Migrate replace map
        if (nbt.getInt("ReplacePosSize") > 0) {
            HashMap<BlockPos, Integer> tempReplaceMap = new HashMap<>();
            for (int i = 0; i < nbt.getInt("ReplacePosSize"); i++) {
                tempReplaceMap.put(new BlockPos(nbt.getInt("ReplacePosX" + i), nbt.getInt("ReplacePosY" + i), nbt.getInt("ReplacePosZ" + i)), nbt.getInt("ReplaceBlockId" + i));
            }
            runtimeData.setReplacePosBlockIdMap(tempReplaceMap);
        }

        // Migrate moving block map
        if (nbt.getInt("MovingPosSize") > 0) {
            Map<BlockPos, Integer> tempMovingMap = new HashMap<>();
            for (int i = 0; i < nbt.getInt("MovingPosSize"); i++) {
                tempMovingMap.put(new BlockPos(nbt.getInt("MovingPosX" + i), nbt.getInt("MovingPosY" + i), nbt.getInt("MovingPosZ" + i)), nbt.getInt("MovingBlockId" + i));
            }
            runtimeData.setMovingBlockMap(tempMovingMap);
        }

        // Migrate powered block map
        if (nbt.getInt("PoweredPosSize") > 0) {
            Map<BlockPos, Powered> tempPoweredMap = new HashMap<>();
            for (int i = 0; i < nbt.getInt("PoweredPosSize"); i++) {
                int[] poweredPos = nbt.getIntArray("PoweredPos" + i);
                if (poweredPos.length >= 7) {
                    boolean isPowered = poweredPos[4] == 1;
                    tempPoweredMap.put(new BlockPos(poweredPos[0], poweredPos[1], poweredPos[2]), new Powered(poweredPos[3], isPowered, poweredPos[5], poweredPos[6]));
                }
            }
            runtimeData.setPoweredBlockMap(tempPoweredMap);
        }

        // Migrate dungeon edge list
        if (nbt.getInt("DungeonEdgeSize") > 0) {
            List<Integer> tempEdgeList = new ArrayList<>();
            for (int i = 0; i < nbt.getInt("DungeonEdgeSize") / 3; i++) {
                tempEdgeList.add(nbt.getInt("DungeonEdgeX" + i));
                tempEdgeList.add(nbt.getInt("DungeonEdgeY" + i));
                tempEdgeList.add(nbt.getInt("DungeonEdgeZ" + i));
            }
            runtimeData.setDungeonEdgeList(tempEdgeList);
        }

        // Save migrated data to new system
        net.dungeonz.dungeon.DungeonDataManager.migrateFromOldNbt(world, this.pos, runtimeData);

        LOGGER.info("Migration complete for dungeon portal at {}. Data moved to separate file.", this.pos);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);

        // NEW SYSTEM: Only save minimal metadata to block entity NBT
        // Large runtime data is stored separately via DungeonDataManager

        // Basic metadata
        nbt.putString("DungeonType", this.dungeonType);
        nbt.putString("Difficulty", this.difficulty);
        nbt.putBoolean("DungeonStructureGenerated", this.dungeonStructureGenerated);
        nbt.putInt("MaxGroupSize", this.maxGroupSize);
        nbt.putInt("MinGroupSize", this.minGroupSize);
        nbt.putInt("CooldownTime", this.cooldownTime);
        nbt.putInt("DungeonStartTime", this.dungeonStartTime);
        nbt.putBoolean("DungeonTimerActive", this.dungeonTimerActive);
        nbt.putInt("AutoKickTime", this.autoKickTime);
        nbt.putBoolean("PrivateGroup", this.privateGroup);

        // Player UUIDs
        nbt.putInt("DungeonPlayerCount", this.dungeonPlayerUuids.size());
        for (int i = 0; i < this.dungeonPlayerUuids.size(); i++) {
            nbt.putUuid("PlayerUUID" + i, this.dungeonPlayerUuids.get(i));
        }

        nbt.putInt("DeadDungeonPlayerCount", this.deadDungeonPlayerUuids.size());
        for (int i = 0; i < this.deadDungeonPlayerUuids.size(); i++) {
            nbt.putUuid("DeadPlayerUUID" + i, this.deadDungeonPlayerUuids.get(i));
        }

        // Boss positions (small, always needed)
        nbt.putInt("BossPosX", this.bossBlockPos.getX());
        nbt.putInt("BossPosY", this.bossBlockPos.getY());
        nbt.putInt("BossPosZ", this.bossBlockPos.getZ());
        nbt.putInt("BossLootPosX", this.bossLootBlockPos.getX());
        nbt.putInt("BossLootPosY", this.bossLootBlockPos.getY());
        nbt.putInt("BossLootPosZ", this.bossLootBlockPos.getZ());

        // Large runtime data is NO LONGER stored in NBT
        // It's managed by DungeonDataManager in separate files
        // Migration logic in readNbt() handles old format

        // If migration from old format hasn't completed yet, preserve old data in NBT
        // to prevent data loss if chunk is saved before first serverTick
        if (this.needsMigration && this.pendingMigrationData != null) {
            for (String key : this.pendingMigrationData.getKeys()) {
                if (!nbt.contains(key)) {
                    nbt.put(key, this.pendingMigrationData.get(key).copy());
                }
            }
            LOGGER.debug("Preserved old NBT format for dungeon portal at {} (migration pending)", this.pos);
        } else {
            LOGGER.debug("Saved minimal NBT for dungeon portal at {} (new system)", this.pos);
        }
    }


    public static void clientTick(World world, BlockPos pos, BlockState state, DungeonPortalEntity blockEntity) {
    }

    public static void serverTick(World world, BlockPos pos, BlockState state, DungeonPortalEntity blockEntity) {
        // MIGRATION: Perform deferred migration from old NBT format on first tick
        if (blockEntity.needsMigration && blockEntity.pendingMigrationData != null && world instanceof ServerWorld) {
            blockEntity.performMigration((ServerWorld) world);
            blockEntity.needsMigration = false;
            blockEntity.pendingMigrationData = null;
        }

        // Validate and repair portal data once after load
        if (!blockEntity.validationChecked && !blockEntity.dungeonType.isEmpty()) {
            blockEntity.validationChecked = true;
            Dungeon dungeon = blockEntity.getDungeon();
            if (dungeon != null) {
                boolean repaired = false;
                if (blockEntity.difficulty.isEmpty() || !dungeon.getDifficultyList().contains(blockEntity.difficulty)) {
                    blockEntity.setDifficulty(dungeon.getDifficultyList().get(0));
                    repaired = true;
                }
                if (blockEntity.maxGroupSize != dungeon.getMaxGroupSize()) {
                    blockEntity.setMaxGroupSize(dungeon.getMaxGroupSize());
                    repaired = true;
                }
                if (blockEntity.minGroupSize != dungeon.getMinGroupSize()) {
                    blockEntity.setMinGroupSize(dungeon.getMinGroupSize());
                    repaired = true;
                }
                if (repaired) {
                    LOGGER.info("Repaired dungeon portal '{}' at {}", blockEntity.dungeonType, pos);
                    blockEntity.markDirty();
                }
            }
        }

        if (blockEntity.getDungeonPlayerCount() > 0) {
            if (blockEntity.autoKickTime == 0) {
                blockEntity.autoKickTime = (int) world.getTime() + 144000;
            } else if (blockEntity.autoKickTime < (int) world.getTime()) {
                if (blockEntity.getDungeon() != null) {
                    blockEntity.setCooldownTime(blockEntity.getDungeon().getCooldown() + (int) blockEntity.getWorld().getTime());
                    for (int i = 0; i < blockEntity.getDungeonPlayerUuids().size(); i++) {
                        ServerPlayerEntity player = (ServerPlayerEntity) world.getPlayerByUuid(blockEntity.getDungeonPlayerUuids().get(i));
                        if (DungeonHelper.getCurrentDungeon(player) != null) {
                            DungeonHelper.teleportOutOfDungeon(player);
                            player.sendMessage(Text.translatable("text.dungeonz.dungeon_autokick"));
                        }
                    }
                }
                blockEntity.getDungeonPlayerUuids().clear();
                blockEntity.getDeadDungeonPlayerUuids().clear();
                blockEntity.autoKickTime = 0;
            }
        } else if (blockEntity.autoKickTime != 0) {
            blockEntity.autoKickTime = 0;
        }
        
        if (blockEntity.dungeonTeleportCountdown >= 1) {
            if (blockEntity.dungeonTeleportCountdown % 20 == 0) {
                for (int i = 0; i < blockEntity.getWaitingUuids().size(); i++) {
                    if (((ServerWorld) blockEntity.getWorld()).getEntity(blockEntity.getWaitingUuids().get(i)) != null
                            && ((ServerWorld) blockEntity.getWorld()).getEntity(blockEntity.getWaitingUuids().get(i)) instanceof ServerPlayerEntity serverPlayerEntity) {
                        DungeonServerPacket.writeS2CDungeonTeleportCountdown(serverPlayerEntity, blockEntity.dungeonTeleportCountdown);
                    }
                }
            }
            blockEntity.dungeonTeleportCountdown--;

            if (blockEntity.dungeonTeleportCountdown == (ConfigInit.CONFIG.defaultDungeonTeleportCountdown / 2)) {
               DungeonPlacementHandler.refreshDungeon(((ServerWorld) blockEntity.getWorld()).getServer(), blockEntity.getWorld().getServer().getWorld(DimensionInit.DUNGEON_WORLD), blockEntity,
                        blockEntity.getDungeon(), blockEntity.getDifficulty());
            }

            if (blockEntity.dungeonTeleportCountdown == 0) {
                for (int i = 0; i < blockEntity.getWaitingUuids().size(); i++) {
                    if (((ServerWorld) blockEntity.getWorld()).getEntity(blockEntity.getWaitingUuids().get(i)) != null
                            && ((ServerWorld) blockEntity.getWorld()).getEntity(blockEntity.getWaitingUuids().get(i)) instanceof ServerPlayerEntity serverPlayerEntity) {
                        DungeonHelper.teleportPlayer(serverPlayerEntity, blockEntity.getWorld().getServer().getWorld(DimensionInit.DUNGEON_WORLD), blockEntity, blockEntity.getPos());
                    }
                }
                blockEntity.getWaitingUuids().clear();
                blockEntity.startDungeonTimer();
            }
        }

        // Stop timer if no players are in dungeon
        if (blockEntity.isDungeonTimerActive() && blockEntity.getDungeonPlayerCount() == 0) {
            blockEntity.stopDungeonTimer();
        }
        
        // Sync timer every second for real-time GUI updates
        if (blockEntity.isDungeonTimerActive() && world.getTime() % 20 == 0) {
            blockEntity.syncGuiToAllViewers();
        }
        
        // Check if timer expired
        if (blockEntity.isDungeonTimerExpired()) {
            blockEntity.handleDungeonTimerExpired();
        }
    }

    @Override
    public Text getDisplayName() {
        if (this.getDungeon() != null) {
            return Text.translatable("dungeon." + this.getDungeonType());
        }
        return title;
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return this.createNbt();
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity playerEntity) {
        return new DungeonPortalScreenHandler(syncId, playerInventory, this, ScreenHandlerContext.create(world, pos));
    }

    @Override
    public boolean shouldDrawSide(Direction direction) {
        return true;
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeBlockPos(this.pos);
        buf.writeBlockPos(this.pos);

        buf.writeInt(this.getDungeonPlayerCount());
        for (int i = 0; i < this.getDungeonPlayerCount(); i++) {
            buf.writeUuid(this.getDungeonPlayerUuids().get(i));
        }
        buf.writeInt(this.getDeadDungeonPlayerUuids().size());
        for (int i = 0; i < this.getDeadDungeonPlayerUuids().size(); i++) {
            buf.writeUuid(this.getDeadDungeonPlayerUuids().get(i));
        }

        if (this.getDungeon() != null) {
            // Difficulty
            buf.writeInt(this.getDungeon().getDifficultyList().size());
            for (int i = 0; i < this.getDungeon().getDifficultyList().size(); i++) {
                buf.writeString(this.getDungeon().getDifficultyList().get(i));
            }
            // Possible Loot Items
            Map<String, List<ItemStack>> possibleLoot = DungeonHelper.getPossibleLootItemStackMap(this.getDungeon(), player.getServer());
            buf.writeInt(possibleLoot.size());
            Iterator<Entry<String, List<ItemStack>>> possibleLootIterator = possibleLoot.entrySet().iterator();
            while (possibleLootIterator.hasNext()) {
                Entry<String, List<ItemStack>> entry = possibleLootIterator.next();
                buf.writeString(entry.getKey());
                buf.writeInt(entry.getValue().size());
                for (int i = 0; i < entry.getValue().size(); i++) {
                    buf.writeItemStack(entry.getValue().get(i));
                }
            }
            // Required Items
            Map<String, List<ItemStack>> requiredItem = DungeonHelper.getRequiredItemStackList(this.getDungeon());
            buf.writeInt(requiredItem.size());
            Iterator<Entry<String, List<ItemStack>>> requiredItemIterator = requiredItem.entrySet().iterator();
            while (requiredItemIterator.hasNext()) {
                Entry<String, List<ItemStack>> entry = requiredItemIterator.next();
                buf.writeString(entry.getKey());
                buf.writeInt(entry.getValue().size());
                for (int i = 0; i < entry.getValue().size(); i++) {
                    buf.writeItemStack(entry.getValue().get(i));
                }
            }
        } else {
            buf.writeInt(0);
            buf.writeInt(0);
            buf.writeInt(0);
        }

        buf.writeInt(this.getMaxGroupSize());
        buf.writeInt(this.getMinGroupSize());
        buf.writeInt(this.getWaitingUuids().size());
        for (int i = 0; i < this.getWaitingUuids().size(); i++) {
            buf.writeUuid(this.getWaitingUuids().get(i));
        }
        buf.writeInt(this.getCooldownTime());
        buf.writeString(this.getDifficulty());
        buf.writeBoolean(this.getPrivateGroup());

        buf.writeBoolean(this.getDungeon() != null ? this.getDungeon().isElytraAllowed() : false);
        buf.writeBoolean(this.getDungeon() != null ? this.getDungeon().isRespawnAllowed() : false);
        buf.writeBoolean(this.getDungeon() != null ? this.getDungeon().isMobsLootAllowed() : true);
        buf.writeBoolean(this.getDungeon() != null ? this.getDungeon().isBossLootAllowed() : true);
        buf.writeBoolean(this.isDungeonTimerActive());
        buf.writeInt(this.getDungeonTimeRemaining());
    }

    public void finishDungeon(ServerWorld world, BlockPos pos) {
        List<PlayerEntity> players = world.getPlayers(TargetPredicate.createAttackable().setBaseMaxDistance(64.0), null, new Box(pos).expand(64.0, 64.0, 64.0));

        // Get fame reward for current difficulty
        int fameReward = 0;
        if (this.getDungeon() != null) {
            fameReward = this.getDungeon().getFameReward(this.getDifficulty());
        }

        for (PlayerEntity player : players) {
            CriteriaInit.DUNGEON_COMPLETION.trigger((ServerPlayerEntity) player, this.getDungeonType(), this.getDifficulty());
            player.sendMessage(
                Text.literal("Congratulations! You've completed the dungeon! Don't forget to check the Boss chest that appeared in the room.")
                    .formatted(Formatting.GOLD),
                    false
            );
            player.sendMessage(
                Text.literal("Click here to leave or go through portal or use /dungeon leave")
                            .styled(style -> style
                            .withColor(Formatting.GREEN)
                            .withUnderline(true)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/dungeon leave"))
                ),
                false
            );

            // Grant fame power if available
            if (fameReward > 0) {
                boolean fameGranted = FameHelper.grantFamePower((ServerPlayerEntity) player, fameReward);
                FameHelper.sendFameNotification((ServerPlayerEntity) player, fameReward, fameGranted);
            }

            world.playSound(null, pos, SoundInit.DUNGEON_COMPLETION_EVENT, SoundCategory.BLOCKS, 1.0f, 0.9f + world.getRandom().nextFloat() * 0.2f);
        }

        for (int i = 0; i < this.getExitPosList().size(); i++) {
            world.setBlockState(this.getExitPosList().get(i), BlockInit.DUNGEON_PORTAL.getDefaultState(), 3);
        }

        String bossLootTableString = this.getDungeon().getDifficultyBossLootTableMap().get(this.getDifficulty());
        if (ConfigInit.CONFIG.lootrIntegration && LootrCompat.isLootrAvailable()) {
            world.setBlockState(this.getBossLootBlockPos(), Blocks.CHEST.getDefaultState(), 3);
            LootrCompat.convertToLootrChest(world, this.getBossLootBlockPos(), bossLootTableString);
        } else {
            world.setBlockState(this.getBossLootBlockPos(), Blocks.CHEST.getDefaultState(), 3);
            InventoryHelper.fillInventoryWithLoot(world.getServer(), world, this.getBossLootBlockPos(), bossLootTableString);
        }

        this.stopDungeonTimer();
        this.setCooldownTime(this.getDungeon().getCooldown() + (int) this.getWorld().getTime());
        markDirty();
    }

    private void syncGuiToAllViewers() {
        if (this.world != null && !this.world.isClient && this.world.getServer() != null) {
            DungeonServerPacket.writeS2CSyncScreenPacketToAllViewing(this.world.getServer(), this);
        }
    }

    @Nullable
    public Dungeon getDungeon() {
        return Dungeon.getDungeon(this.dungeonType);
    }

    public void setDungeonType(String dungeonType) {
        this.dungeonType = dungeonType;
        this.markDirty();
        this.syncGuiToAllViewers();
    }

    public String getDungeonType() {
        return this.dungeonType;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
        this.markDirty();
        this.syncGuiToAllViewers();
    }

    public String getDifficulty() {
        return this.difficulty;
    }

    public void setDungeonStructureGenerated() {
        this.dungeonStructureGenerated = true;
    }

    public boolean isDungeonStructureGenerated() {
        return this.dungeonStructureGenerated;
    }

    public void joinDungeon(UUID playerUuid) {
        if (!this.dungeonPlayerUuids.contains(playerUuid)) {
            this.dungeonPlayerUuids.add(playerUuid);
            this.markDirty();
            this.syncGuiToAllViewers();
        }
    }

    public void leaveDungeon(UUID playerUuid) {
        boolean removed = this.dungeonPlayerUuids.remove(playerUuid);
        if (removed) {
            this.markDirty();
            this.syncGuiToAllViewers();
        }
    }

    public int getDungeonPlayerCount() {
        return this.dungeonPlayerUuids.size();
    }

    public void setDungeonPlayerUuids(List<UUID> dungeonPlayerUuids) {
        this.dungeonPlayerUuids = dungeonPlayerUuids;
        this.markDirty();
        this.syncGuiToAllViewers();
    }

    public List<UUID> getDungeonPlayerUuids() {
        return this.dungeonPlayerUuids;
    }

    public void addDeadDungeonPlayerUuids(UUID deadDungeonPlayerUuids) {
        this.deadDungeonPlayerUuids.add(deadDungeonPlayerUuids);
        this.markDirty();
        this.syncGuiToAllViewers();
    }

    public void setDeadDungeonPlayerUuids(List<UUID> deadDungeonPlayerUuids) {
        this.deadDungeonPlayerUuids = deadDungeonPlayerUuids;
        this.markDirty();
        this.syncGuiToAllViewers();
    }

    public List<UUID> getDeadDungeonPlayerUuids() {
        return this.deadDungeonPlayerUuids;
    }

    // NEW SYSTEM: Use DungeonDataManager for runtime data

    /**
     * Updates all runtime data at once without triggering multiple saves.
     * Use this during dungeon generation to avoid save spam.
     */
    public void updateAllRuntimeData(HashMap<Integer, ArrayList<BlockPos>> blockMap,
                                      List<BlockPos> chestPosList,
                                      List<BlockPos> exitPosList,
                                      List<BlockPos> gatePosList,
                                      Map<BlockPos, Integer> movingBlockMap,
                                      Map<BlockPos, Powered> poweredBlockMap,
                                      HashMap<BlockPos, String> spawnerPosEntityIdMap) {
        if (!this.world.isClient && this.world instanceof ServerWorld) {
            net.dungeonz.dungeon.DungeonRuntimeData data = net.dungeonz.dungeon.DungeonDataManager.getData((ServerWorld) this.world, this.pos);
            data.setBlockBlockPosMap(blockMap);
            data.setChestPosList(chestPosList);
            data.setExitPosList(exitPosList);
            data.setGatePosList(gatePosList);
            data.setMovingBlockMap(movingBlockMap);
            data.setPoweredBlockMap(poweredBlockMap);
            data.setSpawnerPosEntityIdMap(spawnerPosEntityIdMap);
            // Single save instead of 7 separate saves
            net.dungeonz.dungeon.DungeonDataManager.saveData((ServerWorld) this.world, this.pos, data);
        }
    }

    public void setBlockMap(HashMap<Integer, ArrayList<BlockPos>> map) {
        if (!this.world.isClient && this.world instanceof ServerWorld) {
            net.dungeonz.dungeon.DungeonRuntimeData data = net.dungeonz.dungeon.DungeonDataManager.getData((ServerWorld) this.world, this.pos);
            data.setBlockBlockPosMap(map);
            net.dungeonz.dungeon.DungeonDataManager.saveData((ServerWorld) this.world, this.pos, data);
        }
    }

    public HashMap<Integer, ArrayList<BlockPos>> getBlockMap() {
        if (!this.world.isClient && this.world instanceof ServerWorld) {
            return net.dungeonz.dungeon.DungeonDataManager.getData((ServerWorld) this.world, this.pos).getBlockBlockPosMap();
        }
        return new HashMap<>(); // Return empty for client
    }

    public void setCooldownTime(int cooldownTime) {
        this.cooldownTime = cooldownTime;
        this.markDirty();
        this.syncGuiToAllViewers();
    }

    public int getCooldownTime() {
        return this.cooldownTime;
    }

    public boolean isOnCooldown(int currentTime) {
        if (this.cooldownTime <= currentTime) {
            return false;
        }
        return true;
    }

    public void setMaxGroupSize(int maxGroupSize) {
        this.maxGroupSize = maxGroupSize;
        this.markDirty();
        this.syncGuiToAllViewers();
    }

    public void setMinGroupSize(int minGroupSize) {
        this.minGroupSize = minGroupSize;
        this.markDirty();
        this.syncGuiToAllViewers();
    }

    public List<UUID> getWaitingUuids() {
        return this.waitingUuids;
    }

    public void setWaitingUuids(List<UUID> waitingUuids) {
        this.waitingUuids = waitingUuids;
        this.markDirty();
        this.syncGuiToAllViewers();
    }

    public void addWaitingUuid(UUID uuid) {
        if (!this.waitingUuids.contains(uuid)) {
            this.waitingUuids.add(uuid);
            this.markDirty();
            this.syncGuiToAllViewers();
        }
    }

    // Add this new method for removing waiting players
    public void removeWaitingUuid(UUID uuid) {
        boolean removed = this.waitingUuids.remove(uuid);
        if (removed) {
            this.markDirty();
            this.syncGuiToAllViewers();
        }
    }

    // Add this method to clear all waiting players (useful when dungeon starts)
    public void clearWaitingUuids() {
        if (!this.waitingUuids.isEmpty()) {
            this.waitingUuids.clear();
            this.markDirty();
            this.syncGuiToAllViewers();
        }
    }

    public int getMaxGroupSize() {
        return this.maxGroupSize;
    }

    public int getMinGroupSize() {
        return this.minGroupSize;
    }

    public void setPrivateGroup(boolean privateGroup) {
        this.privateGroup = privateGroup;
        this.markDirty();
        this.syncGuiToAllViewers();
    }

    public boolean getPrivateGroup() {
        return this.privateGroup;
    }

    public void setBossBlockPos(BlockPos pos) {
        this.bossBlockPos = pos;
    }

    public BlockPos getBossBlockPos() {
        return this.bossBlockPos;
    }

    public void setBossLootBlockPos(BlockPos pos) {
        this.bossLootBlockPos = pos;
    }

    public BlockPos getBossLootBlockPos() {
        return this.bossLootBlockPos;
    }

    public void setChestPosList(List<BlockPos> chestPosList) {
        if (!this.world.isClient && this.world instanceof ServerWorld) {
            net.dungeonz.dungeon.DungeonRuntimeData data = net.dungeonz.dungeon.DungeonDataManager.getData((ServerWorld) this.world, this.pos);
            data.setChestPosList(chestPosList);
            net.dungeonz.dungeon.DungeonDataManager.saveData((ServerWorld) this.world, this.pos, data);
        }
    }

    public List<BlockPos> getChestPosList() {
        if (!this.world.isClient && this.world instanceof ServerWorld) {
            return net.dungeonz.dungeon.DungeonDataManager.getData((ServerWorld) this.world, this.pos).getChestPosList();
        }
        return new ArrayList<>();
    }

    public void setGatePosList(List<BlockPos> gatePosList) {
        if (!this.world.isClient && this.world instanceof ServerWorld) {
            net.dungeonz.dungeon.DungeonRuntimeData data = net.dungeonz.dungeon.DungeonDataManager.getData((ServerWorld) this.world, this.pos);
            data.setGatePosList(gatePosList);
            net.dungeonz.dungeon.DungeonDataManager.saveData((ServerWorld) this.world, this.pos, data);
        }
    }

    public List<BlockPos> getGatePosList() {
        if (!this.world.isClient && this.world instanceof ServerWorld) {
            return net.dungeonz.dungeon.DungeonDataManager.getData((ServerWorld) this.world, this.pos).getGatePosList();
        }
        return new ArrayList<>();
    }

    public void setMovingBlockMap(Map<BlockPos, Integer> movingBlockMap) {
        if (!this.world.isClient && this.world instanceof ServerWorld) {
            net.dungeonz.dungeon.DungeonRuntimeData data = net.dungeonz.dungeon.DungeonDataManager.getData((ServerWorld) this.world, this.pos);
            data.setMovingBlockMap(movingBlockMap);
            net.dungeonz.dungeon.DungeonDataManager.saveData((ServerWorld) this.world, this.pos, data);
        }
    }

    public Map<BlockPos, Integer> getMovingBlockMap() {
        if (!this.world.isClient && this.world instanceof ServerWorld) {
            return net.dungeonz.dungeon.DungeonDataManager.getData((ServerWorld) this.world, this.pos).getMovingBlockMap();
        }
        return new HashMap<>();
    }

    public void setPoweredBlockMap(Map<BlockPos, Powered> poweredBlockMap) {
        if (!this.world.isClient && this.world instanceof ServerWorld) {
            net.dungeonz.dungeon.DungeonRuntimeData data = net.dungeonz.dungeon.DungeonDataManager.getData((ServerWorld) this.world, this.pos);
            data.setPoweredBlockMap(poweredBlockMap);
            net.dungeonz.dungeon.DungeonDataManager.saveData((ServerWorld) this.world, this.pos, data);
        }
    }

    public Map<BlockPos, Powered> getPoweredBlockMap() {
        if (!this.world.isClient && this.world instanceof ServerWorld) {
            return net.dungeonz.dungeon.DungeonDataManager.getData((ServerWorld) this.world, this.pos).getPoweredBlockMap();
        }
        return new HashMap<>();
    }

    public void setExitPosList(List<BlockPos> exitPosList) {
        if (!this.world.isClient && this.world instanceof ServerWorld) {
            net.dungeonz.dungeon.DungeonRuntimeData data = net.dungeonz.dungeon.DungeonDataManager.getData((ServerWorld) this.world, this.pos);
            data.setExitPosList(exitPosList);
            net.dungeonz.dungeon.DungeonDataManager.saveData((ServerWorld) this.world, this.pos, data);
        }
    }

    public List<BlockPos> getExitPosList() {
        if (!this.world.isClient && this.world instanceof ServerWorld) {
            return net.dungeonz.dungeon.DungeonDataManager.getData((ServerWorld) this.world, this.pos).getExitPosList();
        }
        return new ArrayList<>();
    }

    public void addDungeonEdge(int edgeX, int edgeY, int edgeZ) {
        if (!this.world.isClient && this.world instanceof ServerWorld) {
            net.dungeonz.dungeon.DungeonRuntimeData data = net.dungeonz.dungeon.DungeonDataManager.getData((ServerWorld) this.world, this.pos);
            data.addDungeonEdge(edgeX, edgeY, edgeZ);
            net.dungeonz.dungeon.DungeonDataManager.saveData((ServerWorld) this.world, this.pos, data);
        }
    }

    public List<Integer> getDungeonEdgeList() {
        if (!this.world.isClient && this.world instanceof ServerWorld) {
            return net.dungeonz.dungeon.DungeonDataManager.getData((ServerWorld) this.world, this.pos).getDungeonEdgeList();
        }
        return new ArrayList<>();
    }

    public void setSpawnerPosEntityIdMap(HashMap<BlockPos, String> spawnerPosEntityIdMap) {
        if (!this.world.isClient && this.world instanceof ServerWorld) {
            net.dungeonz.dungeon.DungeonRuntimeData data = net.dungeonz.dungeon.DungeonDataManager.getData((ServerWorld) this.world, this.pos);
            data.setSpawnerPosEntityIdMap(spawnerPosEntityIdMap);
            net.dungeonz.dungeon.DungeonDataManager.saveData((ServerWorld) this.world, this.pos, data);
        }
    }

    public HashMap<BlockPos, String> getSpawnerPosEntityIdMap() {
        if (!this.world.isClient && this.world instanceof ServerWorld) {
            return net.dungeonz.dungeon.DungeonDataManager.getData((ServerWorld) this.world, this.pos).getSpawnerPosEntityIdMap();
        }
        return new HashMap<>();
    }

    public void setReplaceBlockIdMap(HashMap<BlockPos, Integer> replacePosBlockIdMap) {
        if (!this.world.isClient && this.world instanceof ServerWorld) {
            net.dungeonz.dungeon.DungeonRuntimeData data = net.dungeonz.dungeon.DungeonDataManager.getData((ServerWorld) this.world, this.pos);
            data.setReplacePosBlockIdMap(replacePosBlockIdMap);
            net.dungeonz.dungeon.DungeonDataManager.saveData((ServerWorld) this.world, this.pos, data);
        }
    }

    public void addReplaceBlockId(BlockPos pos, Block block) {
        if (!this.world.isClient && this.world instanceof ServerWorld) {
            net.dungeonz.dungeon.DungeonRuntimeData data = net.dungeonz.dungeon.DungeonDataManager.getData((ServerWorld) this.world, this.pos);
            data.addReplaceBlockId(pos, Registries.BLOCK.getRawId(block));
            net.dungeonz.dungeon.DungeonDataManager.saveData((ServerWorld) this.world, this.pos, data);
        }
    }

    public HashMap<BlockPos, Integer> getReplaceBlockIdMap() {
        if (!this.world.isClient && this.world instanceof ServerWorld) {
            return net.dungeonz.dungeon.DungeonDataManager.getData((ServerWorld) this.world, this.pos).getReplacePosBlockIdMap();
        }
        return new HashMap<>();
    }

    public void startDungeonTeleportCountdown(ServerWorld dungeonWorld) {
        this.dungeonTeleportCountdown = ConfigInit.CONFIG.defaultDungeonTeleportCountdown;

        boolean isDungeonStructureGenerated = this.isDungeonStructureGenerated();
        BlockPos origin = new BlockPos(0, 0, 0).add(this.getPos().getX() * 16, 100, this.getPos().getZ() * 16);

        if (!isDungeonStructureGenerated) {
            this.setDungeonStructureGenerated();
            DungeonPlacementHandler.clearArea(dungeonWorld, origin);
            DungeonPlacementHandler.generateDungeonStructure(dungeonWorld, origin, this);
        } else {
            if (ConfigInit.CONFIG.forcedRegeneration) {
                for (int i = 0; i < this.getDungeonPlayerUuids().size(); i++) {
                    ServerPlayerEntity player = (ServerPlayerEntity) dungeonWorld.getPlayerByUuid(this.getDungeonPlayerUuids().get(i));
                    if (player != null && DungeonHelper.getCurrentDungeon(player) != null) {
                        DungeonHelper.teleportOutOfDungeon(player);
                        player.sendMessage(Text.translatable("text.dungeonz.dungeon_safekick"));
                    }
                }
                DungeonPlacementHandler.clearDungeonAreaWithEntities(dungeonWorld, this);
                DungeonPlacementHandler.generateDungeonStructure(dungeonWorld, origin, this);
            } else {
                DungeonPlacementHandler.prepareDungeon(dungeonWorld, this);
            }
        }

        this.markDirty();
        this.syncGuiToAllViewers();
    }

    public int getdungeonTeleportCountdown() {
        return this.dungeonTeleportCountdown;
    }

    public static class Powered {
        private final int blockId;
        private final boolean powered;
        private final int facing;
        private final int blockFacing;

        public Powered(int blockId, boolean powered, int facing, int blockFacing) {
            this.blockId = blockId;
            this.powered = powered;
            this.facing = facing;
            this.blockFacing = blockFacing;
        }

        public int getBlockId() {
            return blockId;
        }

        public boolean getPowered() {
            return powered;
        }

        // Horizontal facing
        public int getFacing() {
            return facing;
        }

        // Block facing for example: cealing
        // 0 = none, 1 = ("floor"), 2 = WALL("wall"), 3 = CEILING("ceiling");
        public int getBlockFacing() {
            return blockFacing;
        }
    }

    public void startDungeonTimer() {
        if (this.getDungeon() != null && this.getDungeon().hasTimeLimit()) {
            this.dungeonStartTime = (int) this.world.getTime();
            this.dungeonTimerActive = true;
            this.markDirty();
            this.syncGuiToAllViewers();
        }
    }

    public void stopDungeonTimer() {
        this.dungeonTimerActive = false;
        this.markDirty();
        this.syncGuiToAllViewers();
    }

    public boolean isDungeonTimerActive() {
        return this.dungeonTimerActive;
    }

    public int getDungeonTimeRemaining() {
        if (!this.dungeonTimerActive || this.getDungeon() == null || !this.getDungeon().hasTimeLimit()) {
            return 0;
        }
        
        int currentTime = (int) this.world.getTime();
        int elapsed = (currentTime - this.dungeonStartTime) / 20; // Convert to seconds
        int remaining = this.getDungeon().getTimeLimit() - elapsed;
        return Math.max(0, remaining);
    }

    public boolean isDungeonTimerExpired() {
        return this.dungeonTimerActive && this.getDungeonTimeRemaining() <= 0;
    }

    private void handleDungeonTimerExpired() {
        if (!this.world.isClient() && this.isDungeonTimerActive()) {
            // Create a copy of the player list to avoid ConcurrentModificationException
            List<UUID> playersToTeleport = new ArrayList<>(this.getDungeonPlayerUuids());
            
            // Teleport all players out and send message
            for (UUID playerUuid : playersToTeleport) {
                ServerPlayerEntity player = null;
                
                // Try to find player in dungeon world
                ServerWorld dungeonWorld = this.world.getServer().getWorld(DimensionInit.DUNGEON_WORLD);
                if (dungeonWorld != null) {
                    player = (ServerPlayerEntity) dungeonWorld.getPlayerByUuid(playerUuid);
                }
                
                // If not found, try server player manager
                if (player == null) {
                    player = this.world.getServer().getPlayerManager().getPlayer(playerUuid);
                }
                
                if (player != null) {
                    DungeonHelper.teleportOutOfDungeon(player);
                    player.sendMessage(Text.translatable("text.dungeonz.dungeon_time_expired"), false);
                }
            }
            
            // Clear player lists and start cooldown
            this.setDungeonPlayerUuids(new ArrayList<>());
            this.setDeadDungeonPlayerUuids(new ArrayList<>());
            this.stopDungeonTimer();
            this.setCooldownTime(this.getDungeon().getCooldown() + (int) this.getWorld().getTime());
            this.markDirty();
        }
    }

}