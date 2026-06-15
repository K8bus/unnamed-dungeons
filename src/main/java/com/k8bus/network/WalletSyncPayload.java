package com.k8bus.network;

import com.k8bus.UnnamedDungeons;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record WalletSyncPayload(int balance) implements CustomPacketPayload {
    public static final Type<WalletSyncPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UnnamedDungeons.MOD_ID, "wallet_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WalletSyncPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeInt(payload.balance());
            },
            buf -> new WalletSyncPayload(
                    buf.readInt()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
