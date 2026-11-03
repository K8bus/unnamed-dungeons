package com.k8bus.mixin;

import com.k8bus.effect.ModEffects;
import com.k8bus.enchantment.ModEnchantments;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(LivingEntity.class)
public abstract class NeurotoxicityMixin {

    @Inject(method = "actuallyHurt", at = @At("TAIL"))
    private void onActuallyHurt(ServerLevel level, DamageSource source, float damage, CallbackInfo ci) {
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity livingAttacker)) return;

        LivingEntity self = (LivingEntity)(Object)this;

        Optional<Holder.Reference<Enchantment>> neuroHolder = level
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(ModEnchantments.NEUROTOXICITY);

        if (neuroHolder.isEmpty()) return;

        int enchantLevel = EnchantmentHelper.getEnchantmentLevel(neuroHolder.get(), livingAttacker);
        if (enchantLevel == 0) return;

        // I — amplifier 0, 3s | II — amplifier 0, 6s | III — amplifier 1, 6s
        int amplifier = enchantLevel >= 3 ? 1 : 0;
        int duration = enchantLevel >= 2 ? 120 : 60; // 3s = 60 ticków, 6s = 120 ticków

        self.addEffect(new MobEffectInstance(ModEffects.NEUROTOXICITY, duration, amplifier));
        com.k8bus.vfx.DungeonsVfx.impactBurst(level, self.position().add(0, self.getBbHeight() * 0.5, 0), livingAttacker.getLookAngle(), com.k8bus.vfx.VfxTheme.POISON);
        com.k8bus.vfx.DungeonsVfx.aura(level, self, com.k8bus.vfx.VfxTheme.POISON, duration);
    }
}