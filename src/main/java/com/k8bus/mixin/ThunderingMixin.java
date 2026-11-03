package com.k8bus.mixin;

import com.k8bus.enchantment.ModEnchantments;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(LivingEntity.class)
public abstract class ThunderingMixin {

    @Inject(method = "actuallyHurt", at = @At("TAIL"))
    private void onActuallyHurt(ServerLevel level, DamageSource source, float damageAmount, CallbackInfo ci) {
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity livingAttacker)) return;

        LivingEntity target = (LivingEntity) (Object) this;

        Optional<Holder.Reference<Enchantment>> thunderingHolder = level
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(ModEnchantments.THUNDERING);

        if (thunderingHolder.isEmpty()) return;

        int thunderingLevel = EnchantmentHelper.getEnchantmentLevel(thunderingHolder.get(), livingAttacker);
        if (thunderingLevel == 0) return;

        if (level.getRandom().nextFloat() < 0.20f) { // 20% szansy
            LightningBolt lightning = EntityTypes.LIGHTNING_BOLT.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            if (lightning != null) {
                lightning.setPos(target.getX(), target.getY(), target.getZ());
                lightning.setVisualOnly(true); // Wizualny piorun
                level.addFreshEntity(lightning);

                // AoE Damage
                float aoeDamage = thunderingLevel * 3.0f;
                java.util.List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(5.0), e -> e != livingAttacker && e != target && e.isAlive());
                for (LivingEntity e : near) {
                    e.hurt(level.damageSources().lightningBolt(), aoeDamage);
                    com.k8bus.vfx.DungeonsVfx.chain(level, target.position().add(0, 0.5, 0), e.position().add(0, 0.5, 0), com.k8bus.vfx.VfxTheme.LIGHTNING);
                }

                com.k8bus.vfx.DungeonsVfx.impactBurst(level, target.position().add(0, 0.5, 0), new net.minecraft.world.phys.Vec3(0, 1, 0), com.k8bus.vfx.VfxTheme.LIGHTNING);
                com.k8bus.vfx.DungeonsVfx.groundRing(level, target.position(), 2.8f, com.k8bus.vfx.VfxTheme.LIGHTNING);
            }
        }
    }
}
