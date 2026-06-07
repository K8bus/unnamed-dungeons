package com.k8bus.mixin;

import com.k8bus.visual_engine.VisualEngine;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractArrow.class)
public abstract class ArrowTrailMixin {
    // Dodaj to pole wewnątrz Mixinu, żeby pamiętać gdzie strzała była ostatnio
    private Vec3 lastPos = null;

    @Inject(method = "tick", at = @At("TAIL"))
    private void injectArrowVfx(CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (entity.level().isClientSide()) return;

        Vec3 currentPos = entity.position();

        // Jeśli to pierwszy tick, tylko zapisujemy pozycję
        if (lastPos == null) {
            lastPos = currentPos;
            return;
        }

        // Rysujemy smugę tylko, jeśli strzała się ruszyła
        if (entity.getDeltaMovement().lengthSqr() > 0.01) {
            VisualEngine.getInstance().spawnTrailSegment(
                    "sonic_ring_trail", // Użyj modelu, który jest wydłużony
                    (ServerLevel) entity.level(),
                    lastPos,
                    currentPos,
                    new Vector3f(0.5f, 0.5f, 1.0f) // Grubość 0.5
            );
        }

        lastPos = currentPos;
    }
}