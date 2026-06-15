package com.k8bus.block;

import com.k8bus.dungeon.ActiveDungeon;
import com.k8bus.dungeon.DungeonState;
import com.k8bus.dungeon.DungeonTickHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class EldritchChestBlock extends WoodenDungeonChestBlock {
    public EldritchChestBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void onOpened(Level level, BlockPos pos, Player player) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        java.util.Random random = new java.util.Random();

        if (random.nextFloat() < 0.15f) { // 15% Unique
             net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment>[] uniquePool = new net.minecraft.resources.ResourceKey[] { 
                 com.k8bus.enchantment.ModEnchantments.VINES, com.k8bus.enchantment.ModEnchantments.WEBBING 
             };
             net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> rolled = uniquePool[random.nextInt(uniquePool.length)];
             dropEnchantedBook(serverLevel, pos, rolled, 1);
        } else { // Powerful
             net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment>[] powerfulPool = new net.minecraft.resources.ResourceKey[] {
                 com.k8bus.enchantment.ModEnchantments.JUNGLES_WRATH, com.k8bus.enchantment.ModEnchantments.LIFE_LEECH,
                 com.k8bus.enchantment.ModEnchantments.NEUROTOXICITY, com.k8bus.enchantment.ModEnchantments.THUNDERING,
                 com.k8bus.enchantment.ModEnchantments.VOID_STRIKE
             };
             net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> rolled = powerfulPool[random.nextInt(powerfulPool.length)];
             dropEnchantedBook(serverLevel, pos, rolled, random.nextInt(3) + 1);
        }

        dropItem(level, pos, new ItemStack(Items.NETHERITE_INGOT, 1));
        dropItem(level, pos, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1));
        dropItem(level, pos, new ItemStack(Items.DIAMOND_BLOCK, 1));

        DungeonState state = DungeonState.get(serverLevel);
        ActiveDungeon dungeon = state.getDungeonAt(pos);
        if (dungeon != null) {
            DungeonTickHandler.completeDungeon(serverLevel, dungeon, state);
            state.setDirty();
        }
    }
}
