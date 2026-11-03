package com.k8bus.dungeon;

import com.k8bus.api.DungeonEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class RoomEncounter {

    public static final Codec<RoomEncounter> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BoundingBox.CODEC.fieldOf("room_box").forGetter(RoomEncounter::getRoomBox),
                    BlockPos.CODEC.fieldOf("reward_gate_pos").forGetter(RoomEncounter::getRewardGatePos),
                    BlockPos.CODEC.fieldOf("hero_chest_pos").forGetter(RoomEncounter::getHeroChestPos),
                    WaveDefinition.CODEC.listOf().fieldOf("waves").forGetter(RoomEncounter::getWaves),
                    Codec.STRING.fieldOf("phase").forGetter(room -> room.getPhase().name()),
                    Codec.INT.fieldOf("current_wave_index").forGetter(RoomEncounter::getCurrentWaveIndex),
                    Codec.INT.fieldOf("spawned_in_current_wave").forGetter(RoomEncounter::getSpawnedInCurrentWave),
                    Codec.STRING.listOf().optionalFieldOf("active_mob_ids", List.of()).forGetter(RoomEncounter::getActiveMobIdStrings),
                    BlockPos.CODEC.listOf().optionalFieldOf("mob_spawn_points", List.of()).forGetter(RoomEncounter::getMobSpawnPoints),
                    Identifier.CODEC.optionalFieldOf("reward_loot_table").forGetter(room -> Optional.ofNullable(room.rewardLootTable))
            ).apply(instance, (roomBox, rewardGatePos, heroChestPos, waves, phase, currentWaveIndex, spawnedInCurrentWave, activeMobIds, mobSpawnPoints, rewardLootTable) -> {
                RoomEncounter room = new RoomEncounter(roomBox, rewardGatePos, heroChestPos, waves);
                room.phase = RoomPhase.valueOf(phase);
                room.currentWaveIndex = currentWaveIndex;
                room.spawnedInCurrentWave = spawnedInCurrentWave;
                room.mobSpawnPoints = new ArrayList<>(mobSpawnPoints);
                room.rewardLootTable = rewardLootTable.orElse(null);
                for (String idStr : activeMobIds) {
                    try {
                        room.activeMobIds.add(UUID.fromString(idStr));
                    } catch (Exception ignored) {}
                }
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
    private Identifier rewardLootTable;
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
        return phase == RoomPhase.WAVE_ACTIVE && activeMobIds.isEmpty();
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

    public List<String> getActiveMobIdStrings() {
        List<String> list = new ArrayList<>();
        for (UUID id : activeMobIds) {
            list.add(id.toString());
        }
        return list;
    }

    public List<BlockPos> getMobSpawnPoints() {
        return mobSpawnPoints;
    }

    public void clearSpawnedMobs(ServerLevel level) {
        for (UUID id : activeMobIds) {
            net.minecraft.world.entity.Entity entity = level.getEntity(id);
            if (entity != null) {
                entity.discard();
            }
        }
        activeMobIds.clear();
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

    public Identifier getRewardLootTable() {
        return rewardLootTable;
    }

    public void setRewardLootTable(Identifier rewardLootTable) {
        this.rewardLootTable = rewardLootTable;
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
        clearSpawnedMobs(level);
        betweenWavesTimer = 0;

        WaveDefinition wave = waves.get(currentWaveIndex);
        spawnedInCurrentWave = wave.getScaledMobCount(playerCount);

        // Zamknięcie wrót (lock reward gate)
        DungeonGateHelper.lockGate(level, rewardGatePos, DungeonGateHelper.GateType.ROOM);

        // Wywołanie hooka API WAVE_START
        DungeonEvents.WAVE_START.invoker().onWaveStart(level, this, currentWaveIndex, wave);

        // Powiadomienie graczy o fali
        for (net.minecraft.server.level.ServerPlayer p : level.players()) {
            if (roomBox.isInside(p.blockPosition())) {
                p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(
                        net.minecraft.network.chat.Component.literal("§c§lFALA " + (currentWaveIndex + 1) + "/" + waves.size())
                ));
                p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(
                        net.minecraft.network.chat.Component.literal("§7Pokonaj wszystkich wrogów!")
                ));
                level.playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.RAID_HORN.value(), net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.0f);
            }
        }

        // Spawnowanie mobów
        Random random = new Random();
        for (int i = 0; i < spawnedInCurrentWave; i++) {
            BlockPos pos = null;
            if (!mobSpawnPoints.isEmpty()) {
                pos = mobSpawnPoints.get(random.nextInt(mobSpawnPoints.size()));
            } else {
                pos = findRandomSpawnPos(level, random);
            }

            if (pos != null) {
                EntityType<?> type = pickMobTypeForWave(wave, currentWaveIndex, random);
                net.minecraft.world.entity.Entity entity = type.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                if (entity != null) {
                    entity.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                    if (entity instanceof net.minecraft.world.entity.Mob mob) {
                        mob.setPersistenceRequired();
                        equipMobForWave(mob, currentWaveIndex, random);
                    }
                    // Wywołanie hooka API MOB_SPAWNED (pozwala nadpisać eq, statystyki, tagi itp.)
                    DungeonEvents.MOB_SPAWNED.invoker().onMobSpawned(level, this, currentWaveIndex, entity);

                    level.addFreshEntity(entity);
                    activeMobIds.add(entity.getUUID());

                    // Cząsteczki spawnu moba
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.05);
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.02);
                }
            } else {
                // Nie udało się znaleźć miejsca na spawn
                spawnedInCurrentWave--;
            }
        }

        if (spawnedInCurrentWave <= 0) {
            completeCurrentWave();
        } else {
            phase = RoomPhase.WAVE_ACTIVE;
        }
    }

    private EntityType<?> pickMobTypeForWave(WaveDefinition wave, int waveIndex, Random random) {
        if (wave != null && !wave.getMobPool().isEmpty()) {
            Identifier chosenId = wave.getMobPool().get(random.nextInt(wave.getMobPool().size()));
            Optional<EntityType<?>> customType = BuiltInRegistries.ENTITY_TYPE.getOptional(chosenId);
            if (customType.isPresent()) {
                return customType.get();
            }
        }

        if (waveIndex == 0) {
            return random.nextBoolean() ? EntityTypes.ZOMBIE : EntityTypes.HUSK;
        } else if (waveIndex == 1) {
            float r = random.nextFloat();
            if (r < 0.4f) return EntityTypes.SKELETON;
            if (r < 0.7f) return EntityTypes.STRAY;
            return EntityTypes.SPIDER;
        } else {
            float r = random.nextFloat();
            if (r < 0.35f) return EntityTypes.WITHER_SKELETON;
            if (r < 0.65f) return EntityTypes.VINDICATOR;
            if (r < 0.85f) return EntityTypes.PILLAGER;
            return EntityTypes.ZOMBIE;
        }
    }

    private void equipMobForWave(net.minecraft.world.entity.Mob mob, int waveIndex, Random random) {
        if (mob instanceof net.minecraft.world.entity.monster.skeleton.AbstractSkeleton skeleton) {
            skeleton.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BOW));
        } else if (mob instanceof net.minecraft.world.entity.monster.illager.Pillager pillager) {
            pillager.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CROSSBOW));
        } else if (mob instanceof net.minecraft.world.entity.monster.zombie.Zombie zombie && waveIndex >= 2) {
            if (random.nextBoolean()) {
                zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
            }
            zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CHAINMAIL_HELMET));
        }
    }

    private BlockPos findRandomSpawnPos(ServerLevel level, Random random) {
        int maxAttempts = 20;
        for (int i = 0; i < maxAttempts; i++) {
            int x = roomBox.minX() + random.nextInt(roomBox.maxX() - roomBox.minX() + 1);
            int y = roomBox.minY() + random.nextInt(roomBox.maxY() - roomBox.minY() + 1);
            int z = roomBox.minZ() + random.nextInt(roomBox.maxZ() - roomBox.minZ() + 1);
            BlockPos pos = new BlockPos(x, y, z);

            // Szukamy bloku powietrza na podłodze (blok niżej nie jest powietrzem)
            if (level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() &&
                level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty() &&
                !level.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty()) {
                return pos;
            }
        }
        return null;
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
