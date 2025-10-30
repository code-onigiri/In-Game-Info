package com.codeoinigiri.ingameinfo.config;

public enum HudPosition {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    CENTER,
    CENTER_LEFT,
    CENTER_RIGHT,
    TOP_CENTER,
    BOTTOM_CENTER;

    public int getX(int screenWidth, int totalWidth) {
        return switch (this) {
            case TOP_LEFT, BOTTOM_LEFT, CENTER_LEFT -> 0;
            case TOP_RIGHT, BOTTOM_RIGHT, CENTER_RIGHT -> screenWidth - totalWidth;
            case TOP_CENTER, BOTTOM_CENTER, CENTER -> screenWidth / 2 - totalWidth / 2;
        };
    }

    public int getY(int screenHeight, int totalHeight) {
        return switch (this) {
            case TOP_LEFT, TOP_RIGHT, TOP_CENTER -> 0;
            case BOTTOM_LEFT, BOTTOM_RIGHT, BOTTOM_CENTER -> screenHeight - totalHeight;
            case CENTER_LEFT, CENTER_RIGHT, CENTER -> screenHeight / 2 - totalHeight / 2;
        };
    }
}