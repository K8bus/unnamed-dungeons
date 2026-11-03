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

    public static void loadDungeon(ServerLevel level, BlockPos origin, Identifier templateId, int killsRequired) {
        loadDungeon(level, origin, templateId, killsRequired, Rotation.NONE);
    }

    public static void loadDungeon(ServerLevel level, BlockPos origin, Identifier templateId, int killsRequired, Rotation rotation) {
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
                Object metaObj = info.nbt().getString("metadata");
                String meta = metaObj instanceof Optional ? ((Optional<?>) metaObj).map(Object::toString).orElse("") : metaObj.toString();
                BlockPos worldPos = info.pos();
                parseMetadata(meta, worldPos, data);
            }
        }

        // Jeśli wczytano plik JSON, nałóż jego definicje fal, mobów i loot table'i na odczytane pokoje z NBT
        if (jsonDefOpt.isPresent()) {
            DungeonJsonDefinition jsonDef = jsonDefOpt.get();
            jsonDef.eldritchChestLootTable().ifPresent(loot -> data.eldritchChestLootTable = loot);

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

        // Walidacja BoundingBoxa
        if (data.dungeonBoxStart == null || data.dungeonBoxEnd == null) {
            // Automatyczny fallback na rozmiar szablonu, jeśli budowniczy nie postawił markerów boxa
            net.minecraft.core.Vec3i size = template.getSize();
            data.dungeonBoxStart = origin;
            data.dungeonBoxEnd = origin.offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1);
        }

        BoundingBox dungeonBox = new BoundingBox(
                Math.min(data.dungeonBoxStart.getX(), data.dungeonBoxEnd.getX()),
                Math.min(data.dungeonBoxStart.getY(), data.dungeonBoxEnd.getY()),
                Math.min(data.dungeonBoxStart.getZ(), data.dungeonBoxEnd.getZ()),
                Math.max(data.dungeonBoxStart.getX(), data.dungeonBoxEnd.getX()),
                Math.max(data.dungeonBoxStart.getY(), data.dungeonBoxEnd.getY()),
                Math.max(data.dungeonBoxStart.getZ(), data.dungeonBoxEnd.getZ())
        );

        ActiveDungeon dungeon = new ActiveDungeon(dungeonBox, actualKillsRequired);

        if (data.waitingSpawnPos != null) {
            dungeon.setWaitingSpawnPos(data.waitingSpawnPos);
        } else {
            dungeon.setWaitingSpawnPos(origin.above());
        }

        if (data.waitingGatePos != null) {
            dungeon.setWaitingGatePos(data.waitingGatePos);
            DungeonGateHelper.unlockGate(level, data.waitingGatePos, DungeonGateHelper.GateType.WAITING);
        }

        if (data.dungeonGatePos != null) {
            dungeon.setDungeonGatePos(data.dungeonGatePos);
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
    }

    /**
     * Aplikuje samą konfigurację fal i mobów z pliku JSON do już istniejącego w świecie ActiveDungeon.
     */
    public static void applyJsonToActiveDungeon(ActiveDungeon dungeon, DungeonJsonDefinition jsonDef) {
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
        } else if (meta.equals("waiting_gate") || meta.equals("waitinggate")) {
            data.waitingGatePos = pos;
        } else if (meta.equals("dungeon_gate") || meta.equals("dungeongate")) {
            data.dungeonGatePos = pos;
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

                if (action.equals("gate")) {
                    room.rewardGatePos = pos;
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
}
