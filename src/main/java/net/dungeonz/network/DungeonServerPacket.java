package net.dungeonz.network;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.dungeonz.DungeonzMain;
import net.dungeonz.block.entity.DungeonGateEntity;
import net.dungeonz.block.entity.DungeonPortalEntity;
import net.dungeonz.dungeon.Dungeon;
import net.dungeonz.init.ItemInit;
import net.dungeonz.item.DungeonCompassItem;
import net.dungeonz.util.DungeonHelper;
import net.dungeonz.util.InventoryHelper;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class DungeonServerPacket {

    public static final Identifier DUNGEON_INFO_PACKET = new Identifier("dungeonz", "dungeon_info");

    public static final Identifier DUNGEON_TELEPORT_PACKET = new Identifier("dungeonz", "dungeon_teleport");
    public static final Identifier DUNGEON_TELEPORT_COUNTDOWN_PACKET = new Identifier("dungeonz", "dungeon_teleport_countdown");

    public static final Identifier CHANGE_DUNGEON_DIFFICULTY_PACKET = new Identifier("dungeonz", "change_dungeon_difficulty");
    public static final Identifier CHANGE_DUNGEON_EFFECTS_PACKET = new Identifier("dungeonz", "change_dungeon_effects");
    public static final Identifier CHANGE_DUNGEON_PRIVATE_GROUP_PACKET = new Identifier("dungeonz", "change_dungeon_private_group");

    public static final Identifier SET_DUNGEON_TYPE_PACKET = new Identifier("dungeonz", "set_dungeon_type");
    public static final Identifier SET_DUNGEON_COMPASS_PACKET = new Identifier("dungeonz", "set_dungeon_compass");
    public static final Identifier SET_GATE_BLOCK_PACKET = new Identifier("dungeonz", "set_gate_block");
    public static final Identifier SYNC_GATE_BLOCK_PACKET = new Identifier("dungeonz", "sync_gate_block");

    public static final Identifier SYNC_SCREEN_PACKET = new Identifier("dungeonz", "sync_screen");
    public static final Identifier OP_SCREEN_PACKET = new Identifier("dungeonz", "op_screen");
    public static final Identifier COMPASS_SCREEN_PACKET = new Identifier("dungeonz", "compass_screen");
    public static final Identifier DUNGEON_PORTAL_PACKET = new Identifier("dungeonz", "dungeon_portal_packet");

    public static final Identifier LEAVE_WAITING_PACKET = new Identifier("dungeonz", "leave_waiting");
    
    // New packets for tracking GUI state
    public static final Identifier GUI_OPENED_PACKET = new Identifier("dungeonz", "gui_opened");
    public static final Identifier GUI_CLOSED_PACKET = new Identifier("dungeonz", "gui_closed");

    // Track which players have which GUIs open
    // Map: BlockPos -> Set of player UUIDs who have that GUI open
    private static final Map<BlockPos, Set<UUID>> openGuis = new ConcurrentHashMap<>();

    public static void init() {
        // Register GUI tracking packets
        ServerPlayNetworking.registerGlobalReceiver(GUI_OPENED_PACKET, (server, player, handler, buffer, sender) -> {
            BlockPos pos = buffer.readBlockPos();
            server.execute(() -> {
                openGuis.computeIfAbsent(pos, k -> ConcurrentHashMap.newKeySet()).add(player.getUuid());
            });
        });
        
        ServerPlayNetworking.registerGlobalReceiver(GUI_CLOSED_PACKET, (server, player, handler, buffer, sender) -> {
            BlockPos pos = buffer.readBlockPos();
            server.execute(() -> {
                Set<UUID> players = openGuis.get(pos);
                if (players != null) {
                    players.remove(player.getUuid());
                    if (players.isEmpty()) {
                        openGuis.remove(pos);
                    }
                }
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(LEAVE_WAITING_PACKET, (server, player, handler, buffer, sender) -> {
            BlockPos dungeonPortalPos = buffer.readBlockPos();
            server.execute(() -> {
                if (player.getWorld().getBlockEntity(dungeonPortalPos) instanceof DungeonPortalEntity dungeonPortalEntity) {
                    // Check if player is actually in the waiting list
                    if (dungeonPortalEntity.getWaitingUuids().contains(player.getUuid())) {
                        // Remove player from waiting list (use setWaitingUuids to avoid sync issues)
                        List<UUID> waitingList = new ArrayList<>(dungeonPortalEntity.getWaitingUuids());
                        waitingList.remove(player.getUuid());
                        dungeonPortalEntity.setWaitingUuids(waitingList);
                        
                        // Only refund items if player is NOT in creative mode
                        if (!player.isCreative()) {
                            // Get the required items for refund
                            if (dungeonPortalEntity.getDungeon() != null) {
                                Map<String, List<ItemStack>> requiredItemStacksMap = DungeonHelper.getRequiredItemStackList(dungeonPortalEntity.getDungeon());
                                String difficulty = dungeonPortalEntity.getDifficulty();
                                
                                if (requiredItemStacksMap.containsKey(difficulty)) {
                                    List<ItemStack> requiredItems = requiredItemStacksMap.get(difficulty);
                                    
                                    // Add items back to player's inventory
                                    for (ItemStack stack : requiredItems) {
                                        if (stack != null && !stack.isEmpty()) {
                                            player.getInventory().offerOrDrop(stack.copy());
                                        }
                                    }
                                }
                            }
                            player.sendMessage(Text.translatable("text.dungeonz.left_waiting_list_with_refund"), false);
                        } else {
                            // Creative players don't get refunds since they didn't pay anything
                            player.sendMessage(Text.translatable("text.dungeonz.left_waiting_list"), false);
                        }
                        
                        // GUI sync is automatically handled by setWaitingUuids() in the entity
                    }
                }
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(CHANGE_DUNGEON_DIFFICULTY_PACKET, (server, player, handler, buffer, sender) -> {
            BlockPos dungeonPortalPos = buffer.readBlockPos();
            server.execute(() -> {
                if (player.getWorld().getBlockEntity(dungeonPortalPos) != null && player.getWorld().getBlockEntity(dungeonPortalPos) instanceof DungeonPortalEntity dungeonPortalEntity) {

                    if (dungeonPortalEntity.getDungeonPlayerCount() == 0) {
                        List<String> difficulties = dungeonPortalEntity.getDungeon().getDifficultyList();
                        if (dungeonPortalEntity.getDifficulty().equals("")) {
                            dungeonPortalEntity.setDifficulty(difficulties.get(0));
                        } else {
                            int index = difficulties.indexOf(dungeonPortalEntity.getDifficulty()) + 1;
                            if (index >= difficulties.size()) {
                                index = 0;
                            }
                            dungeonPortalEntity.setDifficulty(difficulties.get(index));
                        }
                        dungeonPortalEntity.markDirty();
                        // Updated to sync with all players who have the GUI open (now includes required items)
                        writeS2CSyncScreenPacketToAllViewing(server, dungeonPortalEntity);
                    }
                }
            });
        });
        
        ServerPlayNetworking.registerGlobalReceiver(DUNGEON_TELEPORT_PACKET, (server, player, handler, buffer, sender) -> {
            BlockPos dungeonPortalPos = buffer.readBlockPos();
            Boolean isMinGroupRequired = buffer.readBoolean();
            UUID uuid = isMinGroupRequired ? buffer.readUuid() : null;
            server.execute(() -> {
                DungeonHelper.teleportDungeon(player, dungeonPortalPos, uuid);
            });
        });
        
        ServerPlayNetworking.registerGlobalReceiver(CHANGE_DUNGEON_EFFECTS_PACKET, (server, player, handler, buffer, sender) -> {
            BlockPos dungeonPortalPos = buffer.readBlockPos();
            boolean disableEffects = buffer.readBoolean();
            server.execute(() -> {
                if (player.getWorld().getBlockEntity(dungeonPortalPos) != null && player.getWorld().getBlockEntity(dungeonPortalPos) instanceof DungeonPortalEntity) {
                    DungeonPortalEntity dungeonPortalEntity = (DungeonPortalEntity) player.getWorld().getBlockEntity(dungeonPortalPos);

                    if (dungeonPortalEntity.getDungeonPlayerCount() == 0) {
                        dungeonPortalEntity.setDisableEffects(disableEffects);
                        dungeonPortalEntity.markDirty();
                        // Updated to sync with all players who have the GUI open
                        writeS2CSyncScreenPacketToAllViewing(player.getServer(), dungeonPortalEntity);
                    }
                }
            });
        });
        
        ServerPlayNetworking.registerGlobalReceiver(CHANGE_DUNGEON_PRIVATE_GROUP_PACKET, (server, player, handler, buffer, sender) -> {
            BlockPos dungeonPortalPos = buffer.readBlockPos();
            boolean privateGroup = buffer.readBoolean();
            server.execute(() -> {
                if (player.getWorld().getBlockEntity(dungeonPortalPos) != null && player.getWorld().getBlockEntity(dungeonPortalPos) instanceof DungeonPortalEntity) {
                    DungeonPortalEntity dungeonPortalEntity = (DungeonPortalEntity) player.getWorld().getBlockEntity(dungeonPortalPos);

                    if (dungeonPortalEntity.getDungeonPlayerCount() == 0) {
                        dungeonPortalEntity.setPrivateGroup(privateGroup);
                        dungeonPortalEntity.markDirty();
                        // Updated to sync with all players who have the GUI open
                        writeS2CSyncScreenPacketToAllViewing(player.getServer(), dungeonPortalEntity);
                    }
                }
            });
        });
        
        ServerPlayNetworking.registerGlobalReceiver(SET_DUNGEON_TYPE_PACKET, (server, player, handler, buffer, sender) -> {
            BlockPos dungeonPortalPos = buffer.readBlockPos();
            String dungeonType = buffer.readString();
            String defaultDifficulty = buffer.readString();
            server.execute(() -> {
                if (player.isCreativeLevelTwoOp()) {
                    if (Dungeon.getDungeon(dungeonType) != null) {
                        Dungeon dungeon = Dungeon.getDungeon(dungeonType);
                        if (dungeon.getDifficultyList().contains(defaultDifficulty)) {
                            if (player.getWorld().getBlockEntity(dungeonPortalPos) != null && player.getWorld().getBlockEntity(dungeonPortalPos) instanceof DungeonPortalEntity) {
                                DungeonPortalEntity dungeonPortalEntity = (DungeonPortalEntity) player.getWorld().getBlockEntity(dungeonPortalPos);
                                dungeonPortalEntity.setDungeonType(dungeonType);
                                dungeonPortalEntity.setDifficulty(defaultDifficulty);
                                dungeonPortalEntity.setMaxGroupSize(dungeon.getMaxGroupSize());
                                dungeonPortalEntity.setMinGroupSize(dungeon.getMinGroupSize());
                                dungeonPortalEntity.setRequiredLevel(dungeon.getRequiredLevel());
                                dungeonPortalEntity.markDirty();
                                player.sendMessage(Text.of("Set dungeon type successfully!"), false);
                                // Updated to sync with all players who have the GUI open
                                writeS2CSyncScreenPacketToAllViewing(server, dungeonPortalEntity);
                                return;
                            }
                        } else {
                            player.sendMessage(Text.of("Failed to set dungeon type cause difficulty " + defaultDifficulty + " does not exist in type " + dungeonType + "!"), false);
                        }
                    } else {
                        player.sendMessage(Text.of("Failed to set dungeon type cause " + dungeonType + " does not exist!"), false);
                    }
                }
            });
        });
        
        ServerPlayNetworking.registerGlobalReceiver(SET_GATE_BLOCK_PACKET, (server, player, handler, buffer, sender) -> {
            BlockPos gatePos = buffer.readBlockPos();
            String blockId = buffer.readString();
            String particleId = buffer.readString();
            String unlockItemId = buffer.readString();
            server.execute(() -> {
                if (player.isCreativeLevelTwoOp()) {
                    if (player.getWorld().getBlockEntity(gatePos) != null && player.getWorld().getBlockEntity(gatePos) instanceof DungeonGateEntity) {
                        List<BlockPos> otherDungeonGatesPosList = DungeonGateEntity.getConnectedDungeonGatePosList(player.getWorld(), gatePos);
                        for (int i = 0; i < otherDungeonGatesPosList.size(); i++) {
                            if (player.getWorld().getBlockEntity(otherDungeonGatesPosList.get(i)) != null
                                    && player.getWorld().getBlockEntity(otherDungeonGatesPosList.get(i)) instanceof DungeonGateEntity) {
                                DungeonGateEntity otherDungeonGateEntity = (DungeonGateEntity) player.getWorld().getBlockEntity(otherDungeonGatesPosList.get(i));
                                otherDungeonGateEntity.setBlockId(new Identifier(blockId));
                                otherDungeonGateEntity.setParticleEffectId(particleId);
                                otherDungeonGateEntity.setUnlockItemId(unlockItemId);
                                otherDungeonGateEntity.markDirty();
                            }
                        }
                        writeS2CSyncGatePacket(player, (DungeonGateEntity) player.getWorld().getBlockEntity(gatePos), otherDungeonGatesPosList);
                    }
                }
            });
        });
        
        ServerPlayNetworking.registerGlobalReceiver(SET_DUNGEON_COMPASS_PACKET, (server, player, handler, buffer, sender) -> {
            String dungeonType = buffer.readString();
            server.execute(() -> {
                if (player.getMainHandStack().isOf(ItemInit.DUNGEON_COMPASS) && InventoryHelper.hasRequiredItemStacks(player.getInventory(), ItemInit.REQUIRED_DUNGEON_COMPASS_CALIBRATION_ITEMS)) {
                    InventoryHelper.decrementRequiredItemStacks(player.getInventory(), ItemInit.REQUIRED_DUNGEON_COMPASS_CALIBRATION_ITEMS);
                    DungeonCompassItem.setCompassDungeonStructure((ServerWorld) player.getWorld(), player.getBlockPos(), player.getMainHandStack(), dungeonType);
                }
            });
        });
    }

    // Helper method to clean up disconnected players
    public static void cleanupDisconnectedPlayer(UUID playerUuid) {
        openGuis.values().forEach(playerSet -> playerSet.remove(playerUuid));
        openGuis.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    public static void writeS2CDungeonInfoPacket(ServerPlayerEntity serverPlayerEntity, List<Integer> breakableBlockIdList, List<Integer> placeableBlockIdList, boolean allowElytra) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeIntList(new IntArrayList(breakableBlockIdList));
        buf.writeIntList(new IntArrayList(placeableBlockIdList));
        buf.writeBoolean(allowElytra);
        CustomPayloadS2CPacket packet = new CustomPayloadS2CPacket(DUNGEON_INFO_PACKET, buf);
        serverPlayerEntity.networkHandler.sendPacket(packet);
    }

    // Original method for backward compatibility
    public static void writeS2CSyncScreenPacket(ServerPlayerEntity serverPlayerEntity, DungeonPortalEntity dungeonPortalEntity) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeBlockPos(dungeonPortalEntity.getPos());
        buf.writeString(dungeonPortalEntity.getDifficulty());
        List<UUID> dungeonPlayerUUIDs = dungeonPortalEntity.getDungeonPlayerUuids();
        buf.writeInt(dungeonPlayerUUIDs.size());
        for (UUID uuid : dungeonPlayerUUIDs) {
            buf.writeUuid(uuid);
        }
        List<UUID> waitingUUIDs = dungeonPortalEntity.getWaitingUuids();
        buf.writeInt(waitingUUIDs.size());
        for (UUID uuid : waitingUUIDs) {
            buf.writeUuid(uuid);
        }
        CustomPayloadS2CPacket packet = new CustomPayloadS2CPacket(SYNC_SCREEN_PACKET, buf);
        serverPlayerEntity.networkHandler.sendPacket(packet);
    }

    // New method to sync screen with all players who have the GUI open
    public static void writeS2CSyncScreenPacketToAllViewing(net.minecraft.server.MinecraftServer server, DungeonPortalEntity dungeonPortalEntity) {
        Set<UUID> viewingPlayers = openGuis.get(dungeonPortalEntity.getPos());
        if (viewingPlayers == null || viewingPlayers.isEmpty()) {
            return;
        }

        // Get the required items for the current difficulty
        Map<String, List<ItemStack>> requiredItemStacksMap = DungeonHelper.getRequiredItemStackList(dungeonPortalEntity.getDungeon());
        List<ItemStack> requiredItems = requiredItemStacksMap.getOrDefault(dungeonPortalEntity.getDifficulty(), new ArrayList<>());

        // Create the packet data once
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeBlockPos(dungeonPortalEntity.getPos());
        buf.writeString(dungeonPortalEntity.getDifficulty());
        List<UUID> dungeonPlayerUUIDs = dungeonPortalEntity.getDungeonPlayerUuids();
        buf.writeInt(dungeonPlayerUUIDs.size());
        for (UUID uuid : dungeonPlayerUUIDs) {
            buf.writeUuid(uuid);
        }
        List<UUID> waitingUUIDs = dungeonPortalEntity.getWaitingUuids();
        buf.writeInt(waitingUUIDs.size());
        for (UUID uuid : waitingUUIDs) {
            buf.writeUuid(uuid);
        }
        
        // Add required items to the sync packet
        buf.writeInt(requiredItems.size());
        for (ItemStack stack : requiredItems) {
            buf.writeItemStack(stack);
        }

        // Add DisableEffects and PrivateGroup to the sync packet
        buf.writeBoolean(dungeonPortalEntity.getDisableEffects());
        buf.writeBoolean(dungeonPortalEntity.getPrivateGroup());

        // Add Timer data to the sync packet
        buf.writeBoolean(dungeonPortalEntity.isDungeonTimerActive());
        buf.writeInt(dungeonPortalEntity.getDungeonTimeRemaining());

        CustomPayloadS2CPacket packet = new CustomPayloadS2CPacket(SYNC_SCREEN_PACKET, buf);

        // Send to all viewing players
        for (UUID playerUuid : viewingPlayers) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);
            if (player != null) {
                player.networkHandler.sendPacket(packet);
            }
        }
    }

    public static void writeS2COpenOpScreenPacket(ServerPlayerEntity serverPlayerEntity, @Nullable DungeonPortalEntity dungeonPortalEntity, @Nullable DungeonGateEntity dungeonGateEntity) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        if (dungeonPortalEntity != null) {
            buf.writeString("portal");
            buf.writeBlockPos(dungeonPortalEntity.getPos());
            buf.writeString(dungeonPortalEntity.getDungeonType());
            buf.writeString(dungeonPortalEntity.getDifficulty());
        }
        if (dungeonGateEntity != null) {
            buf.writeString("gate");
            buf.writeBlockPos(dungeonGateEntity.getPos());
            buf.writeString(Registries.BLOCK.getId(dungeonGateEntity.getBlockState().getBlock()).toString());
            buf.writeString(dungeonGateEntity.getParticleEffect() != null ? dungeonGateEntity.getParticleEffect().asString() : "");
            buf.writeString(dungeonGateEntity.getUnlockItem() != null ? Registries.ITEM.getId(dungeonGateEntity.getUnlockItem()).toString() : "");
        }
        CustomPayloadS2CPacket packet = new CustomPayloadS2CPacket(OP_SCREEN_PACKET, buf);
        serverPlayerEntity.networkHandler.sendPacket(packet);
    }

    public static void writeS2COpenCompassScreenPacket(ServerPlayerEntity serverPlayerEntity, String dungeonType) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeString(dungeonType);
        int dungeonCount = DungeonzMain.DUNGEONS.size();
        buf.writeInt(dungeonCount);
        for (int i = 0; i < dungeonCount; i++) {
            buf.writeString(DungeonzMain.DUNGEONS.get(i).getDungeonTypeId());
        }
        CustomPayloadS2CPacket packet = new CustomPayloadS2CPacket(COMPASS_SCREEN_PACKET, buf);
        serverPlayerEntity.networkHandler.sendPacket(packet);
    }

    public static void writeS2CSyncGatePacket(ServerPlayerEntity serverPlayerEntity, DungeonGateEntity dungeonGateEntity, List<BlockPos> dungeonGatesPosList) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeInt(dungeonGatesPosList.size());
        for (int i = 0; i < dungeonGatesPosList.size(); i++) {
            buf.writeBlockPos(dungeonGatesPosList.get(i));
        }
        buf.writeString(Registries.BLOCK.getId(dungeonGateEntity.getBlockState().getBlock()).toString());
        buf.writeString(dungeonGateEntity.getParticleEffect() != null ? dungeonGateEntity.getParticleEffect().asString() : "");
        buf.writeString(dungeonGateEntity.getUnlockItem() != null ? Registries.ITEM.getId(dungeonGateEntity.getUnlockItem()).toString() : "");

        CustomPayloadS2CPacket packet = new CustomPayloadS2CPacket(SYNC_GATE_BLOCK_PACKET, buf);
        serverPlayerEntity.networkHandler.sendPacket(packet);
    }

    public static void writeS2CDungeonTeleportCountdown(ServerPlayerEntity serverPlayerEntity, int countdownTicks) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeInt(countdownTicks);
        CustomPayloadS2CPacket packet = new CustomPayloadS2CPacket(DUNGEON_TELEPORT_COUNTDOWN_PACKET, buf);
        serverPlayerEntity.networkHandler.sendPacket(packet);
    }
}