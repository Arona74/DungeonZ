package net.dungeonz.mixin.misc;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.dungeonz.access.BossEntityAccess;
import net.dungeonz.access.DungeonMobAccess;
import net.dungeonz.block.entity.DungeonPortalEntity;
import net.dungeonz.init.AttributeInit;
import net.dungeonz.init.BlockInit;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
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

    @Inject(method = "createMobAttributes", at = @At("RETURN"), cancellable = true)
    private static void dungeonz_addSpellPower(CallbackInfoReturnable<DefaultAttributeContainer.Builder> cir) {
        cir.setReturnValue(cir.getReturnValue().add(AttributeInit.SPELL_POWER));
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
            ServerWorld nonDungeonWorld = getWorld().getServer().getWorld(RegistryKey.of(RegistryKeys.WORLD, new Identifier(this.worldRegistryKey)));

            if (nonDungeonWorld != null && nonDungeonWorld.getBlockEntity(this.portalPos) != null && nonDungeonWorld.getBlockEntity(this.portalPos) instanceof DungeonPortalEntity) {
                ((DungeonPortalEntity) nonDungeonWorld.getBlockEntity(this.portalPos)).finishDungeon((ServerWorld) this.getWorld(), this.getBlockPos());
            } else {
                this.getWorld().setBlockState(this.getBlockPos(), BlockInit.DUNGEON_PORTAL.getDefaultState());
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
