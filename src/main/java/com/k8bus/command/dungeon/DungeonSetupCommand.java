package com.k8bus.command.dungeon;

import com.k8bus.block.ModBlocks;
import com.k8bus.command.DungeonCommandUtils;
import com.k8bus.dungeon.ActiveDungeon;
import com.k8bus.dungeon.DungeonState;
import com.k8bus.dungeon.RoomEncounter;
import com.k8bus.dungeon.WaveDefinition;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;

public class DungeonSetupCommand {

    public enum GateTarget {
        WAITING,
        DUNGEON
    }

    public static int setGate(CommandContext<CommandSourceStack> ctx, GateTarget target) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            ServerLevel level = player.level();

            ActiveDungeon dungeon = DungeonState.get(level).getDungeonAt(player.blockPosition());

            if (dungeon == null) {
                ctx.getSource().sendFailure(Component.literal("Musisz stać wewnątrz dungeonu."));
                return 0;
            }

            BlockHitResult hitResult = DungeonCommandUtils.getLookedAtBlock(player);

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

    public static int setRoomGate(CommandContext<CommandSourceStack> ctx, int index) {
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

            BlockHitResult hit = DungeonCommandUtils.getLookedAtBlock(player);
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

    public static int setHeroChest(CommandContext<CommandSourceStack> ctx, int index) {
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

            BlockHitResult hit = DungeonCommandUtils.getLookedAtBlock(player);
            if (hit.getType() != HitResult.Type.BLOCK) {
                ctx.getSource().sendFailure(Component.literal("Patrz na blok."));
                return 0;
            }

            BlockPos chestPos = hit.getBlockPos();

            dungeon.getRooms().get(index).setHeroChestPos(chestPos);
            DungeonState.get(level).setDirty();

            ctx.getSource().sendSuccess(() -> Component.literal("Ustawiono Hero Chest pokoju " + index + ": " + chestPos), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Błąd: " + e.getMessage()));
            return 0;
        }
    }

    public static int setWaitingSpawn(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            ServerLevel level = player.level();

            ActiveDungeon dungeon = DungeonState.get(level).getDungeonAt(player.blockPosition());

            if (dungeon == null) {
                ctx.getSource().sendFailure(Component.literal("Musisz stać wewnątrz dungeonu."));
                return 0;
            }

            BlockPos spawnPos = player.blockPosition();
            dungeon.setWaitingSpawnPos(spawnPos);
            DungeonState.get(level).setDirty();

            ctx.getSource().sendSuccess(() -> Component.literal("Ustawiono spawnpoint poczekalni: " + spawnPos), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Błąd: " + e.getMessage()));
            return 0;
        }
    }

    public static int setRoomMobs(CommandContext<CommandSourceStack> ctx, int index, String mobsCsv) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            ServerLevel level = player.level();
            ActiveDungeon dungeon = DungeonState.get(level).getDungeonAt(player.blockPosition());

            if (dungeon == null) {
                ctx.getSource().sendFailure(Component.literal("Nie stoisz w dungeonie."));
                return 0;
            }

            if (index >= dungeon.getRooms().size()) {
                ctx.getSource().sendFailure(Component.literal("Brak pokoju o indeksie " + index));
                return 0;
            }

            List<Identifier> mobPool = new ArrayList<>();
            for (String entry : mobsCsv.split(",")) {
                Identifier id = Identifier.tryParse(entry.trim());
                if (id != null) {
                    mobPool.add(id);
                }
            }

            if (mobPool.isEmpty()) {
                ctx.getSource().sendFailure(Component.literal("Nie podano prawidłowych identyfikatorów mobów (np. minecraft:blaze,minecraft:skeleton)."));
                return 0;
            }

            RoomEncounter room = dungeon.getRooms().get(index);
            List<WaveDefinition> updatedWaves = new ArrayList<>();
            for (WaveDefinition wave : room.getWaves()) {
                updatedWaves.add(new WaveDefinition(wave.getBaseCount(), wave.getExtraPerPlayer(), mobPool));
            }
            room.getWaves().clear();
            room.getWaves().addAll(updatedWaves);

            DungeonState.get(level).setDirty();
            ctx.getSource().sendSuccess(() -> Component.literal("Ustawiono pulę mobów dla pokoju " + index + ": " + mobPool), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Błąd: " + e.getMessage()));
            return 0;
        }
    }
}
