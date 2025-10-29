package com.codeoinigiri.ingameinfo.client.edit;

import com.codeoinigiri.ingameinfo.hud.HudContext;
import com.codeoinigiri.ingameinfo.hud.HudContextIO;
import com.codeoinigiri.ingameinfo.hud.HudContextManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.Objects;

/**
 * HUDの単一コンテキスト用 設定画面。
 * - 以前の画面と同等の編集機能（色・影・スケール・整列・背景・行間・パディング・マージン）
 * - シンプルなレイアウト計算で安定描画
 */
public class HudContextSettingsScreen extends Screen {

    private final String contextName;
    private final Screen parent;

    // Controls
    private EditBox colorHex;
    private Button shadowToggle;
    private EditBox scaleBox;
    private EditBox alignBox; // LEFT/CENTER/RIGHT

    private Button backgroundToggle;
    private EditBox bgRgbHex;
    private EditBox bgAlphaBox; // 0.0 - 1.0
    private Button bgPerLineToggle;

    private EditBox paddingTopBox, paddingBottomBox, paddingLeftBox, paddingRightBox;
    private EditBox lineSpacingBox, lineSpacingPadTopBox, lineSpacingPadBottomBox;

    private EditBox marginTopBox, marginBottomBox, marginLeftBox, marginRightBox;

    private Button applyBtn, cancelBtn, resetBtn;

    // Row Y positions cached for label rendering
    private int[] rowY;
    private int topY;

    // Layout numbers (computed in init)
    private int labelColRight;
    private int inputColLeft;
    private int inputWidth;
    private int smallColLeft;  // 2nd column left (after its label width)
    private int smallWidth;
    private int rightWidth;    // common wide width for some 2nd column fields
    private int marginColLeft;

    public HudContextSettingsScreen(String contextName) { this(contextName, null); }

    public HudContextSettingsScreen(String contextName, Screen parent) {
        // タイトルは言語ファイルから取得（%s に contextName を入れる）
        super(Component.translatable("ingameinfo.screen.settings.title", contextName));
        this.contextName = contextName;
        this.parent = parent;
    }

    private HudContext getCtx() {
        for (HudContext c : HudContextManager.getContexts()) {
            if (Objects.equals(c.name(), contextName)) return c;
        }
        return null;
    }

