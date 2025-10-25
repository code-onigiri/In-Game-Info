package com.codeoinigiri.ingameinfo.hud;

import com.codeoinigiri.ingameinfo.config.HudPosition;

import java.util.List;

public record HudContext(String name, HudPosition position, int color, List<String> lines, Align align, float scale,
                         boolean shadow, boolean background, int backgroundRgb, double backgroundAlpha,
                         boolean backgroundPerLine,
                         int paddingTop, int paddingBottom, int paddingLeft, int paddingRight,
                         int lineSpacing, int lineSpacingPaddingTop, int lineSpacingPaddingBottom,
                         int marginTop, int marginBottom, int marginLeft, int marginRight) {

    public enum Align {
        LEFT, CENTER, RIGHT
    }
}