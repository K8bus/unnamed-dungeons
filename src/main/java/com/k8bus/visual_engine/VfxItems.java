package com.k8bus.visual_engine;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public final class VfxItems {

    private static final ResourceKey<Item> VFX_MARKER_KEY = ResourceKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath("unnamed-dungeons", "vfx_marker")
    );

    public static final Item VFX_MARKER = Registry.register(
            BuiltInRegistries.ITEM,
            VFX_MARKER_KEY,
            new Item(new Item.Properties().setId(VFX_MARKER_KEY))
    );

    public static void register() {}

    private VfxItems() {}
}
