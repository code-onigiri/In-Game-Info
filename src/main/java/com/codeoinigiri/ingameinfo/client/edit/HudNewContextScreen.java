package com.codeoinigiri.ingameinfo.client.edit;

import com.codeoinigiri.ingameinfo.hud.HudContextIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Simple dialog to create a new HUD context (overlay).
 */
public class HudNewContextScreen extends Screen {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    private final Screen parent;
    private EditBox nameBox;

    public HudNewContextScreen(Screen parent) {
        super(Component.translatable("ingameinfo.new_context.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int w = Math.min(260, this.width - 40);
        int x = centerX - w / 2;
        int y = centerY - 30;

        nameBox = new EditBox(this.font, x, y, w, 20, Component.translatable("ingameinfo.new_context.placeholder"));
        nameBox.setMaxLength(64);
        nameBox.setValue("new_context");
        nameBox.setFocused(true);
        this.addRenderableWidget(nameBox);

        Button createBtn = Button.builder(Component.translatable("ingameinfo.new_context.create_button"), b -> onCreate()).bounds(centerX - 100, y + 30, 90, 20).build();
        Button cancelBtn = Button.builder(Component.translatable("ingameinfo.new_context.cancel_button"), b -> onCancel()).bounds(centerX + 10, y + 30, 90, 20).build();
        this.addRenderableWidget(createBtn);
        this.addRenderableWidget(cancelBtn);
    }

    private void onCreate() {
        String name = nameBox.getValue();
        String created = HudContextIO.createNewContext(name);
        if (created != null && !created.equals(name)) {
            // Notify user that name was adjusted for uniqueness
            LOGGER.info("Context name adjusted for uniqueness: '{}' -> '{}'", name, created);
        }
        onClose();
    }


    private void onCancel() { onClose(); }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        int titleY = this.height / 2 - 60;
        g.drawCenteredString(this.font, this.getTitle(), this.width / 2, titleY, 0xFFFFFF);
        g.drawCenteredString(this.font, Component.translatable("ingameinfo.new_context.hint_filename").getString(), this.width / 2, titleY + 12, 0xAAAAAA);
        if (nameBox != null) {
            int lx = nameBox.getX() - 6 - this.font.width(Component.translatable("ingameinfo.new_context.name_label").getString());
            int ly = nameBox.getY() + (20 - this.font.lineHeight) / 2;
            g.drawString(this.font, Component.translatable("ingameinfo.new_context.name_label").getString(), lx, ly, 0xFFFFFF, false);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (keyCode == 257 || keyCode == 335) { onCreate(); return true; }
        if (keyCode == 256) { onCancel(); return true; }
        return false;
    }

    @Override
    public void onClose() {
        var mc = Minecraft.getInstance();
        mc.setScreen(parent);
    }
}
