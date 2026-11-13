package com.k8bus.block;

import com.k8bus.UnnamedDungeons;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class ModBlockTags {

    private ModBlockTags() {}

    /**
     * Tag bloków rozpoznawanych przez silnik jako bramy dungeonu (Dungeon Gate).
     * Pozwala zewnętrznym modom i datapackom dodawać własne bloki bram z własnymi modelami.
     */
    public static final TagKey<Block> DUNGEON_GATES = TagKey.create(
            Registries.BLOCK,
            Identifier.fromNamespaceAndPath(UnnamedDungeons.MOD_ID, "dungeon_gates")
    );
}
