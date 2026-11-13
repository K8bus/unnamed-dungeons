package com.k8bus.dungeon;

import com.k8bus.block.DungeonGateBlock;
import com.k8bus.block.ModBlockTags;
import com.k8bus.block.ModBlocks;
import com.k8bus.block.entity.DungeonGateBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class DungeonGateHelper {

    private DungeonGateHelper() {
    }

    public enum GateType {
        ROOM,
        WAITING,
        DUNGEON
    }

    public record GateSize(int width, int height) {}

    /**
     * Sprawdza, czy dany blok jest rozpoznawany jako brama dungeonu
     * (poprzez domyślny blok, tag {@code #unnamed-dungeons:dungeon_gates} lub podklasę {@link DungeonGateBlock}).
     */
    public static boolean isGateBlock(BlockState state) {
        return state.is(ModBlocks.DUNGEON_GATE)
                || state.is(ModBlockTags.DUNGEON_GATES)
                || state.getBlock() instanceof DungeonGateBlock;
    }

    public static Optional<GateSize> getConfiguredGateSize(ServerLevel level, BlockPos pos) {
        if (pos == null || pos.equals(BlockPos.ZERO)) {
            return Optional.empty();
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof DungeonGateBlockEntity gate && gate.hasCustomBarrierSize()) {
            return Optional.of(new GateSize(gate.getBarrierWidth(), gate.getBarrierHeight()));
        }
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof DungeonGateBlock gateBlock
                && gateBlock.getDefaultBarrierWidth() > 0 && gateBlock.getDefaultBarrierHeight() > 0) {
            return Optional.of(new GateSize(gateBlock.getDefaultBarrierWidth(), gateBlock.getDefaultBarrierHeight()));
        }
        return Optional.empty();
    }

    public static boolean setGateSize(ServerLevel level, BlockPos pos, int width, int height) {
        if (pos == null || pos.equals(BlockPos.ZERO)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (!isGateBlock(state)) {
            return false;
        }

        int safeW = Math.max(1, width);
        int safeH = Math.max(1, height);

        // Usuń stare bariery jeśli brama była zablokowana
        boolean wasLocked = state.hasProperty(BlockStateProperties.OPEN) && !state.getValue(BlockStateProperties.OPEN);
        if (wasLocked) {
            clearBarriersAroundGate(level, pos, state, GateType.DUNGEON);
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof DungeonGateBlockEntity gate) {
            gate.setBarrierSize(safeW, safeH);
        }

        if (wasLocked) {
            lockGate(level, pos, GateType.DUNGEON);
        }
        return true;
    }

    public static boolean lockGate(ServerLevel level, BlockPos pos, GateType type) {
        if (pos == null || pos.equals(BlockPos.ZERO)) {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        if (!isGateBlock(state)) {
            return false;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof DungeonGateBlockEntity gate) {
            gate.lock();
        }

        if (state.hasProperty(BlockStateProperties.OPEN)) {
            state = state.setValue(BlockStateProperties.OPEN, false);
            level.setBlock(pos, state, 3);
        }

        Direction facing = state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                : Direction.NORTH;

        GateSize size = resolveSize(level, pos, state, type);
        List<BlockPos> barrierPositions = getBarrierPositions(pos, facing, size.width(), size.height());
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
        if (!isGateBlock(state)) {
            return false;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof DungeonGateBlockEntity gate) {
            gate.unlock();
        }

        if (state.hasProperty(BlockStateProperties.OPEN)) {
            state = state.setValue(BlockStateProperties.OPEN, true);
            level.setBlock(pos, state, 3);
        }

        clearBarriersAroundGate(level, pos, state, type);
        return true;
    }

    public static boolean removeGate(ServerLevel level, BlockPos pos, GateType type) {
        if (pos == null || pos.equals(BlockPos.ZERO)) {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        if (!isGateBlock(state)) {
            return false;
        }

        clearBarriersAroundGate(level, pos, state, type);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        return true;
    }

    private static void clearBarriersAroundGate(ServerLevel level, BlockPos pos, BlockState state, GateType type) {
        Direction facing = state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                : Direction.NORTH;

        GateSize size = resolveSize(level, pos, state, type);
        List<BlockPos> barrierPositions = getBarrierPositions(pos, facing, size.width(), size.height());
        ActiveDungeon dungeon = DungeonState.get(level).getDungeonAt(pos);

        for (BlockPos bPos : barrierPositions) {
            if (level.getBlockState(bPos).is(Blocks.BARRIER)) {
                level.setBlock(bPos, Blocks.AIR.defaultBlockState(), 3);
                if (dungeon != null) {
                    dungeon.removeActiveBarrier(bPos);
                }
            }
        }
    }

    private static GateSize resolveSize(ServerLevel level, BlockPos pos, BlockState state, GateType type) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof DungeonGateBlockEntity gate && gate.hasCustomBarrierSize()) {
            return new GateSize(gate.getBarrierWidth(), gate.getBarrierHeight());
        }
        if (state.getBlock() instanceof DungeonGateBlock gateBlock
                && gateBlock.getDefaultBarrierWidth() > 0 && gateBlock.getDefaultBarrierHeight() > 0) {
            return new GateSize(gateBlock.getDefaultBarrierWidth(), gateBlock.getDefaultBarrierHeight());
        }
        return type == GateType.ROOM ? new GateSize(2, 2) : new GateSize(3, 3);
    }

    private static List<BlockPos> getBarrierPositions(BlockPos pos, Direction facing, int width, int height) {
        List<BlockPos> list = new ArrayList<>();
        Direction leftOfViewer = facing.getClockWise();
        Direction rightOfViewer = facing.getCounterClockWise();

        int w = Math.max(1, width);
        int h = Math.max(1, height);

        int startDx = (w == 2) ? -1 : -(w / 2);
        int endDx = startDx + w - 1;

        for (int dy = 0; dy < h; dy++) {
            for (int dx = startDx; dx <= endDx; dx++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }
                BlockPos target = pos.above(dy);
                if (dx < 0) {
                    target = target.relative(leftOfViewer, -dx);
                } else if (dx > 0) {
                    target = target.relative(rightOfViewer, dx);
                }
                list.add(target);
            }
        }

        return list;
    }
}
