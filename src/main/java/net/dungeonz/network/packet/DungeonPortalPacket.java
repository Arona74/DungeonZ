package net.dungeonz.network.packet;

import java.util.*;
import java.util.Map.Entry;

import io.netty.buffer.Unpooled;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class DungeonPortalPacket {
    public static final Identifier DUNGEON_PORTAL_PACKET = new Identifier("dungeonz", "dungeon_portal_packet");

    public final BlockPos blockPos;
    public final List<UUID> playerUuids;
    public final List<UUID> deadPlayerUuids;
    public final List<String> difficulties;
    public final Map<String, List<ItemStack>> possibleLoot;
    public final List<ItemStack> requiredItemStacks;
    public final int maxGroupSize, minGroupSize, waitingPlayerCount, requiredLevel, cooldownTime;
    public final String difficulty;
    public final boolean disableEffects, privateGroup;
    public final Optional<Identifier> backgroundId;

    public DungeonPortalPacket(BlockPos blockPos,
                               List<UUID> playerUuids,
                               List<UUID> deadPlayerUuids,
                               List<String> difficulties,
                               Map<String, List<ItemStack>> possibleLoot,
                               List<ItemStack> requiredItemStacks,
                               int maxGroupSize,
                               int minGroupSize,
                               int waitingPlayerCount,
                               int requiredLevel,
                               int cooldownTime,
                               String difficulty,
                               boolean disableEffects,
                               boolean privateGroup,
                               Optional<Identifier> backgroundId) {
        this.blockPos = blockPos;
        this.playerUuids = playerUuids;
        this.deadPlayerUuids = deadPlayerUuids;
        this.difficulties = difficulties;
        this.possibleLoot = possibleLoot;
        this.requiredItemStacks = requiredItemStacks;
        this.maxGroupSize = maxGroupSize;
        this.minGroupSize = minGroupSize;
        this.waitingPlayerCount = waitingPlayerCount;
        this.requiredLevel = requiredLevel;
        this.cooldownTime = cooldownTime;
        this.difficulty = difficulty;
        this.disableEffects = disableEffects;
        this.privateGroup = privateGroup;
        this.backgroundId = backgroundId;
    }

    public static void encode(DungeonPortalPacket p, PacketByteBuf buf) {
        buf.writeBlockPos(p.blockPos);

        buf.writeInt(p.playerUuids.size());
        p.playerUuids.forEach(buf::writeUuid);

        buf.writeInt(p.deadPlayerUuids.size());
        p.deadPlayerUuids.forEach(buf::writeUuid);

        buf.writeInt(p.difficulties.size());
        p.difficulties.forEach(buf::writeString);

        buf.writeInt(p.possibleLoot.size());
        for (Entry<String, List<ItemStack>> e : p.possibleLoot.entrySet()) {
            buf.writeString(e.getKey());
            buf.writeInt(e.getValue().size());
            e.getValue().forEach(buf::writeItemStack);
        }

        buf.writeInt(p.requiredItemStacks.size());
        p.requiredItemStacks.forEach(buf::writeItemStack);

        buf.writeInt(p.maxGroupSize);
        buf.writeInt(p.minGroupSize);
        buf.writeInt(p.waitingPlayerCount);
        buf.writeInt(p.requiredLevel);
        buf.writeInt(p.cooldownTime);
        buf.writeString(p.difficulty);
        buf.writeBoolean(p.disableEffects);
        buf.writeBoolean(p.privateGroup);

        buf.writeBoolean(p.backgroundId.isPresent());
        p.backgroundId.ifPresent(buf::writeIdentifier);
    }

    public static DungeonPortalPacket decode(PacketByteBuf buf) {
        BlockPos blockPos = buf.readBlockPos();

        int nPlayers = buf.readInt();
        List<UUID> playerUuids = new ArrayList<>(nPlayers);
        for (int i = 0; i < nPlayers; i++) playerUuids.add(buf.readUuid());

        int nDead = buf.readInt();
        List<UUID> dead = new ArrayList<>(nDead);
        for (int i = 0; i < nDead; i++) dead.add(buf.readUuid());

        int nDiff = buf.readInt();
        List<String> diffs = new ArrayList<>(nDiff);
        for (int i = 0; i < nDiff; i++) diffs.add(buf.readString(32767));

        int lootEntries = buf.readInt();
        Map<String, List<ItemStack>> possibleLoot = new HashMap<>();
        for (int i = 0; i < lootEntries; i++) {
            String key = buf.readString(32767);
            int count = buf.readInt();
            List<ItemStack> list = new ArrayList<>();
            for (int j = 0; j < count; j++) list.add(buf.readItemStack());
            possibleLoot.put(key, list);
        }

        int reqCount = buf.readInt();
        List<ItemStack> reqStacks = new ArrayList<>();
        for (int i = 0; i < reqCount; i++) reqStacks.add(buf.readItemStack());

        int max = buf.readInt();
        int min = buf.readInt();
        int waiting = buf.readInt();
        int level = buf.readInt();
        int cooldown = buf.readInt();

        String difficulty = buf.readString(32767);
        boolean disable = buf.readBoolean();
        boolean priv = buf.readBoolean();

        Optional<Identifier> bg = buf.readBoolean() ? Optional.of(buf.readIdentifier()) : Optional.empty();

        return new DungeonPortalPacket(blockPos, playerUuids, dead, diffs, possibleLoot, reqStacks,
                max, min, waiting, level, cooldown, difficulty, disable, priv, bg);
    }

    public static PacketByteBuf toBuf(DungeonPortalPacket p) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        encode(p, buf);
        return buf;
    }
}
