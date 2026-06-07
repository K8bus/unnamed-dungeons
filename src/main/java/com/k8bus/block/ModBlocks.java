package com.k8bus.block;

import com.k8bus.UnnamedDungeons;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ModBlocks {

    public static Block DUNGEON_GATE;

    public static void initialize() {
        DUNGEON_GATE = Registry.register(
                BuiltInRegistries.BLOCK,
                Identifier.fromNamespaceAndPath(UnnamedDungeons.MOD_ID, "dungeon_gate"),
                new DungeonGateBlock(BlockBehaviour.Properties.of()
                        .strength(-1.0f, 3600000.0f)
                        .noOcclusion()
                        .setId(ResourceKey.create(
                                Registries.BLOCK,
                                Identifier.fromNamespaceAndPath(UnnamedDungeons.MOD_ID, "dungeon_gate")
                        )))
        );

        // Rejestracja itemu dla bloku
        Registry.register(
                BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(UnnamedDungeons.MOD_ID, "dungeon_gate"),
                new BlockItem(DUNGEON_GATE, new Item.Properties()
                        .setId(ResourceKey.create(
                                Registries.ITEM,
                                Identifier.fromNamespaceAndPath(UnnamedDungeons.MOD_ID, "dungeon_gate")
                        )))
        );
    }

}