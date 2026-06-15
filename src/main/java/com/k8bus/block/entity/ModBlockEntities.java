package com.k8bus.block.entity;

import com.k8bus.UnnamedDungeons;
import com.k8bus.block.ModBlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntities {

    public static BlockEntityType<DungeonGateBlockEntity> DUNGEON_GATE;
    public static BlockEntityType<WalletDepositBlockEntity> WALLET_DEPOSIT;

    public static void initialize() {
        DUNGEON_GATE = FabricBlockEntityTypeBuilder
                .create(DungeonGateBlockEntity::new, ModBlocks.DUNGEON_GATE)
                .build();

        Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(UnnamedDungeons.MOD_ID, "dungeon_gate"),
                DUNGEON_GATE
        );

        WALLET_DEPOSIT = FabricBlockEntityTypeBuilder
                .create(WalletDepositBlockEntity::new, ModBlocks.WALLET_DEPOSIT)
                .build();

        Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(UnnamedDungeons.MOD_ID, "wallet_deposit"),
                WALLET_DEPOSIT
        );
    }
}