package com.k8bus.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.LevelData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DungeonSpawnpointHelper {

    // Trzymamy oryginalne spawnpointy w pamięci — wystarczy na czas sesji.
    // Wartość null oznacza brak ustawionego spawnpointu (gracz używa world spawn).
    private static final Map<UUID, ServerPlayer.RespawnConfig> originalSpawnpoints = new HashMap<>();

    private DungeonSpawnpointHelper() {}

    public static void saveAndSetSpawnpoint(ServerPlayer player, BoundingBox box) {
        // Zapisz oryginalny (może być null — gracz nie miał ustawionego spawnpointu)
        originalSpawnpoints.put(player.getUUID(), player.getRespawnConfig());

        // Ustaw tymczasowy w środku dungeonu
        BlockPos center = new BlockPos(
                (box.minX() + box.maxX()) / 2,
                (box.minY() + box.maxY()) / 2,
                (box.minZ() + box.maxZ()) / 2
        );

        LevelData.RespawnData respawnData = new LevelData.RespawnData(
                net.minecraft.core.GlobalPos.of(player.level().dimension(), center),
                player.getYRot(),
                player.getXRot()
        );

        player.setRespawnPosition(
                new ServerPlayer.RespawnConfig(respawnData, true),
                false
        );
    }

    public static void restoreSpawnpoint(ServerPlayer player) {
        // remove() zwraca null jeśli gracz nie był w mapie (np. podwójne wywołanie)
        if (!originalSpawnpoints.containsKey(player.getUUID())) {
            return;
        }

        ServerPlayer.RespawnConfig original = originalSpawnpoints.remove(player.getUUID());
        // original może być null — oznacza brak spawnpointu przed wejściem do dungeonu
        player.setRespawnPosition(original, false);
    }
}