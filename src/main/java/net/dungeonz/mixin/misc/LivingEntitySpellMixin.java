package net.dungeonz.mixin.misc;

import net.dungeonz.init.AttributeInit;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntitySpellMixin {

    @Unique
    private float dungeonz_pendingSpellScale = 1.0f;

    // Step 1: at the start of damage(), read both arguments and store the scale factor.
    @Inject(method = "damage", at = @At("HEAD"))
    private void dungeonz_captureSpellScale(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        dungeonz_pendingSpellScale = 1.0f;
        Entity directSource = source.getSource();
        Entity attacker = source.getAttacker();
        if (attacker instanceof LivingEntity livingAttacker
                && directSource != null
                && directSource != attacker
                && !(directSource instanceof PersistentProjectileEntity)
                && livingAttacker.getAttributes().hasAttribute(AttributeInit.SPELL_POWER)) {
            double spellPower = livingAttacker.getAttributeValue(AttributeInit.SPELL_POWER);
            if (spellPower != 1.0) {
                dungeonz_pendingSpellScale = (float) spellPower;
            }
        }
    }

    // Step 2: when damage() calls applyDamage() internally, scale the amount.
    @ModifyArg(
        method = "damage",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;applyDamage(Lnet/minecraft/entity/damage/DamageSource;F)V"),
        index = 1
    )
    private float dungeonz_applySpellScale(float amount) {
        if (dungeonz_pendingSpellScale != 1.0f) {
            float scale = dungeonz_pendingSpellScale;
            dungeonz_pendingSpellScale = 1.0f;
            return amount * scale;
        }
        return amount;
    }

}
