package net.dungeonz.mixin.misc;

import net.dungeonz.init.DimensionInit;
import net.dungeonz.util.DungeonHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.AbstractWindChargeEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.WindChargeEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractWindChargeEntity.class)
public abstract class WindChargeEntityMixin extends ProjectileEntity {

    public WindChargeEntityMixin(EntityType<? extends ProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "onCollision", at = @At("HEAD"), cancellable = true)
    protected void onCollisionMixin(HitResult hitResult, CallbackInfo info) {
        if (!((Object) this instanceof WindChargeEntity)) {
            return;
        }
        if (this.getWorld().getRegistryKey() == DimensionInit.DUNGEON_WORLD) {
            Entity entity = this.getOwner();
            if (entity instanceof ServerPlayerEntity serverPlayerEntity && DungeonHelper.getCurrentDungeon(serverPlayerEntity) != null && !DungeonHelper.getCurrentDungeon(serverPlayerEntity).isWindChargeAllowed()) {
                this.discard();
                info.cancel();
            }
        }
    }
}
