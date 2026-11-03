package com.k8bus.api;

import com.k8bus.dungeon.ActiveDungeon;
import com.k8bus.dungeon.DungeonJsonDefinition;
import com.k8bus.dungeon.DungeonJsonManager;
import com.k8bus.dungeon.DungeonState;
import com.k8bus.dungeon.DungeonTemplateLoader;
import com.k8bus.dungeon.RoomEncounter;
import com.k8bus.dungeon.RoomSpaceDetector;
import com.k8bus.dungeon.WaveDefinition;
import com.k8bus.dungeon.handler.DungeonPhaseHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Główny punkt wejścia (Fasada API) do zarządzania instancjami dungeonów z poziomu kodu innych modów.
 */
public final class DungeonApi {

    private DungeonApi() {}

    /**
     * Zwraca aktywny dungeon w podanej pozycji na świecie (jeśli istnieje).
     */
    public static Optional<ActiveDungeon> getDungeonAt(ServerLevel level, BlockPos pos) {
        return Optional.ofNullable(DungeonState.get(level).getDungeonAt(pos));
    }

    /**
     * Zwraca listę wszystkich zarejestrowanych dungeonów na danym wymiarze.
     */
    public static List<ActiveDungeon> getAllDungeons(ServerLevel level) {
        return List.copyOf(DungeonState.get(level).getActiveDungeons());
    }

    /**
     * Rejestruje nowy, programowo utworzony dungeon w danym świecie.
     */
    public static ActiveDungeon registerDungeon(ServerLevel level, ActiveDungeon dungeon) {
        DungeonState state = DungeonState.get(level);
        state.addDungeon(dungeon);
        state.setDirty();
        return dungeon;
    }

    /**
     * Tworzy i rejestruje prosty dungeon na podstawie BoundingBoxa i wymaganej liczby zabójstw.
     */
    public static ActiveDungeon createDungeon(ServerLevel level, BoundingBox box, int killsRequired) {
        ActiveDungeon dungeon = new ActiveDungeon(box, killsRequired);
        return registerDungeon(level, dungeon);
    }

    /**
     * Wczytuje i stawia strukturę dungeonu z pliku .nbt wraz z automatycznym parsowaniem metadanych Structure Blocków
     * oraz nałożeniem definicji JSON (z data/<namespace>/dungeons/<path>.json), jeśli istnieje.
     */
    public static void spawnFromTemplate(ServerLevel level, BlockPos origin, Identifier templateOrJsonId, int killsRequired, Rotation rotation) {
        DungeonTemplateLoader.loadDungeon(level, origin, templateOrJsonId, killsRequired, rotation);
    }

    /**
     * Wczytuje definicję JSON dungeonu z datapacka lub folderu świata.
     */
    public static Optional<DungeonJsonDefinition> loadJsonDefinition(MinecraftServer server, Identifier id) {
        return DungeonJsonManager.loadDefinition(server, id);
    }

    /**
     * Eksportuje konfigurację pokoi i fal istniejącego dungeonu do pliku JSON w folderze świata.
     */
    public static Path exportDungeonToJson(MinecraftServer server, ActiveDungeon dungeon, Identifier id, Optional<Identifier> templateId) throws IOException {
        return DungeonJsonManager.exportDungeonToJson(server, dungeon, id, templateId);
    }

    /**
     * Automatycznie wykrywa zamkniętą przestrzeń pokoju (algorytmem BFS 3D do ścian i bram DungeonGate)
     * i dodaje nowy pokój z podanymi falami do istniejącego dungeonu.
     */
    public static RoomEncounter detectAndAddRoom(ServerLevel level, ActiveDungeon dungeon, BlockPos startPos, List<WaveDefinition> waves) {
        BoundingBox roomBox = RoomSpaceDetector.detectRoomBounds(level, startPos);
        RoomEncounter room = new RoomEncounter(roomBox, BlockPos.ZERO, BlockPos.ZERO, waves);
        dungeon.addRoom(room);
        DungeonState.get(level).setDirty();
        return room;
    }

    /**
     * Resetuje podany dungeon do stanu początkowego (usuwa moby, otwiera bramę poczekalni, przywraca graczy).
     */
    public static void resetDungeon(ServerLevel level, ActiveDungeon dungeon) {
        DungeonPhaseHandler.resetDungeon(level, dungeon, DungeonState.get(level));
    }

    /**
     * Wymusza ukończenie podanego dungeonu.
     */
    public static void completeDungeon(ServerLevel level, ActiveDungeon dungeon) {
        DungeonPhaseHandler.completeDungeon(level, dungeon, DungeonState.get(level));
    }

    /**
     * Wymusza rezygnację z dungeonu i teleportację graczy do poczekalni.
     */
    public static void resignDungeon(ServerLevel level, ActiveDungeon dungeon) {
        DungeonPhaseHandler.resignDungeon(level, dungeon, DungeonState.get(level));
    }
}
