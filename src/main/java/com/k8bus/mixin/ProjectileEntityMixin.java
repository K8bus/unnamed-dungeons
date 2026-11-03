package com.k8bus.mixin;

import com.k8bus.enchantment.ModEnchantments;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(Entity.class)
public abstract class ProjectileEntityMixin {

    @Inject(method = "deflection", at = @At("HEAD"), cancellable = true)
    private void onDeflection(Projectile projectile, CallbackInfoReturnable<ProjectileDeflection> ci) {
        Entity self = (Entity)(Object)this;
        if (!(self instanceof LivingEntity living)) return;

        Optional<Holder.Reference<Enchantment>> deflectHolder = living.level()
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(ModEnchantments.DEFLECT);

        if (deflectHolder.isEmpty()) return;

        int deflectLevel = EnchantmentHelper.getEnchantmentLevel(deflectHolder.get(), living);
        if (deflectLevel == 0) return;

        float[] chances = {0.25f, 0.40f, 0.55f};
        float chance = chances[Math.min(deflectLevel, 3) - 1];

        if (living.getRandom().nextFloat() < chance) {
            ci.setReturnValue((projectile1 , entity, random) -> {
                Vec3 velocity = projectile1.getDeltaMovement();
                // Losowy kąt boczny 60-120 stopni
                double angle = (random.nextDouble() * 60 + 60) * (random.nextBoolean() ? 1 : -1);
                double radians = Math.toRadians(angle);
                double cos = Math.cos(radians);
                double sin = Math.sin(radians);
                // Obrót wektora poziomo
                double newX = velocity.x * cos - velocity.z * sin;
                double newZ = velocity.x * sin + velocity.z * cos;
                projectile1.setDeltaMovement(new Vec3(newX, velocity.y * 0.5, newZ));
                projectile1.needsSync = true;

                if (projectile1.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    com.k8bus.vfx.DungeonsVfx.projectileTrail(serverLevel, projectile1, com.k8bus.vfx.VfxTheme.DEFLECT);
                    com.k8bus.vfx.DungeonsVfx.impactBurst(serverLevel, projectile1.position(), projectile1.getDeltaMovement(), com.k8bus.vfx.VfxTheme.DEFLECT);
                }
            });
        }
    }
}