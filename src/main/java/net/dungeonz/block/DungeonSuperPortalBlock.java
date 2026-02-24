package net.dungeonz.block;

import net.dungeonz.DungeonzMain;
import net.dungeonz.block.entity.DungeonPortalEntity;
import net.dungeonz.block.entity.DungeonSuperPortalEntity;
import net.dungeonz.init.BlockInit;
import net.dungeonz.network.DungeonServerPacket;
import net.dungeonz.util.DungeonHelper;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.partyaddon.access.GroupManagerAccess;
import net.partyaddon.network.PartyAddonServerPacket;
import org.jetbrains.annotations.Nullable;

public class DungeonSuperPortalBlock extends DungeonPortalBlock {

    public DungeonSuperPortalBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new DungeonSuperPortalEntity(pos, state);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        if (player.isSpectator()) {
            return ActionResult.PASS;
        }

        BlockPos mainPos = pos;
        if (isOtherSuperPortalBlockNearby(world, pos)) {
            BlockPos mp = getMainSuperPortalBlockPos(world, pos);
            if (mp != null) mainPos = mp;
        }

        BlockEntity be = world.getBlockEntity(mainPos);
        if (!(be instanceof DungeonSuperPortalEntity superEntity)) {
            return ActionResult.PASS;
        }

        if (player.isCreativeLevelTwoOp()
                && (superEntity.getDungeon() == null || player.isSneaking())) {
            DungeonServerPacket.writeS2COpenOpScreenPacket(
                    (ServerPlayerEntity) player, superEntity, null);
            return ActionResult.success(true);
        }

        if (superEntity.getDungeonType().isEmpty()) {
            DungeonServerPacket.writeS2CSuperPortalSelectionPacket(
                    (ServerPlayerEntity) player, superEntity);
            return ActionResult.success(true);
        }

        if (DungeonzMain.isPartyAddonLoaded) {
            PartyAddonServerPacket.writeS2CSyncGroupManagerPacket(
                    (ServerPlayerEntity) player,
                    ((GroupManagerAccess) player).getGroupManager());
        }

        ExtendedScreenHandlerFactory factory =
                DungeonSuperPortalEntity.createScreenFactory(superEntity, world, mainPos);
        ((ServerPlayerEntity) player).openHandledScreen(factory);

        return ActionResult.success(true);
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isClient() && !entity.hasVehicle() && !entity.hasPassengers()
                && entity.canUsePortals() && entity instanceof ServerPlayerEntity) {
            if (!entity.hasPortalCooldown()) {
                if (isOtherSuperPortalBlockNearby(world, pos)) {
                    pos = getMainSuperPortalBlockPos(world, pos);
                }
                DungeonHelper.teleportDungeon((ServerPlayerEntity) entity, pos, entity.getUuid());
                entity.resetPortalCooldown();
            }
        }
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state,
                                                                   BlockEntityType<T> type) {
        return DungeonGateBlock.checkType(type, BlockInit.DUNGEON_SUPER_PORTAL_ENTITY,
                world.isClient() ? DungeonSuperPortalEntity::clientTick
                        : DungeonPortalEntity::serverTick);
    }

    public static boolean isOtherSuperPortalBlockNearby(World world, BlockPos pos) {
        return DungeonPortalBlock.isOtherPortalBlockNearby(world, pos,
                BlockInit.DUNGEON_SUPER_PORTAL);
    }

    @Nullable
    public static BlockPos getMainSuperPortalBlockPos(World world, BlockPos pos) {
        return DungeonPortalBlock.getMainPortalBlockPos(world, pos,
                BlockInit.DUNGEON_SUPER_PORTAL);
    }

    @Nullable
    public static DungeonSuperPortalEntity getMainSuperPortalEntity(World world, BlockPos pos) {
        BlockPos mainPos = getMainSuperPortalBlockPos(world, pos);
        return mainPos != null
                ? (DungeonSuperPortalEntity) world.getBlockEntity(mainPos)
                : null;
    }
}
