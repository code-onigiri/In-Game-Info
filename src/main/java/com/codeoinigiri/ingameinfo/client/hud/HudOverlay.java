package com.codeoinigiri.ingameinfo.client.hud;

import com.codeoinigiri.ingameinfo.hud.HudContext;
import com.codeoinigiri.ingameinfo.hud.HudContextManager;
import com.codeoinigiri.ingameinfo.variable.ExpressionUtils;
import com.codeoinigiri.ingameinfo.variable.VariableManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.util.FastColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = "ingameinfo", bus = Mod.EventBusSubscriber.Bus.MOD)
public class HudOverlay {

    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("ingameinfo-hud", (forgeGui, guiGraphics, partialTick, screenWidth, screenHeight) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.options.renderDebug) return;
            if (mc.player == null) return;

            Font font = mc.font;

            for (HudContext ctx : HudContextManager.getContexts()) {
                Map<String, String> vars = VariableManager.getInstance().getResolvedVariables();
                List<String> resolvedLines = ctx.lines().stream()
                        .map(line -> ExpressionUtils.evaluateEmbedded(line, vars))
                        .toList();

                float scale = ctx.scale();

                // スケール適用後のパディング
                int paddingTopPx = (int) (ctx.paddingTop() * scale);
                int paddingBottomPx = (int) (ctx.paddingBottom() * scale);
                int paddingLeftPx = (int) (ctx.paddingLeft() * scale);
                int paddingRightPx = (int) (ctx.paddingRight() * scale);
                int lineSpacingPx = (int) (ctx.lineSpacing() * scale);
                int lineSpacingPaddingTopPx = (int) (ctx.lineSpacingPaddingTop() * scale);
                int lineSpacingPaddingBottomPx = (int) (ctx.lineSpacingPaddingBottom() * scale);

                // スケール適用後のマージン
                int marginTopPx = (int) (ctx.marginTop() * scale);
                int marginBottomPx = (int) (ctx.marginBottom() * scale);
                int marginLeftPx = (int) (ctx.marginLeft() * scale);
                int marginRightPx = (int) (ctx.marginRight() * scale);

                // フォント高さ
                int fontLineHeightScaled = (int) (font.lineHeight * scale);

                // 各行の幅を個別に計算
                int maxWidth = resolvedLines.stream().mapToInt(font::width).max().orElse(0);

                // 全体の高さ計算
                int totalHeight;
                if (ctx.background() && ctx.backgroundPerLine()) {
                    // 行ごと背景: 各行 + 行間パディング + 行間
                    int singleLineHeight = paddingTopPx + fontLineHeightScaled + paddingBottomPx;
                    int singleLineGapHeight = lineSpacingPaddingBottomPx + lineSpacingPx + lineSpacingPaddingTopPx;

                    totalHeight = resolvedLines.size() * singleLineHeight;
                    if (resolvedLines.size() > 1) {
                        totalHeight += (resolvedLines.size() - 1) * singleLineGapHeight;
                    }
                } else if (ctx.background()) {
                    // 全体背景
                    int contentHeight = resolvedLines.size() * fontLineHeightScaled;
                    if (resolvedLines.size() > 1) {
                        contentHeight += (resolvedLines.size() - 1) * lineSpacingPx;
                    }
                    totalHeight = paddingTopPx + contentHeight + paddingBottomPx;
                } else {
                    // 背景なし
                    totalHeight = resolvedLines.size() * fontLineHeightScaled;
                    if (resolvedLines.size() > 1) {
                        totalHeight += (resolvedLines.size() - 1) * lineSpacingPx;
                    }
                }

                // 全体の幅（最大幅基準）
                int totalWidth = (int) (maxWidth * scale) + (ctx.background() ? paddingLeftPx + paddingRightPx : 0);

                // 位置計算（マージンを適用、ベースの10pxは削除）
                int x = 0, y = 0;
                switch (ctx.position()) {
                    case TOP_LEFT -> { x = marginLeftPx; y = marginTopPx; }
                    case TOP_RIGHT -> { x = screenWidth - totalWidth - marginRightPx; y = marginTopPx; }
                    case BOTTOM_LEFT -> { x = marginLeftPx; y = screenHeight - totalHeight - marginBottomPx; }
                    case BOTTOM_RIGHT -> { x = screenWidth - totalWidth - marginRightPx; y = screenHeight - totalHeight - marginBottomPx; }
                    case CENTER_LEFT -> { x = marginLeftPx; y = (int) ((screenHeight / 2f) - (totalHeight / 2f)); }
                    case CENTER_RIGHT -> { x = screenWidth - totalWidth - marginRightPx; y = (int) ((screenHeight / 2f) - (totalHeight / 2f)); }
                    case CENTER_TOP -> { x = (int) ((screenWidth / 2f) - (totalWidth / 2f)); y = marginTopPx; }
                    case CENTER_BOTTOM -> { x = (int) ((screenWidth / 2f) - (totalWidth / 2f)); y = screenHeight - totalHeight - marginBottomPx; }
                }

                // 背景描画
                if (ctx.background()) {
                    double clampedAlpha = Math.max(0.0, Math.min(1.0, ctx.backgroundAlpha()));
                    int alphaByte = (int)(clampedAlpha * 255.0);
                    int redByte = (ctx.backgroundRgb() >> 16) & 0xFF;
                    int greenByte = (ctx.backgroundRgb() >> 8) & 0xFF;
                    int blueByte = ctx.backgroundRgb() & 0xFF;
                    int finalColor = FastColor.ARGB32.color(alphaByte, redByte, greenByte, blueByte);

                    if (ctx.backgroundPerLine()) {
                        // 行ごとの背景（各行の文字幅に合わせる）
                        int currentY = y;

                        for (int i = 0; i < resolvedLines.size(); i++) {
                            String line = resolvedLines.get(i);
                            int lineWidth = (int) (font.width(line) * scale);

                            // アライメントに応じてX座標を調整
                            int lineX = x;
                            switch (ctx.align()) {
                                case CENTER -> lineX = x + (totalWidth - paddingLeftPx - paddingRightPx - lineWidth) / 2;
                                case RIGHT -> lineX = x + totalWidth - paddingRightPx - lineWidth;
                                default -> {}
                            }

                            // 行の背景（パディング込み）
                            int bgX1 = lineX;
                            int bgY1 = currentY;
                            int bgX2 = lineX + paddingLeftPx + lineWidth + paddingRightPx;
                            int bgY2 = currentY + paddingTopPx + fontLineHeightScaled + paddingBottomPx;
                            guiGraphics.fill(bgX1, bgY1, bgX2, bgY2, finalColor);

                            // 次の行に移動する前に行間パディングを描画
                            if (i < resolvedLines.size() - 1) {
                                // 下行間パディング
                                if (lineSpacingPaddingBottomPx > 0) {
                                    int gapBottomY1 = bgY2;
                                    int gapBottomY2 = bgY2 + lineSpacingPaddingBottomPx;
                                    guiGraphics.fill(bgX1, gapBottomY1, bgX2, gapBottomY2, finalColor);
                                }

                                // 上行間パディング（次の行の上）
                                if (lineSpacingPaddingTopPx > 0) {
                                    int nextLineY = bgY2 + lineSpacingPaddingBottomPx + lineSpacingPx;

                                    // 次の行の幅を事前に計算
                                    String nextLine = resolvedLines.get(i + 1);
                                    int nextLineWidth = (int) (font.width(nextLine) * scale);
                                    int nextLineX = x;
                                    switch (ctx.align()) {
                                        case CENTER -> nextLineX = x + (totalWidth - paddingLeftPx - paddingRightPx - nextLineWidth) / 2;
                                        case RIGHT -> nextLineX = x + totalWidth - paddingRightPx - nextLineWidth;
                                        default -> {}
                                    }

                                    int gapTopY1 = nextLineY;
                                    int gapTopY2 = nextLineY + lineSpacingPaddingTopPx;
                                    int gapTopX1 = nextLineX;
                                    int gapTopX2 = nextLineX + paddingLeftPx + nextLineWidth + paddingRightPx;
                                    guiGraphics.fill(gapTopX1, gapTopY1, gapTopX2, gapTopY2, finalColor);
                                }
                            }

                            // 次の行の位置
                            currentY = bgY2 + lineSpacingPaddingBottomPx + lineSpacingPx + lineSpacingPaddingTopPx;
                        }
                    } else {
                        // 全体背景
                        guiGraphics.fill(x, y, x + totalWidth, y + totalHeight, finalColor);
                    }
                }

                // テキスト描画
                guiGraphics.pose().pushPose();
                guiGraphics.pose().scale(scale, scale, 1.0F);

                int currentY = y;

                for (int i = 0; i < resolvedLines.size(); i++) {
                    String line = resolvedLines.get(i);
                    int lineWidth = (int) (font.width(line) * scale);

                    // アライメント
                    int textX = x + paddingLeftPx;
                    switch (ctx.align()) {
                        case CENTER -> textX = x + (totalWidth - paddingLeftPx - paddingRightPx - lineWidth) / 2 + paddingLeftPx;
                        case RIGHT -> textX = x + totalWidth - paddingRightPx - lineWidth;
                        default -> {}
                    }

                    // Y座標
                    int textY;
                    if (ctx.background() && ctx.backgroundPerLine()) {
                        textY = currentY + paddingTopPx;
                        // 次の行の位置を更新
                        currentY = currentY + paddingTopPx + fontLineHeightScaled + paddingBottomPx
                                 + lineSpacingPaddingBottomPx + lineSpacingPx + lineSpacingPaddingTopPx;
                    } else if (ctx.background()) {
                        textY = y + paddingTopPx + i * (fontLineHeightScaled + lineSpacingPx);
                    } else {
                        textY = y + i * (fontLineHeightScaled + lineSpacingPx);
                    }

                    guiGraphics.drawString(font, line, (int) (textX / scale), (int) (textY / scale), ctx.color(), ctx.shadow());
                }

                guiGraphics.pose().popPose();
            }
        });
    }
}

