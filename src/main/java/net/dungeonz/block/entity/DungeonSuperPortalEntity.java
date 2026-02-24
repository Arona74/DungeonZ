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
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
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

        if (world.getRandom().nextInt(2) != 0) {
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

        double bx = pos.getX(), by = pos.getY(), bz = pos.getZ();
        double x, y, z, vx = 0, vy = 0, vz = 0;
        switch (world.getRandom().nextInt(6)) {
            case 0 -> { x = bx + world.getRandom().nextDouble(); y = by + 1.01; z = bz + world.getRandom().nextDouble(); vy =  0.04; }
            case 1 -> { x = bx + world.getRandom().nextDouble(); y = by - 0.01; z = bz + world.getRandom().nextDouble(); vy = -0.04; }
            case 2 -> { x = bx + 1.01; y = by + world.getRandom().nextDouble(); z = bz + world.getRandom().nextDouble(); vx =  0.04; }
            case 3 -> { x = bx - 0.01; y = by + world.getRandom().nextDouble(); z = bz + world.getRandom().nextDouble(); vx = -0.04; }
            case 4 -> { x = bx + world.getRandom().nextDouble(); y = by + world.getRandom().nextDouble(); z = bz + 1.01; vz =  0.04; }
            default -> { x = bx + world.getRandom().nextDouble(); y = by + world.getRandom().nextDouble(); z = bz - 0.01; vz = -0.04; }
        }
        world.addParticle(new DustParticleEffect(color, 1.2f), x, y, z, vx, vy, vz);
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
            possibleLoot = DungeonHelper.getPossibleLootItemStackMap(dungeon, player.getServer());
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
