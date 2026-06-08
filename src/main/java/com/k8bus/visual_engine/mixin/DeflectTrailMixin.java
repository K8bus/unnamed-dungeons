package com.k8bus.visual_engine.mixin;

import com.k8bus.visual_engine.DeflectTrail;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks into AbstractArrow.tick() — co tick sprawdza czy strzała
 * jest oznaczona jako odbita i jeśli tak, spawnuje segment smugi.
 *
 * Żeby smuga się pojawiła, enchant musi wywołać:
 *   DeflectTrail.mark(arrow);
 */
@Mixin(AbstractArrow.class)
public abstract class DeflectTrailMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        AbstractArrow self = (AbstractArrow)(Object) this;

        // Tylko server-side, tylko oznaczone strzały
        if (self.level().isClientSide()) return;
        if (!DeflectTrail.isMarked(self)) return;

        DeflectTrail.tick(self);
    }
}