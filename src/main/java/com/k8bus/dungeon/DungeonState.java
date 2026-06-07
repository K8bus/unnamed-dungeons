package com.k8bus.dungeon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;

public class DungeonState extends SavedData {

    public static final Codec<DungeonState> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ActiveDungeon.CODEC.listOf().fieldOf("dungeons")
                            .forGetter(state -> state.activeDungeons)
            ).apply(instance, DungeonState::new)
    );

    public static final SavedDataType<DungeonState> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("unnamed-dungeons", "dungeon_state"),
            DungeonState::new,
            CODEC,
            DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES
    );

    private final List<ActiveDungeon> activeDungeons;

    public DungeonState() {
        this.activeDungeons = new ArrayList<>();
    }

    private DungeonState(List<ActiveDungeon> dungeons) {
        this.activeDungeons = new ArrayList<>(dungeons);
    }

    public static DungeonState get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public void addDungeon(ActiveDungeon dungeon) {
        activeDungeons.add(dungeon);
        setDirty();
    }

    public void removeDungeon(ActiveDungeon dungeon) {
        activeDungeons.remove(dungeon);
        setDirty();
    }

    public ActiveDungeon getDungeonAt(net.minecraft.core.BlockPos pos) {
        for (ActiveDungeon dungeon : activeDungeons) {
            if (dungeon.getBoundingBox().isInside(pos)) {
                return dungeon;
            }
        }
        return null;
    }

    public List<ActiveDungeon> getActiveDungeons() {
        return activeDungeons;
    }
}