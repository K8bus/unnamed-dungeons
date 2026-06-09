package com.k8bus.visual_engine;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Efekt pnączy — identyczny kształt co Spider Web, inna treść.
 * Płaski ring z GLOW_LICHEN / VINE rozszerza się na ziemi wokół celu.
 *
 * Wywołaj z enchanta:
 *   VineEnsnareEffect.apply(target, 60);
 */
public final class VineEnsnareEffect {

    public static final int DEFAULT_HOLD_TICKS = 60;

    private VineEnsnareEffect() {}

    public static void apply(LivingEntity target, int holdTicks) {
        if (!(target.level() instanceof ServerLevel level)) return;

        Vec3 ground = target.position();

        // GLOW_LICHEN: płaski, wielokierunkowy, lekko świecący zielony —
        // najlepiej odzwierciedla "pnącza rozkładające się na ziemi".
        // Zmień na Blocks.VINE jeśli wolisz klasyczne pnącze.
        SpreadGroundEffect.spawn(
                level, ground,
                Blocks.GLOW_LICHEN,
                2.4f,   // spreadSize — nieco szersze niż web
                0.10f,  // thickness
                7,      // fadeInTicks — wolniejsze rozłożenie niż web
                holdTicks,
                8       // fadeOutTicks
        );


        // Mechaniczny root — identyczny co web
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