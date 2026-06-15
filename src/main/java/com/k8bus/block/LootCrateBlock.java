package com.k8bus.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public abstract class LootCrateBlock extends Block {
    public LootCrateBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            onOpened(level, pos, player);
            level.destroyBlock(pos, false);
        }
        return InteractionResult.SUCCESS;
    }

    protected abstract void onOpened(Level level, BlockPos pos, Player player);
}
