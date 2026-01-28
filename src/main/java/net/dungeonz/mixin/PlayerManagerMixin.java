package net.dungeonz.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.asm.mixin.injection.At;

import net.dungeonz.access.ServerPlayerAccess;
import net.dungeonz.block.entity.DungeonPortalEntity;
import net.dungeonz.dungeon.Dungeon;
import net.dungeonz.init.DimensionInit;
import net.dungeonz.network.DungeonServerPacket;
import net.dungeonz.util.DungeonHelper;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {

    @Inject(method = "onPlayerConnect", at = @At("TAIL"))
    private void onPlayerConnectMixin(ClientConnection connection, ServerPlayerEntity player, CallbackInfo info) {
        if (player.getWorld().getRegistryKey() == DimensionInit.DUNGEON_WORLD) {
            if (DungeonHelper.getCurrentDungeon(player) != null && DungeonHelper.getDungeonPortalEntity(player).getDungeonPlayerUuids().contains(player.getUuid())
                    && !DungeonHelper.getDungeonPortalEntity(player).isOnCooldown((int) player.getWorld().getTime())) {
                Dungeon dungeon = DungeonHelper.getCurrentDungeon(player);
                DungeonServerPacket.writeS2CDungeonInfoPacket(player, dungeon.getBreakableBlockIdList(), dungeon.getplaceableBlockIdList(), dungeon.isElytraAllowed());
            } else {
                DungeonHelper.teleportOutOfDungeon(player);
            }
        }
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void removeMixin(ServerPlayerEntity player, CallbackInfo info) {
        if (player.getWorld().getRegistryKey() == DimensionInit.DUNGEON_WORLD && DungeonHelper.getCurrentDungeon(player) != null) {
            DungeonPortalEntity dungeonPortalEntity = DungeonHelper.getDungeonPortalEntity(player);
            if (dungeonPortalEntity != null) {
                dungeonPortalEntity.getDungeonPlayerUuids().remove(player.getUuid());
                if (dungeonPortalEntity.getDungeonPlayerCount() == 0) {
                    dungeonPortalEntity.setCooldownTime(dungeonPortalEntity.getDungeon().getCooldown() + (int) player.getWorld().getTime());
                }
                dungeonPortalEntity.markDirty();
            }
            ServerWorld oldWorld = ((ServerPlayerAccess) player).getOldServerWorld();
            if (oldWorld != null) {
                BlockPos spawnPos = ((ServerPlayerAccess) player).getDungeonSpawnBlockPos();
                player.setPosition(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                player.setServerWorld(oldWorld);
            }
        }
    }

    @Inject(method = "respawnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;onPlayerRespawned(Lnet/minecraft/server/network/ServerPlayerEntity;)V"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void respawnPlayerMixin(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfoReturnable<ServerPlayerEntity> info, BlockPos blockPos, float f, boolean bl, ServerWorld serverWorld,
            Optional<Vec3d> optional, ServerWorld serverWorld2, ServerPlayerEntity serverPlayerEntity) {
        if (!alive && oldPlayer.getWorld().getRegistryKey() == DimensionInit.DUNGEON_WORLD && DungeonHelper.getDungeonPortalEntity(oldPlayer) != null) {
            DungeonPortalEntity dungeonPortalEntity = DungeonHelper.getDungeonPortalEntity(oldPlayer);
            if (!dungeonPortalEntity.getDungeon().isRespawnAllowed()) {
                dungeonPortalEntity.getDungeonPlayerUuids().remove(oldPlayer.getUuid());
                dungeonPortalEntity.addDeadDungeonPlayerUuids(serverPlayerEntity.getUuid());
                if (dungeonPortalEntity.getDungeonPlayerCount() == 0) {
                    dungeonPortalEntity.setCooldownTime(dungeonPortalEntity.getDungeon().getCooldown() + (int) serverWorld.getTime());
                }
                dungeonPortalEntity.markDirty();
            }
        }
    }

    @ModifyVariable(method = "respawnPlayer", at = @At(value = "INVOKE", target = "Ljava/util/Optional;isPresent()Z", ordinal = 0), ordinal = 0)
    private Optional<Vec3d> respawnPlayerMixin(Optional<Vec3d> original, ServerPlayerEntity oldPlayer, boolean alive) {
        if (!alive && oldPlayer.getWorld().getRegistryKey() == DimensionInit.DUNGEON_WORLD && DungeonHelper.getDungeonPortalEntity(oldPlayer) != null
                && DungeonHelper.getDungeonPortalEntity(oldPlayer).getDungeon().isRespawnAllowed()) {
            BlockPos pos = DungeonHelper.getDungeonPortalEntity(oldPlayer).getPos();
            return Optional.of(new Vec3d(pos.getX() * 16, 100, pos.getZ() * 16));
        }
        return original;
    }

    @ModifyVariable(method = "respawnPlayer", at = @At(value = "INVOKE", target = "Ljava/util/Optional;isPresent()Z", ordinal = 0), ordinal = 0)
    private BlockPos respawnPlayerMixin(BlockPos original, ServerPlayerEntity oldPlayer, boolean alive) {
        if (!alive && oldPlayer.getWorld().getRegistryKey() == DimensionInit.DUNGEON_WORLD && DungeonHelper.getDungeonPortalEntity(oldPlayer) != null
                && DungeonHelper.getDungeonPortalEntity(oldPlayer).getDungeon().isRespawnAllowed()) {
            BlockPos pos = DungeonHelper.getDungeonPortalEntity(oldPlayer).getPos();
            return new BlockPos(pos.getX() * 16, 100, pos.getZ() * 16);
        }
        return original;
    }

    @ModifyVariable(method = "respawnPlayer", at = @At(value = "FIELD", target = "Lnet/minecraft/server/PlayerManager;server:Lnet/minecraft/server/MinecraftServer;", ordinal = 2), ordinal = 1)
    private ServerWorld respawnPlayerMixin(ServerWorld original, ServerPlayerEntity oldPlayer, boolean alive) {
        if (!alive && oldPlayer.getWorld().getRegistryKey() == DimensionInit.DUNGEON_WORLD && DungeonHelper.getDungeonPortalEntity(oldPlayer) != null
                && DungeonHelper.getDungeonPortalEntity(oldPlayer).getDungeon().isRespawnAllowed()) {
            return oldPlayer.getServerWorld();
        }
        return original;
    }

}
