package com.k8bus.dungeon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.k8bus.UnnamedDungeons;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Obsługa ładowania i eksportowania definicji dungeonów w formacie JSON (Data-Driven).
 * Wczytuje pliki z datapacków: data/<namespace>/dungeons/<path>.json
 * oraz z folderu świata: <world>/dungeons/<namespace>/<path>.json
 */
public final class DungeonJsonManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private DungeonJsonManager() {}

    /**
     * Wczytuje definicję JSON dungeonu z datapacka (data/<namespace>/dungeons/<path>.json)
     * lub z lokalnego katalogu świata (<world>/dungeons/<namespace>/<path>.json).
     */
    public static Optional<DungeonJsonDefinition> loadDefinition(MinecraftServer server, Identifier id) {
        // 1. Sprawdź w lokalnym folderze świata (wyeksportowane JSON-y)
        Path worldFile = getExportFilePath(server, id);
        if (Files.isRegularFile(worldFile)) {
            try (Reader reader = Files.newBufferedReader(worldFile, StandardCharsets.UTF_8)) {
                JsonElement json = JsonParser.parseReader(reader);
                DataResult<DungeonJsonDefinition> result = DungeonJsonDefinition.CODEC.parse(JsonOps.INSTANCE, json);
                if (result.result().isPresent()) {
                    return result.result();
                }
            } catch (Exception e) {
                UnnamedDungeons.LOGGER.error("Błąd odczytu lokalnego pliku JSON dungeonu {}: {}", worldFile, e.getMessage());
            }
        }

        // 2. Sprawdź w ResourceManagerze (datapacki: data/<namespace>/dungeons/<path>.json)
        ResourceManager resourceManager = server.getResourceManager();
        Identifier resourceLocation = Identifier.fromNamespaceAndPath(id.getNamespace(), "dungeons/" + id.getPath() + ".json");
        Optional<Resource> resourceOpt = resourceManager.getResource(resourceLocation);

        if (resourceOpt.isPresent()) {
            try (Reader reader = resourceOpt.get().openAsReader()) {
                JsonElement json = JsonParser.parseReader(reader);
                DataResult<DungeonJsonDefinition> result = DungeonJsonDefinition.CODEC.parse(JsonOps.INSTANCE, json);
                return result.resultOrPartial(err ->
                        UnnamedDungeons.LOGGER.error("Błąd parsowania JSON dungeonu {}: {}", resourceLocation, err)
                );
            } catch (Exception e) {
                UnnamedDungeons.LOGGER.error("Nie udało się wczytać zasobu JSON dungeonu {}: {}", resourceLocation, e.getMessage());
            }
        }

        return Optional.empty();
    }

    /**
     * Eksportuje aktualnie skonfigurowany w grze ActiveDungeon (wraz z jego pokojami, falami i pulami mobów)
     * do gotowego pliku JSON w katalogu świata (<world>/dungeons/<namespace>/<path>.json).
     */
    public static Path exportDungeonToJson(MinecraftServer server, ActiveDungeon dungeon, Identifier id, Optional<Identifier> templateId) throws IOException {
        Map<String, DungeonJsonDefinition.RoomJsonConfig> roomsMap = new LinkedHashMap<>();

        for (int i = 0; i < dungeon.getRooms().size(); i++) {
            RoomEncounter room = dungeon.getRooms().get(i);
            roomsMap.put(
                    String.valueOf(i + 1),
                    new DungeonJsonDefinition.RoomJsonConfig(
                            room.getWaves(),
                            Optional.ofNullable(room.getRewardLootTable())
                    )
            );
        }

        DungeonJsonDefinition def = new DungeonJsonDefinition(
                templateId,
                dungeon.getKillRequired(),
                Optional.empty(),
                roomsMap
        );

        JsonElement jsonElement = DungeonJsonDefinition.CODEC.encodeStart(JsonOps.INSTANCE, def)
                .getOrThrow(IllegalStateException::new);

        Path targetPath = getExportFilePath(server, id);
        Files.createDirectories(targetPath.getParent());
        Files.writeString(targetPath, GSON.toJson(jsonElement), StandardCharsets.UTF_8);
        return targetPath;
    }

    /**
     * Eksportuje pełny stan ActiveDungeon (łącznie z BoundingBoxami i koordynatami bram) do pliku JSON.
     */
    public static Path exportFullActiveDungeonSnapshot(MinecraftServer server, ActiveDungeon dungeon, String name) throws IOException {
        JsonElement jsonElement = ActiveDungeon.CODEC.encodeStart(JsonOps.INSTANCE, dungeon)
                .getOrThrow(IllegalStateException::new);

        Path dir = server.getWorldPath(LevelResource.ROOT).resolve("dungeons").resolve("snapshots");
        Files.createDirectories(dir);
        Path file = dir.resolve(name + ".json");
        Files.writeString(file, GSON.toJson(jsonElement), StandardCharsets.UTF_8);
        return file;
    }

    public static Path getExportFilePath(MinecraftServer server, Identifier id) {
        return server.getWorldPath(LevelResource.ROOT)
                .resolve("dungeons")
                .resolve(id.getNamespace())
                .resolve(id.getPath() + ".json");
    }
}
