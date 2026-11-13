package com.k8bus.dungeon.handler;

import com.k8bus.UnnamedDungeons;
import com.k8bus.api.DungeonEvents;
import com.k8bus.dungeon.ActiveDungeon;
import com.k8bus.dungeon.DungeonSpawnpointHelper;
import com.k8bus.dungeon.DungeonState;
import com.k8bus.dungeon.RoomEncounter;
import com.k8bus.network.DungeonHudPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

public class DungeonPlayerHandler {

    public static void handlePlayers(ServerLevel level, ActiveDungeon dungeon, DungeonState state) {
        for (ServerPlayer player : level.players()) {
            boolean inBox = dungeon.getBoundingBox().isInside(player.blockPosition());

            if (isBuilderMode(player)) {
                continue;
            }

            if (inBox && !dungeon.hasPlayer(player.getUUID())) {
                if (DungeonEvents.CAN_ENTER_DUNGEON.invoker().canEnter(level, dungeon, player)) {
                    onPlayerEnter(player, dungeon, state);
                }
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
        ServerLevel level = (ServerLevel) player.level();

        // Add player to the dungeon
        dungeon.addPlayer(player.getUUID());

        if (dungeon.getPhase() == ActiveDungeon.DungeonPhase.IDLE) {
            DungeonPhaseHandler.transitionPhaseWithTimer(
                    level,
                    dungeon,
                    ActiveDungeon.DungeonPhase.WAITING,
                    dungeon.getWaitingSeconds()
            );
            UnnamedDungeons.LOGGER.info("Dungeon przechodzi do fazy WAITING");
        }

        // Save original spawnpoint and set temporary spawnpoint
        DungeonSpawnpointHelper.saveAndSetSpawnpoint(player, dungeon.getBoundingBox());

        // Save original game mode and set Adventure mode (only if not already adventure)
        if (dungeon.getOriginalGameMode(player.getUUID()) == null || player.gameMode.getGameModeForPlayer() != GameType.ADVENTURE) {
            dungeon.saveOriginalGameMode(player.getUUID(), player.gameMode.getGameModeForPlayer());
        }
        player.setGameMode(GameType.ADVENTURE);

        DungeonEvents.PLAYER_CHANGE.invoker().onPlayerChange(level, dungeon, player, true);

        state.setDirty();
        UnnamedDungeons.LOGGER.info("{} wszedł do dungeonu", player.getName().getString());
    }

    private static void onPlayerExit(ServerPlayer player, ActiveDungeon dungeon, DungeonState state) {
        ServerLevel level = (ServerLevel) player.level();
        dungeon.removePlayer(player.getUUID());
        DungeonEvents.PLAYER_CHANGE.invoker().onPlayerChange(level, dungeon, player, false);

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
        ServerPlayNetworking.send(player, new DungeonHudPayload(0, 0, 0));

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

        ServerPlayNetworking.send(player, new DungeonHudPayload(wave, mobs, phaseTimer));
    }
}
