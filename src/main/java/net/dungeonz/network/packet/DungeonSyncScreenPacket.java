package net.dungeonz.network.packet;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

public record DungeonSyncScreenPacket(BlockPos blockPos, String difficulty, boolean dungeonTimerActive, int dungeonTimeRemaining, int cooldownTime, List<UUID> dungeonPlayerUuids, List<UUID> deadPlayerUuids) implements CustomPayload {

    public static final CustomPayload.Id<DungeonSyncScreenPacket> PACKET_ID = new CustomPayload.Id<>(Identifier.of("dungeonz", "dungeon_sync_screen_packet"));

    public static final PacketCodec<RegistryByteBuf, DungeonSyncScreenPacket> PACKET_CODEC = PacketCodec.of((value, buf) -> {
        buf.writeBlockPos(value.blockPos);
        buf.writeString(value.difficulty);
        buf.writeBoolean(value.dungeonTimerActive);
        buf.writeInt(value.dungeonTimeRemaining);
        buf.writeInt(value.cooldownTime);
        buf.writeCollection(value.dungeonPlayerUuids, (buffer, uuid) -> buffer.writeUuid(uuid));
        buf.writeCollection(value.deadPlayerUuids, (buffer, uuid) -> buffer.writeUuid(uuid));
    }, buf -> new DungeonSyncScreenPacket(
        buf.readBlockPos(),
        buf.readString(),
        buf.readBoolean(),
        buf.readInt(),
        buf.readInt(),
        buf.readList((buffer) -> PacketByteBuf.readUuid(buffer)),
        buf.readList((buffer) -> PacketByteBuf.readUuid(buffer))
    ));

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }

}
