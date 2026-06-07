package com.k8bus.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

public class NeurotoxicityEffect extends MobEffect {

    public NeurotoxicityEffect() {
        super(MobEffectCategory.HARMFUL, 0x9ACD32);
    }

    @Override
    public boolean applyEffectTick(ServerLevel serverLevel, LivingEntity entity, int amplifier) {
        entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, amplifier, false, false));
        entity.addEffect(new MobEffectInstance(MobEffects.POISON, 40, amplifier, false, false));
        entity.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 60, 0, false, false));
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return tickCount % 20 == 0;
    }
}