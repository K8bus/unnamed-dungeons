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
                            .forGetter(state -> state.activeDungeons),
                    Codec.unboundedMap(net.minecraft.core.UUIDUtil.STRING_CODEC, Codec.INT)
                            .optionalFieldOf("player_wallets", java.util.Map.of())
                            .forGetter(state -> state.playerWallets)
            ).apply(instance, DungeonState::new)
    );

    public static final SavedDataType<DungeonState> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("unnamed-dungeons", "dungeon_state"),
            DungeonState::new,
            CODEC,
            DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES
    );

    private final List<ActiveDungeon> activeDungeons;
    private final java.util.Map<java.util.UUID, Integer> playerWallets;

    public DungeonState() {
        this.activeDungeons = new ArrayList<>();
        this.playerWallets = new java.util.HashMap<>();
    }

    private DungeonState(List<ActiveDungeon> dungeons, java.util.Map<java.util.UUID, Integer> wallets) {
        this.activeDungeons = new ArrayList<>(dungeons);
        this.playerWallets = new java.util.HashMap<>(wallets);
    }

    public int getWalletBalance(java.util.UUID uuid) {
        return playerWallets.getOrDefault(uuid, 0);
    }

    public void addWalletBalance(java.util.UUID uuid, int amount) {
        playerWallets.put(uuid, getWalletBalance(uuid) + amount);
        setDirty();
    }

    public void setWalletBalance(java.util.UUID uuid, int amount) {
        playerWallets.put(uuid, amount);
        setDirty();
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