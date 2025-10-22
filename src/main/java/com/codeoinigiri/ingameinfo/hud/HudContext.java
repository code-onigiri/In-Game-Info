package com.codeoinigiri.ingameinfo.hud;

import com.codeoinigiri.ingameinfo.config.HudPosition;

import java.util.List;

public class HudContext {
    public final String name;
    public final HudPosition position;
    public final int color;
    public final List<String> lines;

    public final Align align;
    public final float scale;
    public final boolean shadow;

    public final boolean background;
    public final int backgroundColor;
    public final int backgroundPadding;

    public HudContext(String name, HudPosition position, int color, List<String> lines,
                      Align align, float scale, boolean shadow,
                      boolean background, int backgroundColor, int backgroundPadding) {
        this.name = name;
        this.position = position;
        this.color = color;
        this.lines = lines;
        this.align = align;
        this.scale = scale;
        this.shadow = shadow;
        this.background = background;
        this.backgroundColor = backgroundColor;
        this.backgroundPadding = backgroundPadding;
    }

    public enum Align {
        LEFT, CENTER, RIGHT
    }
}
