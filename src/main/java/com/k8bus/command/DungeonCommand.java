package com.k8bus.command;

import com.k8bus.command.dungeon.DungeonCreateCommand;
import com.k8bus.command.dungeon.DungeonSetupCommand;
import com.k8bus.command.dungeon.DungeonStateCommand;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;

public class DungeonCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("dungeon")
                    .requires(source -> source.permissions().hasPermission(
                            new net.minecraft.server.permissions.Permission.HasCommandLevel(
                                    net.minecraft.server.permissions.PermissionLevel.GAMEMASTERS
                            )
                    ))

                    // /dungeon setbox <size> <killsRequired> — ustawia box wokół gracza
                    .then(Commands.literal("setbox")
                            .then(Commands.argument("size", IntegerArgumentType.integer(1, 100))
                                    .then(Commands.argument("kills", IntegerArgumentType.integer(1, 1000))
                                            .executes(ctx -> DungeonCreateCommand.setBox(ctx,
                                                    IntegerArgumentType.getInteger(ctx, "size"),
                                                    IntegerArgumentType.getInteger(ctx, "kills"))))))

                    // /dungeon status — pokaż aktywne dungeony
                    .then(Commands.literal("status")
                            .executes(DungeonStateCommand::status))

                    // /dungeon setwaitinggate — ustawia kratę poczekalni na blok, na który patrzy gracz
                    .then(Commands.literal("setwaitinggate")
                            .executes(ctx -> DungeonSetupCommand.setGate(ctx, DungeonSetupCommand.GateTarget.WAITING)))

                    // /dungeon setdungeongate — ustawia kratę wejścia do dungeonu na blok, na który patrzy gracz
                    .then(Commands.literal("setdungeongate")
                            .executes(ctx -> DungeonSetupCommand.setGate(ctx, DungeonSetupCommand.GateTarget.DUNGEON)))

                    // /dungeon setroomgate <index> — ustawia kratę wejścia do pokoju na blok, na który patrzy gracz
                    .then(Commands.literal("setroomgate")
                            .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                    .executes(ctx -> DungeonSetupCommand.setRoomGate(ctx, IntegerArgumentType.getInteger(ctx, "index")))))

                    // /dungeon setherochest <index> — ustawia hero chest na punkt, na który patrzy gracz
                    .then(Commands.literal("setherochest")
                            .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                    .executes(ctx -> DungeonSetupCommand.setHeroChest(ctx, IntegerArgumentType.getInteger(ctx, "index")))))

                    // /dungeon setroommobs <index> <mobsCsv> — ustawia niestandardową pulę mobów dla pokoju (np. "minecraft:blaze,minecraft:wither_skeleton")
                    .then(Commands.literal("setroommobs")
                            .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                    .then(Commands.argument("mobs", StringArgumentType.string())
                                            .executes(ctx -> DungeonSetupCommand.setRoomMobs(ctx,
                                                    IntegerArgumentType.getInteger(ctx, "index"),
                                                    StringArgumentType.getString(ctx, "mobs"))))))

                    // /dungeon export <id> — eksportuje skonfigurowany w grze dungeon (pokoje, fale, moby) do pliku JSON
                    .then(Commands.literal("export")
                            .then(Commands.argument("id", StringArgumentType.string())
                                    .executes(ctx -> DungeonCreateCommand.exportJson(ctx,
                                            StringArgumentType.getString(ctx, "id")))))

                    // /dungeon applyjson <id> — nakłada konfigurację fal i mobów z pliku JSON na aktualny dungeon
                    .then(Commands.literal("applyjson")
                            .then(Commands.argument("id", StringArgumentType.string())
                                    .executes(ctx -> DungeonCreateCommand.applyJson(ctx,
                                            StringArgumentType.getString(ctx, "id")))))

                    // /dungeon load <templateId> [killsRequired] [rotation] — ładuje strukturę z NBT (+ automatycznie nakłada JSON jeśli istnieje)
                    .then(Commands.literal("load")
                            .then(Commands.argument("template", StringArgumentType.string())
                                    .executes(ctx -> DungeonCreateCommand.loadTemplate(ctx,
                                            StringArgumentType.getString(ctx, "template"), 30, "none"))
                                    .then(Commands.argument("kills", IntegerArgumentType.integer(1, 1000))
                                            .executes(ctx -> DungeonCreateCommand.loadTemplate(ctx,
                                                    StringArgumentType.getString(ctx, "template"),
                                                    IntegerArgumentType.getInteger(ctx, "kills"), "none"))
                                            .then(Commands.argument("rotation", StringArgumentType.word())
                                                    .executes(ctx -> DungeonCreateCommand.loadTemplate(ctx,
                                                            StringArgumentType.getString(ctx, "template"),
                                                            IntegerArgumentType.getInteger(ctx, "kills"),
                                                            StringArgumentType.getString(ctx, "rotation")))))))

                    // /dungeon addroom <wave1> <wave2> <wave3> — dodaje pokój z trzema falami (automatyczny flood-fill rozmiaru)
                    .then(Commands.literal("addroom")
                            .then(Commands.argument("wave1", IntegerArgumentType.integer(1, 1000))
                                    .then(Commands.argument("wave2", IntegerArgumentType.integer(1, 1000))
                                            .then(Commands.argument("wave3", IntegerArgumentType.integer(1, 1000))
                                                    .executes(ctx -> DungeonCreateCommand.addRoom(ctx,
                                                            IntegerArgumentType.getInteger(ctx, "wave1"),
                                                            IntegerArgumentType.getInteger(ctx, "wave2"),
                                                            IntegerArgumentType.getInteger(ctx, "wave3")))))))

                    // /dungeon setwaitingspawn — ustawia punkt teleportu poczekalni
                    .then(Commands.literal("setwaitingspawn")
                            .executes(DungeonSetupCommand::setWaitingSpawn))

                    // /dungeon resign — rezygnuje z dungeonu i resetuje stan
                    .then(Commands.literal("resign")
                            .executes(DungeonStateCommand::resign))

                    // /dungeon reset — resetuje dungeon do stanu początkowego i otwiera kraty
                    .then(Commands.literal("reset")
                            .executes(DungeonStateCommand::reset))

                    // /dungeon forcereset — awaryjnie resetuje wszystkie dungeony i usuwa bariery/moby
                    .then(Commands.literal("forcereset")
                            .executes(DungeonStateCommand::forceReset))

                    // /dungeon clear — usuń wszystkie dungeony
                    .then(Commands.literal("clear")
                            .executes(DungeonCreateCommand::clear))
            );
        });
    }
}
