package com.k8bus.dungeon.handler;

import com.k8bus.UnnamedDungeons;
import com.k8bus.api.DungeonEvents;
import com.k8bus.dungeon.ActiveDungeon;
import com.k8bus.dungeon.DungeonSpawnpointHelper;
import com.k8bus.dungeon.DungeonState;
import com.k8bus.dungeon.RoomEncounter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class DungeonPlayerHandler {

    private static final int WAITING_SECONDS = 30;

    public static void handlePlayers(ServerLevel level, ActiveDungeon dungeon, DungeonState state) {
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
            
            if (dungeon.hasPlayer(player.getUUID())) {
                syncHud(player, dungeon);
            }
        }
    }

    public static boolean isBuilderMode(ServerPlayer player) {
        return player.isCreative() || player.isSpectator();
    }

    private static void onPlayerEnter(ServerPlayer player, ActiveDungeon dungeon, DungeonState state) {
        // Check if the player enters any room and start a wave if necessary
        for (RoomEncounter room : dungeon.getRooms()) {
            if (room.getPhase() == RoomEncounter.RoomPhase.IDLE && room.getRoomBox().isInside(player.blockPosition())) {
                room.setPhase(RoomEncounter.RoomPhase.WAVE_ACTIVE);
                room.onWaveStartClearsActiveMobs();
                room.startCurrentWave(dungeon.getPlayerSnapshotCount(), (ServerLevel) player.level());
                state.setDirty();
                break;
            }
        }

        // Add player to the dungeon
        dungeon.addPlayer(player.getUUID());

        if (dungeon.getPhase() == ActiveDungeon.DungeonPhase.IDLE) {
            dungeon.setPhaseWithTimer(ActiveDungeon.DungeonPhase.WAITING, WAITING_SECONDS);
            UnnamedDungeons.LOGGER.info("Dungeon przechodzi do fazy WAITING");
        }

        // Save original spawnpoint and set temporary spawnpoint
        DungeonSpawnpointHelper.saveAndSetSpawnpoint(player, dungeon.getBoundingBox());

        // Save original game mode and set Adventure mode (only if not already adventure)
        if (dungeon.getOriginalGameMode(player.getUUID()) == null || player.gameMode.getGameModeForPlayer() != net.minecraft.world.level.GameType.ADVENTURE) {
            dungeon.saveOriginalGameMode(player.getUUID(), player.gameMode.getGameModeForPlayer());
        }
        player.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);

        DungeonEvents.PLAYER_CHANGE.invoker().onPlayerChange((ServerLevel) player.level(), dungeon, player, true);

        state.setDirty();
        UnnamedDungeons.LOGGER.info("{} wszedł do dungeonu", player.getName().getString());
    }

    private static void onPlayerExit(ServerPlayer player, ActiveDungeon dungeon, DungeonState state) {
        dungeon.removePlayer(player.getUUID());
        DungeonEvents.PLAYER_CHANGE.invoker().onPlayerChange((ServerLevel) player.level(), dungeon, player, false);

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

        // Wyczyść HUD klienta
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, new com.k8bus.network.DungeonHudPayload(0, 0, 0));

        state.setDirty();
        UnnamedDungeons.LOGGER.info("{} wyszedł z dungeonu", player.getName().getString());
    }

    private static void syncHud(ServerPlayer player, ActiveDungeon dungeon) {
        int wave = 0;
        int mobs = 0;

        if (dungeon.getPhase() == ActiveDungeon.DungeonPhase.ACTIVE) {
            for (RoomEncounter room : dungeon.getRooms()) {
                if (room.getRoomBox().isInside(player.blockPosition())) {
                    if (room.getPhase() == RoomEncounter.RoomPhase.WAVE_ACTIVE || room.getPhase() == RoomEncounter.RoomPhase.BETWEEN_WAVES) {
                        wave = room.getCurrentWaveNumber();
                        mobs = room.getActiveMobIdsCount();
                    }
                    break;
                }
            }
        }

        int phaseTimer = 0;
        if (dungeon.getPhase() == ActiveDungeon.DungeonPhase.WAITING) {
            phaseTimer = dungeon.getPhaseTimer();
        }

        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, new com.k8bus.network.DungeonHudPayload(wave, mobs, phaseTimer));
    }
}
