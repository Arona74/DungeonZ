package net.dungeonz.block.entity;

import net.dungeonz.DungeonzMain;
import net.dungeonz.block.screen.DungeonSuperPortalScreenHandler;
import net.dungeonz.dungeon.Dungeon;
import net.dungeonz.init.BlockInit;
import net.dungeonz.network.packet.DungeonSuperPortalPacket;
import net.dungeonz.util.DungeonHelper;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

public class DungeonSuperPortalEntity extends DungeonPortalEntity {

    public DungeonSuperPortalEntity(BlockPos pos, BlockState state) {
        super(BlockInit.DUNGEON_SUPER_PORTAL_ENTITY, pos, state);
    }

    public DungeonSuperPortalPacket getSuperPortalScreenData(ServerPlayerEntity player) {
        List<String> difficulties = new ArrayList<>();
        Map<String, List<ItemStack>> possibleLoot = new HashMap<>();
        Map<String, List<ItemStack>> requiredItemStacks = new HashMap<>();
        Optional<Identifier> backgroundId = Optional.empty();
        int requiredLevel = 0;
        boolean allowRespawn = false;
        boolean keepInventory = false;
        boolean allowPositiveEffects = false;
        boolean allowEnderPearl = false;
        boolean allowWindCharge = false;
        boolean allowElytra = false;
        boolean allowMobsLoot = true;
        boolean allowBossLoot = true;

        if (this.getDungeon() instanceof Dungeon dungeon) {
            difficulties = dungeon.getDifficultyList();
            possibleLoot = dungeon.isHidePossibleLoot() ? new java.util.HashMap<>() : DungeonHelper.getPossibleLootItemStackMap(dungeon, player.getServer());
            requiredItemStacks = DungeonHelper.getRequiredItemStackList(dungeon);
            backgroundId = Optional.ofNullable(dungeon.getBackgroundId());
            requiredLevel = dungeon.getRequiredLevel();
            allowEnderPearl = dungeon.isEnderPearlAllowed();
            allowWindCharge = dungeon.isWindChargeAllowed();
            allowPositiveEffects = dungeon.isPositiveEffectsAllowed();
            allowRespawn = dungeon.isRespawnAllowed();
            keepInventory = dungeon.isKeepInventory();
            allowElytra = dungeon.isElytraAllowed();
            allowMobsLoot = dungeon.isMobsLootAllowed();
            allowBossLoot = dungeon.isBossLootAllowed();
        }

        List<String> dungeonIdList = new ArrayList<>();
        for (Dungeon d : DungeonzMain.DUNGEONS) {
            dungeonIdList.add(d.getDungeonTypeId());
        }

        return new DungeonSuperPortalPacket(this.getDungeonType(), this.getPos(), this.getDungeonPlayerUuids(),
                this.getDeadDungeonPlayerUUIDs(), difficulties, possibleLoot, requiredItemStacks,
                this.getMaxGroupSize(), this.getMinGroupSize(), this.getWaitingUuids().size(), requiredLevel,
                this.getCooldownTime(), this.getDifficulty(), allowEnderPearl, allowWindCharge, allowPositiveEffects,
                allowElytra, allowRespawn, keepInventory, allowMobsLoot, allowBossLoot, this.getPrivateGroup(),
                backgroundId, dungeonIdList, this.isDungeonTimerActive(), this.getDungeonTimeRemaining());
    }

    public static ExtendedScreenHandlerFactory<DungeonSuperPortalPacket> createScreenFactory(
            DungeonSuperPortalEntity entity, World world, BlockPos pos) {
        return new ExtendedScreenHandlerFactory<DungeonSuperPortalPacket>() {
            @Override
            public Text getDisplayName() {
                return entity.getDisplayName();
            }

            @Override
            public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity p) {
                return new DungeonSuperPortalScreenHandler(syncId, inv, entity,
                        ScreenHandlerContext.create(world, pos));
            }

            @Override
            public DungeonSuperPortalPacket getScreenOpeningData(ServerPlayerEntity p) {
                return entity.getSuperPortalScreenData(p);
            }
        };
    }
}
