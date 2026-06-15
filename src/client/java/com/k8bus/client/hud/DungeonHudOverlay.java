package com.k8bus.client.hud;

import com.k8bus.UnnamedDungeons;
import com.k8bus.client.network.ClientDungeonState;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class DungeonHudOverlay implements HudElement {
    public static void register() {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(UnnamedDungeons.MOD_ID, "dungeon_hud"), new DungeonHudOverlay());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        Font font = client.font;
        int width = client.getWindow().getGuiScaledWidth();
        int y = 5;

        // Content
        String dungeonName = ClientDungeonState.dungeonName;
        String waveText = ClientDungeonState.waveCounter > 0 ? "Wave: " + ClientDungeonState.waveCounter + " | Mobs: " + ClientDungeonState.aliveMobs : "Waiting for players...";
        String walletText = String.valueOf(ClientDungeonState.walletBalance);

        // Dimensions
        int iconSize = 16;
        int padding = 4;
        int gap = 4;

        int w1 = iconSize + padding + font.width(dungeonName);
        int w2 = iconSize + padding + font.width(waveText);
        int w3 = iconSize + padding + font.width(walletText);

        int totalWidth = w1 + w2 + w3 + gap * 2;
        int startX = (width - totalWidth) / 2;

        // Render Pills
        drawPill(graphics, font, startX, y, w1, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COMPASS), dungeonName, 0xFFFFFF);
        drawPill(graphics, font, startX + w1 + gap, y, w2, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CLOCK), waveText, 0xFFFFFF);
        drawPill(graphics, font, startX + w1 + gap + w2 + gap, y, w3, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD), walletText, 0x55FF55);
        
        if (ClientDungeonState.phaseTimer > 0) {
            int seconds = ClientDungeonState.phaseTimer / 20;
            String waitingTitle = "Dungeon starts in: " + seconds + "s";
            int tw = font.width(waitingTitle) * 2;
            int tx = (width - tw) / 2;
            int ty = client.getWindow().getGuiScaledHeight() / 3;
            graphics.pose().pushMatrix();
            graphics.pose().scale(2.0f, 2.0f);
            graphics.text(font, waitingTitle, tx / 2, ty / 2, 0xFFFF00);
            graphics.pose().popMatrix();
        }
    }

    private void drawPill(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, net.minecraft.world.item.ItemStack icon, String text, int textColor) {
        int height = 20;
        int padding = 2;
        
        // Background
        graphics.fill(x, y, x + width + padding * 4, y + height, 0xB0000000); // darker background
        
        // Icon
        graphics.item(icon, x + padding, y + padding);
        
        // Text
        graphics.text(font, text, x + padding + 16 + padding * 2, y + 6, textColor);
    }
}
