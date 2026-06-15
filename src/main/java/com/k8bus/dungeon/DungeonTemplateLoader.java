package com.k8bus.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DungeonTemplateLoader {

    public static void loadDungeon(ServerLevel level, BlockPos origin, Identifier templateId, int killsRequired) {
        StructureTemplateManager manager = level.getServer().getStructureManager();
        Optional<StructureTemplate> optionalTemplate = manager.get(templateId);

        if (optionalTemplate.isEmpty()) {
            throw new IllegalArgumentException("Template not found: " + templateId);
        }

        StructureTemplate template = optionalTemplate.get();
        DungeonTemplateData data = new DungeonTemplateData();

        StructurePlaceSettings settings = new StructurePlaceSettings();
        List<StructureTemplate.StructureBlockInfo> blocks = template.filterBlocks(origin, settings, Blocks.STRUCTURE_BLOCK);

        for (StructureTemplate.StructureBlockInfo info : blocks) {
            if (info.nbt() != null && info.nbt().contains("metadata")) {
                Object metaObj = info.nbt().getString("metadata");
                String meta = metaObj instanceof Optional ? ((Optional<?>) metaObj).map(Object::toString).orElse("") : metaObj.toString();
                BlockPos worldPos = info.pos(); // pos() is absolute because filterBlocks offsets it by origin!
                parseMetadata(meta, worldPos, data);
            }
        }

        // Place the structure
        template.placeInWorld(level, origin, origin, settings, level.getRandom(), 3);

        // After placing, physically remove the structure blocks
        for (StructureTemplate.StructureBlockInfo info : blocks) {
            BlockPos worldPos = info.pos();
            level.setBlock(worldPos, Blocks.AIR.defaultBlockState(), 3);
        }

        // Build ActiveDungeon
        if (data.dungeonBoxStart == null || data.dungeonBoxEnd == null) {
            throw new IllegalStateException("Brak markerów dungeon_box_start lub dungeon_box_end!");
        }

        BoundingBox dungeonBox = new BoundingBox(
                Math.min(data.dungeonBoxStart.getX(), data.dungeonBoxEnd.getX()),
                Math.min(data.dungeonBoxStart.getY(), data.dungeonBoxEnd.getY()),
                Math.min(data.dungeonBoxStart.getZ(), data.dungeonBoxEnd.getZ()),
                Math.max(data.dungeonBoxStart.getX(), data.dungeonBoxEnd.getX()),
                Math.max(data.dungeonBoxStart.getY(), data.dungeonBoxEnd.getY()),
                Math.max(data.dungeonBoxStart.getZ(), data.dungeonBoxEnd.getZ())
        );

        ActiveDungeon dungeon = new ActiveDungeon(dungeonBox, killsRequired);

        if (data.waitingGatePos != null) dungeon.setWaitingGatePos(data.waitingGatePos);
        if (data.dungeonGatePos != null) dungeon.setDungeonGatePos(data.dungeonGatePos);

        if (data.eldritchChestPos != null) {
            level.setBlock(data.eldritchChestPos, com.k8bus.block.ModBlocks.ELDRITCH_CHEST.defaultBlockState(), 3);
        }

        // Build rooms
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

            RoomEncounter room = new RoomEncounter(
                    roomBox,
                    roomData.rewardGatePos != null ? roomData.rewardGatePos : BlockPos.ZERO,
                    roomData.heroChestPos != null ? roomData.heroChestPos : BlockPos.ZERO,
                    roomData.waves.isEmpty() ? List.of(new WaveDefinition(5, 2)) : roomData.waves
            );

            if (!roomData.spawnPoints.isEmpty()) {
                room.setMobSpawnPoints(roomData.spawnPoints);
            }

            dungeon.addRoom(room);
        }

        DungeonState state = DungeonState.get(level);
        state.addDungeon(dungeon);
        state.setDirty();
    }

    private static void parseMetadata(String meta, BlockPos pos, DungeonTemplateData data) {
        String[] parts = meta.split("_");
        if (parts.length == 0) return;

        if (meta.equals("waiting_gate")) {
            data.waitingGatePos = pos;
        } else if (meta.equals("dungeon_gate")) {
            data.dungeonGatePos = pos;
        } else if (meta.equals("dungeon_box_start")) {
            data.dungeonBoxStart = pos;
        } else if (meta.equals("dungeon_box_end")) {
            data.dungeonBoxEnd = pos;
        } else if (meta.equals("eldritch_chest")) {
            data.eldritchChestPos = pos;
        } else if (parts[0].equals("room") && parts.length >= 3) {
            try {
                int roomId = Integer.parseInt(parts[1]);
                DungeonTemplateData.RoomData room = data.getOrCreateRoom(roomId);
                
                String action = parts[2];
                if (action.equals("gate")) {
                    room.rewardGatePos = pos;
                } else if (action.equals("hero") && parts.length >= 4 && parts[3].equals("chest")) {
                    room.heroChestPos = pos;
                } else if (action.equals("spawn")) {
                    room.spawnPoints.add(pos);
                } else if (action.equals("box") && parts.length >= 4) {
                    if (parts[3].equals("start")) room.boxStart = pos;
                    else if (parts[3].equals("end")) room.boxEnd = pos;
                } else if (action.equals("waves") && parts.length >= 4) {
                    for (int i = 3; i < parts.length; i++) {
                        int baseCount = Integer.parseInt(parts[i]);
                        room.waves.add(new WaveDefinition(baseCount, Math.max(1, baseCount / 2)));
                    }
                }
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
    }
}
