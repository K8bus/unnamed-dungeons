package com.k8bus.dungeon;

import com.k8bus.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class RoomSpaceDetector {

    private static final int MAX_BLOCKS = 50000; // Limit to avoid infinite loops

    public static BoundingBox detectRoomBounds(Level level, BlockPos startPos) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();

        int minX = startPos.getX();
        int minY = startPos.getY();
        int minZ = startPos.getZ();
        int maxX = startPos.getX();
        int maxY = startPos.getY();
        int maxZ = startPos.getZ();

        queue.add(startPos);
        visited.add(startPos);

        while (!queue.isEmpty() && visited.size() < MAX_BLOCKS) {
            BlockPos current = queue.poll();

            minX = Math.min(minX, current.getX());
            minY = Math.min(minY, current.getY());
            minZ = Math.min(minZ, current.getZ());
            maxX = Math.max(maxX, current.getX());
            maxY = Math.max(maxY, current.getY());
            maxZ = Math.max(maxZ, current.getZ());

            for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                BlockPos neighbor = current.relative(dir);

                if (!visited.contains(neighbor)) {
                    BlockState state = level.getBlockState(neighbor);

                    // Stop at dungeon gates
                    if (state.is(ModBlocks.DUNGEON_GATE)) {
                        visited.add(neighbor); // Mark as visited to avoid re-checking, but don't add to queue
                        // We still want to include the gate in the bounding box
                        minX = Math.min(minX, neighbor.getX());
                        minY = Math.min(minY, neighbor.getY());
                        minZ = Math.min(minZ, neighbor.getZ());
                        maxX = Math.max(maxX, neighbor.getX());
                        maxY = Math.max(maxY, neighbor.getY());
                        maxZ = Math.max(maxZ, neighbor.getZ());
                        continue;
                    }

                    // Continue if it's passable (empty collision shape) or air
                    if (state.getCollisionShape(level, neighbor).isEmpty() || state.isAir()) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    } else {
                        // Solid wall: mark as visited but do not queue.
                        // Include the wall in the bounding box so the box fully encapsulates the room.
                        visited.add(neighbor);
                        minX = Math.min(minX, neighbor.getX());
                        minY = Math.min(minY, neighbor.getY());
                        minZ = Math.min(minZ, neighbor.getZ());
                        maxX = Math.max(maxX, neighbor.getX());
                        maxY = Math.max(maxY, neighbor.getY());
                        maxZ = Math.max(maxZ, neighbor.getZ());
                    }
                }
            }
        }

        return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
