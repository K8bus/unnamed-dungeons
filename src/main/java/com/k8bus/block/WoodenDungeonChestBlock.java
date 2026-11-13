package com.k8bus.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class WoodenDungeonChestBlock extends LootCrateBlock {
    public WoodenDungeonChestBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void onOpened(Level level, BlockPos pos, Player player) {
        Random random = new Random();
        dropItem(level, pos, new ItemStack(Items.EMERALD, random.nextInt(3) + 1));
        dropItem(level, pos, new ItemStack(Items.COOKED_BEEF, random.nextInt(5) + 2));
    }

    protected void dropItem(Level level, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
        level.addFreshEntity(entity);
    }

    protected boolean dropFromLootTable(ServerLevel level, BlockPos pos, Player player, Identifier lootTableId, int rolls) {
        if (lootTableId == null) return false;
        ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, lootTableId);
        LootTable table = level.getServer().reloadableRegistries().getLootTable(key);
        if (table == LootTable.EMPTY) return false;

        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withOptionalParameter(LootContextParams.THIS_ENTITY, player)
                .create(LootContextParamSets.CHEST);

        for (int i = 0; i < Math.max(1, rolls); i++) {
            for (ItemStack stack : table.getRandomItems(params)) {
                dropItem(level, pos, stack);
            }
        }
        return true;
    }

    protected void dropEnchantedBook(ServerLevel level, BlockPos pos, ResourceKey<net.minecraft.world.item.enchantment.Enchantment> enchantKey, int levelEnchant) {
        java.util.Optional<net.minecraft.core.Holder.Reference<net.minecraft.world.item.enchantment.Enchantment>> enchantHolder = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(enchantKey);
        if (enchantHolder.isPresent()) {
            ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
            book.enchant(enchantHolder.get(), levelEnchant);
            dropItem(level, pos, book);
        }
    }
}
