package net.dungeonz.block.screen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.dungeonz.block.entity.DungeonPortalEntity;
import net.dungeonz.block.entity.DungeonSuperPortalEntity;
import net.dungeonz.init.BlockInit;
import net.dungeonz.network.packet.DungeonSuperPortalPacket;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class DungeonSuperPortalScreenHandler extends ScreenHandler {

    private final World world;
    private final ScreenHandlerContext context;
    private final DungeonPortalEntity dungeonPortalEntity;
    private BlockPos pos;

    private List<String> difficulties = new ArrayList<>();
    private Map<String, List<ItemStack>> possibleLootDifficultyItemStackMap = new HashMap<>();
    private Map<String, List<ItemStack>> requiredItemStacks = new HashMap<>();
    private int waitingGroupSize = 0;

    private int requiredLevel = 0;
    private boolean allowRespawn = false;
    private boolean keepInventory = false;
    private boolean allowPositiveEffects = false;
    private boolean allowEnderPearl = false;
    private boolean allowElytra = false;
    private boolean allowMobsLoot = true;
    private boolean allowBossLoot = true;

    private List<String> dungeonIdList = new ArrayList<>();

    @Nullable
    private Identifier backgroundId = null;

    private boolean dungeonTimerActive = false;
    private int dungeonTimeRemaining = 0;

    public DungeonSuperPortalScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        super(BlockInit.SUPER_PORTAL, syncId);

        DungeonSuperPortalPacket packet = DungeonSuperPortalPacket.decode(buf);

        this.world = playerInventory.player.getWorld();
        this.context = ScreenHandlerContext.EMPTY;
        this.pos = packet.blockPos;

        this.dungeonPortalEntity = new DungeonSuperPortalEntity(
                packet.blockPos, this.world.getBlockState(packet.blockPos));

        this.getDungeonPortalEntity().setDungeonType(packet.dungeonType);
        this.getDungeonPortalEntity().setDungeonPlayerUuids(packet.playerUuids);
        this.getDungeonPortalEntity().setDeadDungeonPlayerUuids(packet.deadPlayerUuids);
        this.getDungeonPortalEntity().setMaxGroupSize(packet.maxGroupSize);
        this.getDungeonPortalEntity().setMinGroupSize(packet.minGroupSize);
        this.getDungeonPortalEntity().setCooldownTime(packet.cooldownTime);
        this.getDungeonPortalEntity().setDifficulty(packet.difficulty);
        this.getDungeonPortalEntity().setPrivateGroup(packet.privateGroup);

        this.setDifficulties(packet.difficulties);
        this.setPossibleLootItemStacks(packet.possibleLoot);
        this.setRequiredItemStacks(packet.requiredItemStacks);
        this.setWaitingGroupSize(packet.waitingPlayerCount);

        this.requiredLevel = packet.requiredLevel;
        this.allowEnderPearl = packet.allowEnderPearl;
        this.allowPositiveEffects = packet.allowPositiveEffects;
        this.allowElytra = packet.allowElytra;
        this.allowRespawn = packet.allowRespawn;
        this.keepInventory = packet.keepInventory;
        this.allowMobsLoot = packet.allowMobsLoot;
        this.allowBossLoot = packet.allowBossLoot;
        this.backgroundId = packet.backgroundId.orElse(null);
        this.dungeonIdList = packet.dungeonIdList;
    }

    @Override
    public ItemStack quickMove(PlayerEntity var1, int var2) {
        return null;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.context.get((world, pos) -> {
            if (!world.getBlockState(pos).isOf(BlockInit.DUNGEON_SUPER_PORTAL)) {
                return false;
            }
            return player.squaredDistanceTo(
                    (double) pos.getX() + 0.5,
                    (double) pos.getY() + 0.5,
                    (double) pos.getZ() + 0.5) <= 64.0;
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

    public void setPossibleLootItemStacks(Map<String, List<ItemStack>> map) {
        this.possibleLootDifficultyItemStackMap = map;
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

    public List<String> getDungeonIdList() {
        return this.dungeonIdList;
    }

    public boolean isDungeonTimerActive() {
        return this.dungeonTimerActive;
    }

    public void setDungeonTimerActive(boolean dungeonTimerActive) {
        this.dungeonTimerActive = dungeonTimerActive;
    }

    public int getDungeonTimeRemaining() {
        return this.dungeonTimeRemaining;
    }

    public void setDungeonTimeRemaining(int dungeonTimeRemaining) {
        this.dungeonTimeRemaining = dungeonTimeRemaining;
    }
}
