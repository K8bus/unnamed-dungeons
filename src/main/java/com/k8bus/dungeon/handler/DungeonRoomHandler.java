package com.k8bus.dungeon.handler;

import com.k8bus.api.DungeonEvents;
import com.k8bus.dungeon.ActiveDungeon;
import com.k8bus.dungeon.DungeonGateHelper;
import com.k8bus.dungeon.DungeonState;
import com.k8bus.dungeon.RoomEncounter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class DungeonRoomHandler {

    public static void tickRooms(ServerLevel level, ActiveDungeon dungeon, DungeonState state) {
        boolean stateDirty = false;

        for (RoomEncounter room : dungeon.getRooms()) {

            // BETWEEN_WAVES: timer między falami
            if (room.getPhase() == RoomEncounter.RoomPhase.BETWEEN_WAVES) {
                room.tickBetweenWavesTimer();
                int secondsLeft = (room.getBetweenWavesTimer() / 20) + 1;

                for (ServerPlayer player : level.players()) {
                    if (room.getRoomBox().isInside(player.blockPosition())) {
                        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket(
                                net.minecraft.network.chat.Component.literal("§e⚔ Następna fala za: §f" + secondsLeft + "s...")
                        ));
                        if (room.getBetweenWavesTimer() % 20 == 0 && secondsLeft <= 3) {
                            level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.value(), net.minecraft.sounds.SoundSource.PLAYERS, 0.6f, 1.2f);
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
                    net.minecraft.world.entity.Entity entity = level.getEntity(uuid);
                    return entity == null || !entity.isAlive();
                });
                
                if (removedAny) {
                    stateDirty = true;
                }

                // Anti-stuck: podświetl ostatnie <= 2 moby w pokoju
                if (room.getActiveMobIds().size() <= 2 && !room.getActiveMobIds().isEmpty()) {
                    for (java.util.UUID mobId : room.getActiveMobIds()) {
                        net.minecraft.world.entity.Entity entity = level.getEntity(mobId);
                        if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
                            living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                    net.minecraft.world.effect.MobEffects.GLOWING, 30, 0, false, false, false
                            ));
                        }
                    }
                }

                boolean progressed = room.tryCompleteWaveAfterAllMobsGone();
                if (progressed) {
                    stateDirty = true;
                }
            }

            // REWARD: zawsze po ostatniej fali (albo gdy currentWaveIndex się skończy)
            if (room.getPhase() == RoomEncounter.RoomPhase.REWARD) {
                DungeonGateHelper.unlockGate(level, room.getRewardGatePos(), DungeonGateHelper.GateType.ROOM);
                
                if (!room.getHeroChestPos().equals(net.minecraft.core.BlockPos.ZERO)) {
                    level.setBlock(room.getHeroChestPos(), com.k8bus.block.ModBlocks.HERO_CHEST.defaultBlockState(), 3);
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.TOTEM_OF_UNDYING,
                            room.getHeroChestPos().getX() + 0.5, room.getHeroChestPos().getY() + 0.5, room.getHeroChestPos().getZ() + 0.5,
                            20, 0.4, 0.4, 0.4, 0.1);
                }

                DungeonEvents.ROOM_CLEAR.invoker().onRoomClear(level, dungeon, room);

                // Wiadomość i dźwięk ukończenia pokoju
                for (ServerPlayer player : level.players()) {
                    if (room.getRoomBox().isInside(player.blockPosition())) {
                        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(
                                net.minecraft.network.chat.Component.literal("§a§lPOKÓJ OCZYSZCZONY!")
                        ));
                        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(
                                net.minecraft.network.chat.Component.literal("§6Odbierz nagrodę ze skrzyni!")
                        ));
                        level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 0.9f, 1.0f);
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
