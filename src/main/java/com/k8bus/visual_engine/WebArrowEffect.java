package com.k8bus.visual_engine;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Efekt pajęczyny — płaski ring COBWEB rozkłada się na ziemi.
 *
 * Wywołaj z enchanta gdy strzała trafia:
 *   WebArrowEffect.apply(target, 60);
 */
public final class WebArrowEffect {

    public static final int DEFAULT_HOLD_TICKS = 60;

    private WebArrowEffect() {}

    public static void apply(LivingEntity target, int holdTicks) {
        if (!(target.level() instanceof ServerLevel level)) return;

        Vec3 ground = target.position();

        SpreadGroundEffect.spawn(
                level, ground,
                Blocks.COBWEB,
                2.2f,   // spreadSize
                0.12f,  // thickness
                5,      // fadeInTicks — szybkie rozkładanie jak rzucona sieć
                holdTicks,
                6       // fadeOutTicks
        );

        target.addEffect(new MobEffectInstance(
                MobEffects.SLOWNESS,
                holdTicks,
                10,
                false, false, false
        ));
    }

    public static void apply(LivingEntity target) {
        apply(target, DEFAULT_HOLD_TICKS);
    }
}
