package com.k8bus.effect;

import com.k8bus.UnnamedDungeons;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;


public class ModEffects {
    public static Holder<MobEffect> NEUROTOXICITY;

    public static void initialize() {
        NEUROTOXICITY = Registry.registerForHolder(
                BuiltInRegistries.MOB_EFFECT,
                Identifier.fromNamespaceAndPath(UnnamedDungeons.MOD_ID, "neurotoxicity"),
                new NeurotoxicityEffect()
        );
    }
}