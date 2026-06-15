package com.k8bus.client;

import com.k8bus.client.hud.DungeonHudOverlay;
import com.k8bus.client.network.ClientNetworkHandler;
import com.k8bus.client.screen.WalletDepositScreen;
import com.k8bus.menu.ModMenuTypes;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public class UnnamedDungeonsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientNetworkHandler.initialize();
        DungeonHudOverlay.register();
        MenuScreens.register(ModMenuTypes.WALLET_DEPOSIT, WalletDepositScreen::new);
    }
}
