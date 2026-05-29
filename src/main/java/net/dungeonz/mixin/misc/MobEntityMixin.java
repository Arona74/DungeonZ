package net.dungeonz.mixin.misc;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.dungeonz.access.BossEntityAccess;
import net.dungeonz.access.DungeonMobAccess;
import net.dungeonz.block.entity.DungeonPortalEntity;
import net.dungeonz.init.BlockInit;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin extends LivingEntity implements BossEntityAccess, DungeonMobAccess {

    @Unique
    private static final Logger DUNGEONZ_LOGGER = LogManager.getLogger("DungeonZ");

    @Unique
    private boolean isDungeonBossEntity = false;
    @Unique
    private boolean dungeonNoLoot = false;
    @Unique
    private BlockPos portalPos = new BlockPos(0, 0, 0);
    @Unique
    private String worldRegistryKey = "";

    public MobEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void writeCustomDataToNbtMixin(NbtCompound nbt, CallbackInfo info) {
        if (this.isDungeonBossEntity) {
            nbt.putBoolean("IsDungeonBossEntity", this.isDungeonBossEntity);
            nbt.putString("WorldRegistryKey", this.worldRegistryKey);
            nbt.putInt("PortalPosX", this.portalPos.getX());
            nbt.putInt("PortalPosY", this.portalPos.getY());
            nbt.putInt("PortalPosZ", this.portalPos.getZ());
        }
        if (this.dungeonNoLoot) {
            nbt.putBoolean("DungeonNoLoot", true);
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void readCustomDataFromNbtMixin(NbtCompound nbt, CallbackInfo info) {
        if (nbt.contains("IsDungeonBossEntity")) {
            this.isDungeonBossEntity = nbt.getBoolean("IsDungeonBossEntity");
            this.worldRegistryKey = nbt.getString("WorldRegistryKey");
            this.portalPos = new BlockPos(nbt.getInt("PortalPosX"), nbt.getInt("PortalPosY"), nbt.getInt("PortalPosZ"));
        }
        if (nbt.contains("DungeonNoLoot")) {
            this.dungeonNoLoot = nbt.getBoolean("DungeonNoLoot");
        }
    }

    @Inject(method = "dropLoot", at = @At("HEAD"), cancellable = true)
    private void dropLootMixin(DamageSource damageSource, boolean causedByPlayer, CallbackInfo info) {
        if (this.dungeonNoLoot) {
            info.cancel();
        }
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        if (!this.getWorld().isClient() && this.isDungeonBossEntity) {
            DUNGEONZ_LOGGER.info("[DungeonZ] Boss onDeath fired via mixin for {} at {} (portal={}, world={})",
                    this.getType().toString(), this.getBlockPos(), this.portalPos, this.worldRegistryKey);
            ServerWorld nonDungeonWorld = getWorld().getServer().getWorld(RegistryKey.of(RegistryKeys.WORLD, Identifier.of(this.worldRegistryKey)));

            if (nonDungeonWorld == null) {
                DUNGEONZ_LOGGER.warn("[DungeonZ] Boss death: overworld '{}' not found - falling back to portal block", this.worldRegistryKey);
                this.getWorld().setBlockState(this.getBlockPos(), BlockInit.DUNGEON_PORTAL.getDefaultState());
            } else if (!(nonDungeonWorld.getBlockEntity(this.portalPos) instanceof DungeonPortalEntity)) {
                DUNGEONZ_LOGGER.warn("[DungeonZ] Boss death: no DungeonPortalEntity at {} in world '{}' (found: {}) - falling back to portal block",
                        this.portalPos, this.worldRegistryKey, nonDungeonWorld.getBlockEntity(this.portalPos));
                this.getWorld().setBlockState(this.getBlockPos(), BlockInit.DUNGEON_PORTAL.getDefaultState());
            } else {
                DUNGEONZ_LOGGER.info("[DungeonZ] Boss death: calling finishDungeon for portal at {}", this.portalPos);
                ((DungeonPortalEntity) nonDungeonWorld.getBlockEntity(this.portalPos)).finishDungeon((ServerWorld) this.getWorld(), this.getBlockPos());
            }
        }
        super.onDeath(damageSource);
    }

    @Override
    public void setBoss(BlockPos portalPos, String worldRegistryKey) {
        this.isDungeonBossEntity = true;
        this.portalPos = portalPos;
        this.worldRegistryKey = worldRegistryKey;
    }

    @Override
    public void setDungeonNoLoot(boolean noLoot) {
        this.dungeonNoLoot = noLoot;
    }

    @Override
    public boolean hasDungeonNoLoot() {
        return this.dungeonNoLoot;
    }

}
