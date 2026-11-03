package com.k8bus.dungeon;

import com.k8bus.block.DungeonGateBlock;
import com.k8bus.block.ModBlocks;
import com.k8bus.block.entity.DungeonGateBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public final class DungeonGateHelper {

    private DungeonGateHelper() {
    }

    public enum GateType {
        ROOM,
        WAITING,
        DUNGEON
    }

    public static boolean lockGate(ServerLevel level, BlockPos pos, GateType type) {
        if (pos == null || pos.equals(BlockPos.ZERO)) {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlocks.DUNGEON_GATE)) {
            return false;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof DungeonGateBlockEntity gate) {
            gate.lock();
        }

        level.setBlock(pos, state.setValue(DungeonGateBlock.OPEN, false), 3);

        Direction facing = state.getValue(DungeonGateBlock.FACING);
        List<BlockPos> barrierPositions = getBarrierPositions(pos, facing, type);
        ActiveDungeon dungeon = DungeonState.get(level).getDungeonAt(pos);

        for (BlockPos bPos : barrierPositions) {
            if (level.getBlockState(bPos).canBeReplaced() || level.getBlockState(bPos).isAir()) {
                level.setBlock(bPos, Blocks.BARRIER.defaultBlockState(), 3);
                if (dungeon != null) {
                    dungeon.addActiveBarrier(bPos);
                }
            }
        }

        return true;
    }

    public static boolean unlockGate(ServerLevel level, BlockPos pos, GateType type) {
        if (pos == null || pos.equals(BlockPos.ZERO)) {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlocks.DUNGEON_GATE)) {
            return false;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof DungeonGateBlockEntity gate) {
            gate.unlock();
        }

        level.setBlock(pos, state.setValue(DungeonGateBlock.OPEN, true), 3);

        Direction facing = state.getValue(DungeonGateBlock.FACING);
        List<BlockPos> barrierPositions = getBarrierPositions(pos, facing, type);
        ActiveDungeon dungeon = DungeonState.get(level).getDungeonAt(pos);

        for (BlockPos bPos : barrierPositions) {
            if (level.getBlockState(bPos).is(Blocks.BARRIER)) {
                level.setBlock(bPos, Blocks.AIR.defaultBlockState(), 3);
                if (dungeon != null) {
                    dungeon.removeActiveBarrier(bPos);
                }
            }
        }

        return true;
    }

    public static boolean removeGate(ServerLevel level, BlockPos pos, GateType type) {
        if (pos == null || pos.equals(BlockPos.ZERO)) {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlocks.DUNGEON_GATE)) {
            return false;
        }

        Direction facing = state.getValue(DungeonGateBlock.FACING);
        List<BlockPos> barrierPositions = getBarrierPositions(pos, facing, type);

        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);

        for (BlockPos bPos : barrierPositions) {
            if (level.getBlockState(bPos).is(Blocks.BARRIER)) {
                level.setBlock(bPos, Blocks.AIR.defaultBlockState(), 3);
            }
        }

        return true;
    }

    private static List<BlockPos> getBarrierPositions(BlockPos pos, Direction facing, GateType type) {
        List<BlockPos> list = new ArrayList<>();
        Direction leftOfViewer = facing.getClockWise();
        Direction rightOfViewer = facing.getCounterClockWise();

        if (type == GateType.ROOM) {
            // 2x2. Gate block is bottom-right.
            // Barriers: left, up, up-left
            list.add(pos.relative(leftOfViewer));
            list.add(pos.above());
            list.add(pos.relative(leftOfViewer).above());
        } else {
            // 3x3. Gate block is bottom-center.
            // Barriers: left, right, and 3 blocks above, and 3 blocks above that
            list.add(pos.relative(leftOfViewer));
            list.add(pos.relative(rightOfViewer));

            list.add(pos.above());
            list.add(pos.relative(leftOfViewer).above());
            list.add(pos.relative(rightOfViewer).above());

            list.add(pos.above(2));
            list.add(pos.relative(leftOfViewer).above(2));
            list.add(pos.relative(rightOfViewer).above(2));
        }

        return list;
    }
}