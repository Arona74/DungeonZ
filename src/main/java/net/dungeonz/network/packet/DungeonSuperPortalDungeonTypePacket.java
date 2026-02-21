package net.dungeonz.network.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record DungeonSuperPortalDungeonTypePacket(BlockPos portalPos, String dungeonType) implements CustomPayload {

    public static final CustomPayload.Id<DungeonSuperPortalDungeonTypePacket> PACKET_ID = new CustomPayload.Id<>(Identifier.of("dungeonz", "dungeon_super_portal_dungeon_type_packet"));

    public static final PacketCodec<RegistryByteBuf, DungeonSuperPortalDungeonTypePacket> PACKET_CODEC = PacketCodec.of((value, buf) -> {
        buf.writeBlockPos(value.portalPos);
        buf.writeString(value.dungeonType);
    }, buf -> new DungeonSuperPortalDungeonTypePacket(buf.readBlockPos(), buf.readString()));

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }

}
