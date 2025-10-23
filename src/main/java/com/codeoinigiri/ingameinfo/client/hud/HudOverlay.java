package com.codeoinigiri.ingameinfo.client.hud;

import com.codeoinigiri.ingameinfo.config.ClientConfig;
import com.codeoinigiri.ingameinfo.config.HudPosition;
import com.codeoinigiri.ingameinfo.hud.HudContext;
import com.codeoinigiri.ingameinfo.hud.HudContextManager;
import com.codeoinigiri.ingameinfo.hud.variable.VariableResolver;
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
    private static final VariableResolver resolver = new VariableResolver();

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.renderDebug) return;

        GuiGraphics gui = event.getGuiGraphics();
        Font font = mc.font;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        for (HudContext ctx : HudContextManager.getContexts()) {
            var resolvedLines = resolver.resolveLines(ctx.lines);

            float scale = ctx.scale;
            int lineHeight = (int) ((font.lineHeight + 2) * scale);
            int totalHeight = resolvedLines.size() * lineHeight;
            int maxWidth = resolvedLines.stream().mapToInt(font::width).max().orElse(0);

            // --- 位置計算（スケールは反映せず、後でまとめて行う） ---
            int x = 0, y = 0;
            switch (ctx.position) {
                case TOP_LEFT -> { x = 10; y = 10; }
                case TOP_RIGHT -> { x = (int) (screenWidth - (maxWidth * scale) - 10); y = 10; }
                case BOTTOM_LEFT -> { x = 10; y = (int) (screenHeight - (totalHeight) - 10); }
                case BOTTOM_RIGHT -> { x = (int) (screenWidth - (maxWidth * scale) - 10); y = (int) (screenHeight - totalHeight - 10); }
                case CENTER_LEFT -> { x = 10; y = (int) ((screenHeight / 2f) - (totalHeight / 2f)); }
                case CENTER_RIGHT -> { x = (int) (screenWidth - (maxWidth * scale) - 10); y = (int) ((screenHeight / 2f) - (totalHeight / 2f)); }
                case CENTER_TOP -> { x = (int) ((screenWidth / 2f) - (maxWidth * scale / 2f)); y = 10; }
                case CENTER_BOTTOM -> { x = (int) ((screenWidth / 2f) - (maxWidth * scale / 2f)); y = (int) (screenHeight - totalHeight - 10); }
            }

            RenderSystem.enableBlend();

            if (ctx.background) {
                int padding = (int) (ctx.backgroundPadding * scale);
                int bgX1 = x - padding;
                int bgY1 = y - padding;
                int bgX2 = x + (int) (maxWidth * scale) + padding;
                int bgY2 = y + totalHeight + padding;
                gui.fill(bgX1, bgY1, bgX2, bgY2, ctx.backgroundColor);
            }

            gui.pose().pushPose();
            gui.pose().scale(scale, scale, 1.0F);

            for (int i = 0; i < resolvedLines.size(); i++) {
                String line = resolvedLines.get(i);
                gui.drawString(font, line, (int) (x / scale), (int) ((y + i * lineHeight) / scale), ctx.color, ctx.shadow);
            }

            gui.pose().popPose();
            RenderSystem.disableBlend();
        }
    }
}
