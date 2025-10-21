package com.codeoinigiri.ingameinfo.client.hud;

import com.codeoinigiri.ingameinfo.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT,modid = "ingameinfo")
public class HudOverlay {
    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.renderDebug) return;
        if (!ClientConfig.INSTANCE.hudEnabled.get()) return;

        GuiGraphics gui = event.getGuiGraphics();
        Font font = mc.font;

        String text = ClientConfig.INSTANCE.hudText.get();
        int color = ClientConfig.INSTANCE.hudColor.get();
        String position = ClientConfig.INSTANCE.hudPosition.get().toLowerCase();
        int offsetX = ClientConfig.INSTANCE.hudOffsetX.get();
        int offsetY = ClientConfig.INSTANCE.hudOffsetY.get();

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int textWidth = font.width(text);
        int textHeight = font.lineHeight;

        int x = 0;
        int y = 0;

        switch (position) {
            case "top_left" -> {
                x = offsetX;
                y = offsetY;
            }
            case "top_right" -> {
                x = screenWidth - textWidth - offsetX;
                y = offsetY;
            }
            case "bottom_left" -> {
                x = offsetX;
                y = screenHeight - textHeight - offsetY;
            }
            case "bottom_right" -> {
                x = screenWidth - textWidth - offsetX;
                y = screenHeight - textHeight - offsetY;
            }
            default -> {
                // 不正な値があった場合は右上にフォールバック
                x = screenWidth - textWidth - offsetX;
                y = offsetY;
            }
        }

        gui.drawString(font, text, x, y, color, true);
    }
}
