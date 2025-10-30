package com.codeoinigiri.ingameinfo.client.edit;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Lightweight transparent screen used during HUD edit modes (POSITION/TEXT).
 * Purpose: release in-game mouse capture to show the cursor while allowing
 * our edit interactions to continue via input events.
 */
public class HudEditScreen extends Screen {

    public HudEditScreen() {
        super(Component.translatable("ingameinfo.edit.overlay.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false; // Don't pause game while editing
    }

    @Override
    public boolean shouldCloseOnEsc() {
        // Keep the screen open on Esc; mode toggle key will exit the mode and close this screen.
        return false;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Intentionally do not render any blocking UI here.
        // HUD overlays (including edit hints) are rendered by HudOverlay.
        // Draw nothing to keep it transparent.
    }

    // Return false to avoid consuming mouse input here; our InputEvent listeners will handle it.
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) { return false; }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) { return false; }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) { return false; }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) { return false; }
}
