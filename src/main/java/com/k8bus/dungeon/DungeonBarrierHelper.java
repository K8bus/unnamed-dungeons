package com.k8bus.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.SlabBlock;

import java.util.HashSet;
import java.util.Set;

public final class DungeonBarrierHelper {

    private DungeonBarrierHelper() {}

    public static void placeBarriers(ServerLevel level, BoundingBox box) {
        Set<BlockPos> targetBarrierPositions = new HashSet<>();

        // ETAP 1: Szukanie krawędzi i płotków
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                for (int y = box.minY(); y <= box.maxY(); y++) { // Adjusted to include minY and maxY
                    BlockPos currentPos = new BlockPos(x, y, z);

                    if (isPassableDecoration(level, currentPos)) continue;
                    if (!level.getBlockState(currentPos).isAir()) continue;

                    BlockPos blockBelow = currentPos.below();
                    if (!isDungeonFloor(level, blockBelow)) continue;

                    // Zasada "Sąsiedniego Klocka" - zapobiega błędom przy ozdobach
                    if (!hasValidFloorNeighbor(level, blockBelow)) continue;

                    for (Direction dir : Direction.Plane.HORIZONTAL) {
                        BlockPos neighborPos = currentPos.relative(dir);
                        BlockState neighborState = level.getBlockState(neighborPos);

                        if (isGateNearby(level, currentPos) || isGateNearby(level, neighborPos)) {
                            continue;
                        }

                        // PRZYPADEK 1: Wykryto płotek / murek graniczny
                        if (isFenceOrWall(neighborState)) {
                            BlockPos beyondFence = neighborPos.relative(dir);
                    
                            if (isDeadlyDrop(level, beyondFence, dir)) {
                                BlockPos mainBarrier = neighborPos.above(1);
                                targetBarrierPositions.add(mainBarrier);
                                targetBarrierPositions.add(mainBarrier.relative(dir.getCounterClockWise()));
                                targetBarrierPositions.add(mainBarrier.relative(dir.getClockWise()));
                            }
                        } 
                        // PRZYPADEK 2: Otwarta krawędź dungeona
                        else {
                            if (isAirOrPassable(level, neighborPos) && isDeadlyDrop(level, neighborPos.below(), dir)) {
                                targetBarrierPositions.add(currentPos);
                                targetBarrierPositions.add(currentPos.above());
                            }
                        }
                    }
                }
            }
        }

        for (BlockPos pos : targetBarrierPositions) {
            placeBarrierIfStrictAir(level, pos);
        }
    }

    private static boolean hasValidFloorNeighbor(ServerLevel level, BlockPos floorPos) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(floorPos.relative(dir)).isSolid()) return true;
        }
        return false;
    }

    private static boolean isDungeonFloor(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.is(Blocks.BARRIER) || !state.getFluidState().isEmpty() || isFenceOrWall(state)) {
            return false;
        }
        if (isPassableDecoration(level, pos)) return false;

        // Optimize redundant checks
        boolean isSolid = state.getBlock() instanceof StairBlock 
            || state.getBlock() instanceof SlabBlock 
            || state.isSolid();
        
        return isSolid;
    }

    private static boolean isDeadlyDrop(ServerLevel level, BlockPos startPos, Direction scanDir) {
        for (int i = 0; i < 4; i++) {
            BlockState state = level.getBlockState(startPos.below(i));
            if (state.getBlock() instanceof StairBlock || state.getBlock() instanceof SlabBlock) return false;
        }

        BlockPos nextStepHorizontal = startPos.relative(scanDir);
        for (int downOffset = 0; downOffset <= 2; downOffset++) {
            BlockState floorState = level.getBlockState(nextStepHorizontal.below(downOffset));
            if (floorState.getBlock() instanceof StairBlock || floorState.getBlock() instanceof SlabBlock || floorState.isSolid()) {
                return false; 
            }
        }

        // Ensure the intermediate state is checked properly
        BlockState intermediateState = level.getBlockState(startPos.relative(scanDir).below(3));
        if (intermediateState.isAir() || intermediateState.getFluidState().isEmpty()) {
            return true;
        }
        return false;
    }

    private static boolean isAirOrPassable(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).isAir() || level.getBlockState(pos).is(Blocks.BARRIER) || isPassableDecoration(level, pos);
    }

    private static boolean isPassableDecoration(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;
        String blockId = state.getBlock().getDescriptionId().toLowerCase();
        return state.is(Blocks.COBWEB) || state.is(Blocks.VINE) || state.is(Blocks.GLOW_LICHEN) || state.is(Blocks.CAVE_VINES) || state.is(Blocks.CAVE_VINES_PLANT) || state.is(Blocks.TALL_GRASS) || state.is(Blocks.SHORT_GRASS) || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN) || blockId.contains("banner") || blockId.contains("plant") || blockId.contains("grass") || blockId.contains("flower") || blockId.contains("candle") || blockId.contains("lantern") || blockId.contains("head") || blockId.contains("skull") || blockId.contains("torch") || blockId.contains("chain") || blockId.contains("button") || blockId.contains("lever") || blockId.contains("pot");
    }

    private static boolean isFenceOrWall(BlockState state) {
        return state.getBlock() instanceof FenceBlock || state.getBlock() instanceof IronBarsBlock || state.getBlock().getDescriptionId().contains("wall");
    }

    private static boolean isGateNearby(ServerLevel level, BlockPos pos) {
        return isGateBlock(level.getBlockState(pos)) || isGateBlock(level.getBlockState(pos.below()));
    }

    private static void placeBarrierIfStrictAir(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).isAir() && !isPassableDecoration(level, pos)) {
            // Nie blokuj schodów i slabów (barierki pojawiają się na krawędziach przy ich sąsiedztwie)
            BlockState below = level.getBlockState(pos.below());
            if (below.getBlock() instanceof StairBlock || below.getBlock() instanceof SlabBlock) {
                return;
            }

            level.setBlock(pos, Blocks.BARRIER.defaultBlockState(), 3);
        }
    }

    public static void removeBarriers(ServerLevel level, BoundingBox box) {
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int y = box.minY(); y <= box.maxY(); y++) {
                for (int z = box.minZ(); z <= box.maxZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (level.getBlockState(pos).is(Blocks.BARRIER)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    private static boolean isGateBlock(BlockState state) {
        return state.is(com.k8bus.block.ModBlocks.DUNGEON_GATE); 
    }
}
