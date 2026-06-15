package com.k8bus.enchantment;

import net.minecraft.core.registries.Registries;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

public class ModEnchantments {
    public static final ResourceKey<Enchantment> DEFLECT = ResourceKey.create(
            Registries.ENCHANTMENT,
            Identifier.fromNamespaceAndPath("unnamed-dungeons", "deflect")
    );

    public static final ResourceKey<Enchantment> ANTI_VENOM = ResourceKey.create(
            Registries.ENCHANTMENT,
            Identifier.fromNamespaceAndPath("unnamed-dungeons", "anti_venom")
    );

    public static final ResourceKey<Enchantment> POISON = ResourceKey.create(
            Registries.ENCHANTMENT,
            Identifier.fromNamespaceAndPath("unnamed-dungeons", "poison")
    );

    public static final ResourceKey<Enchantment> POISON_ARROW = ResourceKey.create(
            Registries.ENCHANTMENT,
            Identifier.fromNamespaceAndPath("unnamed-dungeons", "poison_arrow")
    );

    public static final ResourceKey<Enchantment> NEUROTOXICITY = ResourceKey.create(
            Registries.ENCHANTMENT,
            Identifier.fromNamespaceAndPath("unnamed-dungeons", "neurotoxicity")
    );

    public static final ResourceKey<Enchantment> VINES = ResourceKey.create(
            Registries.ENCHANTMENT,
            Identifier.fromNamespaceAndPath("unnamed-dungeons", "vines")
    );

    public static final ResourceKey<Enchantment> WEBBING = ResourceKey.create(
            Registries.ENCHANTMENT,
            Identifier.fromNamespaceAndPath("unnamed-dungeons", "webbing")
    );

    public static final ResourceKey<Enchantment> JUNGLES_WRATH = ResourceKey.create(
            Registries.ENCHANTMENT,
            Identifier.fromNamespaceAndPath("unnamed-dungeons", "jungles_wrath")
    );

    public static final ResourceKey<Enchantment> LIFE_LEECH = ResourceKey.create(
            Registries.ENCHANTMENT,
            Identifier.fromNamespaceAndPath("unnamed-dungeons", "life_leech")
    );

    public static final ResourceKey<Enchantment> RICOCHET = ResourceKey.create(
            Registries.ENCHANTMENT,
            Identifier.fromNamespaceAndPath("unnamed-dungeons", "ricochet")
    );

    public static final ResourceKey<Enchantment> THUNDERING = ResourceKey.create(
            Registries.ENCHANTMENT,
            Identifier.fromNamespaceAndPath("unnamed-dungeons", "thundering")
    );

    public static final ResourceKey<Enchantment> VOID_STRIKE = ResourceKey.create(
            Registries.ENCHANTMENT,
            Identifier.fromNamespaceAndPath("unnamed-dungeons", "void_strike")
    );

    public static void initialize() {}
}