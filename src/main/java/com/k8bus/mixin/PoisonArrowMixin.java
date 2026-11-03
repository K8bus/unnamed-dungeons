package com.k8bus.mixin;

import com.k8bus.enchantment.ModEnchantments;
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

    private int unnamed_dungeons$bounceCount = 0;

    @Inject(method = "onHitEntity", at = @At("TAIL"))
    private void onArrowHit(EntityHitResult hitResult, CallbackInfo ci) {
        AbstractArrow arrow = (AbstractArrow) (Object) this;
        if (!(arrow.level() instanceof ServerLevel serverLevel))
            return;

        Entity owner = arrow.getOwner();
        if (!(owner instanceof LivingEntity livingOwner))
            return;

        Optional<Holder.Reference<Enchantment>> poisonArrowHolder = serverLevel
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(ModEnchantments.POISON_ARROW);

        Optional<Holder.Reference<Enchantment>> vinesHolder = serverLevel
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(ModEnchantments.VINES);

        Optional<Holder.Reference<Enchantment>> webbingHolder = serverLevel
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(ModEnchantments.WEBBING);

        Optional<Holder.Reference<Enchantment>> ricochetHolder = serverLevel
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(ModEnchantments.RICOCHET);

        if (poisonArrowHolder.isPresent()) {
            int level = EnchantmentHelper.getEnchantmentLevel(poisonArrowHolder.get(), livingOwner);
            if (level > 0) {
                AreaEffectCloud cloud = new AreaEffectCloud(serverLevel, arrow.getX(), arrow.getY(), arrow.getZ());
                cloud.setOwner(livingOwner);
                cloud.setRadius(1.5f + level * 0.5f);
                cloud.setDuration(60 + level * 20);
                cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
                cloud.setWaitTime(0);

                int amplifier = level >= 2 ? 1 : 0;
                cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 60 + level * 20, amplifier));
                serverLevel.addFreshEntity(cloud);

                com.k8bus.vfx.DungeonsVfx.impactBurst(serverLevel, arrow.position(), new net.minecraft.world.phys.Vec3(0, 1, 0), com.k8bus.vfx.VfxTheme.POISON);
            }
        }

        if (hitResult.getEntity() instanceof LivingEntity target) {
            if (vinesHolder.isPresent()) {
                int level = EnchantmentHelper.getEnchantmentLevel(vinesHolder.get(), livingOwner);
                if (level > 0) {
                    com.k8bus.vfx.DungeonsVfx.ensnare(serverLevel, target, com.k8bus.vfx.VfxTheme.VINES, 40 + level * 10);
                }
            }

            if (webbingHolder.isPresent()) {
                int levelEnchant = EnchantmentHelper.getEnchantmentLevel(webbingHolder.get(), livingOwner);
                if (levelEnchant > 0) {
                    com.k8bus.vfx.DungeonsVfx.ensnare(serverLevel, target, com.k8bus.vfx.VfxTheme.WEBBING, 40 + levelEnchant * 10);
                }
            }

            if (ricochetHolder.isPresent()) {
                int levelEnchant = EnchantmentHelper.getEnchantmentLevel(ricochetHolder.get(), livingOwner);
                if (levelEnchant > 0) {
                    if (this.unnamed_dungeons$bounceCount < levelEnchant) {
                        // Find a new target
                        double range = 8.0;
                        java.util.List<LivingEntity> near = serverLevel.getEntitiesOfClass(LivingEntity.class,
                                target.getBoundingBox().inflate(range),
                                e -> e != target && e != livingOwner && e.isAlive());
                        if (!near.isEmpty()) {
                            near.sort(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(target)));
                            LivingEntity nextTarget = near.get(0);

                            // Spawn new arrow heading to next target
                            net.minecraft.world.phys.Vec3 dir = nextTarget.position()
                                     .add(0, nextTarget.getBbHeight() / 2, 0).subtract(arrow.position()).normalize();

                            AbstractArrow newArrow = (AbstractArrow) arrow
                                    .getType()
                                    .create(serverLevel, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                            if (newArrow != null) {
                                newArrow.setPos(arrow.getX(), arrow.getY(), arrow.getZ());
                                newArrow.setOwner(livingOwner);
                                newArrow.setBaseDamage(2.0); // Simple fallback damage
                                newArrow.shoot(dir.x, dir.y, dir.z, 1.5f, 0);
                                ((PoisonArrowMixin) (Object) newArrow).unnamed_dungeons$bounceCount = this.unnamed_dungeons$bounceCount
                                        + 1;

                                serverLevel.addFreshEntity(newArrow);

                                com.k8bus.vfx.DungeonsVfx.chain(serverLevel, arrow.position(), nextTarget.position().add(0, nextTarget.getBbHeight() * 0.5, 0), com.k8bus.vfx.VfxTheme.LIGHTNING);
                            }
                        }
                    }
                }
            }
        }
    }
}