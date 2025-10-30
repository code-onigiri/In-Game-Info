package com.codeoinigiri.ingameinfo.client.edit;

import com.codeoinigiri.ingameinfo.hud.HudContextIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class HudLineEditScreen extends Screen {

    private final String contextName;
    private final int lineIndex;
    private final String initialText;

    private EditBox input;

    public HudLineEditScreen(Component title, String contextName, int lineIndex, String initialText) {
        super(title);
        this.contextName = contextName;
        this.lineIndex = lineIndex;
        this.initialText = initialText;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int boxWidth = Math.min(420, this.width - 40);
        int boxX = centerX - boxWidth / 2;
        int boxY = centerY - 40;

        input = new EditBox(this.font, boxX, boxY, boxWidth, 20, Component.translatable("ingameinfo.line_edit.placeholder"));
        input.setMaxLength(4096);
        input.setValue(initialText);
        input.setFocused(true);
        this.addRenderableWidget(input);

        int btnY = boxY + 30;
        Button saveButton = Button.builder(Component.translatable("ingameinfo.line_edit.save"), b -> onSave())
                .bounds(centerX - 190, btnY, 80, 20)
                .build();
        Button cancelButton = Button.builder(Component.translatable("ingameinfo.line_edit.cancel"), b -> onCancel())
                .bounds(centerX - 100, btnY, 80, 20)
                .build();
        Button deleteButton = Button.builder(Component.translatable("ingameinfo.line_edit.delete_line"), b -> onDelete())
                .bounds(centerX - 10, btnY, 100, 20)
                .build();
        Button insertBelowButton = Button.builder(Component.translatable("ingameinfo.line_edit.insert_below"), b -> onInsertBelow())
                .bounds(centerX + 100, btnY, 110, 20)
                .build();
        this.addRenderableWidget(saveButton);
        this.addRenderableWidget(cancelButton);
        this.addRenderableWidget(deleteButton);
        this.addRenderableWidget(insertBelowButton);
    }

    private void onSave() {
        String value = input.getValue();
        HudContextIO.saveLine(contextName, lineIndex, value);
        onClose();
    }

    private void onDelete() {
        HudContextIO.deleteLine(contextName, lineIndex);
        onClose();
    }

    private void onInsertBelow() {
        HudContextIO.insertLine(contextName, lineIndex + 1, "");
        onClose();
    }

    private void onCancel() { onClose(); }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        int y = this.height / 2 - 70;
        guiGraphics.drawCenteredString(this.font, this.getTitle(), this.width / 2, y, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, Component.translatable("ingameinfo.line_edit.hint").getString(), this.width / 2, y + 12, 0xAAAAAA);
    }

    @Override
    public void tick() { if (input != null) input.tick(); }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (keyCode == 257 || keyCode == 335) { // Enter or KP_Enter
            onSave();
            return true;
        }
        // The delete key check was redundant with the button and could cause accidental deletes.
        // Users can use the dedicated button.
        return false;
    }

    @Override
    public void onClose() {
        var mc = Minecraft.getInstance();
        mc.setScreen(null);
        if (HudEditManager.isEditMode() && !(mc.screen instanceof HudEditScreen)) {
            mc.setScreen(new HudEditScreen());
        }
    }
}