    private static String toHex(int rgb) { return String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF); }
    private static double clamp01(double d) { return Math.max(0.0, Math.min(1.0, d)); }
    private static int parseHexColor(String s, int fallback) {
        if (s == null) return fallback;
        s = s.trim();
        try {
            if (s.startsWith("#")) s = s.substring(1);
            if (s.startsWith("0x") || s.startsWith("0X")) s = s.substring(2);
            if (s.length() > 6) s = s.substring(s.length() - 6);
            return Integer.parseInt(s, 16) & 0xFFFFFF;
        } catch (Exception e) { return fallback; }
    }
    private static int parseIntSafe(String s, int fallback) { try { return Math.max(0, Integer.parseInt(s.trim())); } catch (Exception e) { return fallback; } }
    private static float parseFloatSafe(String s, float fallback) { try { return Float.parseFloat(s.trim()); } catch (Exception e) { return fallback; } }
    private static double parseDoubleSafe(String s, double fallback) { try { return Double.parseDouble(s.trim()); } catch (Exception e) { return fallback; } }

    // ON/OFF 表示は言語キーから取得
    private Component boolText(boolean v) { return Component.translatable(v ? "ingameinfo.screen.toggle.on" : "ingameinfo.screen.toggle.off"); }
    // ボタンがONか判定する際は言語ファイルの ON 表示と比較する
    private boolean parseBoolButton(Button b) { return b.getMessage().getString().equalsIgnoreCase(Component.translatable("ingameinfo.screen.toggle.on").getString()); }

    private EditBox addEdit(int x, int y, int w, String initial, int maxLen) {
        EditBox eb = new EditBox(this.font, x, y, w, 20, Component.empty());
        eb.setMaxLength(maxLen);
        if (initial != null) eb.setValue(initial);
        this.addRenderableWidget(eb);
        return eb;
    }

    private Button addButton(int x, int y, int w, Component text, Button.OnPress onPress) {
        Button b = Button.builder(text, onPress).bounds(x, y, w, 20).build();
        this.addRenderableWidget(b);
        return b;
    }

    @Override
    protected void init() {
        super.init();
        HudContext ctx = getCtx();

        int gap = 8;                 // column gap
        int gapLabel = 6;            // label->box gap
        int rowSmall = 24;
        int rowLarge = 30;

        // Left labels (column 1) - 言語キーを使う
        String[] leftLabels = new String[]{
                "ingameinfo.label.text_color", "ingameinfo.label.scale", "ingameinfo.label.background", "ingameinfo.label.alpha",
                "ingameinfo.label.padding_t", "ingameinfo.label.padding_l", "ingameinfo.label.line_spacing", "ingameinfo.label.pad_bottom"
        };
        int maxLabelW = 0;
        for (String lbl : leftLabels) maxLabelW = Math.max(maxLabelW, this.font.width(Component.translatable(lbl).getString()));

        // Second column labels used next to 2nd column widgets (言語キー)
        String[] secondLabels = new String[]{
                "ingameinfo.label.shadow",      // row0
                "ingameinfo.label.align",       // row1
                "ingameinfo.label.rgb",         // row2
                "ingameinfo.label.per_line",    // row3
                "ingameinfo.label.padding_b",   // row4
                "ingameinfo.label.padding_r",   // row5
                "ingameinfo.label.pad_top"       // row6
                // row7: none
        };
        int maxSecondLabelW = 0;
        for (String s : secondLabels) maxSecondLabelW = Math.max(maxSecondLabelW, this.font.width(Component.translatable(s).getString()));

        inputWidth = 140;            // wide fields width (column 1)
        smallWidth = 72;             // toggle/small fields width (column 2 small)
        rightWidth = 140;            // some 2nd column fields use this wider width

        // Compute column lefts
        int firstBlockWidth = maxLabelW + gapLabel + inputWidth; // label + gap + field
        int secondBlockWidth = (maxSecondLabelW + gapLabel) + Math.max(smallWidth, rightWidth);
        int totalMainWidth = firstBlockWidth + gap + secondBlockWidth;

        int betweenBlocks = 36; // space between main area and margin pane
        int marginBlockWidth = rightWidth;
        int totalWidth = totalMainWidth + betweenBlocks + marginBlockWidth;

        int startX = Math.max(8, this.width / 2 - totalWidth / 2);
        labelColRight = startX + maxLabelW;
        inputColLeft = labelColRight + gapLabel; // column1 input left
        smallColLeft = inputColLeft + inputWidth + gap + maxSecondLabelW + gapLabel; // column2 input left (after its label)
        marginColLeft = startX + totalMainWidth + betweenBlocks;

        topY = Math.max(32, this.height / 2 - 110);

        int[] heights = new int[]{rowSmall, rowLarge, rowSmall, rowLarge, rowSmall, rowLarge, rowSmall, rowLarge};
        rowY = new int[heights.length];
        rowY[0] = topY;
        for (int i = 1; i < heights.length; i++) rowY[i] = rowY[i-1] + heights[i-1];

        // Row 0: Text Color + Shadow (Align moved to row 1)
        int cy = rowCenter(rowY[0], heights[0]);
        colorHex = addEdit(inputColLeft, cy, inputWidth, toHex(ctx != null ? ctx.color() : 0xFFFFFF), 64);
        shadowToggle = addButton(smallColLeft, cy, smallWidth, boolText(ctx != null && ctx.shadow()), b -> toggleButton(shadowToggle));

        // Row 1: Scale + Align (2nd column)
        cy = rowCenter(rowY[1], heights[1]);
        scaleBox = addEdit(inputColLeft, cy, inputWidth, ctx != null ? String.format(Locale.ROOT, "%.2f", ctx.scale()) : "1.00", 32);
        alignBox = addEdit(smallColLeft, cy, smallWidth, ctx != null ? ctx.align().name() : "LEFT", 16);

        // Row 2: Background (toggle) + RGB (wide)
        cy = rowCenter(rowY[2], heights[2]);
        backgroundToggle = addButton(inputColLeft, cy, smallWidth, boolText(ctx != null && ctx.background()), b -> toggleButton(backgroundToggle));
        bgRgbHex = addEdit(smallColLeft, cy, rightWidth, toHex(ctx != null ? ctx.backgroundRgb() : 0x000000), 64);

        // Row 3: Alpha (wide) + Per-Line (toggle)
        cy = rowCenter(rowY[3], heights[3]);
        bgAlphaBox = addEdit(inputColLeft, cy, inputWidth, ctx != null ? String.format(Locale.ROOT, "%.2f", clamp01(ctx.backgroundAlpha())) : "0.33", 32);
        bgPerLineToggle = addButton(smallColLeft, cy, smallWidth, boolText(ctx != null && ctx.backgroundPerLine()), b -> toggleButton(bgPerLineToggle));

        // Row 4: Padding T/B (left/right columns)
        cy = rowCenter(rowY[4], heights[4]);
        paddingTopBox = addEdit(inputColLeft, cy, inputWidth, Integer.toString(ctx != null ? ctx.paddingTop() : 4), 10);
        paddingBottomBox = addEdit(smallColLeft, cy, rightWidth, Integer.toString(ctx != null ? ctx.paddingBottom() : 4), 10);

        // Row 5: Padding L/R
        cy = rowCenter(rowY[5], heights[5]);
        paddingLeftBox = addEdit(inputColLeft, cy, inputWidth, Integer.toString(ctx != null ? ctx.paddingLeft() : 4), 10);
        paddingRightBox = addEdit(smallColLeft, cy, rightWidth, Integer.toString(ctx != null ? ctx.paddingRight() : 4), 10);

        // Row 6: Line Spacing / PadTop
        cy = rowCenter(rowY[6], heights[6]);
        lineSpacingBox = addEdit(inputColLeft, cy, inputWidth, Integer.toString(ctx != null ? ctx.lineSpacing() : 0), 10);
        lineSpacingPadTopBox = addEdit(smallColLeft, cy, rightWidth, Integer.toString(ctx != null ? ctx.lineSpacingPaddingTop() : 0), 10);

        // Row 7: PadBottom
        cy = rowCenter(rowY[7], heights[7]);
        lineSpacingPadBottomBox = addEdit(inputColLeft, cy, inputWidth, Integer.toString(ctx != null ? ctx.lineSpacingPaddingBottom() : 0), 10);

        // Margin (right separate block, align to rows 0..3)
        marginTopBox = addEdit(marginColLeft, rowCenter(rowY[0], heights[0]), rightWidth, Integer.toString(ctx != null ? ctx.marginTop() : 0), 10);
        marginBottomBox = addEdit(marginColLeft, rowCenter(rowY[1], heights[1]), rightWidth, Integer.toString(ctx != null ? ctx.marginBottom() : 0), 10);
        marginLeftBox = addEdit(marginColLeft, rowCenter(rowY[2], heights[2]), rightWidth, Integer.toString(ctx != null ? ctx.marginLeft() : 0), 10);
        marginRightBox = addEdit(marginColLeft, rowCenter(rowY[3], heights[3]), rightWidth, Integer.toString(ctx != null ? ctx.marginRight() : 0), 10);

        // Buttons
        int btnY = Math.min(this.height - 24, (rowY[rowY.length-1] + (heights[heights.length-1])) + 12);
        int centerX = this.width / 2;
        applyBtn = Button.builder(Component.translatable("ingameinfo.button.apply"), b -> onApply()).bounds(centerX - 160, btnY, 90, 20).build();
        cancelBtn = Button.builder(Component.translatable("ingameinfo.button.cancel"), b -> onCancel()).bounds(centerX - 60, btnY, 90, 20).build();
        resetBtn = Button.builder(Component.translatable("ingameinfo.button.reset"), b -> onReset()).bounds(centerX + 40, btnY, 90, 20).build();
        this.addRenderableWidget(applyBtn);
        this.addRenderableWidget(cancelBtn);
        this.addRenderableWidget(resetBtn);
    }

    private int rowCenter(int rowTop, int rowHeight) { return rowTop + (rowHeight - 20) / 2; }
    private void toggleButton(Button button) { boolean v = !parseBoolButton(button); button.setMessage(boolText(v)); }

    private void onApply() {
        HudContext current = getCtx();
        if (current == null) { onCancel(); return; }
        int color = parseHexColor(colorHex.getValue(), current.color());
        boolean shadow = parseBoolButton(shadowToggle);
        float scale = Math.max(0.5f, Math.min(3.0f, parseFloatSafe(scaleBox.getValue(), current.scale())));
        String alignStr = alignBox.getValue().trim().toUpperCase(Locale.ROOT);
        HudContext.Align align; try { align = HudContext.Align.valueOf(alignStr); } catch (Exception ex) { align = current.align(); }
        boolean background = parseBoolButton(backgroundToggle);
        int bgRgb = parseHexColor(bgRgbHex.getValue(), current.backgroundRgb());
        double bgAlpha = clamp01(parseDoubleSafe(bgAlphaBox.getValue(), current.backgroundAlpha()));
        boolean bgPerLine = parseBoolButton(bgPerLineToggle);
        int pTop = parseIntSafe(paddingTopBox.getValue(), current.paddingTop());
        int pBottom = parseIntSafe(paddingBottomBox.getValue(), current.paddingBottom());
        int pLeft = parseIntSafe(paddingLeftBox.getValue(), current.paddingLeft());
        int pRight = parseIntSafe(paddingRightBox.getValue(), current.paddingRight());
        int lineSp = parseIntSafe(lineSpacingBox.getValue(), current.lineSpacing());
        int linePadTop = parseIntSafe(lineSpacingPadTopBox.getValue(), current.lineSpacingPaddingTop());
        int linePadBottom = parseIntSafe(lineSpacingPadBottomBox.getValue(), current.lineSpacingPaddingBottom());
        int mTop = parseIntSafe(marginTopBox.getValue(), current.marginTop());
        int mBottom = parseIntSafe(marginBottomBox.getValue(), current.marginBottom());
        int mLeft = parseIntSafe(marginLeftBox.getValue(), current.marginLeft());
        int mRight = parseIntSafe(marginRightBox.getValue(), current.marginRight());

        HudContextIO.Settings s = new HudContextIO.Settings();
        s.color = color; s.shadow = shadow; s.scale = scale; s.align = align;
        s.background = background; s.backgroundRgb = bgRgb; s.backgroundAlpha = bgAlpha; s.backgroundPerLine = bgPerLine;
        s.paddingTop = pTop; s.paddingBottom = pBottom; s.paddingLeft = pLeft; s.paddingRight = pRight;
        s.lineSpacing = lineSp; s.lineSpacingPaddingTop = linePadTop; s.lineSpacingPaddingBottom = linePadBottom;
        s.marginTop = mTop; s.marginBottom = mBottom; s.marginLeft = mLeft; s.marginRight = mRight;

        HudContextIO.saveContextSettings(contextName, s);
        onClose();
    }

    private void onReset() { this.init(); }
    private void onCancel() { onClose(); }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, this.getTitle(), this.width / 2, topY - 18, 0xFFFFFF);

        int labelColor = 0xFFFFFF;
        int lineH = this.font.lineHeight;
        int labelOffsetY = (20 - lineH) / 2;
        int gapLabel = 6;

        // Column 1 labels (to the left of each first-column widget)
        if (colorHex != null) {
            int y = colorHex.getY() + labelOffsetY;
            int x = colorHex.getX() - gapLabel - this.font.width(Component.translatable("ingameinfo.label.text_color").getString());
            g.drawString(this.font, Component.translatable("ingameinfo.label.text_color").getString(), x, y, labelColor, false);
        }
        if (scaleBox != null) {
            int y = scaleBox.getY() + labelOffsetY;
            int x = scaleBox.getX() - gapLabel - this.font.width(Component.translatable("ingameinfo.label.scale").getString());
            g.drawString(this.font, Component.translatable("ingameinfo.label.scale").getString(), x, y, labelColor, false);
        }
        if (backgroundToggle != null) {
            int y = backgroundToggle.getY() + labelOffsetY;
            int x = backgroundToggle.getX() - gapLabel - this.font.width(Component.translatable("ingameinfo.label.background").getString());
            g.drawString(this.font, Component.translatable("ingameinfo.label.background").getString(), x, y, labelColor, false);
        }
        if (bgAlphaBox != null) {
            int y = bgAlphaBox.getY() + labelOffsetY;
            int x = bgAlphaBox.getX() - gapLabel - this.font.width(Component.translatable("ingameinfo.label.alpha").getString());
            g.drawString(this.font, Component.translatable("ingameinfo.label.alpha").getString(), x, y, labelColor, false);
        }
        if (paddingTopBox != null) {
            int y = paddingTopBox.getY() + labelOffsetY;
            int x = paddingTopBox.getX() - gapLabel - this.font.width(Component.translatable("ingameinfo.label.padding_t").getString());
            g.drawString(this.font, Component.translatable("ingameinfo.label.padding_t").getString(), x, y, labelColor, false);
        }
        if (paddingLeftBox != null) {
            int y = paddingLeftBox.getY() + labelOffsetY;
            int x = paddingLeftBox.getX() - gapLabel - this.font.width(Component.translatable("ingameinfo.label.padding_l").getString());
            g.drawString(this.font, Component.translatable("ingameinfo.label.padding_l").getString(), x, y, labelColor, false);
        }
        if (lineSpacingBox != null) {
            int y = lineSpacingBox.getY() + labelOffsetY;
            int x = lineSpacingBox.getX() - gapLabel - this.font.width(Component.translatable("ingameinfo.label.line_spacing").getString());
            g.drawString(this.font, Component.translatable("ingameinfo.label.line_spacing").getString(), x, y, labelColor, false);
        }
        if (lineSpacingPadBottomBox != null) {
            int y = lineSpacingPadBottomBox.getY() + labelOffsetY;
            int x = lineSpacingPadBottomBox.getX() - gapLabel - this.font.width(Component.translatable("ingameinfo.label.pad_bottom").getString());
            g.drawString(this.font, Component.translatable("ingameinfo.label.pad_bottom").getString(), x, y, labelColor, false);
        }

        // Column 2 labels (to the left of second-column widgets)
        if (shadowToggle != null) {
            int y = shadowToggle.getY() + labelOffsetY;
            int x = shadowToggle.getX() - gapLabel - this.font.width(Component.translatable("ingameinfo.label.shadow").getString());
            g.drawString(this.font, Component.translatable("ingameinfo.label.shadow").getString(), x, y, labelColor, false);
        }
        if (alignBox != null) {
            int y = alignBox.getY() + labelOffsetY;
            int x = alignBox.getX() - gapLabel - this.font.width(Component.translatable("ingameinfo.label.align").getString());
            g.drawString(this.font, Component.translatable("ingameinfo.label.align").getString(), x, y, labelColor, false);
        }
        if (bgRgbHex != null) {
            int y = bgRgbHex.getY() + labelOffsetY;
            int x = bgRgbHex.getX() - gapLabel - this.font.width(Component.translatable("ingameinfo.label.rgb").getString());
            g.drawString(this.font, Component.translatable("ingameinfo.label.rgb").getString(), x, y, labelColor, false);
        }
        if (bgPerLineToggle != null) {
            int y = bgPerLineToggle.getY() + labelOffsetY;
            int x = bgPerLineToggle.getX() - gapLabel - this.font.width(Component.translatable("ingameinfo.label.per_line").getString());
            g.drawString(this.font, Component.translatable("ingameinfo.label.per_line").getString(), x, y, labelColor, false);
        }
        if (paddingBottomBox != null) {
            int y = paddingBottomBox.getY() + labelOffsetY;
            int x = paddingBottomBox.getX() - gapLabel - this.font.width(Component.translatable("ingameinfo.label.padding_b").getString());
            g.drawString(this.font, Component.translatable("ingameinfo.label.padding_b").getString(), x, y, labelColor, false);
        }
        if (paddingRightBox != null) {
            int y = paddingRightBox.getY() + labelOffsetY;
            int x = paddingRightBox.getX() - gapLabel - this.font.width(Component.translatable("ingameinfo.label.padding_r").getString());
            g.drawString(this.font, Component.translatable("ingameinfo.label.padding_r").getString(), x, y, labelColor, false);
        }
        if (lineSpacingPadTopBox != null) {
            int y = lineSpacingPadTopBox.getY() + labelOffsetY;
            int x = lineSpacingPadTopBox.getX() - gapLabel - this.font.width(Component.translatable("ingameinfo.label.pad_top").getString());
            g.drawString(this.font, Component.translatable("ingameinfo.label.pad_top").getString(), x, y, labelColor, false);
        }

        // Right pane: margin labels to the right of margin boxes
        String[] marginLabels = new String[]{"ingameinfo.margin.top", "ingameinfo.margin.bottom", "ingameinfo.margin.left", "ingameinfo.margin.right"};
        EditBox[] marginBoxes = new EditBox[]{marginTopBox, marginBottomBox, marginLeftBox, marginRightBox};
        int labelXRightOfBox = marginColLeft + rightWidth + 8;
        for (int i = 0; i < marginLabels.length; i++) {
            int boxY = (marginBoxes[i] != null ? marginBoxes[i].getY() : (rowY[i]));
            int y = boxY + labelOffsetY;
            g.drawString(this.font, Component.translatable(marginLabels[i]).getString(), labelXRightOfBox, y, labelColor, false);
        }
    }

    @Override
    public void onClose() {
        var mc = Minecraft.getInstance();
        if (this.parent != null) mc.setScreen(parent); else mc.setScreen(null);
    }
}
