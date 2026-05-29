package net.dungeonz.compat;

import net.dungeonz.DungeonzMain;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;

/**
 * Optional Trinkets integration using reflection to avoid compile-time
 * dependencies on Trinkets and its transitive dependency Cardinal Components.
 *
 * Called only when DungeonzMain.isTrinketsLoaded is true.
 */
public class TrinketsCompat {

    // Each Object[] stores: [String group, String slotName, int index, ItemStack copy]
    private static final Map<UUID, List<Object[]>> SNAPSHOTS = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static void snapshotTrinkets(ServerPlayerEntity player) {
        DungeonzMain.LOGGER.info("[TrinketsCompat] snapshotTrinkets called for {}", player.getName().getString());
        try {
            Class<?> api = Class.forName("dev.emi.trinkets.api.TrinketsApi");
            Optional<Object> opt = (Optional<Object>) api
                    .getMethod("getTrinketComponent", net.minecraft.entity.LivingEntity.class)
                    .invoke(null, player);
            if (!opt.isPresent()) {
                DungeonzMain.LOGGER.info("[TrinketsCompat] No trinket component for {}", player.getName().getString());
                return;
            }
            Object component = opt.get();

            // getInventory() → Map<String, Map<String, TrinketInventory>>
            Map<String, Map<String, Inventory>> inventoryMap = (Map<String, Map<String, Inventory>>)
                    component.getClass().getMethod("getInventory").invoke(component);

            List<Object[]> snap = new ArrayList<>();
            for (Map.Entry<String, Map<String, Inventory>> groupEntry : inventoryMap.entrySet()) {
                for (Map.Entry<String, Inventory> slotEntry : groupEntry.getValue().entrySet()) {
                    Inventory inv = slotEntry.getValue();
                    for (int i = 0; i < inv.size(); i++) {
                        ItemStack stack = inv.getStack(i);
                        if (!stack.isEmpty()) {
                            DungeonzMain.LOGGER.info("[TrinketsCompat] Snapshotting {}/{}/{} = {}", groupEntry.getKey(), slotEntry.getKey(), i, stack);
                            snap.add(new Object[]{ groupEntry.getKey(), slotEntry.getKey(), i, stack.copy() });
                        }
                    }
                }
            }

            DungeonzMain.LOGGER.info("[TrinketsCompat] Snapshotted {} items for {}", snap.size(), player.getName().getString());
            if (!snap.isEmpty()) {
                SNAPSHOTS.put(player.getUuid(), snap);
                // Clear slots so Trinkets finds nothing to drop during onDeath
                int cleared = 0;
                for (Object[] entry : snap) {
                    Map<String, Inventory> groupMap = inventoryMap.get((String) entry[0]);
                    if (groupMap == null) continue;
                    Inventory inv = groupMap.get((String) entry[1]);
                    if (inv == null) continue;
                    inv.setStack((int) entry[2], ItemStack.EMPTY);
                    cleared++;
                }
                DungeonzMain.LOGGER.info("[TrinketsCompat] Cleared {} slots", cleared);
            }
        } catch (Exception e) {
            DungeonzMain.LOGGER.warn("[TrinketsCompat] Failed to snapshot trinkets for {}", player.getName().getString(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public static void restoreTrinkets(ServerPlayerEntity newPlayer, UUID oldPlayerUuid) {
        DungeonzMain.LOGGER.info("[TrinketsCompat] restoreTrinkets called for {} (old UUID {})", newPlayer.getName().getString(), oldPlayerUuid);
        List<Object[]> snap = SNAPSHOTS.remove(oldPlayerUuid);
        if (snap == null || snap.isEmpty()) {
            DungeonzMain.LOGGER.info("[TrinketsCompat] No snapshot found for {}", oldPlayerUuid);
            return;
        }
        DungeonzMain.LOGGER.info("[TrinketsCompat] Restoring {} items to {}", snap.size(), newPlayer.getName().getString());
        try {
            Class<?> api = Class.forName("dev.emi.trinkets.api.TrinketsApi");
            Optional<Object> opt = (Optional<Object>) api
                    .getMethod("getTrinketComponent", net.minecraft.entity.LivingEntity.class)
                    .invoke(null, newPlayer);
            if (!opt.isPresent()) {
                DungeonzMain.LOGGER.warn("[TrinketsCompat] No trinket component on new player {}", newPlayer.getName().getString());
                return;
            }
            Object component = opt.get();

            Map<String, Map<String, Inventory>> inventory = (Map<String, Map<String, Inventory>>)
                    component.getClass().getMethod("getInventory").invoke(component);

            int restored = 0;
            for (Object[] entry : snap) {
                Map<String, Inventory> groupMap = inventory.get((String) entry[0]);
                if (groupMap == null) { DungeonzMain.LOGGER.warn("[TrinketsCompat] Restore: group not found: {}", entry[0]); continue; }
                Inventory inv = groupMap.get((String) entry[1]);
                if (inv == null) { DungeonzMain.LOGGER.warn("[TrinketsCompat] Restore: slot not found: {}/{}", entry[0], entry[1]); continue; }
                inv.setStack((int) entry[2], (ItemStack) entry[3]);
                DungeonzMain.LOGGER.info("[TrinketsCompat] Restored {}/{}/{} = {}", entry[0], entry[1], entry[2], entry[3]);
                restored++;
            }
            DungeonzMain.LOGGER.info("[TrinketsCompat] Restored {}/{} items to {}", restored, snap.size(), newPlayer.getName().getString());
        } catch (Exception e) {
            DungeonzMain.LOGGER.warn("[TrinketsCompat] Failed to restore trinkets for {}", newPlayer.getName().getString(), e);
        }
    }
}
