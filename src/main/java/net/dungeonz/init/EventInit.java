package net.dungeonz.init;

import net.dungeonz.DungeonzMain;
import net.dungeonz.access.ClientPlayerAccess;
import net.dungeonz.access.ServerPlayerAccess;
import net.dungeonz.compat.TrinketsCompat;
import net.dungeonz.util.DungeonHelper;
import net.fabricmc.fabric.api.entity.event.v1.EntityElytraEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

public class EventInit {

    public static void init() {
        EntityElytraEvents.ALLOW.register((entity) -> {
            if (entity instanceof PlayerEntity) {
                PlayerEntity playerEntity = (PlayerEntity) entity;
                if (playerEntity != null && !playerEntity.isCreative() && playerEntity.getWorld().getRegistryKey() == DimensionInit.DUNGEON_WORLD) {
                    if (!playerEntity.getWorld().isClient()) {
                        if (DungeonHelper.getCurrentDungeon((ServerPlayerEntity) playerEntity) != null) {
                            return DungeonHelper.getCurrentDungeon((ServerPlayerEntity) playerEntity).isElytraAllowed();
                        }
                    } else {
                        return ((ClientPlayerAccess) playerEntity).isElytraAllowed();
                    }
                }
            }
            return true;
        });
        if (DungeonzMain.isTrinketsLoaded) {
            ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
                if (entity instanceof ServerPlayerEntity player
                        && player.getWorld().getRegistryKey() == DimensionInit.DUNGEON_WORLD
                        && DungeonHelper.getCurrentDungeon(player) != null
                        && DungeonHelper.getCurrentDungeon(player).isKeepInventory()) {
                    TrinketsCompat.snapshotTrinkets(player);
                }
                return true;
            });
        }
        ServerPlayerEvents.COPY_FROM.register((ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer, boolean alive) -> {
            if (((ServerPlayerAccess) oldPlayer).getOldServerWorld() != null) {
                ((ServerPlayerAccess) newPlayer).setDungeonInfo(((ServerPlayerAccess) oldPlayer).getOldServerWorld(), ((ServerPlayerAccess) oldPlayer).getDungeonPortalBlockPos(),
                        ((ServerPlayerAccess) oldPlayer).getDungeonSpawnBlockPos());
            }
            if (oldPlayer.getWorld().getRegistryKey() == DimensionInit.DUNGEON_WORLD && DungeonHelper.getCurrentDungeon(oldPlayer) != null
                    && DungeonHelper.getCurrentDungeon(oldPlayer).isKeepInventory()) {
                newPlayer.getInventory().clone(oldPlayer.getInventory());
            }
        });
        if (DungeonzMain.isTrinketsLoaded) {
            ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
                TrinketsCompat.restoreTrinkets(newPlayer, oldPlayer.getUuid());
            });
        }
    }

}
