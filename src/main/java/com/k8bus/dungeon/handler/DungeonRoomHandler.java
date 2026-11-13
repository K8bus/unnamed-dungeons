package com.k8bus.dungeon.handler;

import com.k8bus.api.DungeonEvents;
import com.k8bus.block.ModBlocks;
import com.k8bus.dungeon.ActiveDungeon;
import com.k8bus.dungeon.DungeonGateHelper;
import com.k8bus.dungeon.DungeonState;
import com.k8bus.dungeon.RoomEncounter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

public class DungeonRoomHandler {

    public static void tickRooms(ServerLevel level, ActiveDungeon dungeon, DungeonState state) {
        boolean stateDirty = false;

        for (RoomEncounter room : dungeon.getRooms()) {

            // BETWEEN_WAVES: timer między falami (w sekundach, ponieważ DungeonTickHandler tyka co 20 ticków)
            if (room.getPhase() == RoomEncounter.RoomPhase.BETWEEN_WAVES) {
                room.tickBetweenWavesTimer();
                int secondsLeft = Math.max(1, room.getBetweenWavesTimer());

                if (!room.isBetweenWavesTimerFinished()) {
                    for (ServerPlayer player : level.players()) {
                        if (room.getRoomBox().isInside(player.blockPosition())) {
                            player.connection.send(new ClientboundSetActionBarTextPacket(
                                    Component.literal("§e⚔ Następna fala za: §f" + secondsLeft + "s...")
                            ));
                            if (secondsLeft <= 3) {
                                level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 0.6f, 1.2f);
                            }
                        }
                    }
                }

                if (room.isBetweenWavesTimerFinished()) {
                    room.startCurrentWave(dungeon.getPlayerSnapshotCount(), level);
                }

                stateDirty = true;
                continue;
            }

            // WAVE_ACTIVE: domknij falę po UUID (activeMobIds puste)
            if (room.getPhase() == RoomEncounter.RoomPhase.WAVE_ACTIVE) {
                boolean removedAny = room.getActiveMobIds().removeIf(uuid -> {
                    Entity entity = level.getEntity(uuid);
                    return entity == null || !entity.isAlive();
                });

                if (removedAny) {
                    stateDirty = true;
                }

                // Anti-stuck: podświetl ostatnie <= 2 moby w pokoju
                if (room.getActiveMobIds().size() <= 2 && !room.getActiveMobIds().isEmpty()) {
                    for (UUID mobId : room.getActiveMobIds()) {
                        Entity entity = level.getEntity(mobId);
                        if (entity instanceof LivingEntity living) {
                            living.addEffect(new MobEffectInstance(
                                    MobEffects.GLOWING, 30, 0, false, false, false
                            ));
                        }
                    }
                }

                boolean progressed = room.tryCompleteWaveAfterAllMobsGone(
                        dungeon.getBetweenWavesMinSeconds(),
                        dungeon.getBetweenWavesMaxSeconds()
                );
                if (progressed) {
                    DungeonEvents.WAVE_COMPLETE.invoker().onWaveComplete(
                            level,
                            dungeon,
                            room,
                            room.getCurrentWaveIndex() - 1
                    );
                    stateDirty = true;
                }
            }

            // REWARD: zawsze po ostatniej fali (albo gdy currentWaveIndex się skończy)
            if (room.getPhase() == RoomEncounter.RoomPhase.REWARD) {
                DungeonGateHelper.unlockGate(level, room.getRewardGatePos(), DungeonGateHelper.GateType.ROOM);

                if (!room.getHeroChestPos().equals(BlockPos.ZERO)) {
                    dungeon.recordRestorableBlock(room.getHeroChestPos(), level.getBlockState(room.getHeroChestPos()));
                    level.setBlock(room.getHeroChestPos(), ModBlocks.HERO_CHEST.defaultBlockState(), 3);
                    level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                            room.getHeroChestPos().getX() + 0.5, room.getHeroChestPos().getY() + 0.5, room.getHeroChestPos().getZ() + 0.5,
                            20, 0.4, 0.4, 0.4, 0.1);
                }

                DungeonEvents.ROOM_CLEAR.invoker().onRoomClear(level, dungeon, room);

                // Wiadomość i dźwięk ukończenia pokoju
                for (ServerPlayer player : level.players()) {
                    if (room.getRoomBox().isInside(player.blockPosition())) {
                        player.connection.send(new ClientboundSetTitleTextPacket(
                                Component.literal("§a§lPOKÓJ OCZYSZCZONY!")
                        ));
                        player.connection.send(new ClientboundSetSubtitleTextPacket(
                                Component.literal("§6Odbierz nagrodę ze skrzyni!")
                        ));
                        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.9f, 1.0f);
                    }
                }

                room.completeRoom();
                stateDirty = true;
                continue;
            }

            // IDLE: fala ma startować po wejściu w roomBox
            if (room.getPhase() == RoomEncounter.RoomPhase.IDLE) {
                boolean anyPlayerInsideRoom = false;

                for (ServerPlayer player : level.players()) {
                    if (DungeonPlayerHandler.isBuilderMode(player)) continue;
                    if (!dungeon.hasPlayer(player.getUUID())) continue;

                    if (room.getRoomBox().isInside(player.blockPosition())) {
                        anyPlayerInsideRoom = true;
                        break;
                    }
                }

                if (anyPlayerInsideRoom) {
                    room.startCurrentWave(dungeon.getPlayerSnapshotCount(), level);
                    stateDirty = true;
                }
            }
        }

        if (stateDirty) {
            state.setDirty();
        }
    }
}
