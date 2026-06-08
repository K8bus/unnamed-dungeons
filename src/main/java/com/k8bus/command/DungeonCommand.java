package com.k8bus.command;

import com.k8bus.block.ModBlocks;
import com.k8bus.dungeon.*;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;

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
                                            .executes(ctx -> setBox(ctx,
                                                    IntegerArgumentType.getInteger(ctx, "size"),
                                                    IntegerArgumentType.getInteger(ctx, "kills"))))))

                    // /dungeon status — pokaż aktywne dungeony
                    .then(Commands.literal("status")
                            .executes(DungeonCommand::status))

                    // /dungeon setwaitinggate — ustawia kratę poczekalni na blok, na który patrzy gracz
                    .then(Commands.literal("setwaitinggate")
                            .executes(ctx -> setGate(ctx, GateTarget.WAITING)))

                    // /dungeon setdungeongate — ustawia kratę wejścia do dungeonu na blok, na który patrzy gracz
                    .then(Commands.literal("setdungeongate")
                            .executes(ctx -> setGate(ctx, GateTarget.DUNGEON)))

                    // /dungeon setroomgate <index> — ustawia kratę wejścia do pokoju na blok, na który patrzy gracz
                    .then(Commands.literal("setroomgate")
                            .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                    .executes(ctx -> setRoomGate(ctx, IntegerArgumentType.getInteger(ctx, "index")))))

                    // /dungeon addroom <size> <wave1> <wave2> <wave3> — dodaje pokój z trzema falami
                    .then(Commands.literal("addroom")
                            .then(Commands.argument("size", IntegerArgumentType.integer(1, 100))
                                    .then(Commands.argument("wave1", IntegerArgumentType.integer(1, 1000))
                                            .then(Commands.argument("wave2", IntegerArgumentType.integer(1, 1000))
                                                    .then(Commands.argument("wave3", IntegerArgumentType.integer(1, 1000))
                                                            .executes(ctx -> addRoom(ctx,
                                                                    IntegerArgumentType.getInteger(ctx, "size"),
                                                                    IntegerArgumentType.getInteger(ctx, "wave1"),
                                                                    IntegerArgumentType.getInteger(ctx, "wave2"),
                                                                    IntegerArgumentType.getInteger(ctx, "wave3"))))))))

                    // /dungeon setwaitingspawn — ustawia punkt teleportu poczekalni
                    .then(Commands.literal("setwaitingspawn")
                            .executes(DungeonCommand::setWaitingSpawn))

                    // /dungeon resign — rezygnuje z dungeonu i resetuje stan
                    .then(Commands.literal("resign")
                            .executes(DungeonCommand::resign))

                    // /dungeon reset — resetuje dungeon do stanu początkowego i otwiera kraty
                    .then(Commands.literal("reset")
                            .executes(DungeonCommand::reset))

                    // /dungeon clear — usuń wszystkie dungeony
                    .then(Commands.literal("clear")
                            .executes(DungeonCommand::clear))
            );
        });
    }

    private enum GateTarget {
        WAITING,
        DUNGEON
    }

    private static int setGate(CommandContext<CommandSourceStack> ctx, GateTarget target) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            ServerLevel level = player.level();

            ActiveDungeon dungeon = DungeonState.get(level).getDungeonAt(player.blockPosition());

            if (dungeon == null) {
                ctx.getSource().sendFailure(Component.literal("Musisz stać wewnątrz dungeonu."));
                return 0;
            }

            BlockHitResult hitResult = getLookedAtBlock(player);

            if (hitResult.getType() != HitResult.Type.BLOCK) {
                ctx.getSource().sendFailure(Component.literal("Musisz patrzeć na blok kraty."));
                return 0;
            }

            BlockPos gatePos = hitResult.getBlockPos();
            BlockState gateState = level.getBlockState(gatePos);

            if (!gateState.is(ModBlocks.DUNGEON_GATE)) {
                ctx.getSource().sendFailure(Component.literal("Wybrany blok nie jest Dungeon Gate."));
                return 0;
            }

            if (target == GateTarget.WAITING) {
                dungeon.setWaitingGatePos(gatePos);
                ctx.getSource().sendSuccess(() -> Component.literal("Ustawiono kratę poczekalni: " + gatePos), false);
            } else {
                dungeon.setDungeonGatePos(gatePos);
                ctx.getSource().sendSuccess(() -> Component.literal("Ustawiono kratę dungeonu: " + gatePos), false);
            }

            DungeonState.get(level).setDirty();
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Błąd: " + e.getMessage()));
            return 0;
        }
    }

    private static BlockHitResult getLookedAtBlock(ServerPlayer player) {
        double reachDistance = 8.0D;

        return player.level().clip(new ClipContext(
                player.getEyePosition(),
                player.getEyePosition().add(player.getLookAngle().scale(reachDistance)),
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player
        ));
    }

    private static int addRoom(CommandContext<CommandSourceStack> ctx, int size, int wave1, int wave2, int wave3) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            ServerLevel level = player.level();
            BlockPos pos = player.blockPosition();

            ActiveDungeon dungeon = DungeonState.get(level).getDungeonAt(pos);

            if (dungeon == null) {
                ctx.getSource().sendFailure(Component.literal("Musisz stać wewnątrz dungeonu."));
                return 0;
            }

            BoundingBox roomBox = new BoundingBox(
                    pos.getX() - size, pos.getY() - size, pos.getZ() - size,
                    pos.getX() + size, pos.getY() + size, pos.getZ() + size
            );

            RoomEncounter room = new RoomEncounter(
                    roomBox,
                    BlockPos.ZERO,
                    BlockPos.ZERO,
                    List.of(
                            new WaveDefinition(wave1),
                            new WaveDefinition(wave2),
                            new WaveDefinition(wave3)
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

    private static int setRoomGate(CommandContext<CommandSourceStack> ctx, int index) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            ServerLevel level = (ServerLevel) player.level();
            ActiveDungeon dungeon = DungeonState.get(level).getDungeonAt(player.blockPosition());

            if (dungeon == null) {
                ctx.getSource().sendFailure(Component.literal("Nie stoisz w dungeonie."));
                return 0;
            }

            if (index >= dungeon.getRooms().size()) {
                ctx.getSource().sendFailure(Component.literal("Brak pokoju o indeksie " + index));
                return 0;
            }

            BlockHitResult hit = getLookedAtBlock(player);
            if (hit.getType() != HitResult.Type.BLOCK) {
                ctx.getSource().sendFailure(Component.literal("Patrz na blok kraty."));
                return 0;
            }

            BlockPos gatePos = hit.getBlockPos();
            if (!level.getBlockState(gatePos).is(ModBlocks.DUNGEON_GATE)) {
                ctx.getSource().sendFailure(Component.literal("Wybrany blok nie jest Dungeon Gate."));
                return 0;
            }

            dungeon.getRooms().get(index).setRewardGatePos(gatePos);
            DungeonState.get(level).setDirty();

            ctx.getSource().sendSuccess(() -> Component.literal("Ustawiono kratę pokoju " + index + ": " + gatePos), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Błąd: " + e.getMessage()));
            return 0;
        }
    }

    private static int setWaitingSpawn(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            ServerLevel level = player.level();

            ActiveDungeon dungeon = DungeonState.get(level).getDungeonAt(player.blockPosition());

            if (dungeon == null) {
                ctx.getSource().sendFailure(Component.literal("Musisz stać wewnątrz dungeonu."));
                return 0;
            }

            dungeon.setWaitingSpawnPos(player.blockPosition());
            DungeonState.get(level).setDirty();

            ctx.getSource().sendSuccess(() -> Component.literal(
                    "Ustawiono spawnpoint poczekalni: " + player.blockPosition()
            ), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Błąd: " + e.getMessage()));
            return 0;
        }
    }

    private static int resign(CommandContext<CommandSourceStack> ctx) {
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

            DungeonTickHandler.resignDungeon(level, dungeon, state);

            ctx.getSource().sendSuccess(() -> Component.literal("Zrezygnowano z dungeonu."), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Błąd: " + e.getMessage()));
            return 0;
        }
    }

    private static int reset(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            ServerLevel level = player.level();
            DungeonState state = DungeonState.get(level);
            ActiveDungeon dungeon = state.getDungeonAt(player.blockPosition());

            if (dungeon == null) {
                ctx.getSource().sendFailure(Component.literal("Musisz stać wewnątrz dungeonu."));
                return 0;
            }

            DungeonTickHandler.resetDungeon(level, dungeon, state);

            ctx.getSource().sendSuccess(() -> Component.literal("Dungeon zresetowany."), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Błąd: " + e.getMessage()));
            return 0;
        }
    }

    private static int setBox(CommandContext<CommandSourceStack> ctx, int size, int kills) {
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

    private static int status(CommandContext<CommandSourceStack> ctx) {
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

    private static int clear(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerLevel level = ctx.getSource().getLevel();
            DungeonState state = DungeonState.get(level);

            // Fizycznie usuwamy bariery ze świata dla KAŻDEGO dungeona
            for (ActiveDungeon dungeon : state.getActiveDungeons()) {
                // Czyścimy główny obszar dungeonu
                DungeonBarrierHelper.removeBarriers(level, dungeon.getBoundingBox());

                // Czyścimy bariery z każdego pojedynczego pokoju (roomu), jeśli tam były generowane
                for (RoomEncounter room : dungeon.getRooms()) {
                    DungeonBarrierHelper.removeBarriers(level, room.getRoomBox());
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