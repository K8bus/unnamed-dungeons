package com.k8bus.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
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
        ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
        level.addFreshEntity(entity);
    }

    protected void dropEnchantedBook(net.minecraft.server.level.ServerLevel level, BlockPos pos, net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> enchantKey, int levelEnchant) {
        java.util.Optional<net.minecraft.core.Holder.Reference<net.minecraft.world.item.enchantment.Enchantment>> enchantHolder = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).get(enchantKey);
        if (enchantHolder.isPresent()) {
            ItemStack book = new ItemStack(net.minecraft.world.item.Items.ENCHANTED_BOOK);
            book.enchant(enchantHolder.get(), levelEnchant);
            dropItem(level, pos, book);
        }
    }
}
