package com.codeoinigiri.ingameinfo.client.hud;

import com.codeoinigiri.ingameinfo.hud.HudContext;
import com.codeoinigiri.ingameinfo.hud.HudContextManager;
import com.codeoinigiri.ingameinfo.variable.ExpressionUtils;
import com.codeoinigiri.ingameinfo.variable.VariableManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FastColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Map;

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
            // ✅ シングルトンから取得
            Map<String, String> vars = VariableManager.getInstance().getResolvedVariables();
            List<String> resolvedLines = ctx.lines().stream()
                    .map(line -> ExpressionUtils.evaluateEmbedded(line, vars))
                    .toList();

            float scale = ctx.scale();
            int lineHeight = (int) ((font.lineHeight + 2) * scale);
            int totalHeight = resolvedLines.size() * lineHeight;
            int maxWidth = resolvedLines.stream().mapToInt(font::width).max().orElse(0);

            // --- 位置計算（スケールは反映せず、後でまとめて行う） ---
            int x = 0, y = 0;
            switch (ctx.position()) {
                case TOP_LEFT -> { x = 10; y = 10; }
                case TOP_RIGHT -> { x = (int) (screenWidth - (maxWidth * scale) - 10); y = 10; }
                case BOTTOM_LEFT -> { x = 10; y = (int) (screenHeight - (totalHeight) - 10); }
                case BOTTOM_RIGHT -> { x = (int) (screenWidth - (maxWidth * scale) - 10); y = (int) (screenHeight - totalHeight - 10); }
                case CENTER_LEFT -> { x = 10; y = (int) ((screenHeight / 2f) - (totalHeight / 2f)); }
                case CENTER_RIGHT -> { x = (int) (screenWidth - (maxWidth * scale) - 10); y = (int) ((screenHeight / 2f) - (totalHeight / 2f)); }
                case CENTER_TOP -> { x = (int) ((screenWidth / 2f) - (maxWidth * scale / 2f)); y = 10; }
                case CENTER_BOTTOM -> { x = (int) ((screenWidth / 2f) - (maxWidth * scale / 2f)); y = (int) (screenHeight - totalHeight - 10); }
            }

            if (ctx.background()) {
                int padding = (int) (ctx.backgroundPadding() * scale);
                int bgX1 = x - padding;
                int bgY1 = y - padding;
                int bgX2 = x + (int) (maxWidth * scale) + padding;
                int bgY2 = y + totalHeight + padding;

                double clampedAlpha = Math.max(0.0, Math.min(1.0, ctx.backgroundAlpha()));
                int alphaByte = (int)(clampedAlpha * 255.0);
                int redByte = (ctx.backgroundRgb() >> 16) & 0xFF;
                int greenByte = (ctx.backgroundRgb() >> 8) & 0xFF;
                int blueByte = ctx.backgroundRgb() & 0xFF;
                int finalColor = FastColor.ARGB32.color(alphaByte, redByte, greenByte, blueByte);

                gui.fill(bgX1, bgY1, bgX2, bgY2, finalColor);
            }

            gui.pose().pushPose();
            gui.pose().scale(scale, scale, 1.0F);

            for (int i = 0; i < resolvedLines.size(); i++) {
                String line = resolvedLines.get(i);
                gui.drawString(font, line, (int) (x / scale), (int) ((y + i * lineHeight) / scale), ctx.color(), ctx.shadow());
            }

            gui.pose().popPose();
        }
    }
}
