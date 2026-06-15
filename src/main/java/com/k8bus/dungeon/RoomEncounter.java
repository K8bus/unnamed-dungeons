package com.k8bus.dungeon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Random;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;

public class RoomEncounter {

    public static final Codec<RoomEncounter> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BoundingBox.CODEC.fieldOf("room_box").forGetter(RoomEncounter::getRoomBox),
                    BlockPos.CODEC.fieldOf("reward_gate_pos").forGetter(RoomEncounter::getRewardGatePos),
                    BlockPos.CODEC.fieldOf("hero_chest_pos").forGetter(RoomEncounter::getHeroChestPos),
                    WaveDefinition.CODEC.listOf().fieldOf("waves").forGetter(RoomEncounter::getWaves),
                    Codec.STRING.fieldOf("phase").forGetter(room -> room.getPhase().name()),
                    Codec.INT.fieldOf("current_wave_index").forGetter(RoomEncounter::getCurrentWaveIndex),
                    Codec.INT.fieldOf("spawned_in_current_wave").forGetter(RoomEncounter::getSpawnedInCurrentWave)
            ).apply(instance, (roomBox, rewardGatePos, heroChestPos, waves, phase, currentWaveIndex, spawnedInCurrentWave) -> {
                RoomEncounter room = new RoomEncounter(roomBox, rewardGatePos, heroChestPos, waves);
                room.phase = RoomPhase.valueOf(phase);
                room.currentWaveIndex = currentWaveIndex;
                room.spawnedInCurrentWave = spawnedInCurrentWave;
                return room;
            })
    );

    public enum RoomPhase {
        IDLE,
        WAVE_ACTIVE,
        BETWEEN_WAVES,
        REWARD,
        COMPLETED
    }

    private final BoundingBox roomBox;
    private BlockPos rewardGatePos;
    private BlockPos heroChestPos;
    private final List<WaveDefinition> waves;
    private List<BlockPos> mobSpawnPoints = new ArrayList<>();

    private RoomPhase phase = RoomPhase.IDLE;
    private int currentWaveIndex = 0;
    private int spawnedInCurrentWave = 0;
    private int betweenWavesTimer = 0;
    private static final int BETWEEN_WAVES_MIN_SECONDS = 3;
    private static final int BETWEEN_WAVES_MAX_SECONDS = 5;

    private final Set<UUID> activeMobIds = new HashSet<>();

    private static final int[] BETWEEN_WAVES_SECONDS_BOUNDS = new int[]{BETWEEN_WAVES_MIN_SECONDS, BETWEEN_WAVES_MAX_SECONDS};

    public void tickBetweenWavesTimer() { if (betweenWavesTimer > 0) betweenWavesTimer--; }
    public boolean isBetweenWavesTimerFinished() { return betweenWavesTimer <= 0; }
    public int getBetweenWavesTimer() { return betweenWavesTimer; }

    public boolean isCurrentWaveFinishedByMobUuids() {
        // Żeby uniknąć "instant finish" przy starcie, wymagamy że już spawnowano przynajmniej 1 moba.
        return phase == RoomPhase.WAVE_ACTIVE && spawnedInCurrentWave > 0 && activeMobIds.isEmpty();
    }
    public void onWaveStartClearsActiveMobs() {
        activeMobIds.clear();
        spawnedInCurrentWave = 0;
        betweenWavesTimer = 0;
    }

    public void registerSpawnedMob(UUID id) {
        if (id != null) {
            activeMobIds.add(id);
        }
    }

    public void onMobRemoved(UUID id) {
        if (id != null) {
            activeMobIds.remove(id);
        }
    }



    public RoomEncounter(BoundingBox roomBox, BlockPos rewardGatePos, BlockPos heroChestPos, List<WaveDefinition> waves) {
        this.roomBox = roomBox;
        this.rewardGatePos = rewardGatePos;
        this.heroChestPos = heroChestPos;
        this.waves = new ArrayList<>(waves);
    }

    public void setMobSpawnPoints(List<BlockPos> spawnPoints) {
        this.mobSpawnPoints = spawnPoints;
    }

    public Set<UUID> getActiveMobIds() {
        return activeMobIds;
    }

    public void startCurrentWave(int playerCount, ServerLevel level) {
        if (currentWaveIndex >= waves.size()) {
            phase = RoomPhase.REWARD;
            return;
        }

        // reset runtime state fali
        activeMobIds.clear();
        betweenWavesTimer = 0;

        WaveDefinition wave = waves.get(currentWaveIndex);
        spawnedInCurrentWave = wave.getScaledMobCount(playerCount);

        // Zamknięcie wrót (lock reward gate)
        DungeonGateHelper.lockGate(level, rewardGatePos);

        // Spawnowanie mobów
        if (!mobSpawnPoints.isEmpty()) {
            Random random = new Random();
            for (int i = 0; i < spawnedInCurrentWave; i++) {
                BlockPos pos = mobSpawnPoints.get(random.nextInt(mobSpawnPoints.size()));
                net.minecraft.world.entity.Entity zombie = EntityType.ZOMBIE.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                if (zombie != null) {
                    zombie.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                    level.addFreshEntity(zombie);
                    activeMobIds.add(zombie.getUUID());
                }
            }
        } else {
            // Bez spawnpointów po prostu kończymy falę od razu (odporność na błąd)
            spawnedInCurrentWave = 0;
        }

        phase = RoomPhase.WAVE_ACTIVE;
    }

    public boolean tryCompleteWaveAfterAllMobsGone() {
        if (!isCurrentWaveFinishedByMobUuids()) {
            return false;
        }

        // advance fazy: WAVE_ACTIVE -> (REWARD albo BETWEEN_WAVES)
        completeCurrentWave();
        return true;
    }

    // NOTE: completeCurrentWave zostaje private (tick handler wywołuje tryComplete...)
    private void completeCurrentWave() {
        currentWaveIndex++;
        if (currentWaveIndex >= waves.size()) {
            phase = RoomPhase.REWARD;
        } else {
            phase = RoomPhase.BETWEEN_WAVES;
            int min = BETWEEN_WAVES_SECONDS_BOUNDS[0];
            int max = BETWEEN_WAVES_SECONDS_BOUNDS[1];
            int betweenSeconds = min + (int) (Math.random() * (max - min + 1));
            betweenWavesTimer = betweenSeconds * 20;
        }
    }

    public void completeRoom() {
        phase = RoomPhase.COMPLETED;
    }

    public void resetRuntimeState() {
        phase = RoomPhase.IDLE;
        currentWaveIndex = 0;
        spawnedInCurrentWave = 0;
        betweenWavesTimer = 0;
        activeMobIds.clear();
    }

    public boolean isCompleted() {
        return phase == RoomPhase.COMPLETED;
    }

    public boolean isRewardReady() {
        return phase == RoomPhase.REWARD;
    }

    public int getCurrentWaveNumber() {
        if (waves.isEmpty()) {
            return 0;
        }

        return Math.min(currentWaveIndex + 1, waves.size());
    }

    public int getCurrentWaveIndex() {
        return currentWaveIndex;
    }

    public int getTotalWaves() {
        return waves.size();
    }

    public String getProgressText() {
        int alive = activeMobIds.size();
        int killed = spawnedInCurrentWave - alive;
        return "Fala " + getCurrentWaveNumber() + "/" + getTotalWaves()
                + " — " + killed + "/" + spawnedInCurrentWave;
    }

    public BoundingBox getRoomBox() {
        return roomBox;
    }

    public BlockPos getRewardGatePos() {
        return rewardGatePos;
    }

    public void setRewardGatePos(BlockPos rewardGatePos) {
        this.rewardGatePos = rewardGatePos;
    }

    public BlockPos getHeroChestPos() {
        return heroChestPos;
    }

    public void setHeroChestPos(BlockPos heroChestPos) {
        this.heroChestPos = heroChestPos;
    }

    public List<WaveDefinition> getWaves() {
        return waves;
    }

    public RoomPhase getPhase() {
        return phase;
    }

    public void setPhase(RoomPhase phase) {
        this.phase = phase;
    }

    public int getSpawnedInCurrentWave() {
        return spawnedInCurrentWave;
    }

    // opcjonalnie: przydaje się debug
    public int getActiveMobIdsCount() {
        return activeMobIds.size();
    }
}