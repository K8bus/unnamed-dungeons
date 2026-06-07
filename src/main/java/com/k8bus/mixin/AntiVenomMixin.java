package com.k8bus.mixin;

import com.k8bus.enchantment.ModEnchantments;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(LivingEntity.class)
public abstract class AntiVenomMixin {

    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"), cancellable = true)
    private void onAddEffect(MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> ci) {
        if (!effect.getEffect().is(MobEffects.POISON)) return;

        LivingEntity self = (LivingEntity)(Object)this;

        Optional<Holder.Reference<Enchantment>> antiVenomHolder = self.level()
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(ModEnchantments.ANTI_VENOM);

        if (antiVenomHolder.isEmpty()) return;

        int level = EnchantmentHelper.getEnchantmentLevel(antiVenomHolder.get(), self);
        if (level == 0) return;

        float[] reductions = {0.25f, 0.40f, 0.55f};
        float reduction = reductions[Math.min(level, 3) - 1];

        int newDuration = (int)(effect.getDuration() * (1.0f - reduction));

        // Zamień efekt na skrócony
        MobEffectInstance reduced = new MobEffectInstance(
                effect.getEffect(),
                newDuration,
                effect.getAmplifier(),
                effect.isAmbient(),
                effect.isVisible(),
                effect.showIcon()
        );

        // Anuluj oryginalny i dodaj skrócony
        ci.cancel();
        ((LivingEntity)(Object)this).forceAddEffect(reduced, null);
    }
}