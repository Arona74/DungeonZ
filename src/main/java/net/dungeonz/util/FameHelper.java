package net.dungeonz.util;

import java.lang.reflect.Method;
import java.util.UUID;

import net.dungeonz.DungeonzMain;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class FameHelper {

    private static Method addFamePowerMethod = null;
    private static boolean methodLookupFailed = false;

    /**
     * Attempts to grant fame power to a player's faction.
     * This method safely handles the case where Factions mod is not loaded.
     *
     * @param player The player to grant fame to
     * @param amount The amount of fame to grant
     * @return true if fame was successfully granted, false otherwise
     */
    public static boolean grantFamePower(ServerPlayerEntity player, int amount) {
        if (!DungeonzMain.isFactionsLoaded || amount <= 0) {
            return false;
        }

        try {
            // Use reflection to call FactionsMod.addFamePower(UUID, int)
            if (addFamePowerMethod == null && !methodLookupFailed) {
                try {
                    Class<?> factionsModClass = Class.forName("io.icker.factions.FactionsMod");
                    addFamePowerMethod = factionsModClass.getMethod("addFamePower", UUID.class, int.class);
                } catch (ClassNotFoundException | NoSuchMethodException e) {
                    methodLookupFailed = true;
                    DungeonzMain.LOGGER.warn("Factions mod API not found: {}", e.getMessage());
                    return false;
                }
            }

            if (addFamePowerMethod != null) {
                Object result = addFamePowerMethod.invoke(null, player.getUuid(), amount);
                int added = (Integer) result;
                return added > 0;
            }
        } catch (Exception e) {
            DungeonzMain.LOGGER.debug("Failed to grant fame power: {}", e.getMessage());
        }

        return false;
    }

    /**
     * Sends a fame reward notification to the player.
     *
     * @param player          The player to notify
     * @param amount          The amount of fame earned
     * @param factionReceived Whether the faction actually received the fame
     */
    public static void sendFameNotification(ServerPlayerEntity player, int amount, boolean factionReceived) {
        if (factionReceived) {
            player.sendMessage(
                    Text.translatable("text.dungeonz.fame_reward_gained", amount)
                            .formatted(Formatting.GOLD),
                    false
            );
        }
    }
}
