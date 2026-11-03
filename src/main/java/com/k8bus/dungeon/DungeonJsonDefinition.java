package com.k8bus.dungeon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Definicja JSON dla dungeonu (Data-Driven JSON + opcjonalny szablon NBT).
 */
public record DungeonJsonDefinition(
        Optional<Identifier> template,
        int killsRequired,
        Optional<Identifier> eldritchChestLootTable,
        Map<String, RoomJsonConfig> rooms
) {

    public record RoomJsonConfig(
            List<WaveDefinition> waves,
            Optional<Identifier> rewardLootTable
    ) {
        public static final Codec<RoomJsonConfig> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        WaveDefinition.CODEC.listOf().optionalFieldOf("waves", List.of()).forGetter(RoomJsonConfig::waves),
                        Identifier.CODEC.optionalFieldOf("reward_loot_table").forGetter(RoomJsonConfig::rewardLootTable)
                ).apply(instance, RoomJsonConfig::new)
        );
    }

    public static final Codec<DungeonJsonDefinition> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Identifier.CODEC.optionalFieldOf("template").forGetter(DungeonJsonDefinition::template),
                    Codec.INT.optionalFieldOf("kills_required", 30).forGetter(DungeonJsonDefinition::killsRequired),
                    Identifier.CODEC.optionalFieldOf("eldritch_chest_loot_table").forGetter(DungeonJsonDefinition::eldritchChestLootTable),
                    Codec.unboundedMap(Codec.STRING, RoomJsonConfig.CODEC)
                            .optionalFieldOf("rooms", Map.of())
                            .forGetter(DungeonJsonDefinition::rooms)
            ).apply(instance, DungeonJsonDefinition::new)
    );
}
