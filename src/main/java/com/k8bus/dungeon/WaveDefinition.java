package com.k8bus.dungeon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class WaveDefinition {

    public static final Codec<WaveDefinition> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("base_count").forGetter(WaveDefinition::getBaseCount),
                    Codec.INT.fieldOf("extra_per_player").forGetter(WaveDefinition::getExtraPerPlayer)
            ).apply(instance, WaveDefinition::new)
    );

    private final int baseCount;
    private final int extraPerPlayer;

    public WaveDefinition(int baseCount, int extraPerPlayer) {
        this.baseCount = baseCount;
        this.extraPerPlayer = extraPerPlayer;
    }

    public int getBaseCount() {
        return baseCount;
    }

    public int getExtraPerPlayer() {
        return extraPerPlayer;
    }

    public int getScaledMobCount(int playerCount) {
        int safePlayerCount = Math.max(1, playerCount);
        return baseCount + ((safePlayerCount - 1) * extraPerPlayer);
    }
}