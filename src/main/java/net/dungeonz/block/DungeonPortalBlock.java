package net.dungeonz.block;

import net.minecraft.block.FluidFillable;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.dungeonz.DungeonzMain;
import net.dungeonz.block.entity.DungeonPortalEntity;
import net.dungeonz.block.screen.DungeonPortalScreenHandler;
import net.dungeonz.init.BlockInit;
import net.dungeonz.network.DungeonServerPacket;
import net.dungeonz.network.packet.DungeonPortalPacket;
import net.dungeonz.util.DungeonHelper;
import net.dungeonz.dungeon.DungeonDataManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.partyaddon.access.GroupManagerAccess;
import net.partyaddon.network.PartyAddonServerPacket;

@SuppressWarnings("deprecation")
public class DungeonPortalBlock extends BlockWithEntity implements FluidFillable {

    public DungeonPortalBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new DungeonPortalEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        // Prevent spectators from interacting with portals
        if (player.isSpectator()) {
            return ActionResult.PASS;
        }
        
        // Add additional safety check
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        // Double-check spectator mode on server side before proceeding
        if (player.isSpectator()) {
            return ActionResult.PASS;
        }
        
        // Ensure the block entity exists and is valid before proceeding
        if (!(player.getWorld().getBlockEntity(pos) instanceof DungeonPortalEntity dungeonPortalEntity)) {
            return ActionResult.PASS;
        }

        // Make player leave when using it in dungeonz dimension
        if (!world.isClient() && world.getRegistryKey().getValue().toString().equals("dungeonz:dungeon")) {
			if (player instanceof ServerPlayerEntity serverPlayer) {
				serverPlayer.getServer().getCommandManager().executeWithPrefix(serverPlayer.getCommandSource(), "/dungeon leave");
			}
			return ActionResult.success(true);
		}

		// Get the main portal entity if this is part of a multi-block structure
        BlockPos finalPos = pos;
        if (isOtherDungeonPortalBlockNearby(world, pos)) {
            dungeonPortalEntity = getMainDungeonPortalEntity(world, pos);
            finalPos = getMainDungeonPortalBlockPos(world, pos);
        }
        
        // Create final references for use in anonymous class
        final DungeonPortalEntity finalEntity = dungeonPortalEntity;
        final BlockPos finalBlockPos = finalPos;
        
