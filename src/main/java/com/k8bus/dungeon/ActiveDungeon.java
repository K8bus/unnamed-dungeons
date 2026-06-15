package com.k8bus.dungeon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ActiveDungeon {

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
                    .forGetter(ActiveDungeon::getOriginalGameModesForCodec))
            .apply(instance, (box, killCount, killRequired, phase, phaseTimer, playerSnapshotCount, waitingGatePos,
                    dungeonGatePos, waitingSpawnPos, rooms, originalGameModes) -> {
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
                return dungeon;
            }));

    public enum DungeonPhase {
        IDLE, WAITING, PREPARING, ACTIVE, COMPLETED, RESETTING
    }

    private final BoundingBox boundingBox;
    private final List<UUID> players = new ArrayList<>();
    private final List<RoomEncounter> rooms = new ArrayList<>();
    private final Map<UUID, GameType> originalGameModes = new HashMap<>();

    int killCount = 0;
    int killRequired;
    int phaseTimer = 0;
    int playerSnapshotCount = 0;

    BlockPos waitingGatePos = BlockPos.ZERO;
    BlockPos dungeonGatePos = BlockPos.ZERO;
    BlockPos waitingSpawnPos = BlockPos.ZERO;

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

    public void addRoom(RoomEncounter room) {
        rooms.add(room);
    }
}