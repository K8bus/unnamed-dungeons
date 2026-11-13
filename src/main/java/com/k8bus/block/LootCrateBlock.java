package com.k8bus.block;

import com.k8bus.api.DungeonEvents;
import com.k8bus.dungeon.ActiveDungeon;
import com.k8bus.dungeon.DungeonState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            DungeonState dungeonState = DungeonState.get(serverLevel);
            ActiveDungeon dungeon = dungeonState.getDungeonAt(pos);
            boolean dropLoot = true;

            if (dungeon != null) {
                dungeon.recordRestorableBlock(pos, state);
                if (player instanceof ServerPlayer serverPlayer) {
                    dropLoot = DungeonEvents.CHEST_OPENED.invoker().onChestOpened(serverLevel, dungeon, pos, serverPlayer, state);
                }
                dungeonState.setDirty();
            }

            onOpened(level, pos, player, dropLoot);
            level.destroyBlock(pos, false);
        }
        return InteractionResult.SUCCESS;
    }

    protected void onOpened(Level level, BlockPos pos, Player player, boolean dropLoot) {
        if (dropLoot) {
            onOpened(level, pos, player);
        }
    }

    protected abstract void onOpened(Level level, BlockPos pos, Player player);
}
