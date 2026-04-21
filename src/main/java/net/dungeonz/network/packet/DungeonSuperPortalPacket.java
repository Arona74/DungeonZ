package net.dungeonz.network.packet;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record DungeonSuperPortalPacket(String dungeonType, BlockPos blockPos, List<UUID> playerUuids, List<UUID> deadPlayerUuids, List<String> difficulties, Map<String, List<ItemStack>> possibleLoot,
                                       Map<String, List<ItemStack>> requiredItemStacks, int maxGroupSize, int minGroupSize, int waitingPlayerCount, int requiredLevel, int cooldownTime, String difficulty,
                                       boolean allowEnderPearl, boolean allowWindCharge, boolean allowPositiveEffects, boolean allowElytra, boolean allowRespawn, boolean keepInventory,
                                       boolean allowMobsLoot, boolean allowBossLoot, boolean privateGroup, Optional<Identifier> backgroundId,
                                       List<String> dungeonIdList, boolean dungeonTimerActive, int dungeonTimeRemaining)
        implements CustomPayload {

    public static final CustomPayload.Id<DungeonSuperPortalPacket> PACKET_ID = new CustomPayload.Id<>(Identifier.of("dungeonz", "dungeon_super_portal_packet"));

    public static final PacketCodec<RegistryByteBuf, DungeonSuperPortalPacket> PACKET_CODEC = PacketCodec.of((value, buf) -> {
        buf.writeString(value.dungeonType);
        buf.writeBlockPos(value.blockPos);
        buf.writeCollection(value.playerUuids, (buffer, uuid) -> buffer.writeUuid(uuid));
        buf.writeCollection(value.deadPlayerUuids, (buffer, uuid) -> buffer.writeUuid(uuid));
        buf.writeCollection(value.difficulties, PacketByteBuf::writeString);
        buf.writeMap(value.possibleLoot, PacketByteBuf::writeString, (buffer, stacks) -> ItemStack.LIST_PACKET_CODEC.encode(buf, stacks));
        buf.writeMap(value.requiredItemStacks, PacketByteBuf::writeString, (buffer, stacks) -> ItemStack.LIST_PACKET_CODEC.encode(buf, stacks));
        buf.writeInt(value.maxGroupSize);
        buf.writeInt(value.minGroupSize);
        buf.writeInt(value.waitingPlayerCount);
        buf.writeInt(value.requiredLevel);
        buf.writeInt(value.cooldownTime);
        buf.writeString(value.difficulty);
        buf.writeBoolean(value.allowEnderPearl);
        buf.writeBoolean(value.allowWindCharge);
        buf.writeBoolean(value.allowPositiveEffects);
        buf.writeBoolean(value.allowElytra);
        buf.writeBoolean(value.allowRespawn);
        buf.writeBoolean(value.keepInventory);
        buf.writeBoolean(value.allowMobsLoot);
        buf.writeBoolean(value.allowBossLoot);
        buf.writeBoolean(value.privateGroup);
        buf.writeOptional(value.backgroundId, PacketByteBuf::writeIdentifier);
        buf.writeCollection(value.dungeonIdList, PacketByteBuf::writeString);
        buf.writeBoolean(value.dungeonTimerActive);
        buf.writeInt(value.dungeonTimeRemaining);

    }, buf -> new DungeonSuperPortalPacket(buf.readString(), buf.readBlockPos(), buf.readList((buffer) -> PacketByteBuf.readUuid(buffer)), buf.readList((buffer) -> PacketByteBuf.readUuid(buffer)),
            buf.readList(PacketByteBuf::readString), buf.readMap(PacketByteBuf::readString, (bufx) -> ItemStack.LIST_PACKET_CODEC.decode(buf)),
            buf.readMap(PacketByteBuf::readString, (bufx) -> ItemStack.LIST_PACKET_CODEC.decode(buf)), buf.readInt(),
            buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readString(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(),
            buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readOptional(PacketByteBuf::readIdentifier), buf.readList(PacketByteBuf::readString), buf.readBoolean(), buf.readInt()));

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }

}
