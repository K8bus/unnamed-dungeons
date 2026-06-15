package com.k8bus.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import java.util.Random;

public class HeroChestBlock extends WoodenDungeonChestBlock {
    public HeroChestBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void onOpened(Level level, BlockPos pos, Player player) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;
        super.onOpened(level, pos, player);
        Random random = new Random();

        net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment>[] commonPool = new net.minecraft.resources.ResourceKey[] {
            com.k8bus.enchantment.ModEnchantments.POISON, com.k8bus.enchantment.ModEnchantments.POISON_ARROW,
            com.k8bus.enchantment.ModEnchantments.DEFLECT, com.k8bus.enchantment.ModEnchantments.ANTI_VENOM,
            com.k8bus.enchantment.ModEnchantments.RICOCHET
        };
        net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> rolled = commonPool[random.nextInt(commonPool.length)];
        dropEnchantedBook(serverLevel, pos, rolled, random.nextInt(3) + 1);

        dropItem(level, pos, new ItemStack(Items.DIAMOND, random.nextInt(2) + 1));
        dropItem(level, pos, new ItemStack(Items.GOLDEN_APPLE, 1));
        dropItem(level, pos, new ItemStack(Items.EXPERIENCE_BOTTLE, random.nextInt(5) + 3));
    }
}
