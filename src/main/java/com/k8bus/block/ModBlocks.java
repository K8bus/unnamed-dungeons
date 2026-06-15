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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ModBlocks {

    public static Block DUNGEON_GATE;
    public static Block WOODEN_DUNGEON_CHEST;
    public static Block HERO_CHEST;
    public static Block ELDRITCH_CHEST;
    public static Block WALLET_DEPOSIT;

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
        WOODEN_DUNGEON_CHEST = registerBlockAndItem("wooden_dungeon_chest", new WoodenDungeonChestBlock(BlockBehaviour.Properties.of()
                .strength(2.0f)
                .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(UnnamedDungeons.MOD_ID, "wooden_dungeon_chest")))
        ));
        HERO_CHEST = registerBlockAndItem("hero_chest", new HeroChestBlock(BlockBehaviour.Properties.of()
                .strength(3.0f)
                .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(UnnamedDungeons.MOD_ID, "hero_chest")))
        ));
        ELDRITCH_CHEST = registerBlockAndItem("eldritch_chest", new EldritchChestBlock(BlockBehaviour.Properties.of()
                .strength(5.0f)
                .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(UnnamedDungeons.MOD_ID, "eldritch_chest")))
        ));
        WALLET_DEPOSIT = registerBlockAndItem("wallet_deposit", new WalletDepositBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(UnnamedDungeons.MOD_ID, "wallet_deposit")))
        ));
    }

    private static Block registerBlockAndItem(String name, Block block) {
        Registry.register(
                BuiltInRegistries.BLOCK,
                Identifier.fromNamespaceAndPath(UnnamedDungeons.MOD_ID, name),
                block
        );
        Registry.register(
                BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(UnnamedDungeons.MOD_ID, name),
                new BlockItem(block, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(UnnamedDungeons.MOD_ID, name))))
        );
        return block;
    }

}