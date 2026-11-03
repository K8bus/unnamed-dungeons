package com.k8bus.command.dungeon;

import com.k8bus.dungeon.ActiveDungeon;
import com.k8bus.dungeon.DungeonJsonDefinition;
import com.k8bus.dungeon.DungeonJsonManager;
import com.k8bus.dungeon.DungeonState;
import com.k8bus.dungeon.DungeonTemplateLoader;
import com.k8bus.dungeon.RoomEncounter;
import com.k8bus.dungeon.RoomSpaceDetector;
import com.k8bus.dungeon.WaveDefinition;
import com.k8bus.dungeon.handler.DungeonPhaseHandler;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class DungeonCreateCommand {

    public static int setBox(CommandContext<CommandSourceStack> ctx, int size, int kills) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            ServerLevel level = player.level();
            BlockPos pos = player.blockPosition();

            BoundingBox box = new BoundingBox(
                    pos.getX() - size, pos.getY() - size, pos.getZ() - size,
                    pos.getX() + size, pos.getY() + size, pos.getZ() + size
            );

            ActiveDungeon dungeon = new ActiveDungeon(box, kills);
            DungeonState.get(level).addDungeon(dungeon);

            ctx.getSource().sendSuccess(() -> Component.literal(
                    "Dungeon box ustawiony: " + box + " | kills required: " + kills
            ), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Błąd: " + e.getMessage()));
            return 0;
        }
    }

    public static int addRoom(CommandContext<CommandSourceStack> ctx, int wave1, int wave2, int wave3) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            ServerLevel level = player.level();
            BlockPos pos = player.blockPosition();

            ActiveDungeon dungeon = DungeonState.get(level).getDungeonAt(pos);

            if (dungeon == null) {
                ctx.getSource().sendFailure(Component.literal("Musisz stać wewnątrz dungeonu."));
                return 0;
            }

            BoundingBox roomBox = RoomSpaceDetector.detectRoomBounds(level, pos);

            RoomEncounter room = new RoomEncounter(
                    roomBox,
                    BlockPos.ZERO,
                    BlockPos.ZERO,
                    List.of(
                            new WaveDefinition(wave1, Math.max(1, wave1 / 2)),
                            new WaveDefinition(wave2, Math.max(1, wave2 / 2)),
                            new WaveDefinition(wave3, Math.max(1, wave3 / 2))
                    )
            );

            dungeon.addRoom(room);
            DungeonState.get(level).setDirty();

            ctx.getSource().sendSuccess(() -> Component.literal(
                    "Dodano pokój: " + roomBox
                            + " | fale: "
                            + wave1 + ", "
                            + wave2 + ", "
                            + wave3
            ), false);

            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Błąd: " + e.getMessage()));
            return 0;
        }
    }

    public static int loadTemplate(CommandContext<CommandSourceStack> ctx, String templateIdStr, int kills) {
        return loadTemplate(ctx, templateIdStr, kills, "none");
    }

    public static int loadTemplate(CommandContext<CommandSourceStack> ctx, String templateIdStr, int kills, String rotationStr) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            ServerLevel level = (ServerLevel) player.level();
            BlockPos pos = player.blockPosition();

            Identifier templateId = Identifier.parse(templateIdStr);
            net.minecraft.world.level.block.Rotation rotation = parseRotation(rotationStr);

            DungeonTemplateLoader.loadDungeon(level, pos, templateId, kills, rotation);

            ctx.getSource().sendSuccess(() -> Component.literal(
                    "Pomyślnie załadowano dungeon z szablonu/JSON: " + templateId + " (rotacja: " + rotation + ")"
            ), false);
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
            ctx.getSource().sendFailure(Component.literal("Błąd ładowania szablonu: " + e.getMessage()));
            return 0;
        }
    }

    public static int exportJson(CommandContext<CommandSourceStack> ctx, String idStr) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            ServerLevel level = player.level();
            ActiveDungeon dungeon = DungeonState.get(level).getDungeonAt(player.blockPosition());

            if (dungeon == null) {
                ctx.getSource().sendFailure(Component.literal("Musisz stać wewnątrz dungeonu, aby wyeksportować go do JSON."));
                return 0;
            }

            Identifier id = idStr.contains(":")
                    ? Identifier.parse(idStr)
                    : Identifier.fromNamespaceAndPath("unnamed-dungeons", idStr);

            Path exportedFile = DungeonJsonManager.exportDungeonToJson(level.getServer(), dungeon, id, Optional.of(id));

            ctx.getSource().sendSuccess(() -> Component.literal(
                    "Wyeksportowano definicję JSON dungeonu do: " + exportedFile
            ), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Błąd eksportu JSON: " + e.getMessage()));
            return 0;
        }
    }

    public static int applyJson(CommandContext<CommandSourceStack> ctx, String idStr) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            ServerLevel level = player.level();
            ActiveDungeon dungeon = DungeonState.get(level).getDungeonAt(player.blockPosition());

            if (dungeon == null) {
                ctx.getSource().sendFailure(Component.literal("Musisz stać wewnątrz dungeonu."));
                return 0;
            }

            Identifier id = idStr.contains(":")
                    ? Identifier.parse(idStr)
                    : Identifier.fromNamespaceAndPath("unnamed-dungeons", idStr);

            Optional<DungeonJsonDefinition> defOpt = DungeonJsonManager.loadDefinition(level.getServer(), id);
            if (defOpt.isEmpty()) {
                ctx.getSource().sendFailure(Component.literal("Nie znaleziono pliku JSON dla: " + id));
                return 0;
            }

            DungeonTemplateLoader.applyJsonToActiveDungeon(dungeon, defOpt.get());
            DungeonState.get(level).setDirty();

            ctx.getSource().sendSuccess(() -> Component.literal(
                    "Pomyślnie zaaplikowano konfigurację fal i mobów z JSON: " + id
            ), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Błąd wczytywania JSON: " + e.getMessage()));
            return 0;
        }
    }

    private static net.minecraft.world.level.block.Rotation parseRotation(String rot) {
        if (rot == null) return net.minecraft.world.level.block.Rotation.NONE;
        return switch (rot.toLowerCase()) {
            case "90", "clockwise_90", "cw_90" -> net.minecraft.world.level.block.Rotation.CLOCKWISE_90;
            case "180", "clockwise_180", "180_deg" -> net.minecraft.world.level.block.Rotation.CLOCKWISE_180;
            case "270", "counterclockwise_90", "ccw_90" -> net.minecraft.world.level.block.Rotation.COUNTERCLOCKWISE_90;
            default -> net.minecraft.world.level.block.Rotation.NONE;
        };
    }

    public static int clear(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerLevel level = ctx.getSource().getLevel();
            DungeonState state = DungeonState.get(level);

            // Fizycznie usuwamy bariery ze świata dla KAŻDEGO dungeona
            for (ActiveDungeon dungeon : state.getActiveDungeons()) {
                // Czyścimy główny obszar dungeonu
                DungeonPhaseHandler.removeBarriers(level, dungeon.getBoundingBox());

                // Czyścimy bariery z każdego pojedynczego pokoju (roomu), jeśli tam były generowane
                for (RoomEncounter room : dungeon.getRooms()) {
                    DungeonPhaseHandler.removeBarriers(level, room.getRoomBox());
                }
            }

            // świat jest czysty, możemy bezpiecznie usunąć dungeony z pamięci
            state.getActiveDungeons().clear();
            state.setDirty();

            ctx.getSource().sendSuccess(() -> Component.literal("Wyczyszczono wszystkie dungeony i usunięto bariery ochronne."), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Błąd: " + e.getMessage()));
            return 0;
        }
    }
}
