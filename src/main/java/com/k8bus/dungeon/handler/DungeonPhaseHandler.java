package com.k8bus.dungeon.handler;

import com.k8bus.UnnamedDungeons;
import com.k8bus.api.DungeonEvents;
import com.k8bus.dungeon.ActiveDungeon;
import com.k8bus.dungeon.DungeonGateHelper;
import com.k8bus.dungeon.DungeonSpawnpointHelper;
import com.k8bus.dungeon.DungeonState;
import com.k8bus.dungeon.RoomEncounter;
import com.k8bus.network.DungeonHudPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class DungeonPhaseHandler {

    public static void transitionPhase(ServerLevel level, ActiveDungeon dungeon, ActiveDungeon.DungeonPhase newPhase) {
        ActiveDungeon.DungeonPhase oldPhase = dungeon.getPhase();
        dungeon.setPhase(newPhase);
        if (oldPhase != newPhase) {
            DungeonEvents.DUNGEON_PHASE_CHANGE.invoker().onPhaseChange(level, dungeon, oldPhase, newPhase);
        }
    }

    public static void transitionPhaseWithTimer(ServerLevel level, ActiveDungeon dungeon, ActiveDungeon.DungeonPhase newPhase, int timerSeconds) {
        ActiveDungeon.DungeonPhase oldPhase = dungeon.getPhase();
        dungeon.setPhaseWithTimer(newPhase, timerSeconds);
        if (oldPhase != newPhase) {
            DungeonEvents.DUNGEON_PHASE_CHANGE.invoker().onPhaseChange(level, dungeon, oldPhase, newPhase);
        }
    }

    public static void tickPhase(ServerLevel level, ActiveDungeon dungeon, DungeonState state) {
        if (dungeon.getPhase() == ActiveDungeon.DungeonPhase.WAITING) {
            // Jeśli wszyscy gracze wyszli w czasie oczekiwania — wróć do IDLE
            if (dungeon.getPlayers().isEmpty()) {
                ActiveDungeon.DungeonPhase oldPhase = dungeon.getPhase();
                dungeon.resetRuntimeState();
                if (oldPhase != ActiveDungeon.DungeonPhase.IDLE) {
                    DungeonEvents.DUNGEON_PHASE_CHANGE.invoker().onPhaseChange(level, dungeon, oldPhase, ActiveDungeon.DungeonPhase.IDLE);
                }
                state.setDirty();
                UnnamedDungeons.LOGGER.info("Wszyscy gracze opuścili poczekalnię — dungeon wraca do IDLE.");
                return;
            }

            dungeon.tickPhaseTimer();

            if (dungeon.isPhaseTimerFinished()) {
                dungeon.snapshotPlayers();

                boolean lockedWaitingGate = DungeonGateHelper.lockGate(level, dungeon.getWaitingGatePos(), DungeonGateHelper.GateType.WAITING);

                transitionPhaseWithTimer(level, dungeon, ActiveDungeon.DungeonPhase.PREPARING, dungeon.getPreparingSeconds());
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
                boolean unlockedDungeonGate = DungeonGateHelper.unlockGate(level, dungeon.getDungeonGatePos(), DungeonGateHelper.GateType.DUNGEON);

                transitionPhase(level, dungeon, ActiveDungeon.DungeonPhase.ACTIVE);
                DungeonEvents.DUNGEON_START.invoker().onDungeonStart(level, dungeon);
                UnnamedDungeons.LOGGER.info(
                        "Dungeon przechodzi do fazy ACTIVE. Krata dungeonu otwarta: {}",
                        unlockedDungeonGate
                );
            }

            state.setDirty();
            return;
        }

        if (dungeon.getPhase() == ActiveDungeon.DungeonPhase.RESETTING) {
            // Czekamy aż wszyscy wyjdą z bounding boxa
            if (!isAnyNonBuilderPlayerInside(level, dungeon.getBoundingBox())) {
                transitionPhase(level, dungeon, ActiveDungeon.DungeonPhase.IDLE);
                dungeon.killCount = 0;
                state.setDirty();
                UnnamedDungeons.LOGGER.info("Wszyscy gracze opuścili dungeon — dungeon w stanie IDLE.");
            }

            return;
        }

        if (dungeon.getPhase() == ActiveDungeon.DungeonPhase.COMPLETED) {
            dungeon.tickPhaseTimer();
            int secondsLeft = Math.max(1, dungeon.getPhaseTimer());

            for (UUID pId : dungeon.getPlayers()) {
                ServerPlayer p = level.getServer().getPlayerList().getPlayer(pId);
                if (p != null) {
                    p.connection.send(new ClientboundSetActionBarTextPacket(
                            Component.literal("§6★ Ewakuacja za: §f" + secondsLeft + "s...")
                    ));
                }
            }

            if (dungeon.isPhaseTimerFinished()) {
                completeDungeon(level, dungeon, state);
            }

            state.setDirty();
            return;
        }

        if (dungeon.getPhase() == ActiveDungeon.DungeonPhase.ACTIVE) {
            DungeonRoomHandler.tickRooms(level, dungeon, state);
        }
    }

    public static void resetDungeon(ServerLevel level, ActiveDungeon dungeon, DungeonState state) {
        ActiveDungeon.DungeonPhase oldPhase = dungeon.getPhase();
        DungeonEvents.DUNGEON_RESET.invoker().onDungeonReset(level, dungeon, false);

        List<UUID> playersToRestore = new ArrayList<>(dungeon.getPlayers());

        for (UUID playerId : playersToRestore) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);

            if (player != null) {
                DungeonSpawnpointHelper.restoreSpawnpoint(player);
                player.setGameMode(dungeon.getOriginalGameMode(playerId));
                dungeon.clearOriginalGameMode(playerId);
                ServerPlayNetworking.send(player, new DungeonHudPayload(0, 0, 0));
            }
        }

        // Usuń żyjące moby ze wszystkich pokojów i zamknij ich bramy wyjściowe
        for (RoomEncounter room : dungeon.getRooms()) {
            room.clearSpawnedMobs(level);
            if (!room.getRewardGatePos().equals(BlockPos.ZERO)) {
                DungeonGateHelper.lockGate(level, room.getRewardGatePos(), DungeonGateHelper.GateType.ROOM);
            }
        }

        dungeon.clearAllBarriers(level);
        dungeon.restoreTrackedBlocks(level);

        boolean unlockedWaitingGate = DungeonGateHelper.unlockGate(level, dungeon.getWaitingGatePos(), DungeonGateHelper.GateType.WAITING);
        boolean lockedDungeonGate = DungeonGateHelper.lockGate(level, dungeon.getDungeonGatePos(), DungeonGateHelper.GateType.DUNGEON);

        dungeon.resetRuntimeState();
        if (oldPhase != ActiveDungeon.DungeonPhase.IDLE) {
            DungeonEvents.DUNGEON_PHASE_CHANGE.invoker().onPhaseChange(level, dungeon, oldPhase, ActiveDungeon.DungeonPhase.IDLE);
        }
        state.setDirty();

        UnnamedDungeons.LOGGER.info(
                "Dungeon zresetowany. Krata poczekalni otwarta: {}, krata dungeonu zamknięta: {}",
                unlockedWaitingGate,
                lockedDungeonGate
        );
    }

    public static void resignDungeon(ServerLevel level, ActiveDungeon dungeon, DungeonState state) {
        DungeonEvents.DUNGEON_RESET.invoker().onDungeonReset(level, dungeon, true);

        // Teleportuj wszystkich graczy do spawnpointa poczekalni
        BlockPos spawnPos = dungeon.getWaitingSpawnPos();

        List<UUID> playersToHandle = new ArrayList<>(dungeon.getPlayers());

        for (UUID playerId : playersToHandle) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);

            if (player != null) {
                if (!spawnPos.equals(BlockPos.ZERO)) {
                    player.teleportTo(
                            spawnPos.getX() + 0.5,
                            spawnPos.getY(),
                            spawnPos.getZ() + 0.5
                    );
                }

                // Przywróć spawnpoint, tryb gry i wyczyść HUD
                DungeonSpawnpointHelper.restoreSpawnpoint(player);
                player.setGameMode(dungeon.getOriginalGameMode(playerId));
                dungeon.clearOriginalGameMode(playerId);
                ServerPlayNetworking.send(player, new DungeonHudPayload(0, 0, 0));
            }
        }

        dungeon.getPlayers().clear();

        // Usuń żyjące moby ze wszystkich pokojów i zamknij ich bramy wyjściowe
        for (RoomEncounter room : dungeon.getRooms()) {
            room.clearSpawnedMobs(level);
            room.resetRuntimeState();
            if (!room.getRewardGatePos().equals(BlockPos.ZERO)) {
                DungeonGateHelper.lockGate(level, room.getRewardGatePos(), DungeonGateHelper.GateType.ROOM);
            }
        }

        dungeon.clearAllBarriers(level);
        dungeon.restoreTrackedBlocks(level);

        // Krata poczekalni OTWARTA, krata dungeonu ZAMKNIĘTA
        DungeonGateHelper.unlockGate(level, dungeon.getWaitingGatePos(), DungeonGateHelper.GateType.WAITING);
        DungeonGateHelper.lockGate(level, dungeon.getDungeonGatePos(), DungeonGateHelper.GateType.DUNGEON);

        // Resetuj stan gry dungeonu
        dungeon.killCount = 0;
        dungeon.phaseTimer = 0;
        dungeon.playerSnapshotCount = 0;

        // Przejdź do fazy RESETTING
        transitionPhase(level, dungeon, ActiveDungeon.DungeonPhase.RESETTING);
        state.setDirty();

        UnnamedDungeons.LOGGER.info(
                "Dungeon resign: gracze teleportowani do {}, krata poczekalni otwarta, krata dungeonu zamknięta, faza RESETTING.",
                spawnPos
        );
    }

    public static void completeDungeon(ServerLevel level, ActiveDungeon dungeon, DungeonState state) {
        ActiveDungeon.DungeonPhase oldPhase = dungeon.getPhase();
        DungeonEvents.DUNGEON_COMPLETE.invoker().onDungeonComplete(level, dungeon);
        dungeon.clearAllBarriers(level);

        for (UUID playerId : new ArrayList<>(dungeon.getPlayers())) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
            if (player != null) {
                DungeonSpawnpointHelper.restoreSpawnpoint(player);
                player.setGameMode(dungeon.getOriginalGameMode(playerId));
                dungeon.clearOriginalGameMode(playerId);
                ServerPlayNetworking.send(player, new DungeonHudPayload(0, 0, 0));

                if (dungeon.getWaitingSpawnPos() != null && !dungeon.getWaitingSpawnPos().equals(BlockPos.ZERO)) {
                    player.teleportTo(
                            level,
                            dungeon.getWaitingSpawnPos().getX() + 0.5,
                            dungeon.getWaitingSpawnPos().getY(),
                            dungeon.getWaitingSpawnPos().getZ() + 0.5,
                            Set.of(),
                            player.getYRot(),
                            player.getXRot(),
                            true
                    );
                }
            }
        }

        for (RoomEncounter room : dungeon.getRooms()) {
            room.clearSpawnedMobs(level);
        }

        if (dungeon.isRepeatable()) {
            dungeon.restoreTrackedBlocks(level);

            DungeonGateHelper.unlockGate(level, dungeon.getWaitingGatePos(), DungeonGateHelper.GateType.WAITING);
            DungeonGateHelper.lockGate(level, dungeon.getDungeonGatePos(), DungeonGateHelper.GateType.DUNGEON);
            for (RoomEncounter room : dungeon.getRooms()) {
                if (!room.getRewardGatePos().equals(BlockPos.ZERO)) {
                    DungeonGateHelper.lockGate(level, room.getRewardGatePos(), DungeonGateHelper.GateType.ROOM);
                }
            }

            dungeon.resetRuntimeState();
            ActiveDungeon.DungeonPhase nextPhase = isAnyNonBuilderPlayerInside(level, dungeon.getBoundingBox())
                    ? ActiveDungeon.DungeonPhase.RESETTING
                    : ActiveDungeon.DungeonPhase.IDLE;
            dungeon.setPhase(nextPhase);
            if (oldPhase != nextPhase) {
                DungeonEvents.DUNGEON_PHASE_CHANGE.invoker().onPhaseChange(level, dungeon, oldPhase, nextPhase);
            }
            state.setDirty();

            UnnamedDungeons.LOGGER.info(
                    "Powtarzalny dungeon ukończony i zresetowany do fazy {}.",
                    nextPhase
            );
            return;
        }

        boolean removedWaitingGate = DungeonGateHelper.removeGate(level, dungeon.getWaitingGatePos(), DungeonGateHelper.GateType.WAITING);
        boolean removedDungeonGate = DungeonGateHelper.removeGate(level, dungeon.getDungeonGatePos(), DungeonGateHelper.GateType.DUNGEON);

        transitionPhase(level, dungeon, ActiveDungeon.DungeonPhase.COMPLETED);
        state.removeDungeon(dungeon);

        UnnamedDungeons.LOGGER.info(
                "Dungeon ukończony! Usunięto kratę poczekalni: {}, usunięto kratę dungeonu: {}",
                removedWaitingGate,
                removedDungeonGate
        );
    }

    private static boolean isAnyNonBuilderPlayerInside(ServerLevel level, BoundingBox box) {
        for (ServerPlayer player : level.players()) {
            if (DungeonPlayerHandler.isBuilderMode(player)) {
                continue;
            }
            if (box.isInside(player.blockPosition())) {
                return true;
            }
        }
        return false;
    }

    public static void removeBarriers(ServerLevel level, BoundingBox box) {
        DungeonState state = DungeonState.get(level);
        for (ActiveDungeon d : state.getActiveDungeons()) {
            if (d.getBoundingBox().equals(box)) {
                d.clearAllBarriers(level);
                return;
            }
        }
    }
}
