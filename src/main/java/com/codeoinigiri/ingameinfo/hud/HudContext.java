package com.codeoinigiri.ingameinfo.hud;

import com.codeoinigiri.ingameinfo.config.HudPosition;

import java.util.List;

public record HudContext(String name, HudPosition position, int color, List<String> lines, Align align, float scale,
                         boolean shadow, boolean background, int backgroundRgb, double backgroundAlpha,
                         int backgroundPadding) {

    public enum Align {
        LEFT, CENTER, RIGHT
    }
}