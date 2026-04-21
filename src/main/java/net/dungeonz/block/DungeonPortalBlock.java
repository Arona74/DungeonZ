package net.dungeonz.block;

import java.util.Iterator;

import net.minecraft.block.FluidFillable;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.dungeonz.DungeonzMain;
import net.dungeonz.block.entity.DungeonPortalEntity;
import net.dungeonz.init.BlockInit;
import net.dungeonz.network.DungeonServerPacket;
import net.dungeonz.util.DungeonHelper;
import net.dungeonz.dungeon.DungeonDataManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.partyaddon.access.GroupManagerAccess;
import net.partyaddon.network.PartyAddonServerPacket;

public class DungeonPortalBlock extends BlockWithEntity implements FluidFillable {

    public static final MapCodec<DungeonPortalBlock> CODEC = DungeonPortalBlock.createCodec(DungeonPortalBlock::new);

    public static final EnumProperty<Direction.Axis> AXIS =
            EnumProperty.of("axis", Direction.Axis.class, Direction.Axis.X, Direction.Axis.Z);
    public static final BooleanProperty SOLO = BooleanProperty.of("solo");

    private static final VoxelShape X_SHAPE = Block.createCuboidShape(0, 0, 6, 16, 16, 10);
    private static final VoxelShape Z_SHAPE = Block.createCuboidShape(6, 0, 0, 10, 16, 16);
    private static final VoxelShape FULL_CUBE_SHAPE = Block.createCuboidShape(0, 0, 0, 16, 16, 16);

