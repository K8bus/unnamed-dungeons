package com.k8bus.dungeon;

import com.k8bus.dungeon.handler.DungeonCombatHandler;
import com.k8bus.dungeon.handler.DungeonPhaseHandler;
import com.k8bus.dungeon.handler.DungeonPlayerHandler;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public class DungeonTickHandler {

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(DungeonTickHandler::onServerTick);
        ServerLivingEntityEvents.AFTER_DEATH.register(DungeonCombatHandler::onEntityDeath);
    }

    private static void onServerTick(MinecraftServer server) {
        // Sprawdzaj tylko co 20 ticków (1 sekunda) żeby nie obciążać
        if (server.getTickCount() % 20 != 0) return;

        for (ServerLevel level : server.getAllLevels()) {
            DungeonState state = DungeonState.get(level);

            for (ActiveDungeon dungeon : state.getActiveDungeons()) {
                if (dungeon.getPhase() == ActiveDungeon.DungeonPhase.COMPLETED) continue;

                DungeonPhaseHandler.tickPhase(level, dungeon, state);
                DungeonPlayerHandler.handlePlayers(level, dungeon, state);
            }
        }
    }
}