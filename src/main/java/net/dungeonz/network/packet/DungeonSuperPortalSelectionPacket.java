package net.dungeonz.network.packet;

import java.util.List;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record DungeonSuperPortalSelectionPacket(BlockPos portalPos, List<String> dungeonIdList) implements CustomPayload {

    public static final CustomPayload.Id<DungeonSuperPortalSelectionPacket> PACKET_ID = new CustomPayload.Id<>(Identifier.of("dungeonz", "dungeon_super_portal_selection_packet"));

    public static final PacketCodec<RegistryByteBuf, DungeonSuperPortalSelectionPacket> PACKET_CODEC = PacketCodec.of((value, buf) -> {
        buf.writeBlockPos(value.portalPos);
        buf.writeCollection(value.dungeonIdList, PacketByteBuf::writeString);
    }, buf -> new DungeonSuperPortalSelectionPacket(buf.readBlockPos(), buf.readList(PacketByteBuf::readString)));

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }

}
