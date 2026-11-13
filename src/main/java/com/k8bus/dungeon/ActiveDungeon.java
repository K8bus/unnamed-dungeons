package com.k8bus.dungeon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class ActiveDungeon {

    public record RestorableBlock(BlockPos pos, BlockState state) {
        public static final Codec<RestorableBlock> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(RestorableBlock::pos),
                BlockState.CODEC.fieldOf("state").forGetter(RestorableBlock::state)
        ).apply(instance, RestorableBlock::new));
    }

    private static final DungeonJsonDefinition.TimingsConfig DEFAULT_TIMINGS =
            new DungeonJsonDefinition.TimingsConfig(30, 10, 15, 3, 5);

    public static final Codec<ActiveDungeon> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BoundingBox.CODEC.fieldOf("bounding_box").forGetter(ActiveDungeon::getBoundingBox),
            Codec.INT.fieldOf("kill_count").forGetter(ActiveDungeon::getKillCount),
            Codec.INT.fieldOf("kill_required").forGetter(ActiveDungeon::getKillRequired),
            Codec.STRING.fieldOf("phase").forGetter(d -> d.getPhase().name()),
            Codec.INT.fieldOf("phase_timer").forGetter(ActiveDungeon::getPhaseTimer),
            Codec.INT.optionalFieldOf("player_snapshot_count", 0).forGetter(ActiveDungeon::getPlayerSnapshotCount),
            BlockPos.CODEC.optionalFieldOf("waiting_gate_pos", BlockPos.ZERO)
                    .forGetter(ActiveDungeon::getWaitingGatePos),
            BlockPos.CODEC.optionalFieldOf("dungeon_gate_pos", BlockPos.ZERO)
                    .forGetter(ActiveDungeon::getDungeonGatePos),
            BlockPos.CODEC.optionalFieldOf("waiting_spawn_pos", BlockPos.ZERO)
                    .forGetter(ActiveDungeon::getWaitingSpawnPos),
            RoomEncounter.CODEC.listOf().optionalFieldOf("rooms", List.of()).forGetter(ActiveDungeon::getRooms),
            Codec.unboundedMap(Codec.STRING, Codec.STRING)
                    .optionalFieldOf("original_game_modes", Map.of())
                    .forGetter(ActiveDungeon::getOriginalGameModesForCodec),
            BlockPos.CODEC.listOf().optionalFieldOf("active_barriers", List.of())
                    .forGetter(ActiveDungeon::getActiveBarriersList),
            Identifier.CODEC.optionalFieldOf("eldritch_chest_loot_table")
                    .forGetter(d -> Optional.ofNullable(d.eldritchChestLootTable)),
            Codec.BOOL.optionalFieldOf("repeatable", false).forGetter(ActiveDungeon::isRepeatable),
            DungeonJsonDefinition.TimingsConfig.CODEC.optionalFieldOf("timings", DEFAULT_TIMINGS)
                    .forGetter(ActiveDungeon::getTimingsConfig),
            RestorableBlock.CODEC.listOf().optionalFieldOf("restorable_blocks", List.of())
                    .forGetter(ActiveDungeon::getRestorableBlocksList))
            .apply(instance, (box, killCount, killRequired, phase, phaseTimer, playerSnapshotCount, waitingGatePos,
                    dungeonGatePos, waitingSpawnPos, rooms, originalGameModes, activeBarriers, eldritchChestLootTable,
                    repeatable, timings, restorableBlocks) -> {
                ActiveDungeon dungeon = new ActiveDungeon(box, killRequired);
                dungeon.killCount = killCount;
                dungeon.phase = DungeonPhase.valueOf(phase);
                dungeon.phaseTimer = phaseTimer;
                dungeon.playerSnapshotCount = playerSnapshotCount;
                dungeon.waitingGatePos = waitingGatePos;
                dungeon.dungeonGatePos = dungeonGatePos;
                dungeon.waitingSpawnPos = waitingSpawnPos;
                dungeon.rooms.addAll(rooms);
                dungeon.loadOriginalGameModes(originalGameModes);
                dungeon.activeBarriers.addAll(activeBarriers);
                dungeon.eldritchChestLootTable = eldritchChestLootTable.orElse(null);
                dungeon.repeatable = repeatable;
                dungeon.setTimings(
                        timings.waitingSeconds(),
                        timings.preparingSeconds(),
                        timings.completionSeconds(),
                        timings.betweenWavesMinSeconds(),
                        timings.betweenWavesMaxSeconds()
                );
                for (RestorableBlock rb : restorableBlocks) {
                    dungeon.restorableBlocks.put(rb.pos(), rb.state());
                }
                return dungeon;
            }));

    public enum DungeonPhase {
        IDLE, WAITING, PREPARING, ACTIVE, COMPLETED, RESETTING
    }

    private final BoundingBox boundingBox;
    private final List<UUID> players = new ArrayList<>();
    private final List<RoomEncounter> rooms = new ArrayList<>();
    private final Map<UUID, GameType> originalGameModes = new HashMap<>();
    private final Set<BlockPos> activeBarriers = new HashSet<>();
    private final Map<BlockPos, BlockState> restorableBlocks = new HashMap<>();

    public int killCount = 0;
    int killRequired;
    public int phaseTimer = 0;
    public int playerSnapshotCount = 0;

    BlockPos waitingGatePos = BlockPos.ZERO;
    BlockPos dungeonGatePos = BlockPos.ZERO;
    BlockPos waitingSpawnPos = BlockPos.ZERO;
    private Identifier eldritchChestLootTable;

    private boolean repeatable = false;
    private int waitingSeconds = 30;
    private int preparingSeconds = 10;
    private int completionSeconds = 15;
    private int betweenWavesMinSeconds = 3;
    private int betweenWavesMaxSeconds = 5;

    DungeonPhase phase = DungeonPhase.IDLE;

    public ActiveDungeon(BoundingBox boundingBox, int killRequired) {
        this.boundingBox = boundingBox;
        this.killRequired = killRequired;
    }

    public void addPlayer(UUID playerId) {
        if (!players.contains(playerId))
            players.add(playerId);
    }

    public void removePlayer(UUID playerId) {
        players.remove(playerId);
    }

    public boolean hasPlayer(UUID playerId) {
        return players.contains(playerId);
    }

    public void incrementKillCount() {
        killCount++;
    }

    public void snapshotPlayers() {
        playerSnapshotCount = Math.max(1, players.size());
    }

    public void resetRuntimeState() {
        killCount = 0;
        phaseTimer = 0;
        playerSnapshotCount = 0;
        phase = DungeonPhase.IDLE;
        players.clear();

        for (RoomEncounter room : rooms) {
            room.resetRuntimeState();
        }
    }

    public void saveOriginalGameMode(UUID playerId, GameType gameType) {
        originalGameModes.putIfAbsent(playerId, gameType);
    }

    public GameType getOriginalGameMode(UUID playerId) {
        return originalGameModes.getOrDefault(playerId, GameType.SURVIVAL);
    }

    public void clearOriginalGameMode(UUID playerId) {
        originalGameModes.remove(playerId);
    }

    public Map<String, String> getOriginalGameModesForCodec() {
        Map<String, String> encoded = new HashMap<>();

        for (Map.Entry<UUID, GameType> entry : originalGameModes.entrySet()) {
            encoded.put(entry.getKey().toString(), entry.getValue().getName());
        }

        return encoded;
    }

    private void loadOriginalGameModes(Map<String, String> encoded) {
        originalGameModes.clear();

        for (Map.Entry<String, String> entry : encoded.entrySet()) {
            try {
                UUID playerId = UUID.fromString(entry.getKey());
                GameType gameType = GameType.byName(entry.getValue(), GameType.SURVIVAL);
                originalGameModes.put(playerId, gameType);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public boolean isCompleted() {
        if (rooms.isEmpty()) {
            return killCount >= killRequired;
        }

        for (RoomEncounter room : rooms) {
            if (!room.isCompleted()) {
                return false;
            }
        }

        return true;
    }

    public RoomEncounter getRoomAt(BlockPos pos) {
        for (RoomEncounter room : rooms) {
            if (room.getRoomBox().isInside(pos)) {
                return room;
            }
        }

        return null;
    }

    public void setPhaseWithTimer(DungeonPhase phase, int phaseTimer) {
        this.phase = phase;
        this.phaseTimer = phaseTimer;
    }

    public void tickPhaseTimer() {
        if (phaseTimer > 0) {
            phaseTimer--;
        }
    }

    public boolean isPhaseTimerFinished() {
        return phaseTimer <= 0;
    }

    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    public DungeonPhase getPhase() {
        return phase;
    }

    public void setPhase(DungeonPhase phase) {
        this.phase = phase;
    }

    public int getKillCount() {
        return killCount;
    }

    public int getKillRequired() {
        return killRequired;
    }

    public int getPhaseTimer() {
        return phaseTimer;
    }

    public List<UUID> getPlayers() {
        return players;
    }

    public int getPlayerSnapshotCount() {
        return playerSnapshotCount;
    }

    public BlockPos getWaitingGatePos() {
        return waitingGatePos;
    }

    public BlockPos getDungeonGatePos() {
        return dungeonGatePos;
    }

    public List<RoomEncounter> getRooms() {
        return rooms;
    }

    public void setWaitingSpawnPos(BlockPos waitingSpawnPos) {
        this.waitingSpawnPos = waitingSpawnPos;
    }

    public BlockPos getWaitingSpawnPos() {
        return waitingSpawnPos;
    }

    public void setWaitingGatePos(BlockPos waitingGatePos) {
        this.waitingGatePos = waitingGatePos;
    }

    public void setDungeonGatePos(BlockPos dungeonGatePos) {
        this.dungeonGatePos = dungeonGatePos;
    }

    public Identifier getEldritchChestLootTable() {
        return eldritchChestLootTable;
    }

    public void setEldritchChestLootTable(Identifier eldritchChestLootTable) {
        this.eldritchChestLootTable = eldritchChestLootTable;
    }

    public boolean isRepeatable() {
        return repeatable;
    }

    public void setRepeatable(boolean repeatable) {
        this.repeatable = repeatable;
    }

    public int getWaitingSeconds() {
        return waitingSeconds;
    }

    public int getPreparingSeconds() {
        return preparingSeconds;
    }

    public int getCompletionSeconds() {
        return completionSeconds;
    }

    public int getBetweenWavesMinSeconds() {
        return betweenWavesMinSeconds;
    }

    public int getBetweenWavesMaxSeconds() {
        return betweenWavesMaxSeconds;
    }

    public DungeonJsonDefinition.TimingsConfig getTimingsConfig() {
        return new DungeonJsonDefinition.TimingsConfig(
                waitingSeconds,
                preparingSeconds,
                completionSeconds,
                betweenWavesMinSeconds,
                betweenWavesMaxSeconds
        );
    }

    public void setTimings(int waitingSeconds, int preparingSeconds, int completionSeconds,
                           int betweenWavesMinSeconds, int betweenWavesMaxSeconds) {
        this.waitingSeconds = Math.max(0, waitingSeconds);
        this.preparingSeconds = Math.max(0, preparingSeconds);
        this.completionSeconds = Math.max(0, completionSeconds);
        this.betweenWavesMinSeconds = Math.max(0, betweenWavesMinSeconds);
        this.betweenWavesMaxSeconds = Math.max(this.betweenWavesMinSeconds, betweenWavesMaxSeconds);
    }

    public void addRoom(RoomEncounter room) {
        rooms.add(room);
    }

    public Set<BlockPos> getActiveBarriers() {
        return activeBarriers;
    }

    public List<BlockPos> getActiveBarriersList() {
        return new ArrayList<>(activeBarriers);
    }

    public void addActiveBarrier(BlockPos pos) {
        if (pos != null) {
            activeBarriers.add(pos);
        }
    }

    public void removeActiveBarrier(BlockPos pos) {
        if (pos != null) {
            activeBarriers.remove(pos);
        }
    }

    public void clearAllBarriers(ServerLevel level) {
        for (BlockPos pos : activeBarriers) {
            if (level.getBlockState(pos).is(Blocks.BARRIER)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        activeBarriers.clear();
    }

    public void recordRestorableBlock(BlockPos pos, BlockState originalState) {
        if (pos != null && originalState != null) {
            restorableBlocks.putIfAbsent(pos.immutable(), originalState);
        }
    }

    public List<RestorableBlock> getRestorableBlocksList() {
        List<RestorableBlock> list = new ArrayList<>();
        for (Map.Entry<BlockPos, BlockState> entry : restorableBlocks.entrySet()) {
            list.add(new RestorableBlock(entry.getKey(), entry.getValue()));
        }
        return list;
    }

    public void restoreTrackedBlocks(ServerLevel level) {
        for (Map.Entry<BlockPos, BlockState> entry : restorableBlocks.entrySet()) {
            level.setBlock(entry.getKey(), entry.getValue(), 3);
        }
        restorableBlocks.clear();
    }
}
