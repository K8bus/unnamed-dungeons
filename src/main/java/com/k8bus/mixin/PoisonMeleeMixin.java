package com.k8bus.mixin;

import com.k8bus.enchantment.ModEnchantments;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
public abstract class PoisonMeleeMixin {

    @Inject(method = "actuallyHurt", at = @At("TAIL"))
    private void onActuallyHurt(ServerLevel level, DamageSource source, float damage, CallbackInfo ci) {
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity livingAttacker)) return;

        LivingEntity self = (LivingEntity)(Object)this;

        Optional<Holder.Reference<Enchantment>> poisonHolder = level
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(ModEnchantments.POISON);

        if (poisonHolder.isEmpty()) return;

        int poisonLevel = EnchantmentHelper.getEnchantmentLevel(poisonHolder.get(), livingAttacker);
        if (poisonLevel == 0) return;

        // Poziom I — siła 1, 2s | Poziom II — siła 2, 3s | Poziom III — siła 2, 4s
        int amplifier = poisonLevel >= 2 ? 1 : 0;
        int[] durations = {40, 60, 80}; // w tickach
        int duration = durations[Math.min(poisonLevel, 3) - 1];

        self.addEffect(new MobEffectInstance(MobEffects.POISON, duration, amplifier));
    }
}