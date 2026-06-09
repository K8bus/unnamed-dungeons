package com.k8bus.mixin;

import com.k8bus.enchantment.ModEnchantments;
import com.k8bus.visual_engine.PyramidEnsnareEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(AbstractArrow.class)
public abstract class PoisonArrowMixin {

    @Inject(method = "onHitEntity", at = @At("TAIL"))
    private void onArrowHit(EntityHitResult hitResult, CallbackInfo ci) {
        AbstractArrow arrow = (AbstractArrow)(Object)this;
        if (!(arrow.level() instanceof ServerLevel serverLevel)) return;

        Entity owner = arrow.getOwner();
        if (!(owner instanceof LivingEntity livingOwner)) return;

        Optional<Holder.Reference<Enchantment>> poisonArrowHolder = serverLevel
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(ModEnchantments.POISON_ARROW);

        if (poisonArrowHolder.isEmpty()) return;

        int level = EnchantmentHelper.getEnchantmentLevel(poisonArrowHolder.get(), livingOwner);
        if (level == 0) return;

        // Stwórz chmurę trucizny w miejscu trafienia
        AreaEffectCloud cloud = new AreaEffectCloud(serverLevel, arrow.getX(), arrow.getY(), arrow.getZ());
        cloud.setOwner(livingOwner);
        cloud.setRadius(1.5f + level * 0.5f); // większa chmura per poziom
        cloud.setDuration(60 + level * 20);   // dłuższa per poziom
        cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration()); // kurczy się
        cloud.setWaitTime(0);

        int amplifier = level >= 2 ? 1 : 0;
        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 60 + level * 20, amplifier));

        serverLevel.addFreshEntity(cloud);

        // Pnącza — ostrosłup zaciska się na trafionym
        if (hitResult.getEntity() instanceof LivingEntity target) {
            PyramidEnsnareEffect.apply(target, 40 + level * 10);
        }
    }
}