        if (player.isCreativeLevelTwoOp() && (finalEntity.getDungeon() == null || player.isSneaking())) {
            DungeonServerPacket.writeS2COpenOpScreenPacket((ServerPlayerEntity) player, finalEntity, null);
            return ActionResult.success(world.isClient());
        } else if (finalEntity.getDungeon() != null) {
            // CRITICAL: Final spectator check before opening screen
            if (player.isSpectator()) {
                return ActionResult.PASS;
            }
            if (DungeonzMain.isPartyAddonLoaded) {
                PartyAddonServerPacket.writeS2CSyncGroupManagerPacket((ServerPlayerEntity) player, ((GroupManagerAccess) player).getGroupManager());
            }
            
            // Create and send the packet with all necessary data
            DungeonPortalPacket packet = new DungeonPortalPacket(
                finalEntity.getPos(),
                finalEntity.getDungeonPlayerUuids(),
                finalEntity.getDeadDungeonPlayerUuids(),
                finalEntity.getWaitingUuids(),
                finalEntity.getDungeon().getDifficultyList(),
                DungeonHelper.getPossibleLootItemStackMap(finalEntity.getDungeon(), ((ServerWorld)world).getServer()),
                DungeonHelper.getRequiredItemStackList(finalEntity.getDungeon()),
                finalEntity.getMaxGroupSize(),
                finalEntity.getMinGroupSize(),
                finalEntity.getWaitingUuids().size(),
                finalEntity.getDungeon().getRequiredLevel(),
                finalEntity.getCooldownTime(),
                finalEntity.getDifficulty(),
                finalEntity.getDungeon().isEnderPearlAllowed(),
                finalEntity.getDungeon().isPositiveEffectsAllowed(),
                finalEntity.getDungeon().isElytraAllowed(),
                finalEntity.getDungeon().isRespawnAllowed(),
                finalEntity.getDungeon().isMobsLootAllowed(),
                finalEntity.getDungeon().isBossLootAllowed(),
                finalEntity.getDungeon().isKeepInventory(),
                finalEntity.getPrivateGroup(),
                Optional.ofNullable(finalEntity.getDungeon().getBackgroundId()),
                finalEntity.getDungeon().getDifficultyFameRewardMap()
            );

            // Send the packet to open the screen
            ((ServerPlayerEntity) player).openHandledScreen(new ExtendedScreenHandlerFactory() {
                @Override
                public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
                    return new DungeonPortalScreenHandler(syncId, playerInventory, DungeonPortalPacket.toBuf(packet));
                }
                
                @Override
                public Text getDisplayName() {
                    return finalEntity.getDisplayName();
                }
                
                @Override
                public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
                    DungeonPortalPacket.encode(packet, buf);
                }
            });

            return ActionResult.success(world.isClient());
        }

        return super.onUse(state, world, pos, player, hand, hit);
    }

    @Override
    public boolean canBucketPlace(BlockState state, Fluid fluid) {
        return false;
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isClient() && !entity.hasVehicle() && !entity.hasPassengers() && entity.canUsePortals() && entity instanceof ServerPlayerEntity) {
            if (!entity.hasPortalCooldown()) {
                if (isOtherDungeonPortalBlockNearby(world, pos)) {
                    pos = getMainDungeonPortalBlockPos(world, pos);
                }
                DungeonHelper.teleportDungeon((ServerPlayerEntity) entity, pos, entity.getUuid());
                entity.resetPortalCooldown();
            }
        }
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return DungeonGateBlock.checkType(type, BlockInit.DUNGEON_PORTAL_ENTITY, world.isClient() ? DungeonPortalEntity::clientTick : DungeonPortalEntity::serverTick);
    }

    public static boolean isOtherDungeonPortalBlockNearby(World world, BlockPos pos) {
        for (BlockPos checkPos : BlockPos.iterateOutwards(pos, 1, 1, 1)) {
            if (checkPos.equals(pos)) {
                continue;
            }
            if (world.getBlockState(checkPos).isOf(BlockInit.DUNGEON_PORTAL)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static BlockPos getMainDungeonPortalBlockPos(World world, BlockPos pos) {
        BlockPos checkPos = new BlockPos(pos);
        for (int i = 1; i < 30; i++) {
            if (world.getBlockState(checkPos.east(1)).isOf(BlockInit.DUNGEON_PORTAL)) {
                checkPos = checkPos.east(1);
            } else {
                break;
            }
        }
        for (int i = 1; i < 30; i++) {
            if (world.getBlockState(checkPos.south(1)).isOf(BlockInit.DUNGEON_PORTAL)) {
                checkPos = checkPos.south(1);
            } else {
                break;
            }
        }
        for (int i = 1; i < 30; i++) {
            if (world.getBlockState(checkPos.down(1)).isOf(BlockInit.DUNGEON_PORTAL)) {
                checkPos = checkPos.down(1);
            } else {
                break;
            }
        }
        return world.getBlockEntity(checkPos) instanceof DungeonPortalEntity dungeonPortalEntity ? dungeonPortalEntity.getPos() : null;
    }

    @Nullable
    public static DungeonPortalEntity getMainDungeonPortalEntity(World world, BlockPos pos) {
        if (getMainDungeonPortalBlockPos(world, pos) != null) {
            return (DungeonPortalEntity) world.getBlockEntity(getMainDungeonPortalBlockPos(world, pos));
        }
        return null;
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && !world.isClient && world instanceof ServerWorld serverWorld) {
            if (world.getBlockEntity(pos) instanceof DungeonPortalEntity) {
                DungeonDataManager.deleteData(serverWorld, pos);
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public boolean canFillWithFluid(BlockView world, BlockPos pos, BlockState state, Fluid fluid) {
        return false;
    }

    @Override
    public boolean tryFillWithFluid(WorldAccess world, BlockPos pos, BlockState state, FluidState fluidState) {
        return false;
    }

}
