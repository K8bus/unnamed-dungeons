package com.k8bus.command.dungeon;

import com.k8bus.dungeon.ActiveDungeon;
import com.k8bus.dungeon.DungeonState;
import com.k8bus.dungeon.RoomEncounter;
import com.k8bus.dungeon.handler.DungeonPhaseHandler;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class DungeonStateCommand {

    public static int status(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerLevel level = ctx.getSource().getLevel();
            DungeonState state = DungeonState.get(level);

            if (state.getActiveDungeons().isEmpty()) {
                ctx.getSource().sendSuccess(() -> Component.literal("Brak aktywnych dungeonów."), false);
            } else {
                for (ActiveDungeon d : state.getActiveDungeons()) {
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "Dungeon: " + d.getBoundingBox()
                                    + " | kills: " + d.getKillCount() + "/" + d.getKillRequired()
                                    + " | phase: " + d.getPhase()
                                    + " | waiting gate: " + d.getWaitingGatePos()
                                    + " | dungeon gate: " + d.getDungeonGatePos()
                                    + " | rooms: " + d.getRooms().size()
                    ), false);

                    for (int i = 0; i < d.getRooms().size(); i++) {
                        RoomEncounter room = d.getRooms().get(i);
                        int roomIndex = i + 1;

                        ctx.getSource().sendSuccess(() -> Component.literal(
                                "  Room " + roomIndex
                                        + ": " + room.getRoomBox()
                                        + " | phase: " + room.getPhase()
                                        + " | " + room.getProgressText()
                        ), false);
                    }
                }
            }

            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Błąd: " + e.getMessage()));
            return 0;
        }
    }

    public static int resign(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            ServerLevel level = player.level();
            DungeonState state = DungeonState.get(level);
            ActiveDungeon dungeon = state.getDungeonAt(player.blockPosition());

            if (dungeon == null) {
                ctx.getSource().sendFailure(Component.literal("Nie jesteś w żadnym dungeonie."));
                return 0;
            }

            if (dungeon.getPhase() == ActiveDungeon.DungeonPhase.IDLE
                    || dungeon.getPhase() == ActiveDungeon.DungeonPhase.COMPLETED
                    || dungeon.getPhase() == ActiveDungeon.DungeonPhase.RESETTING) {
                ctx.getSource().sendFailure(Component.literal("Dungeon nie jest aktywny."));
                return 0;
            }

            DungeonPhaseHandler.resignDungeon(level, dungeon, state);

            ctx.getSource().sendSuccess(() -> Component.literal("Zrezygnowano z dungeonu."), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Błąd: " + e.getMessage()));
            return 0;
        }
    }

    public static int reset(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            ServerLevel level = player.level();
            DungeonState state = DungeonState.get(level);
            ActiveDungeon dungeon = state.getDungeonAt(player.blockPosition());

            if (dungeon == null) {
                ctx.getSource().sendFailure(Component.literal("Musisz stać wewnątrz dungeonu."));
                return 0;
            }

            DungeonPhaseHandler.resetDungeon(level, dungeon, state);

            ctx.getSource().sendSuccess(() -> Component.literal("Dungeon zresetowany."), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Błąd: " + e.getMessage()));
            return 0;
        }
    }

    public static int forceReset(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerLevel level = ctx.getSource().getLevel();
            DungeonState state = DungeonState.get(level);

            if (state.getActiveDungeons().isEmpty()) {
                ctx.getSource().sendSuccess(() -> Component.literal("Brak aktywnych dungeonów do zresetowania."), false);
                return 1;
            }

            int count = 0;
            for (ActiveDungeon dungeon : new java.util.ArrayList<>(state.getActiveDungeons())) {
                DungeonPhaseHandler.resetDungeon(level, dungeon, state);
                count++;
            }

            final int finalCount = count;
            ctx.getSource().sendSuccess(() -> Component.literal("Pomyślnie zresetowano (" + finalCount + ") dungeonów, usunięto moby i przywrócono stan."), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Błąd: " + e.getMessage()));
            return 0;
        }
    }
}
