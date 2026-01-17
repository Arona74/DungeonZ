package net.dungeonz.block.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import org.jetbrains.annotations.Nullable;

import net.dungeonz.block.DungeonGateBlock;
import net.dungeonz.init.BlockInit;
import net.dungeonz.init.ConfigInit;
import net.dungeonz.init.DimensionInit;
import net.dungeonz.init.SoundInit;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.command.argument.ParticleEffectArgumentType;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class DungeonGateEntity extends BlockEntity {

    private static final List<Direction> directions = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP, Direction.DOWN);
    private Identifier gateBlockId = Identifier.of("minecraft:chiseled_stone_bricks");
    private String unlockItemId = "";
    private String gateParticleId = "minecraft:scrape";
    private List<Integer> dungeonEdgeList = new ArrayList<Integer>();

    public DungeonGateEntity(BlockPos pos, BlockState state) {
        super(BlockInit.DUNGEON_GATE_ENTITY, pos, state);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        this.gateBlockId = Identifier.of(nbt.getString("GateBlockId"));
        this.unlockItemId = nbt.getString("UnlockItemId");
        this.gateParticleId = nbt.getString("GateParticleId");

        if (nbt.getInt("DungeonEdgeSize") > 0) {
            this.dungeonEdgeList.clear();
            for (int i = 0; i < nbt.getInt("DungeonEdgeSize") / 3; i++) {
                this.dungeonEdgeList.add(nbt.getInt("DungeonEdgeX" + i));
                this.dungeonEdgeList.add(nbt.getInt("DungeonEdgeY" + i));
                this.dungeonEdgeList.add(nbt.getInt("DungeonEdgeZ" + i));
            }
        }
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putString("GateBlockId", this.gateBlockId.toString());
        nbt.putString("UnlockItemId", this.unlockItemId.toString());
        nbt.putString("GateParticleId", this.gateParticleId.toString());

        nbt.putInt("DungeonEdgeSize", this.dungeonEdgeList.size());
        if (this.dungeonEdgeList.size() > 0) {
            for (int i = 0; i < this.dungeonEdgeList.size() / 3; i++) {
                nbt.putInt("DungeonEdgeX" + i, this.dungeonEdgeList.get(3 * i));
                nbt.putInt("DungeonEdgeY" + i, this.dungeonEdgeList.get(3 * i + 1));
                nbt.putInt("DungeonEdgeZ" + i, this.dungeonEdgeList.get(3 * i + 2));
            }
        }
    }

    public static void serverTick(World world, BlockPos pos, BlockState state, DungeonGateEntity blockEntity) {
        if (world.getTime() % 20 == 0 && state.get(DungeonGateBlock.ENABLED) && (blockEntity.unlockItemId == null || blockEntity.unlockItemId.isEmpty())
                && blockEntity.getDungeonEdgeList().size() >= 6 && world.getRegistryKey() == DimensionInit.DUNGEON_WORLD && !ConfigInit.CONFIG.devMode) {
            // Only trigger from the "anchor" block (no gate to south, west, or below) to avoid multiple triggers
            boolean isAnchor = !world.getBlockState(pos.down()).isOf(BlockInit.DUNGEON_GATE)
                    && !world.getBlockState(pos.south()).isOf(BlockInit.DUNGEON_GATE)
                    && !world.getBlockState(pos.west()).isOf(BlockInit.DUNGEON_GATE);
            if (isAnchor && !blockEntity.areHostileEntitiesAlive()) {
                blockEntity.unlockGate(pos);
            }
        }
    }

    private boolean areHostileEntitiesAlive() {
        if (this.getDungeonEdgeList().size() < 6) {
            return false;
        }
        List<HostileEntity> hostileEntities = world.getEntitiesByClass(HostileEntity.class, new Box(this.getDungeonEdgeList().get(0), this.getDungeonEdgeList().get(1),
                this.getDungeonEdgeList().get(2), this.getDungeonEdgeList().get(3), this.getDungeonEdgeList().get(4), this.getDungeonEdgeList().get(5)), EntityPredicates.EXCEPT_SPECTATOR);
        if (hostileEntities.isEmpty()) {
            return false;
        }
        return true;
    }

    public void unlockGate(BlockPos pos) {
        world.playSound(null, pos, SoundInit.DUNGEON_GATE_UNLOCK_EVENT, SoundCategory.BLOCKS, 1.0f, 0.9f + world.getRandom().nextFloat() * 0.2f);

        List<BlockPos> dungeonGatesPosList = DungeonGateEntity.getConnectedDungeonGatePosList(world, pos);
        for (int i = 0; i < dungeonGatesPosList.size(); i++) {
            if (world.getBlockEntity(dungeonGatesPosList.get(i)) != null && world.getBlockEntity(dungeonGatesPosList.get(i)) instanceof DungeonGateEntity) {
                DungeonGateEntity otherDungeonGateEntity = (DungeonGateEntity) world.getBlockEntity(dungeonGatesPosList.get(i));

                world.setBlockState(dungeonGatesPosList.get(i), otherDungeonGateEntity.getCachedState().cycle(DungeonGateBlock.ENABLED));
                otherDungeonGateEntity.markDirty();
            }
        }
    }

    public static List<BlockPos> getConnectedDungeonGatePosList(World world, BlockPos pos) {
        List<BlockPos> dungeonGates = new ArrayList<BlockPos>();
        List<Integer> directionLengths = new ArrayList<Integer>();

        for (int i = 0; i < DungeonGateEntity.directions.size(); i++) {
            for (int u = 1; u < 100; u++) {
                if (!world.getBlockState(pos.offset(DungeonGateEntity.directions.get(i), u)).isOf(BlockInit.DUNGEON_GATE)) {
                    directionLengths.add(u - 1);
                    break;
                }
            }
        }
        for (int i = -directionLengths.get(5); i <= directionLengths.get(4); i++) {
            for (int u = -directionLengths.get(0); u <= directionLengths.get(2); u++) {
                for (int o = -directionLengths.get(1); o <= directionLengths.get(3); o++) {
                    BlockPos checkPos = pos.up(i).south(u).west(o);
                    if (world.getBlockState(checkPos).isOf(BlockInit.DUNGEON_GATE) && !dungeonGates.contains(checkPos)) {
                        dungeonGates.add(checkPos);
                    }
                }
            }
        }

        return dungeonGates;
    }

    @Override
    public BlockEntityUpdateS2CPacket toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(WrapperLookup registryLookup) {
        return this.createNbt(registryLookup);
    }

    public void setUnlockItemId(String unlockItemId) {
        this.unlockItemId = unlockItemId;
    }

    @Nullable
    public Item getUnlockItem() {
        if (this.unlockItemId.equals("")) {
            return null;
        }
        return Registries.ITEM.get(Identifier.of(this.unlockItemId));
    }

    public void setBlockId(Identifier gateBlockId) {
        this.gateBlockId = gateBlockId;
    }

    public BlockState getBlockState() {
        return Registries.BLOCK.get(this.gateBlockId).getDefaultState();
    }

    public void setParticleEffectId(String gateParticleId) {
        this.gateParticleId = gateParticleId;
    }

    @Nullable
    public ParticleEffect getParticleEffect() {
        if (this.gateParticleId.equals("")) {
            return null;
        }
        try {
            // return (T)((ParticleEffect)type.getCodec().codec().parse(registryLookup.getOps(NbtOps.INSTANCE), nbtCompound).getOrThrow(INVALID_OPTIONS_EXCEPTION::create));
            // Registries.PARTICLE_TYPE.get(Identifier.of(this.gateParticleId.toString())).getCodec().codec();

            // return ParticleEffectArgumentType.readParameters(new StringReader(this.gateParticleId.toString()), Registries.PARTICLE_TYPE.getReadOnlyWrapper());
            // return ParticleEffectArgumentType.readParameters(new StringReader(this.gateParticleId.toString()),
            // RegistryWrapper.WrapperLookup.of(Registries.PARTICLE_TYPE.getReadOnlyWrapper().streamEntries()));

            return ParticleEffectArgumentType.readParameters(new StringReader(this.gateParticleId.toString()),
                    RegistryWrapper.WrapperLookup.of(Stream.of(Registries.PARTICLE_TYPE.getReadOnlyWrapper())));
        } catch (CommandSyntaxException commandSyntaxException) {
        }
        return null;
    }

    public void addDungeonEdge(int edgeX, int edgeY, int edgeZ) {
        this.dungeonEdgeList.add(edgeX);
        this.dungeonEdgeList.add(edgeY);
        this.dungeonEdgeList.add(edgeZ);
    }

    public List<Integer> getDungeonEdgeList() {
        return this.dungeonEdgeList;
    }

}
