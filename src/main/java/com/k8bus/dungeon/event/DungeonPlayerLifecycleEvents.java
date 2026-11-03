package com.k8bus.dungeon.event;

import com.k8bus.UnnamedDungeons;
import com.k8bus.dungeon.ActiveDungeon;
import com.k8bus.dungeon.DungeonSpawnpointHelper;
import com.k8bus.dungeon.DungeonState;
import com.k8bus.dungeon.handler.DungeonPhaseHandler;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.UUID;

public class DungeonPlayerLifecycleEvents {

    public static void initialize() {
        // 1. Obsługa rozłączenia gracza (Disconnect / Crash)
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.getPlayer();
            UUID playerId = player.getUUID();

            for (ServerLevel level : server.getAllLevels()) {
                DungeonState state = DungeonState.get(level);
                for (ActiveDungeon dungeon : state.getActiveDungeons()) {
                    if (dungeon.hasPlayer(playerId)) {
                        UnnamedDungeons.LOGGER.warn("Gracz {} rozłączył się podczas pobytu w dungeonie. Przywracanie stanu...", player.getName().getString());

                        // Przywróć spawnpoint i tryb gry
                        DungeonSpawnpointHelper.restoreSpawnpoint(player);
                        GameType originalGm = dungeon.getOriginalGameMode(playerId);
                        if (originalGm != null) {
                            player.setGameMode(originalGm);
                        }
                        dungeon.removePlayer(playerId);
                        dungeon.clearOriginalGameMode(playerId);

                        // Jeśli w dungeonie nie ma już żywych graczy, zresetuj lub zrezygnuj
                        if (dungeon.getPlayers().isEmpty()) {
                            if (dungeon.getPhase() == ActiveDungeon.DungeonPhase.WAITING || dungeon.getPhase() == ActiveDungeon.DungeonPhase.PREPARING) {
                                DungeonPhaseHandler.resetDungeon(level, dungeon, state);
                            } else if (dungeon.getPhase() == ActiveDungeon.DungeonPhase.ACTIVE) {
                                DungeonPhaseHandler.resignDungeon(level, dungeon, state);
                            }
                        }
                        state.setDirty();
                    }
                }
            }
        });

        // 2. Obsługa śmierci i respawnu gracza
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            UUID playerId = newPlayer.getUUID();
            ServerLevel level = (ServerLevel) newPlayer.level();
            DungeonState state = DungeonState.get(level);

            for (ActiveDungeon dungeon : state.getActiveDungeons()) {
                if (dungeon.hasPlayer(playerId)) {
                    // Sprawdź czy jakikolwiek gracz drużyny wciąż żyje wewnątrz dungeonu
                    boolean anyTeammateInside = false;
                    for (UUID pId : dungeon.getPlayers()) {
                        ServerPlayer p = level.getServer().getPlayerList().getPlayer(pId);
                        if (p != null && p.isAlive() && dungeon.getBoundingBox().isInside(p.blockPosition())) {
                            anyTeammateInside = true;
                            break;
                        }
                    }

                    if (!anyTeammateInside && dungeon.getPhase() == ActiveDungeon.DungeonPhase.ACTIVE) {
                        UnnamedDungeons.LOGGER.info("Wszyscy gracze drużyny zginęli w dungeonie. Automatyczny resign...");
                        DungeonPhaseHandler.resignDungeon(level, dungeon, state);
                    }
                }
            }
        });
    }
}
