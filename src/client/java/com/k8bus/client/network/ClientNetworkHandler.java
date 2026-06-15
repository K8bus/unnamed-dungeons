package com.k8bus.client.network;

import com.k8bus.network.DungeonHudPayload;
import com.k8bus.network.WalletSyncPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ClientNetworkHandler {
    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(DungeonHudPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                ClientDungeonState.waveCounter = payload.waveCounter();
                ClientDungeonState.aliveMobs = payload.aliveMobs();
                ClientDungeonState.phaseTimer = payload.phaseTimer();
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(WalletSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                ClientDungeonState.walletBalance = payload.balance();
            });
        });
    }
}
