package net.dungeonz.mixin.misc;

import com.llamalad7.mixinextras.sugar.Local;
import net.dungeonz.init.AttributeInit;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public class LivingEntitySpellMixin {

    // Scale damage when: a mob with spell_power dealt damage indirectly (via a spell/projectile
    // entity that is not itself an arrow or trident). Covers evoker fangs, blaze fireballs,
    // ghast fireballs, wither skulls, shulker bullets, witch potions, and any modded equivalent.
    @ModifyVariable(method = "damage", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float dungeonz_scaleSpellDamage(float amount, @Local(argsOnly = true) DamageSource source) {
        Entity directSource = source.getSource();
        Entity attacker = source.getAttacker();
        if (attacker instanceof LivingEntity livingAttacker
                && directSource != null
                && directSource != attacker
                && !(directSource instanceof PersistentProjectileEntity)
                && livingAttacker.getAttributes().hasAttribute(AttributeInit.SPELL_POWER)) {
            double spellPower = livingAttacker.getAttributeValue(AttributeInit.SPELL_POWER);
            if (spellPower != 1.0) {
                return (float) (amount * spellPower);
            }
        }
        return amount;
    }

}
