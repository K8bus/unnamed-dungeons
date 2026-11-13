package com.k8bus.api;

import com.k8bus.dungeon.ActiveDungeon;
import com.k8bus.dungeon.RoomEncounter;
import com.k8bus.dungeon.WaveDefinition;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Publiczne hooki (Fabric Events) dla silnika Unnamed Dungeons.
 * Pozwalają zewnętrznym modom reagować na cykl życia dungeonu, fal i spawnowanych mobów.
 */
public final class DungeonEvents {

    private DungeonEvents() {}

    /**
     * Wywoływane przed dodaniem gracza do dungeonu.
     * Zwrócenie {@code false} blokuje wejście (np. wymagany klucz, poziom lub limit drużyny).
     */
    public static final Event<CanEnterDungeon> CAN_ENTER_DUNGEON = EventFactory.createArrayBacked(
            CanEnterDungeon.class,
            callbacks -> (level, dungeon, player) -> {
                for (CanEnterDungeon callback : callbacks) {
                    if (!callback.canEnter(level, dungeon, player)) {
                        return false;
                    }
                }
                return true;
            }
    );

    /**
     * Wywoływane przy każdej zmianie fazy dungeonu (np. IDLE -> WAITING -> PREPARING -> ACTIVE -> COMPLETED / RESETTING).
     */
    public static final Event<DungeonPhaseChange> DUNGEON_PHASE_CHANGE = EventFactory.createArrayBacked(
            DungeonPhaseChange.class,
            callbacks -> (level, dungeon, oldPhase, newPhase) -> {
                for (DungeonPhaseChange callback : callbacks) {
                    callback.onPhaseChange(level, dungeon, oldPhase, newPhase);
                }
            }
    );

    /**
     * Wywoływane, gdy dungeon przechodzi do fazy ACTIVE (otwarcie głównej bramy po odliczaniu).
     */
    public static final Event<DungeonStart> DUNGEON_START = EventFactory.createArrayBacked(
            DungeonStart.class,
            callbacks -> (level, dungeon) -> {
                for (DungeonStart callback : callbacks) {
                    callback.onDungeonStart(level, dungeon);
                }
            }
    );

    /**
     * Wywoływane, gdy cały dungeon zostaje ukończony (wszystkie pokoje wyczyszczone lub osiągnięty cel zabójstw).
     */
    public static final Event<DungeonComplete> DUNGEON_COMPLETE = EventFactory.createArrayBacked(
            DungeonComplete.class,
            callbacks -> (level, dungeon) -> {
                for (DungeonComplete callback : callbacks) {
                    callback.onDungeonComplete(level, dungeon);
                }
            }
    );

    /**
     * Wywoływane, gdy gracze rezygnują z dungeonu lub dungeon jest resetowany.
     */
    public static final Event<DungeonReset> DUNGEON_RESET = EventFactory.createArrayBacked(
            DungeonReset.class,
            callbacks -> (level, dungeon, resigned) -> {
                for (DungeonReset callback : callbacks) {
                    callback.onDungeonReset(level, dungeon, resigned);
                }
            }
    );

    /**
     * Wywoływane, gdy gracz wchodzi na teren dungeonu lub go opuszcza.
     */
    public static final Event<PlayerDungeonChange> PLAYER_CHANGE = EventFactory.createArrayBacked(
            PlayerDungeonChange.class,
            callbacks -> (level, dungeon, player, entered) -> {
                for (PlayerDungeonChange callback : callbacks) {
                    callback.onPlayerChange(level, dungeon, player, entered);
                }
            }
    );

    /**
     * Wywoływane w momencie rozpoczęcia nowej fali w pokoju.
     */
    public static final Event<WaveStart> WAVE_START = EventFactory.createArrayBacked(
            WaveStart.class,
            callbacks -> (level, room, waveIndex, waveDefinition) -> {
                for (WaveStart callback : callbacks) {
                    callback.onWaveStart(level, room, waveIndex, waveDefinition);
                }
            }
    );

