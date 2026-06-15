package com.k8bus.client.screen;

import com.k8bus.menu.WalletDepositMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class WalletDepositScreen extends AbstractContainerScreen<WalletDepositMenu> {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/hopper.png");

    public WalletDepositScreen(WalletDepositMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        graphics.blit(TEXTURE, x, y, this.imageWidth, this.imageHeight, 0.0f, 0.0f, 256.0f, 256.0f);
        
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }
}
