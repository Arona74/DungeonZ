package net.dungeonz.network.packet;

import io.netty.buffer.Unpooled;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.Map.Entry;

public class DungeonSuperPortalPacket {

    public static final Identifier PACKET_ID = new Identifier("dungeonz", "dungeon_super_portal_packet");

    public final String dungeonType;
    public final BlockPos blockPos;
    public final List<UUID> playerUuids;
    public final List<UUID> deadPlayerUuids;
    public final List<String> difficulties;
    public final Map<String, List<ItemStack>> possibleLoot;
    public final Map<String, List<ItemStack>> requiredItemStacks;
    public final int maxGroupSize, minGroupSize, waitingPlayerCount, requiredLevel, cooldownTime;
    public final String difficulty;
    public final boolean allowEnderPearl, allowPositiveEffects, allowElytra, allowRespawn,
            allowMobsLoot, allowBossLoot, keepInventory, privateGroup;
    public final Optional<Identifier> backgroundId;
    public final List<String> dungeonIdList;

    public DungeonSuperPortalPacket(String dungeonType, BlockPos blockPos,
                                    List<UUID> playerUuids, List<UUID> deadPlayerUuids,
                                    List<String> difficulties,
                                    Map<String, List<ItemStack>> possibleLoot,
                                    Map<String, List<ItemStack>> requiredItemStacks,
                                    int maxGroupSize, int minGroupSize, int waitingPlayerCount,
                                    int requiredLevel, int cooldownTime, String difficulty,
                                    boolean allowEnderPearl, boolean allowPositiveEffects,
                                    boolean allowElytra, boolean allowRespawn,
                                    boolean allowMobsLoot, boolean allowBossLoot,
                                    boolean keepInventory, boolean privateGroup,
                                    Optional<Identifier> backgroundId,
                                    List<String> dungeonIdList) {
        this.dungeonType = dungeonType;
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
        this.allowEnderPearl = allowEnderPearl;
        this.allowPositiveEffects = allowPositiveEffects;
        this.allowElytra = allowElytra;
        this.allowRespawn = allowRespawn;
        this.allowMobsLoot = allowMobsLoot;
        this.allowBossLoot = allowBossLoot;
        this.keepInventory = keepInventory;
        this.privateGroup = privateGroup;
        this.backgroundId = backgroundId;
        this.dungeonIdList = dungeonIdList;
    }

    public static void encode(DungeonSuperPortalPacket p, PacketByteBuf buf) {
        buf.writeString(p.dungeonType);
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
        for (Entry<String, List<ItemStack>> e : p.requiredItemStacks.entrySet()) {
            buf.writeString(e.getKey());
            buf.writeInt(e.getValue().size());
            e.getValue().forEach(buf::writeItemStack);
        }

        buf.writeInt(p.maxGroupSize);
        buf.writeInt(p.minGroupSize);
        buf.writeInt(p.waitingPlayerCount);
        buf.writeInt(p.requiredLevel);
        buf.writeInt(p.cooldownTime);
        buf.writeString(p.difficulty);
        buf.writeBoolean(p.allowEnderPearl);
        buf.writeBoolean(p.allowPositiveEffects);
        buf.writeBoolean(p.allowElytra);
        buf.writeBoolean(p.allowRespawn);
        buf.writeBoolean(p.allowMobsLoot);
        buf.writeBoolean(p.allowBossLoot);
        buf.writeBoolean(p.keepInventory);
        buf.writeBoolean(p.privateGroup);

        buf.writeBoolean(p.backgroundId.isPresent());
        p.backgroundId.ifPresent(buf::writeIdentifier);

        buf.writeInt(p.dungeonIdList.size());
        p.dungeonIdList.forEach(buf::writeString);
    }

    public static DungeonSuperPortalPacket decode(PacketByteBuf buf) {
        String dungeonType = buf.readString(32767);
        BlockPos blockPos = buf.readBlockPos();

        int nPlayers = buf.readInt();
        List<UUID> playerUuids = new ArrayList<>(nPlayers);
        for (int i = 0; i < nPlayers; i++) playerUuids.add(buf.readUuid());

        int nDead = buf.readInt();
        List<UUID> deadPlayerUuids = new ArrayList<>(nDead);
        for (int i = 0; i < nDead; i++) deadPlayerUuids.add(buf.readUuid());

        int nDiff = buf.readInt();
        List<String> difficulties = new ArrayList<>(nDiff);
        for (int i = 0; i < nDiff; i++) difficulties.add(buf.readString(32767));

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
        Map<String, List<ItemStack>> reqStacks = new HashMap<>();
        for (int i = 0; i < reqCount; i++) {
            String key = buf.readString(32767);
            int count = buf.readInt();
            List<ItemStack> list = new ArrayList<>();
            for (int j = 0; j < count; j++) list.add(buf.readItemStack());
            reqStacks.put(key, list);
        }

        int max = buf.readInt();
        int min = buf.readInt();
        int waitingCount = buf.readInt();
        int level = buf.readInt();
        int cooldown = buf.readInt();
        String difficulty = buf.readString(32767);
        boolean enderpearl = buf.readBoolean();
        boolean positive = buf.readBoolean();
        boolean elytra = buf.readBoolean();
        boolean respawn = buf.readBoolean();
        boolean mobsLoot = buf.readBoolean();
        boolean bossLoot = buf.readBoolean();
        boolean keepInventory = buf.readBoolean();
        boolean priv = buf.readBoolean();

        Optional<Identifier> bg = buf.readBoolean() ? Optional.of(buf.readIdentifier()) : Optional.empty();

        int dungeonIdCount = buf.readInt();
        List<String> dungeonIdList = new ArrayList<>(dungeonIdCount);
        for (int i = 0; i < dungeonIdCount; i++) dungeonIdList.add(buf.readString(32767));

        return new DungeonSuperPortalPacket(dungeonType, blockPos, playerUuids, deadPlayerUuids,
                difficulties, possibleLoot, reqStacks, max, min, waitingCount, level, cooldown,
                difficulty, enderpearl, positive, elytra, respawn, mobsLoot, bossLoot,
                keepInventory, priv, bg, dungeonIdList);
    }

    public static PacketByteBuf toBuf(DungeonSuperPortalPacket p) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        encode(p, buf);
        return buf;
    }
}
