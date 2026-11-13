package com.k8bus.dungeon;

import com.k8bus.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DungeonTemplateLoader {

    public static ActiveDungeon loadDungeon(ServerLevel level, BlockPos origin, Identifier templateId, int killsRequired) {
        return loadDungeon(level, origin, templateId, killsRequired, Rotation.NONE);
    }

    public static ActiveDungeon loadDungeon(ServerLevel level, BlockPos origin, Identifier templateId, int killsRequired, Rotation rotation) {
        // Automatycznie sprawdź, czy istnieje definicja JSON dla tego identyfikatora (w datapacku lub folderze świata)
        Optional<DungeonJsonDefinition> jsonDefOpt = DungeonJsonManager.loadDefinition(level.getServer(), templateId);

        Identifier actualTemplateId = jsonDefOpt
                .flatMap(DungeonJsonDefinition::template)
                .orElse(templateId);

        int actualKillsRequired = jsonDefOpt
                .map(DungeonJsonDefinition::killsRequired)
                .orElse(killsRequired);

        StructureTemplateManager manager = level.getServer().getStructureManager();
        Optional<StructureTemplate> optionalTemplate = manager.get(actualTemplateId);

        if (optionalTemplate.isEmpty()) {
            throw new IllegalArgumentException("Nie znaleziono szablonu NBT: " + actualTemplateId);
        }

        StructureTemplate template = optionalTemplate.get();
        DungeonTemplateData data = new DungeonTemplateData();

        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(rotation != null ? rotation : Rotation.NONE)
                .setIgnoreEntities(false);

        // Znajdź wszystkie bloki struktury z metadanymi (z uwzględnieniem rotacji)
        List<StructureTemplate.StructureBlockInfo> blocks = template.filterBlocks(origin, settings, Blocks.STRUCTURE_BLOCK);

        for (StructureTemplate.StructureBlockInfo info : blocks) {
            if (info.nbt() != null && info.nbt().contains("metadata")) {
                String meta = info.nbt().getString("metadata").orElse("");
                BlockPos worldPos = info.pos();
                parseMetadata(meta, worldPos, data);
            }
        }

        // Jeśli wczytano plik JSON, nałóż jego definicje fal, mobów, loot table'i i rozmiarów bram
        if (jsonDefOpt.isPresent()) {
            DungeonJsonDefinition jsonDef = jsonDefOpt.get();
            jsonDef.eldritchChestLootTable().ifPresent(loot -> data.eldritchChestLootTable = loot);
            jsonDef.waitingGateSize().ifPresent(sz -> {
                data.waitingGateWidth = sz.width();
                data.waitingGateHeight = sz.height();
            });
            jsonDef.dungeonGateSize().ifPresent(sz -> {
                data.dungeonGateWidth = sz.width();
                data.dungeonGateHeight = sz.height();
            });

            for (Map.Entry<String, DungeonJsonDefinition.RoomJsonConfig> entry : jsonDef.rooms().entrySet()) {
                try {
                    int roomId = Integer.parseInt(entry.getKey());
                    DungeonTemplateData.RoomData roomData = data.getOrCreateRoom(roomId);
                    DungeonJsonDefinition.RoomJsonConfig roomJson = entry.getValue();

                    if (!roomJson.waves().isEmpty()) {
                        roomData.waves.clear();
                        roomData.waves.addAll(roomJson.waves());
                    }
                    roomJson.rewardLootTable().ifPresent(loot -> roomData.heroChestLootTable = loot);
                    roomJson.rewardGateSize().ifPresent(sz -> {
                        roomData.rewardGateWidth = sz.width();
                        roomData.rewardGateHeight = sz.height();
                    });
                } catch (NumberFormatException ignored) {}
            }
        }

        // Postaw strukturę w świecie
        template.placeInWorld(level, origin, origin, settings, level.getRandom(), 3);

        // Po postawieniu, fizycznie usuń wszystkie Structure Blocki ze świata
        for (StructureTemplate.StructureBlockInfo info : blocks) {
            BlockPos worldPos = info.pos();
            level.setBlock(worldPos, Blocks.AIR.defaultBlockState(), 3);
        }

        // Walidacja BoundingBoxa (uwzględnia rotację w fallbacku)
        BoundingBox dungeonBox;
        if (data.dungeonBoxStart != null && data.dungeonBoxEnd != null) {
            dungeonBox = new BoundingBox(
                    Math.min(data.dungeonBoxStart.getX(), data.dungeonBoxEnd.getX()),
                    Math.min(data.dungeonBoxStart.getY(), data.dungeonBoxEnd.getY()),
                    Math.min(data.dungeonBoxStart.getZ(), data.dungeonBoxEnd.getZ()),
                    Math.max(data.dungeonBoxStart.getX(), data.dungeonBoxEnd.getX()),
                    Math.max(data.dungeonBoxStart.getY(), data.dungeonBoxEnd.getY()),
                    Math.max(data.dungeonBoxStart.getZ(), data.dungeonBoxEnd.getZ())
            );
        } else {
            dungeonBox = template.getBoundingBox(settings, origin);
        }

        ActiveDungeon dungeon = new ActiveDungeon(dungeonBox, actualKillsRequired);
        if (data.eldritchChestLootTable != null) {
            dungeon.setEldritchChestLootTable(data.eldritchChestLootTable);
        }

        if (jsonDefOpt.isPresent()) {
            DungeonJsonDefinition jsonDef = jsonDefOpt.get();
            dungeon.setRepeatable(jsonDef.repeatable());
            jsonDef.timings().ifPresent(t -> dungeon.setTimings(
                    t.waitingSeconds(),
                    t.preparingSeconds(),
                    t.completionSeconds(),
                    t.betweenWavesMinSeconds(),
                    t.betweenWavesMaxSeconds()
            ));
        }

        if (data.waitingSpawnPos != null) {
            dungeon.setWaitingSpawnPos(data.waitingSpawnPos);
        } else {
            dungeon.setWaitingSpawnPos(origin.above());
        }

        if (data.waitingGatePos != null) {
            dungeon.setWaitingGatePos(data.waitingGatePos);
            if (data.waitingGateWidth > 0 && data.waitingGateHeight > 0) {
                DungeonGateHelper.setGateSize(level, data.waitingGatePos, data.waitingGateWidth, data.waitingGateHeight);
            }
            DungeonGateHelper.unlockGate(level, data.waitingGatePos, DungeonGateHelper.GateType.WAITING);
        }

        if (data.dungeonGatePos != null) {
            dungeon.setDungeonGatePos(data.dungeonGatePos);
            if (data.dungeonGateWidth > 0 && data.dungeonGateHeight > 0) {
                DungeonGateHelper.setGateSize(level, data.dungeonGatePos, data.dungeonGateWidth, data.dungeonGateHeight);
            }
            DungeonGateHelper.lockGate(level, data.dungeonGatePos, DungeonGateHelper.GateType.DUNGEON);
        }

        if (data.eldritchChestPos != null) {
            level.setBlock(data.eldritchChestPos, ModBlocks.ELDRITCH_CHEST.defaultBlockState(), 3);
        }

        // Budowa pokoi z NBT + JSON
        for (Map.Entry<Integer, DungeonTemplateData.RoomData> entry : data.rooms.entrySet()) {
            DungeonTemplateData.RoomData roomData = entry.getValue();

            if (roomData.boxStart == null || roomData.boxEnd == null) {
                continue;
            }

            BoundingBox roomBox = new BoundingBox(
                    Math.min(roomData.boxStart.getX(), roomData.boxEnd.getX()),
                    Math.min(roomData.boxStart.getY(), roomData.boxEnd.getY()),
                    Math.min(roomData.boxStart.getZ(), roomData.boxEnd.getZ()),
                    Math.max(roomData.boxStart.getX(), roomData.boxEnd.getX()),
                    Math.max(roomData.boxStart.getY(), roomData.boxEnd.getY()),
                    Math.max(roomData.boxStart.getZ(), roomData.boxEnd.getZ())
            );

            List<WaveDefinition> rawWaves = roomData.waves.isEmpty()
                    ? List.of(
                            new WaveDefinition(5, 2, roomData.mobPool),
                            new WaveDefinition(8, 3, roomData.mobPool),
                            new WaveDefinition(12, 4, roomData.mobPool)
                    )
                    : roomData.waves;

            List<WaveDefinition> waves = new ArrayList<>();
            for (WaveDefinition w : rawWaves) {
                if (w.getMobPool().isEmpty() && !roomData.mobPool.isEmpty()) {
                    waves.add(new WaveDefinition(w.getBaseCount(), w.getExtraPerPlayer(), roomData.mobPool));
                } else {
                    waves.add(w);
                }
            }

            RoomEncounter room = new RoomEncounter(
                    roomBox,
                    roomData.rewardGatePos != null ? roomData.rewardGatePos : BlockPos.ZERO,
                    roomData.heroChestPos != null ? roomData.heroChestPos : BlockPos.ZERO,
                    waves
            );

            if (roomData.rewardGatePos != null && roomData.rewardGateWidth > 0 && roomData.rewardGateHeight > 0) {
                DungeonGateHelper.setGateSize(level, roomData.rewardGatePos, roomData.rewardGateWidth, roomData.rewardGateHeight);
            }

            if (roomData.heroChestLootTable != null) {
                room.setRewardLootTable(roomData.heroChestLootTable);
            }

            if (!roomData.spawnPoints.isEmpty()) {
                room.setMobSpawnPoints(roomData.spawnPoints);
            }

            dungeon.addRoom(room);
        }

        DungeonState state = DungeonState.get(level);
        state.addDungeon(dungeon);
        state.setDirty();
        return dungeon;
    }

    /**
     * Aplikuje konfigurację z pliku JSON (fale, moby, loot table, czasy, powtarzalność i rozmiary bram)
     * do już istniejącego w świecie ActiveDungeon.
     */
    public static void applyJsonToActiveDungeon(ServerLevel level, ActiveDungeon dungeon, DungeonJsonDefinition jsonDef) {
        dungeon.setRepeatable(jsonDef.repeatable());
        jsonDef.timings().ifPresent(t -> dungeon.setTimings(
                t.waitingSeconds(),
                t.preparingSeconds(),
                t.completionSeconds(),
                t.betweenWavesMinSeconds(),
                t.betweenWavesMaxSeconds()
        ));
        jsonDef.eldritchChestLootTable().ifPresent(dungeon::setEldritchChestLootTable);

        if (level != null) {
            jsonDef.waitingGateSize().ifPresent(sz ->
                    DungeonGateHelper.setGateSize(level, dungeon.getWaitingGatePos(), sz.width(), sz.height()));
            jsonDef.dungeonGateSize().ifPresent(sz ->
                    DungeonGateHelper.setGateSize(level, dungeon.getDungeonGatePos(), sz.width(), sz.height()));
        }

        List<RoomEncounter> rooms = dungeon.getRooms();
        for (Map.Entry<String, DungeonJsonDefinition.RoomJsonConfig> entry : jsonDef.rooms().entrySet()) {
            try {
                int roomNumber = Integer.parseInt(entry.getKey());
                int index = roomNumber - 1;
                if (index >= 0 && index < rooms.size()) {
                    RoomEncounter room = rooms.get(index);
                    DungeonJsonDefinition.RoomJsonConfig cfg = entry.getValue();
                    if (!cfg.waves().isEmpty()) {
                        room.getWaves().clear();
                        room.getWaves().addAll(cfg.waves());
                    }
                    cfg.rewardLootTable().ifPresent(room::setRewardLootTable);
                    if (level != null) {
                        cfg.rewardGateSize().ifPresent(sz ->
                                DungeonGateHelper.setGateSize(level, room.getRewardGatePos(), sz.width(), sz.height()));
                    }
                }
            } catch (NumberFormatException ignored) {}
        }
    }

    private static void parseMetadata(String meta, BlockPos pos, DungeonTemplateData data) {
        if (meta == null || meta.isBlank()) return;

        String[] parts = meta.split("_");
        if (parts.length == 0) return;

        if (meta.equals("waiting_spawn") || meta.equals("waitingspawn")) {
            data.waitingSpawnPos = pos;
        } else if (meta.startsWith("waiting_gate") || meta.startsWith("waitinggate")) {
            data.waitingGatePos = pos;
            int[] size = parseGateSizeSuffix(meta);
            if (size != null) {
                data.waitingGateWidth = size[0];
                data.waitingGateHeight = size[1];
            }
        } else if (meta.startsWith("dungeon_gate") || meta.startsWith("dungeongate")) {
            data.dungeonGatePos = pos;
            int[] size = parseGateSizeSuffix(meta);
            if (size != null) {
                data.dungeonGateWidth = size[0];
                data.dungeonGateHeight = size[1];
            }
        } else if (meta.equals("dungeon_box_start")) {
            data.dungeonBoxStart = pos;
        } else if (meta.equals("dungeon_box_end")) {
            data.dungeonBoxEnd = pos;
        } else if (meta.startsWith("eldritch_chest") || meta.startsWith("eldritchchest")) {
            data.eldritchChestPos = pos;
            if (meta.contains(":")) {
                String lootStr = meta.substring(meta.indexOf(":") + 1);
                data.eldritchChestLootTable = Identifier.tryParse(lootStr);
            }
        } else if (parts[0].equals("room") && parts.length >= 3) {
            try {
                int roomId = Integer.parseInt(parts[1]);
                DungeonTemplateData.RoomData room = data.getOrCreateRoom(roomId);
                String action = parts[2];

                if (action.startsWith("gate")) {
                    room.rewardGatePos = pos;
                    int[] size = parseGateSizeSuffix(meta);
                    if (size != null) {
                        room.rewardGateWidth = size[0];
                        room.rewardGateHeight = size[1];
                    }
                } else if (action.equals("hero") || action.equals("chest")) {
                    room.heroChestPos = pos;
                    if (meta.contains(":")) {
                        String lootStr = meta.substring(meta.indexOf(":") + 1);
                        room.heroChestLootTable = Identifier.tryParse(lootStr);
                    }
                } else if (action.equals("spawn")) {
                    room.spawnPoints.add(pos);
                } else if (action.startsWith("mobs") && meta.contains(":")) {
                    // Format: room_1_mobs:minecraft:blaze,minecraft:wither_skeleton
                    String mobsStr = meta.substring(meta.indexOf(":") + 1);
                    for (String mobEntry : mobsStr.split(",")) {
                        Identifier mobId = Identifier.tryParse(mobEntry.trim());
                        if (mobId != null) {
                            room.mobPool.add(mobId);
                        }
                    }
                } else if (action.equals("box") && parts.length >= 4) {
                    if (parts[3].equals("start")) room.boxStart = pos;
                    else if (parts[3].equals("end")) room.boxEnd = pos;
                } else if (action.equals("waves") && parts.length >= 4) {
                    room.waves.clear();
                    for (int i = 3; i < parts.length; i++) {
                        int baseCount = Integer.parseInt(parts[i]);
                        room.waves.add(new WaveDefinition(baseCount, Math.max(1, baseCount / 2)));
                    }
                }
            } catch (NumberFormatException ignored) {}
        }
    }

    private static int[] parseGateSizeSuffix(String meta) {
        int colonIdx = meta.indexOf(':');
        if (colonIdx < 0) return null;
        String sizePart = meta.substring(colonIdx + 1).trim().toLowerCase();
        String[] dims = sizePart.split("x");
        if (dims.length != 2) return null;
        try {
            int w = Integer.parseInt(dims[0].trim());
            int h = Integer.parseInt(dims[1].trim());
            if (w > 0 && h > 0) {
                return new int[]{w, h};
            }
        } catch (NumberFormatException ignored) {}
        return null;
    }
}
