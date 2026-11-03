package com.k8bus.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DungeonTemplateData {
    public BlockPos waitingSpawnPos;
    public BlockPos waitingGatePos;
    public BlockPos dungeonGatePos;
    public BlockPos dungeonBoxStart;
    public BlockPos dungeonBoxEnd;
    public BlockPos eldritchChestPos;
    public Identifier eldritchChestLootTable;
    
    public Map<Integer, RoomData> rooms = new HashMap<>();

    public RoomData getOrCreateRoom(int id) {
        return rooms.computeIfAbsent(id, k -> new RoomData());
    }

    public static class RoomData {
        public BlockPos boxStart;
        public BlockPos boxEnd;
        public BlockPos rewardGatePos;
        public BlockPos heroChestPos;
        public Identifier heroChestLootTable;
        public List<BlockPos> spawnPoints = new ArrayList<>();
        public List<WaveDefinition> waves = new ArrayList<>();
        public List<Identifier> mobPool = new ArrayList<>();
    }
}
