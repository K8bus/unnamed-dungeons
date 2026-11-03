package com.k8bus.dungeon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.List;

public class WaveDefinition {

    public static final Codec<WaveDefinition> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("base_count").forGetter(WaveDefinition::getBaseCount),
                    Codec.INT.fieldOf("extra_per_player").forGetter(WaveDefinition::getExtraPerPlayer),
                    Identifier.CODEC.listOf().optionalFieldOf("mob_pool", List.of()).forGetter(WaveDefinition::getMobPool)
            ).apply(instance, WaveDefinition::new)
    );

    private final int baseCount;
    private final int extraPerPlayer;
    private final List<Identifier> mobPool;

    public WaveDefinition(int baseCount, int extraPerPlayer) {
        this(baseCount, extraPerPlayer, List.of());
    }

    public WaveDefinition(int baseCount, int extraPerPlayer, List<Identifier> mobPool) {
        this.baseCount = baseCount;
        this.extraPerPlayer = extraPerPlayer;
        this.mobPool = mobPool != null ? List.copyOf(mobPool) : List.of();
    }

    public int getBaseCount() {
        return baseCount;
    }

    public int getExtraPerPlayer() {
        return extraPerPlayer;
    }

    /**
     * Lista identyfikatorów mobów (np. "minecraft:blaze", "my_mod:custom_orc") do losowania w tej fali.
     * Jeśli pusta, używany jest domyślny fallback silnika.
     */
    public List<Identifier> getMobPool() {
        return mobPool;
    }

    public int getScaledMobCount(int playerCount) {
        int safePlayerCount = Math.max(1, playerCount);
        return baseCount + ((safePlayerCount - 1) * extraPerPlayer);
    }
}
