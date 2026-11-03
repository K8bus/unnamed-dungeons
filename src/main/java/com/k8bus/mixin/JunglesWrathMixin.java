package com.k8bus.mixin;

import com.k8bus.enchantment.ModEnchantments;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Optional;

@Mixin(LivingEntity.class)
public abstract class JunglesWrathMixin {

    @ModifyVariable(method = "actuallyHurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float modifyDamage(float originalDamage, net.minecraft.server.level.ServerLevel level, DamageSource source) {
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity livingAttacker)) return originalDamage;

        LivingEntity self = (LivingEntity)(Object)this;

        if (!self.hasEffect(MobEffects.POISON)) return originalDamage;

        Optional<Holder.Reference<Enchantment>> wrathHolder = level
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(ModEnchantments.JUNGLES_WRATH);

        if (wrathHolder.isEmpty()) return originalDamage;

        int levelEnchant = EnchantmentHelper.getEnchantmentLevel(wrathHolder.get(), livingAttacker);
        if (levelEnchant == 0) return originalDamage;

        // Jungle's Wrath: +10% damage per level against poisoned targets
        com.k8bus.vfx.DungeonsVfx.impactBurst(level, self.position().add(0, self.getBbHeight() * 0.5, 0), livingAttacker.getLookAngle(), com.k8bus.vfx.VfxTheme.VINES);
        return originalDamage * (1.0f + (0.1f * levelEnchant));
    }
}
