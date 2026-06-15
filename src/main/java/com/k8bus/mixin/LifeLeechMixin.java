package com.k8bus.mixin;

import com.k8bus.enchantment.ModEnchantments;
import com.k8bus.visual_engine.VisualEngine;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
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
public abstract class LifeLeechMixin {

    @Inject(method = "actuallyHurt", at = @At("TAIL"))
    private void onActuallyHurt(ServerLevel level, DamageSource source, float damageAmount, CallbackInfo ci) {
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity livingAttacker)) return;

        Optional<Holder.Reference<Enchantment>> leechHolder = level
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(ModEnchantments.LIFE_LEECH);

        if (leechHolder.isEmpty()) return;

        int leechLevel = EnchantmentHelper.getEnchantmentLevel(leechHolder.get(), livingAttacker);
        if (leechLevel == 0) return;

        // Life Leech: szansa na heal. 
        // W uproszczeniu dajemy małą szansę na uleczenie przy każdym hicie.
        // Np. 15% szansy na uleczenie o 1.0 HP (pół serca) per poziom.
        if (level.getRandom().nextFloat() < 0.15f) {
            float healAmount = leechLevel * 1.0f;
            livingAttacker.heal(healAmount);
            
            // Visual effect
            VisualEngine.get().play("heal_ring", level, livingAttacker.position());
        }
    }
}
