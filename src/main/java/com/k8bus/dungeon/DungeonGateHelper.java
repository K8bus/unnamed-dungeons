package com.k8bus.dungeon;

import com.k8bus.block.ModBlocks;
import com.k8bus.block.entity.DungeonGateBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class DungeonGateHelper {

    private DungeonGateHelper() {
    }

    public static boolean lockGate(ServerLevel level, BlockPos pos) {
        if (pos == null || pos.equals(BlockPos.ZERO)) {
            return false;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (!(blockEntity instanceof DungeonGateBlockEntity gate)) {
            return false;
        }

        gate.lock();
        level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);

        return true;
    }

    public static boolean unlockGate(ServerLevel level, BlockPos pos) {
        if (pos == null || pos.equals(BlockPos.ZERO)) {
            return false;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (!(blockEntity instanceof DungeonGateBlockEntity gate)) {
            return false;
        }

        gate.unlock();
        level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);

        return true;
    }

    public static boolean removeGate(ServerLevel level, BlockPos pos) {
        if (pos == null || pos.equals(BlockPos.ZERO)) {
            return false;
        }

        if (!level.getBlockState(pos).is(ModBlocks.DUNGEON_GATE)) {
            return false;
        }

        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);

        return true;
    }
}