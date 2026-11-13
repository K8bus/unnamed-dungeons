package com.k8bus.client;

import com.k8bus.client.hud.DungeonHudOverlay;
import com.k8bus.client.network.ClientNetworkHandler;
import net.fabricmc.api.ClientModInitializer;

public class UnnamedDungeonsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientNetworkHandler.initialize();
        DungeonHudOverlay.register();

        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            com.k8bus.client.vfx.ClientVfxManager.tick();
        });
    }
}
