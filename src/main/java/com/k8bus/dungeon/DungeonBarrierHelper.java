package com.k8bus.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public final class DungeonBarrierHelper {

    private DungeonBarrierHelper() {}

    public static void placeBarriers(ServerLevel level, BoundingBox box) {
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int y = box.minY(); y <= box.maxY(); y++) {
                for (int z = box.minZ(); z <= box.maxZ(); z++) {
                    boolean onBorder = x == box.minX() || x == box.maxX()
                            || y == box.minY() || y == box.maxY()
                            || z == box.minZ() || z == box.maxZ();

                    if (!onBorder) continue;

                    BlockPos pos = new BlockPos(x, y, z);

                    if (level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, Blocks.BARRIER.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    public static void removeBarriers(ServerLevel level, BoundingBox box) {
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int y = box.minY(); y <= box.maxY(); y++) {
                for (int z = box.minZ(); z <= box.maxZ(); z++) {
                    boolean onBorder = x == box.minX() || x == box.maxX()
                            || y == box.minY() || y == box.maxY()
                            || z == box.minZ() || z == box.maxZ();

                    if (!onBorder) continue;

                    BlockPos pos = new BlockPos(x, y, z);

                    if (level.getBlockState(pos).is(Blocks.BARRIER)) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }
}