    public DungeonPortalBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(AXIS, Direction.Axis.X).with(SOLO, true));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(AXIS, SOLO);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        World world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();
        boolean hasSameNeighbor = false;
        Direction axisDir = null;
        for (Direction dir : Direction.Type.HORIZONTAL) {
            Block neighbor = world.getBlockState(pos.offset(dir)).getBlock();
            if (neighbor == this) hasSameNeighbor = true;
            if (neighbor instanceof DungeonPortalBlock && axisDir == null) axisDir = dir;
        }
        if (axisDir != null) {
            return this.getDefaultState().with(AXIS, axisDir.getAxis()).with(SOLO, !hasSameNeighbor);
        }
        Direction facing = ctx.getPlayer() != null ? ctx.getPlayer().getHorizontalFacing() : Direction.NORTH;
        Direction.Axis axis = facing.getAxis() == Direction.Axis.Z ? Direction.Axis.X : Direction.Axis.Z;
        return this.getDefaultState().with(AXIS, axis).with(SOLO, !hasSameNeighbor);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction,
            BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (direction.getAxis().isHorizontal()) {
            Block thisBlock = state.getBlock();
            boolean hasSameNeighbor = false;
            Direction axisDir = null;
            Block changed = neighborState.getBlock();
            if (changed == thisBlock) hasSameNeighbor = true;
            if (changed instanceof DungeonPortalBlock) axisDir = direction;
            for (Direction dir : Direction.Type.HORIZONTAL) {
                if (dir == direction) continue;
                Block at = world.getBlockState(pos.offset(dir)).getBlock();
                if (at == thisBlock) hasSameNeighbor = true;
                if (at instanceof DungeonPortalBlock && axisDir == null) axisDir = dir;
            }
            BlockState newState = state.with(SOLO, !hasSameNeighbor);
            return axisDir != null ? newState.with(AXIS, axisDir.getAxis()) : newState;
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        if (state.get(SOLO)) return FULL_CUBE_SHAPE;
        return state.get(AXIS) == Direction.Axis.X ? X_SHAPE : Z_SHAPE;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new DungeonPortalEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return net.dungeonz.init.ConfigInit.CONFIG.customPortalRendering ? BlockRenderType.INVISIBLE : BlockRenderType.MODEL;
    }

    @Override
    public boolean isSideInvisible(BlockState state, BlockState stateFrom, Direction direction) {
        return stateFrom.isOf(this) || super.isSideInvisible(state, stateFrom, direction);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (player.getWorld().getBlockEntity(pos) != null && player.getWorld().getBlockEntity(pos) instanceof DungeonPortalEntity dungeonPortalEntity) {
            if (isOtherDungeonPortalBlockNearby(world, pos)) {
                dungeonPortalEntity = getMainDungeonPortalEntity(world, pos);
                pos = getMainDungeonPortalBlockPos(world, pos);
            }
            if (player.isCreativeLevelTwoOp() && (dungeonPortalEntity.getDungeon() == null || player.isSneaking())) {
                if (!world.isClient()) {
                    DungeonServerPacket.writeS2COpenOpScreenPacket((ServerPlayerEntity) player, dungeonPortalEntity, null);
                }
                return ActionResult.success(world.isClient());
            } else if (dungeonPortalEntity.getDungeon() != null) {
                if (!world.isClient()) {
                    if (DungeonzMain.isPartyAddonLoaded) {
                        PartyAddonServerPacket.writeS2CSyncGroupManagerPacket((ServerPlayerEntity) player, ((GroupManagerAccess) player).getGroupManager());
                    }
                    player.openHandledScreen(state.createScreenHandlerFactory(world, pos));
                }
                return ActionResult.success(world.isClient());
            }
        }
        return super.onUse(state, world, pos, player, hit);
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isClient() && !entity.hasVehicle() && !entity.hasPassengers() && entity.canUsePortals(false) && entity instanceof ServerPlayerEntity) {
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
    protected boolean canBucketPlace(BlockState state, Fluid fluid) {
        return false;
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return DungeonGateBlock.validateTicker(type, BlockInit.DUNGEON_PORTAL_ENTITY, world.isClient() ? DungeonPortalEntity::clientTick : DungeonPortalEntity::serverTick);
    }

    public static boolean isOtherPortalBlockNearby(World world, BlockPos pos, Block blockType) {
        for (BlockPos checkPos : BlockPos.iterateOutwards(pos, 1, 1, 1)) {
            if (checkPos.equals(pos)) {
                continue;
            }
            if (world.getBlockState(checkPos).isOf(blockType)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static BlockPos getMainPortalBlockPos(World world, BlockPos pos, Block blockType) {
        BlockPos checkPos = new BlockPos(pos);
        for (int i = 1; i < 30; i++) {
            if (world.getBlockState(checkPos.east(1)).isOf(blockType)) {
                checkPos = checkPos.east(1);
            } else {
                break;
            }
        }
        for (int i = 1; i < 30; i++) {
            if (world.getBlockState(checkPos.south(1)).isOf(blockType)) {
                checkPos = checkPos.south(1);
            } else {
                break;
            }
        }
        for (int i = 1; i < 30; i++) {
            if (world.getBlockState(checkPos.down(1)).isOf(blockType)) {
                checkPos = checkPos.down(1);
            } else {
                break;
            }
        }
        return world.getBlockEntity(checkPos) instanceof DungeonPortalEntity ? checkPos : null;
    }

    @Nullable
    public static DungeonPortalEntity getMainPortalEntity(World world, BlockPos pos, Block blockType) {
        BlockPos mainPos = getMainPortalBlockPos(world, pos, blockType);
        return mainPos != null ? (DungeonPortalEntity) world.getBlockEntity(mainPos) : null;
    }

    public static boolean isOtherDungeonPortalBlockNearby(World world, BlockPos pos) {
        return isOtherPortalBlockNearby(world, pos, BlockInit.DUNGEON_PORTAL);
    }

    @Nullable
    public static BlockPos getMainDungeonPortalBlockPos(World world, BlockPos pos) {
        return getMainPortalBlockPos(world, pos, BlockInit.DUNGEON_PORTAL);
    }

    @Nullable
    public static DungeonPortalEntity getMainDungeonPortalEntity(World world, BlockPos pos) {
        return getMainPortalEntity(world, pos, BlockInit.DUNGEON_PORTAL);
    }

    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && !world.isClient && world instanceof ServerWorld serverWorld) {
            if (world.getBlockEntity(pos) instanceof DungeonPortalEntity) {
                DungeonDataManager.deleteData(serverWorld, pos);
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    // Used for not getting removed by water
    @Override
    public boolean canFillWithFluid(@Nullable PlayerEntity player, BlockView world, BlockPos pos, BlockState state, Fluid fluid) {
        return false;
    }

    @Override
    public boolean tryFillWithFluid(WorldAccess world, BlockPos pos, BlockState state, FluidState fluidState) {
        return false;
    }
}
