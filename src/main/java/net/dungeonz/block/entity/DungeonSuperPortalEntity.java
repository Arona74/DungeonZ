package net.dungeonz.block.entity;

import net.dungeonz.DungeonzMain;
import net.dungeonz.block.DungeonSuperPortalBlock;
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
import net.minecraft.network.PacketByteBuf;
import net.dungeonz.particle.DungeonPortalParticleEffect;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.util.*;

public class DungeonSuperPortalEntity extends DungeonPortalEntity {

    public DungeonSuperPortalEntity(BlockPos pos, BlockState state) {
        super(BlockInit.DUNGEON_SUPER_PORTAL_ENTITY, pos, state);
    }

    public static void clientTick(World world, BlockPos pos, BlockState state, DungeonSuperPortalEntity blockEntity) {
        DungeonSuperPortalEntity source = blockEntity;
        if (DungeonSuperPortalBlock.isOtherSuperPortalBlockNearby(world, pos)) {
            DungeonSuperPortalEntity main = DungeonSuperPortalBlock.getMainSuperPortalEntity(world, pos);
            if (main != null) {
                source = main;
            }
        }

        if (source.getDungeonType().isEmpty()) {
            return;
        }

        if (source == blockEntity && world.getRandom().nextInt(40) == 0) {
            world.playSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.BLOCK_PORTAL_AMBIENT, SoundCategory.BLOCKS,
                    0.5f, world.getRandom().nextFloat() * 0.2f + 0.3f, false);
        }

        if (world.getRandom().nextInt(4) != 0) {
            return;
        }

        Vector3f color;
        if (source.getDungeonPlayerCount() > 0) {
            color = new Vector3f(0.2f, 0.4f, 1.0f);
        } else if (source.isOnCooldown((int) world.getTime())) {
            color = new Vector3f(1.0f, 0.2f, 0.2f);
        } else {
            color = new Vector3f(0.2f, 1.0f, 0.3f);
        }

        boolean axisX = state.contains(DungeonSuperPortalBlock.AXIS)
                && state.get(DungeonSuperPortalBlock.AXIS) == Direction.Axis.X;
        double x = pos.getX() + world.getRandom().nextDouble();
        double y = pos.getY() + world.getRandom().nextDouble();
        double z = pos.getZ() + world.getRandom().nextDouble();
        double vx = (world.getRandom().nextFloat() - 0.5) * 0.5;
        double vy = (world.getRandom().nextFloat() - 0.5) * 0.5;
        double vz = (world.getRandom().nextFloat() - 0.5) * 0.5;
        int d = world.getRandom().nextInt(2) * 2 - 1;
        if (axisX) {
            z = pos.getZ() + 0.5 + 0.25 * d;
            vz = world.getRandom().nextFloat() * 2.0f * d;
        } else {
            x = pos.getX() + 0.5 + 0.25 * d;
            vx = world.getRandom().nextFloat() * 2.0f * d;
        }
        world.addParticle(new DungeonPortalParticleEffect(color), x, y, z, vx, vy, vz);
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
        boolean allowElytra = false;
        boolean allowMobsLoot = true;
        boolean allowBossLoot = true;

        Dungeon dungeon = this.getDungeon();
        if (dungeon != null) {
            difficulties = dungeon.getDifficultyList();
            possibleLoot = dungeon.isHidePossibleLoot() ? new java.util.HashMap<>() : DungeonHelper.getPossibleLootItemStackMap(dungeon, player.getServer());
            requiredItemStacks = DungeonHelper.getRequiredItemStackList(dungeon);
            backgroundId = Optional.ofNullable(dungeon.getBackgroundId());
            requiredLevel = dungeon.getRequiredLevel();
            allowEnderPearl = dungeon.isEnderPearlAllowed();
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

        return new DungeonSuperPortalPacket(
                this.getDungeonType(), this.getPos(),
                this.getDungeonPlayerUuids(), this.getDeadDungeonPlayerUuids(),
                difficulties, possibleLoot, requiredItemStacks,
                this.getMaxGroupSize(), this.getMinGroupSize(),
                this.getWaitingUuids().size(), requiredLevel,
                this.getCooldownTime(), this.getDifficulty(),
                allowEnderPearl, allowPositiveEffects, allowElytra,
                allowRespawn, allowMobsLoot, allowBossLoot,
                keepInventory, this.getPrivateGroup(),
                backgroundId, dungeonIdList);
    }

    public static ExtendedScreenHandlerFactory createScreenFactory(
            DungeonSuperPortalEntity entity, World world, BlockPos pos) {
        return new ExtendedScreenHandlerFactory() {
            @Override
            public Text getDisplayName() {
                return entity.getDisplayName();
            }

            @Override
            public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity p) {
                return new DungeonSuperPortalScreenHandler(
                        syncId, inv,
                        DungeonSuperPortalPacket.toBuf(entity.getSuperPortalScreenData((ServerPlayerEntity) p)));
            }

            @Override
            public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
                DungeonSuperPortalPacket.encode(entity.getSuperPortalScreenData(player), buf);
            }
        };
    }
}
