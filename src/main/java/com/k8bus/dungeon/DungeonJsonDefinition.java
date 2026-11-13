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
        boolean repeatable,
        Optional<TimingsConfig> timings,
        Optional<GateSizeConfig> waitingGateSize,
        Optional<GateSizeConfig> dungeonGateSize,
        Optional<Identifier> eldritchChestLootTable,
        Map<String, RoomJsonConfig> rooms
) {

    public record TimingsConfig(
            int waitingSeconds,
            int preparingSeconds,
            int completionSeconds,
            int betweenWavesMinSeconds,
            int betweenWavesMaxSeconds
    ) {
        public static final Codec<TimingsConfig> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.INT.optionalFieldOf("waiting_seconds", 30).forGetter(TimingsConfig::waitingSeconds),
                        Codec.INT.optionalFieldOf("preparing_seconds", 10).forGetter(TimingsConfig::preparingSeconds),
                        Codec.INT.optionalFieldOf("completion_seconds", 15).forGetter(TimingsConfig::completionSeconds),
                        Codec.INT.optionalFieldOf("between_waves_min_seconds", 3).forGetter(TimingsConfig::betweenWavesMinSeconds),
                        Codec.INT.optionalFieldOf("between_waves_max_seconds", 5).forGetter(TimingsConfig::betweenWavesMaxSeconds)
                ).apply(instance, TimingsConfig::new)
        );
    }

    public record GateSizeConfig(int width, int height) {
        public static final Codec<GateSizeConfig> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.INT.fieldOf("width").forGetter(GateSizeConfig::width),
                        Codec.INT.fieldOf("height").forGetter(GateSizeConfig::height)
                ).apply(instance, GateSizeConfig::new)
        );
    }

    public record RoomJsonConfig(
            List<WaveDefinition> waves,
            Optional<Identifier> rewardLootTable,
            Optional<GateSizeConfig> rewardGateSize
    ) {
        public static final Codec<RoomJsonConfig> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        WaveDefinition.CODEC.listOf().optionalFieldOf("waves", List.of()).forGetter(RoomJsonConfig::waves),
                        Identifier.CODEC.optionalFieldOf("reward_loot_table").forGetter(RoomJsonConfig::rewardLootTable),
                        GateSizeConfig.CODEC.optionalFieldOf("reward_gate_size").forGetter(RoomJsonConfig::rewardGateSize)
                ).apply(instance, RoomJsonConfig::new)
        );
    }

    public static final Codec<DungeonJsonDefinition> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Identifier.CODEC.optionalFieldOf("template").forGetter(DungeonJsonDefinition::template),
                    Codec.INT.optionalFieldOf("kills_required", 30).forGetter(DungeonJsonDefinition::killsRequired),
                    Codec.BOOL.optionalFieldOf("repeatable", false).forGetter(DungeonJsonDefinition::repeatable),
                    TimingsConfig.CODEC.optionalFieldOf("timings").forGetter(DungeonJsonDefinition::timings),
                    GateSizeConfig.CODEC.optionalFieldOf("waiting_gate_size").forGetter(DungeonJsonDefinition::waitingGateSize),
                    GateSizeConfig.CODEC.optionalFieldOf("dungeon_gate_size").forGetter(DungeonJsonDefinition::dungeonGateSize),
                    Identifier.CODEC.optionalFieldOf("eldritch_chest_loot_table").forGetter(DungeonJsonDefinition::eldritchChestLootTable),
                    Codec.unboundedMap(Codec.STRING, RoomJsonConfig.CODEC)
                            .optionalFieldOf("rooms", Map.of())
                            .forGetter(DungeonJsonDefinition::rooms)
            ).apply(instance, DungeonJsonDefinition::new)
    );
}
