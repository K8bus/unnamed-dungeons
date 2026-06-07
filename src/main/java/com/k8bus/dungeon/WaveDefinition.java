package com.k8bus.dungeon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class WaveDefinition {

    public static final Codec<WaveDefinition> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("mob_count").forGetter(WaveDefinition::getMobCount)
            ).apply(instance, WaveDefinition::new)
    );

    private final int mobCount;

    public WaveDefinition(int mobCount) {
        this.mobCount = mobCount;
    }

    public int getMobCount() {
        return mobCount;
    }

    public int getScaledMobCount(int playerCount) {
        int safePlayerCount = Math.max(1, playerCount);
        int extraPerPlayer = Math.max(1, mobCount / 2);

        return mobCount + ((safePlayerCount - 1) * extraPerPlayer);
    }
}