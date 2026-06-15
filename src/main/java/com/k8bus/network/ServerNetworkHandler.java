package com.k8bus.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class ServerNetworkHandler {
    public static void initialize() {
        PayloadTypeRegistry.clientboundPlay().register(DungeonHudPayload.TYPE, DungeonHudPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(WalletSyncPayload.TYPE, WalletSyncPayload.CODEC);
    }
}
