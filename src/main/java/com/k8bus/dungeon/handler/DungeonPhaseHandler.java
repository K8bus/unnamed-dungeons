package com.k8bus.dungeon.handler;

import com.k8bus.UnnamedDungeons;
import com.k8bus.api.DungeonEvents;
import com.k8bus.dungeon.ActiveDungeon;
import com.k8bus.dungeon.DungeonGateHelper;
import com.k8bus.dungeon.DungeonSpawnpointHelper;
import com.k8bus.dungeon.DungeonState;
import com.k8bus.dungeon.RoomEncounter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DungeonPhaseHandler {

    private static final int PREPARING_SECONDS = 10;

    public static void tickPhase(ServerLevel level, ActiveDungeon dungeon, DungeonState state) {
        if (dungeon.getPhase() == ActiveDungeon.DungeonPhase.WAITING) {
            // Jeśli wszyscy gracze wyszli w czasie oczekiwania — wróć do IDLE
            if (dungeon.getPlayers().isEmpty()) {
                dungeon.resetRuntimeState();
                state.setDirty();
                UnnamedDungeons.LOGGER.info("Wszyscy gracze opuścili poczekalnię — dungeon wraca do IDLE.");
                return;
            }

            dungeon.tickPhaseTimer();

            if (dungeon.isPhaseTimerFinished()) {
                dungeon.snapshotPlayers();

                boolean lockedWaitingGate = DungeonGateHelper.lockGate(level, dungeon.getWaitingGatePos(), DungeonGateHelper.GateType.WAITING);

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
                boolean unlockedDungeonGate = DungeonGateHelper.unlockGate(level, dungeon.getDungeonGatePos(), DungeonGateHelper.GateType.DUNGEON);

                dungeon.setPhase(ActiveDungeon.DungeonPhase.ACTIVE);
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
            boolean anyPlayerInsideBox = false;

            for (ServerPlayer player : level.players()) {
                if (dungeon.getBoundingBox().isInside(player.blockPosition())) {
                    anyPlayerInsideBox = true;
                    break;
                }
            }

            if (!anyPlayerInsideBox) {
                dungeon.setPhase(ActiveDungeon.DungeonPhase.IDLE);
                dungeon.killCount = 0;
                state.setDirty();
                UnnamedDungeons.LOGGER.info("Wszyscy gracze opuścili dungeon — dungeon w stanie IDLE.");
            }

            return;
        }

        if (dungeon.getPhase() == ActiveDungeon.DungeonPhase.COMPLETED) {
            dungeon.tickPhaseTimer();
            int secondsLeft = (dungeon.getPhaseTimer() / 20) + 1;

            for (UUID pId : dungeon.getPlayers()) {
                ServerPlayer p = level.getServer().getPlayerList().getPlayer(pId);
                if (p != null) {
                    p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket(
                            net.minecraft.network.chat.Component.literal("§6★ Ewakuacja za: §f" + secondsLeft + "s...")
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
        DungeonEvents.DUNGEON_RESET.invoker().onDungeonReset(level, dungeon, false);

        List<UUID> playersToRestore = new ArrayList<>(dungeon.getPlayers());

        for (UUID playerId : playersToRestore) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);

            if (player != null) {
                DungeonSpawnpointHelper.restoreSpawnpoint(player);
                player.setGameMode(dungeon.getOriginalGameMode(playerId));
                dungeon.clearOriginalGameMode(playerId);
            }
        }

        // Usuń żyjące moby ze wszystkich pokojów
        for (RoomEncounter room : dungeon.getRooms()) {
            room.clearSpawnedMobs(level);
        }

        boolean unlockedWaitingGate = DungeonGateHelper.unlockGate(level, dungeon.getWaitingGatePos(), DungeonGateHelper.GateType.WAITING);
        boolean lockedDungeonGate = DungeonGateHelper.lockGate(level, dungeon.getDungeonGatePos(), DungeonGateHelper.GateType.DUNGEON);

        dungeon.resetRuntimeState();
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

        // Usuń żyjące moby ze wszystkich pokojów
        for (RoomEncounter room : dungeon.getRooms()) {
            room.clearSpawnedMobs(level);
            room.resetRuntimeState();
        }

        // Krata poczekalni OTWARTA
        DungeonGateHelper.unlockGate(level, dungeon.getWaitingGatePos(), DungeonGateHelper.GateType.WAITING);

        // Krata dungeonu ZAMKNIĘTA
        DungeonGateHelper.lockGate(level, dungeon.getDungeonGatePos(), DungeonGateHelper.GateType.DUNGEON);

        // Resetuj stan gry dungeonu
        dungeon.killCount = 0;
        dungeon.phaseTimer = 0;
        dungeon.playerSnapshotCount = 0;

        // Przejdź do fazy RESETTING
        dungeon.setPhase(ActiveDungeon.DungeonPhase.RESETTING);
        state.setDirty();

        UnnamedDungeons.LOGGER.info(
                "Dungeon resign: gracze teleportowani do {}, krata poczekalni otwarta, krata dungeonu zamknięta, faza RESETTING.",
                spawnPos
        );
    }

    public static void completeDungeon(ServerLevel level, ActiveDungeon dungeon, DungeonState state) {
        DungeonEvents.DUNGEON_COMPLETE.invoker().onDungeonComplete(level, dungeon);
        dungeon.clearAllBarriers(level);

        for (UUID playerId : dungeon.getPlayers()) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
            if (player != null) {
                DungeonSpawnpointHelper.restoreSpawnpoint(player);
                player.setGameMode(dungeon.getOriginalGameMode(playerId));
                dungeon.clearOriginalGameMode(playerId);

                if (dungeon.getWaitingSpawnPos() != null && !dungeon.getWaitingSpawnPos().equals(BlockPos.ZERO)) {
                    player.teleportTo(level, dungeon.getWaitingSpawnPos().getX() + 0.5, dungeon.getWaitingSpawnPos().getY(), dungeon.getWaitingSpawnPos().getZ() + 0.5, java.util.Set.of(), player.getYRot(), player.getXRot(), true);
                }
            }
        }

        for (RoomEncounter room : dungeon.getRooms()) {
            room.clearSpawnedMobs(level);
        }

        boolean removedWaitingGate = DungeonGateHelper.removeGate(level, dungeon.getWaitingGatePos(), DungeonGateHelper.GateType.WAITING);
        boolean removedDungeonGate = DungeonGateHelper.removeGate(level, dungeon.getDungeonGatePos(), DungeonGateHelper.GateType.DUNGEON);

        dungeon.setPhase(ActiveDungeon.DungeonPhase.COMPLETED);
        state.removeDungeon(dungeon);

        UnnamedDungeons.LOGGER.info(
                "Dungeon ukończony! Usunięto kratę poczekalni: {}, usunięto kratę dungeonu: {}",
                removedWaitingGate,
                removedDungeonGate
        );
    }

    public static void removeBarriers(ServerLevel level, BoundingBox box) {
        // Zastąpione bezpiecznym i zoptymalizowanym czyszczeniem ze śledzonej listy ActiveDungeon.clearAllBarriers
        DungeonState state = DungeonState.get(level);
        for (ActiveDungeon d : state.getActiveDungeons()) {
            if (d.getBoundingBox().equals(box)) {
                d.clearAllBarriers(level);
                return;
            }
        }
    }
}
