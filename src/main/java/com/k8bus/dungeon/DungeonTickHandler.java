package com.k8bus.dungeon;

import com.k8bus.UnnamedDungeons;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DungeonTickHandler {

    private static final int WAITING_SECONDS = 30;
    private static final int PREPARING_SECONDS = 10;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(DungeonTickHandler::onServerTick);
        ServerLivingEntityEvents.AFTER_DEATH.register(DungeonTickHandler::onEntityDeath);
    }

    private static void onServerTick(MinecraftServer server) {
        // Sprawdzaj tylko co 20 ticków (1 sekunda) żeby nie obciążać
        if (server.getTickCount() % 20 != 0) return;

        for (ServerLevel level : server.getAllLevels()) {
            DungeonState state = DungeonState.get(level);

            for (ActiveDungeon dungeon : state.getActiveDungeons()) {
                if (dungeon.getPhase() == ActiveDungeon.DungeonPhase.COMPLETED) continue;

                tickDungeonPhase(level, dungeon, state);

                for (ServerPlayer player : level.players()) {
                    boolean inBox = dungeon.getBoundingBox().isInside(player.blockPosition());

                    if (isBuilderMode(player)) {
                        continue;
                    }

                    if (inBox && !dungeon.hasPlayer(player.getUUID())) {
                        onPlayerEnter(player, dungeon, state);
                    } else if (!inBox && dungeon.hasPlayer(player.getUUID())) {
                        onPlayerExit(player, dungeon, state);
                    }
                }
            }
        }
    }

    private static void tickDungeonPhase(ServerLevel level, ActiveDungeon dungeon, DungeonState state) {
        if (dungeon.getPhase() == ActiveDungeon.DungeonPhase.WAITING) {
            // Jeśli wszyscy gracze wyszli w czasie oczekiwania — wróć do IDLE
            if (dungeon.getPlayers().isEmpty()) {
                DungeonBarrierHelper.removeBarriers(level, dungeon.getBoundingBox());
                dungeon.resetRuntimeState();
                state.setDirty();
                UnnamedDungeons.LOGGER.info("Wszyscy gracze opuścili poczekalnię — dungeon wraca do IDLE.");
                return;
            }

            dungeon.tickPhaseTimer();

            if (dungeon.isPhaseTimerFinished()) {
                dungeon.snapshotPlayers();

                boolean lockedWaitingGate = DungeonGateHelper.lockGate(level, dungeon.getWaitingGatePos());

                dungeon.setPhaseWithTimer(ActiveDungeon.DungeonPhase.PREPARING, PREPARING_SECONDS);
                UnnamedDungeons.LOGGER.info(
                        "Dungeon przechodzi do fazy PREPARING. Snapshot graczy: {}, krata poczekalni zamknięta: {}",
                        dungeon.getPlayerSnapshotCount(),
                        lockedWaitingGate
                );
            }

            state.setDirty();
            return;
        }

        if (dungeon.getPhase() == ActiveDungeon.DungeonPhase.PREPARING) {
            dungeon.tickPhaseTimer();

            if (dungeon.isPhaseTimerFinished()) {
                boolean unlockedDungeonGate = DungeonGateHelper.unlockGate(level, dungeon.getDungeonGatePos());

                dungeon.setPhase(ActiveDungeon.DungeonPhase.ACTIVE);
                UnnamedDungeons.LOGGER.info(
                        "Dungeon przechodzi do fazy ACTIVE. Krata dungeonu otwarta: {}",
                        unlockedDungeonGate
                );
            }

            state.setDirty();
            return;
        }

        if (dungeon.getPhase() == ActiveDungeon.DungeonPhase.RESETTING) {
            // Bariery zostają dopóki wszyscy gracze nie wyjdą z bounding boxa
            boolean anyPlayerInsideBox = false;

            for (ServerPlayer player : level.players()) {
                if (dungeon.getBoundingBox().isInside(player.blockPosition())) {
                    anyPlayerInsideBox = true;
                    break;
                }
            }

            if (!anyPlayerInsideBox) {
                DungeonBarrierHelper.removeBarriers(level, dungeon.getBoundingBox());
                dungeon.setPhase(ActiveDungeon.DungeonPhase.IDLE);
                dungeon.killCount = 0;
                state.setDirty();
                UnnamedDungeons.LOGGER.info("Wszyscy gracze opuścili dungeon — bariery usunięte, dungeon w stanie IDLE.");
            }

            return;
        }

        if (dungeon.getPhase() == ActiveDungeon.DungeonPhase.ACTIVE) {
            tickRooms(level, dungeon, state);
        }
    }

    private static void tickRooms(ServerLevel level, ActiveDungeon dungeon, DungeonState state) {
        boolean stateDirty = false;

        for (RoomEncounter room : dungeon.getRooms()) {

            // BETWEEN_WAVES: timer między falami
            if (room.getPhase() == RoomEncounter.RoomPhase.BETWEEN_WAVES) {
                room.tickBetweenWavesTimer();

                if (room.isBetweenWavesTimerFinished()) {
                    room.startCurrentWave(dungeon.getPlayerSnapshotCount());
                }

                stateDirty = true;
                continue;
            }

            // WAVE_ACTIVE: 4.3 — domknij falę po UUID (activeMobIds puste)
            if (room.getPhase() == RoomEncounter.RoomPhase.WAVE_ACTIVE) {
                boolean progressed = room.tryCompleteWaveAfterAllMobsGone();
                if (progressed) {
                    stateDirty = true;
                }
            }

            // REWARD: zawsze po ostatniej fali (albo gdy currentWaveIndex się skończy)
            if (room.getPhase() == RoomEncounter.RoomPhase.REWARD) {
                DungeonGateHelper.unlockGate(level, room.getRewardGatePos());
                room.completeRoom();
                stateDirty = true;
                continue;
            }

            // IDLE: fala ma startować po wejściu w roomBox (4.1)
            if (room.getPhase() == RoomEncounter.RoomPhase.IDLE) {
                boolean anyPlayerInsideRoom = false;

                for (ServerPlayer player : level.players()) {
                    if (isBuilderMode(player)) continue;
                    if (!dungeon.hasPlayer(player.getUUID())) continue;

                    if (room.getRoomBox().isInside(player.blockPosition())) {
                        anyPlayerInsideRoom = true;
                        break;
                    }
                }

                if (anyPlayerInsideRoom) {
                    room.startCurrentWave(dungeon.getPlayerSnapshotCount());
                    stateDirty = true;
                }
            }
        }

        if (stateDirty) {
            state.setDirty();
        }
    }

    private static boolean isBuilderMode(ServerPlayer player) {
        return player.isCreative() || player.isSpectator();
    }

    private static void onPlayerEnter(ServerPlayer player, ActiveDungeon dungeon, DungeonState state) {
        dungeon.addPlayer(player.getUUID());

        if (dungeon.getPhase() == ActiveDungeon.DungeonPhase.IDLE) {
            dungeon.setPhaseWithTimer(ActiveDungeon.DungeonPhase.WAITING, WAITING_SECONDS);
            UnnamedDungeons.LOGGER.info("Dungeon przechodzi do fazy WAITING");

            // Postaw bariery na granicy bounding boxa
            DungeonBarrierHelper.placeBarriers(player.level(), dungeon.getBoundingBox());
        }

        // Zapisz oryginalny spawnpoint i ustaw tymczasowy
        DungeonSpawnpointHelper.saveAndSetSpawnpoint(player, dungeon.getBoundingBox());

        // Zapisz oryginalny tryb gry i ustaw Adventure mode
        dungeon.saveOriginalGameMode(player.getUUID(), player.gameMode.getGameModeForPlayer());
        player.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);

        state.setDirty();
        UnnamedDungeons.LOGGER.info("{} wszedł do dungeonu", player.getName().getString());
    }

    private static void onPlayerExit(ServerPlayer player, ActiveDungeon dungeon, DungeonState state) {
        dungeon.removePlayer(player.getUUID());

        // W fazie RESETTING gracze zostali już obsłużeni przez resignDungeon — nie przywracamy ponownie
        if (dungeon.getPhase() == ActiveDungeon.DungeonPhase.RESETTING) {
            state.setDirty();
            UnnamedDungeons.LOGGER.info("{} opuścił dungeon po rezygnacji", player.getName().getString());
            return;
        }

        // Przywróć spawnpoint i oryginalny tryb gry
        DungeonSpawnpointHelper.restoreSpawnpoint(player);
        player.setGameMode(dungeon.getOriginalGameMode(player.getUUID()));
        dungeon.clearOriginalGameMode(player.getUUID());

        state.setDirty();
        UnnamedDungeons.LOGGER.info("{} wyszedł z dungeonu", player.getName().getString());
    }

    public static void resetDungeon(ServerLevel level, ActiveDungeon dungeon, DungeonState state) {
        DungeonBarrierHelper.removeBarriers(level, dungeon.getBoundingBox());

        List<UUID> playersToRestore = new ArrayList<>(dungeon.getPlayers());

        for (UUID playerId : playersToRestore) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);

            if (player != null) {
                DungeonSpawnpointHelper.restoreSpawnpoint(player);
                player.setGameMode(dungeon.getOriginalGameMode(playerId));
                dungeon.clearOriginalGameMode(playerId);
            }
        }

        boolean unlockedWaitingGate = DungeonGateHelper.unlockGate(level, dungeon.getWaitingGatePos());
        boolean lockedDungeonGate = DungeonGateHelper.lockGate(level, dungeon.getDungeonGatePos());

        dungeon.resetRuntimeState();
        state.setDirty();

        UnnamedDungeons.LOGGER.info(
                "Dungeon zresetowany. Krata poczekalni otwarta: {}, krata dungeonu otwarta: {}",
                unlockedWaitingGate,
                lockedDungeonGate
        );
    }

    public static void resignDungeon(ServerLevel level, ActiveDungeon dungeon, DungeonState state) {
        // Teleportuj wszystkich graczy do spawnpointa poczekalni
        BlockPos spawnPos = dungeon.getWaitingSpawnPos();

        List<UUID> playersToHandle = new ArrayList<>(dungeon.getPlayers());

        for (UUID playerId : playersToHandle) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);

            if (player != null) {
                // Teleport do spawnpointa poczekalni
                if (!spawnPos.equals(BlockPos.ZERO)) {
                    player.teleportTo(
                            spawnPos.getX() + 0.5,
                            spawnPos.getY(),
                            spawnPos.getZ() + 0.5
                    );
                }

                // Przywróć spawnpoint i tryb gry
                DungeonSpawnpointHelper.restoreSpawnpoint(player);
                player.setGameMode(dungeon.getOriginalGameMode(playerId));
                dungeon.clearOriginalGameMode(playerId);
            }
        }

        dungeon.getPlayers().clear();

        // Krata poczekalni OTWARTA
        DungeonGateHelper.unlockGate(level, dungeon.getWaitingGatePos());

        // Krata dungeonu ZAMKNIĘTA
        DungeonGateHelper.lockGate(level, dungeon.getDungeonGatePos());

        // Resetuj stan gry dungeonu, ale NIE usuwaj barier (zostaną usunięte w RESETTING)
        dungeon.killCount = 0;
        dungeon.phaseTimer = 0;
        dungeon.playerSnapshotCount = 0;

        for (RoomEncounter room : dungeon.getRooms()) {
            room.resetRuntimeState();
        }

        // Przejdź do fazy RESETTING — bariery zostaną usunięte gdy wszyscy wyjdą
        dungeon.setPhase(ActiveDungeon.DungeonPhase.RESETTING);
        state.setDirty();

        UnnamedDungeons.LOGGER.info(
                "Dungeon resign: gracze teleportowani do {}, krata poczekalni otwarta, krata dungeonu zamknięta, faza RESETTING.",
                spawnPos
        );
    }

    private static void completeDungeon(ServerLevel level, ActiveDungeon dungeon, DungeonState state) {
        DungeonBarrierHelper.removeBarriers(level, dungeon.getBoundingBox());

        for (UUID playerId : dungeon.getPlayers()) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
            if (player != null) {
                DungeonSpawnpointHelper.restoreSpawnpoint(player);
                player.setGameMode(dungeon.getOriginalGameMode(playerId));
                dungeon.clearOriginalGameMode(playerId);
            }
        }

        boolean removedWaitingGate = DungeonGateHelper.removeGate(level, dungeon.getWaitingGatePos());
        boolean removedDungeonGate = DungeonGateHelper.removeGate(level, dungeon.getDungeonGatePos());

        dungeon.setPhase(ActiveDungeon.DungeonPhase.COMPLETED);
        state.removeDungeon(dungeon);

        UnnamedDungeons.LOGGER.info(
                "Dungeon ukończony! Usunięto kratę poczekalni: {}, usunięto kratę dungeonu: {}",
                removedWaitingGate,
                removedDungeonGate
        );
    }

    private static void handleRoomMobDeath(
            ServerLevel level,
            DungeonState state,
            ActiveDungeon dungeon,
            ServerPlayer player,
            LivingEntity entity
    ) {
        RoomEncounter room = dungeon.getRoomAt(entity.blockPosition());

        if (room == null) return;

        if (room.getPhase() == RoomEncounter.RoomPhase.REWARD
                || room.getPhase() == RoomEncounter.RoomPhase.COMPLETED) {
            return;
        }

        if (room.getPhase() == RoomEncounter.RoomPhase.BETWEEN_WAVES) {
            return;
        }

        // 4.1: start fali ma być po wejściu, a nie po death
        if (room.getPhase() == RoomEncounter.RoomPhase.IDLE) {
            return;
        }

        if (room.getPhase() != RoomEncounter.RoomPhase.WAVE_ACTIVE) {
            return;
        }

        // 4.3: death event usuwa UUID (tick domyka falę po pustym secie)
        room.onMobRemoved(entity.getUUID());

        state.setDirty();
    }

    private static void onEntityDeath(LivingEntity entity, net.minecraft.world.damagesource.DamageSource damageSource) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        if (!(damageSource.getEntity() instanceof ServerPlayer player)) return;

        DungeonState state = DungeonState.get(level);
        ActiveDungeon dungeon = state.getDungeonAt(entity.blockPosition());

        if (dungeon == null) return;
        if (dungeon.getPhase() != ActiveDungeon.DungeonPhase.ACTIVE) return;
        if (!dungeon.hasPlayer(player.getUUID())) return;

        if (!dungeon.getRooms().isEmpty()) {
            handleRoomMobDeath(level, state, dungeon, player, entity);
            return;
        }

        dungeon.incrementKillCount();

        UnnamedDungeons.LOGGER.info(
                "{} zabił moba w dungeonie: {}/{}",
                player.getName().getString(),
                dungeon.getKillCount(),
                dungeon.getKillRequired()
        );

        if (dungeon.isCompleted()) {
            completeDungeon(level, dungeon, state);
            return;
        }

        state.setDirty();
    }
}