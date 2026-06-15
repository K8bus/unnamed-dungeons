package com.k8bus.network;

import com.k8bus.UnnamedDungeons;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record DungeonHudPayload(int waveCounter, int aliveMobs, int phaseTimer) implements CustomPacketPayload {
    public static final Type<DungeonHudPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UnnamedDungeons.MOD_ID, "hud_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DungeonHudPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeInt(payload.waveCounter());
                buf.writeInt(payload.aliveMobs());
                buf.writeInt(payload.phaseTimer());
            },
            buf -> new DungeonHudPayload(
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