    /**
     * Wywoływane po pokonaniu wszystkich mobów pojedynczej fali w pokoju.
     */
    public static final Event<WaveComplete> WAVE_COMPLETE = EventFactory.createArrayBacked(
            WaveComplete.class,
            callbacks -> (level, dungeon, room, completedWaveIndex) -> {
                for (WaveComplete callback : callbacks) {
                    callback.onWaveComplete(level, dungeon, room, completedWaveIndex);
                }
            }
    );

    /**
     * Wywoływane tuż po stworzeniu moba fali (przed lub w trakcie dodania do świata).
     * Pozwala zewnętrznym modom nadać mobom własny ekwipunek, statystyki, tagi lub bossbary.
     */
    public static final Event<MobSpawned> MOB_SPAWNED = EventFactory.createArrayBacked(
            MobSpawned.class,
            callbacks -> (level, room, waveIndex, entity) -> {
                for (MobSpawned callback : callbacks) {
                    callback.onMobSpawned(level, room, waveIndex, entity);
                }
            }
    );

    /**
     * Wywoływane, gdy wszystkie fale w pokoju zostaną pokonane (faza REWARD).
     */
    public static final Event<RoomClear> ROOM_CLEAR = EventFactory.createArrayBacked(
            RoomClear.class,
            callbacks -> (level, dungeon, room) -> {
                for (RoomClear callback : callbacks) {
                    callback.onRoomClear(level, dungeon, room);
                }
            }
    );

    /**
     * Wywoływane podczas otwierania skrzyni dungeonu ({@code LootCrateBlock}, {@code HeroChestBlock}, {@code EldritchChestBlock}).
     * Zwrócenie {@code false} pomija domyślny/tabelowy drop silnika, pozwalając modowi całkowicie przejąć generowanie łupu.
     */
    public static final Event<ChestOpened> CHEST_OPENED = EventFactory.createArrayBacked(
            ChestOpened.class,
            callbacks -> (level, dungeon, pos, player, chestState) -> {
                boolean allowDefaultLoot = true;
                for (ChestOpened callback : callbacks) {
                    if (!callback.onChestOpened(level, dungeon, pos, player, chestState)) {
                        allowDefaultLoot = false;
                    }
                }
                return allowDefaultLoot;
            }
    );

    @FunctionalInterface
    public interface CanEnterDungeon {
        boolean canEnter(ServerLevel level, ActiveDungeon dungeon, ServerPlayer player);
    }

    @FunctionalInterface
    public interface DungeonPhaseChange {
        void onPhaseChange(ServerLevel level, ActiveDungeon dungeon, ActiveDungeon.DungeonPhase oldPhase, ActiveDungeon.DungeonPhase newPhase);
    }

    @FunctionalInterface
    public interface DungeonStart {
        void onDungeonStart(ServerLevel level, ActiveDungeon dungeon);
    }

    @FunctionalInterface
    public interface DungeonComplete {
        void onDungeonComplete(ServerLevel level, ActiveDungeon dungeon);
    }

    @FunctionalInterface
    public interface DungeonReset {
        void onDungeonReset(ServerLevel level, ActiveDungeon dungeon, boolean resigned);
    }

    @FunctionalInterface
    public interface PlayerDungeonChange {
        void onPlayerChange(ServerLevel level, ActiveDungeon dungeon, ServerPlayer player, boolean entered);
    }

    @FunctionalInterface
    public interface WaveStart {
        void onWaveStart(ServerLevel level, RoomEncounter room, int waveIndex, WaveDefinition waveDefinition);
    }

    @FunctionalInterface
    public interface WaveComplete {
        void onWaveComplete(ServerLevel level, ActiveDungeon dungeon, RoomEncounter room, int completedWaveIndex);
    }

    @FunctionalInterface
    public interface MobSpawned {
        void onMobSpawned(ServerLevel level, RoomEncounter room, int waveIndex, Entity entity);
    }

    @FunctionalInterface
    public interface RoomClear {
        void onRoomClear(ServerLevel level, ActiveDungeon dungeon, RoomEncounter room);
    }

    @FunctionalInterface
    public interface ChestOpened {
        boolean onChestOpened(ServerLevel level, ActiveDungeon dungeon, BlockPos pos, ServerPlayer player, BlockState chestState);
    }
}
