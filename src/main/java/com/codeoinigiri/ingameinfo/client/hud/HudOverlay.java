package com.codeoinigiri.ingameinfo.client.hud;

import com.codeoinigiri.ingameinfo.config.ClientConfig;
import com.codeoinigiri.ingameinfo.config.HudPosition;
import com.codeoinigiri.ingameinfo.hud.HudContext;
import com.codeoinigiri.ingameinfo.hud.HudContextManager;
import com.mojang.blaze3d.systems.RenderSystem;
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

        GuiGraphics gui = event.getGuiGraphics();
        Font font = mc.font;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        for (HudContext ctx : HudContextManager.getContexts()) {
            float scale = ctx.scale;
            int baseLineHeight = font.lineHeight + 2;
            int lineHeight = (int) (baseLineHeight * scale);
            int totalHeight = ctx.lines.size() * lineHeight;
            int maxWidth = ctx.lines.stream().mapToInt(font::width).max().orElse(0);

            // --- 位置計算（スケールは反映せず、後でまとめて行う） ---
            int x = 0, y = 0;
            final float center = (screenWidth / 2f) - (maxWidth * scale / 2f);
            switch (ctx.position) {
                case TOP_LEFT -> { x = 10; y = 10; }
                case TOP_RIGHT -> { x = (int) (screenWidth - (maxWidth * scale) - 10); y = 10; }
                case BOTTOM_LEFT -> { x = 10; y = (int) (screenHeight - (totalHeight) - 10); }
                case BOTTOM_RIGHT -> { x = (int) (screenWidth - (maxWidth * scale) - 10); y = (int) (screenHeight - totalHeight - 10); }
                case CENTER_LEFT -> { x = 10; y = (int) ((screenHeight / 2f) - (totalHeight / 2f)); }
                case CENTER_RIGHT -> { x = (int) (screenWidth - (maxWidth * scale) - 10); y = (int) ((screenHeight / 2f) - (totalHeight / 2f)); }
                case CENTER_TOP -> { x = (int) center; y = 10; }
                case CENTER_BOTTOM -> { x = (int) center; y = (int) (screenHeight - totalHeight - 10); }
            }

            RenderSystem.enableBlend();

            // --- 🎨 背景描画 (スケール考慮) ---
            if (ctx.background) {
                int padding = (int) (ctx.backgroundPadding * scale);

                int bgWidth = (int) (maxWidth * scale);
                int bgHeight = totalHeight;

                int bgX1 = x - padding;
                int bgY1 = y - padding;
                int bgX2 = x + bgWidth + padding;
                int bgY2 = y + bgHeight + padding;

                gui.fill(bgX1, bgY1, bgX2, bgY2, ctx.backgroundColor);
            }

            // --- 📝 テキスト描画 (スケール適用) ---
            gui.pose().pushPose();
            gui.pose().scale(scale, scale, 1.0F);

            for (int i = 0; i < ctx.lines.size(); i++) {
                String line = ctx.lines.get(i);
                int textWidth = font.width(line);
                int drawX = x;
                if (ctx.align == HudContext.Align.CENTER) {
                    drawX = (int) (x + (maxWidth * scale / 2f - (textWidth * scale) / 2f));
                } else if (ctx.align == HudContext.Align.RIGHT) {
                    drawX = (int) (x + (maxWidth * scale - textWidth * scale));
                }

                gui.drawString(font,
                        line,
                        (int) (drawX / scale),
                        (int) ((y + i * lineHeight) / scale),
                        ctx.color,
                        ctx.shadow);
            }

            gui.pose().popPose();
            RenderSystem.disableBlend();
        }
    }
}
