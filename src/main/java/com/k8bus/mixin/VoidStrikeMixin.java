package com.k8bus.mixin;

import com.k8bus.enchantment.ModEnchantments;
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
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(LivingEntity.class)
public abstract class VoidStrikeMixin {

    private long unnamed_dungeons$voidStrikeStartTime = 0;

    @ModifyVariable(method = "actuallyHurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float applyVoidStrikeBonus(float originalDamage, ServerLevel level, DamageSource source) {
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity livingAttacker)) return originalDamage;

        LivingEntity target = (LivingEntity) (Object) this;

        Optional<Holder.Reference<Enchantment>> voidHolder = level
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(ModEnchantments.VOID_STRIKE);

        if (voidHolder.isEmpty()) return originalDamage;

        int enchantLevel = EnchantmentHelper.getEnchantmentLevel(voidHolder.get(), livingAttacker);
        if (enchantLevel == 0) return originalDamage;

        if (this.unnamed_dungeons$voidStrikeStartTime > 0) {
            long diff = level.getGameTime() - this.unnamed_dungeons$voidStrikeStartTime;
            diff = Math.min(diff, 100);
            
            // +100% per 10 ticków (0.5s), max zależny od levelu
            float maxBonus = enchantLevel * 2.0f; // lvl 1 -> 200%, lvl 2 -> 400%, lvl 3 -> 600%
            float bonus = Math.min((diff / 10.0f) * 1.0f, maxBonus);

            this.unnamed_dungeons$voidStrikeStartTime = 0;
            
            return originalDamage * (1.0f + bonus);
        }

        return originalDamage;
    }

    @Inject(method = "actuallyHurt", at = @At("TAIL"))
    private void applyVoidStrikeTag(ServerLevel level, DamageSource source, float damageAmount, CallbackInfo ci) {
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity livingAttacker)) return;

        LivingEntity target = (LivingEntity) (Object) this;

        Optional<Holder.Reference<Enchantment>> voidHolder = level
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(ModEnchantments.VOID_STRIKE);

        if (voidHolder.isEmpty()) return;

        int enchantLevel = EnchantmentHelper.getEnchantmentLevel(voidHolder.get(), livingAttacker);
        if (enchantLevel == 0) return;

        this.unnamed_dungeons$voidStrikeStartTime = level.getGameTime();
    }
}
