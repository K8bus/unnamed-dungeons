package com.k8bus.block;

import com.k8bus.dungeon.ActiveDungeon;
import com.k8bus.dungeon.DungeonState;
import com.k8bus.enchantment.ModEnchantments;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Random;

public class HeroChestBlock extends WoodenDungeonChestBlock {
    public HeroChestBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void onOpened(Level level, BlockPos pos, Player player) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        super.onOpened(level, pos, player);
        Random random = new Random();

        DungeonState state = DungeonState.get(serverLevel);
        ActiveDungeon dungeon = state.getDungeonAt(pos);
        int playerCount = dungeon != null ? Math.max(1, dungeon.getPlayerSnapshotCount()) : 1;

        List<ResourceKey<Enchantment>> commonPool = List.of(
                ModEnchantments.POISON, ModEnchantments.POISON_ARROW,
                ModEnchantments.DEFLECT, ModEnchantments.ANTI_VENOM,
                ModEnchantments.RICOCHET
        );

        // Skalowanie dropu dla każdego członka drużyny
        for (int i = 0; i < playerCount; i++) {
            ResourceKey<Enchantment> rolled = commonPool.get(random.nextInt(commonPool.size()));
            dropEnchantedBook(serverLevel, pos, rolled, random.nextInt(3) + 1);
            dropItem(level, pos, new ItemStack(Items.DIAMOND, random.nextInt(2) + 1));
            dropItem(level, pos, new ItemStack(Items.GOLDEN_APPLE, 1));
            dropItem(level, pos, new ItemStack(Items.EXPERIENCE_BOTTLE, random.nextInt(4) + 3));
        }
    }
}
