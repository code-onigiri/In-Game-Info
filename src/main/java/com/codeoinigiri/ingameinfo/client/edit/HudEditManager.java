package com.codeoinigiri.ingameinfo.client.edit;

import com.codeoinigiri.ingameinfo.InGameInfo;
import com.codeoinigiri.ingameinfo.config.HudPosition;
import com.codeoinigiri.ingameinfo.hud.HudContext;
import com.codeoinigiri.ingameinfo.hud.HudContextManager;
import com.codeoinigiri.ingameinfo.hud.HudContextIO;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.*;

/**
 * Handles in-game HUD editing state and interactions.
 */
@Mod.EventBusSubscriber(modid = InGameInfo.MOD_ID, value = Dist.CLIENT)
public class HudEditManager {

    public enum Mode { NONE, POSITION, TEXT }

    private static Mode mode = Mode.NONE;

    private static KeyMapping EDIT_TOGGLE;
    private static KeyMapping EDIT_SETTINGS;
    private static KeyMapping NEW_CONTEXT;
    private static KeyMapping LINE_INSERT_BELOW;
    private static KeyMapping LINE_DELETE;

    // Runtime caches per frame for hit testing
    private static int screenWidth;
    private static int screenHeight;

    private static final Map<String, Rect> contextRects = new HashMap<>();
    private static final Map<String, List<Rect>> lineRects = new HashMap<>();

    // Selection + dragging
    private static String selectedContextName = null;
    private static int selectedLineIndex = -1;
    private static boolean dragging = false;
    private static double mouseDownX, mouseDownY;
    private static int contextStartX, contextStartY; // starting top-left on drag begin
    private static int previewDx, previewDy; // translation while dragging

    public static void registerKeyMappings(RegisterKeyMappingsEvent e) {
        if (EDIT_TOGGLE == null) {
            EDIT_TOGGLE = new KeyMapping(
                    "key.ingameinfo.edit_toggle",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_F9,
                    "key.categories.ingameinfo");
        }
        if (EDIT_SETTINGS == null) {
            EDIT_SETTINGS = new KeyMapping(
                    "key.ingameinfo.edit_settings",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_F10,
                    "key.categories.ingameinfo");
        }
        if (NEW_CONTEXT == null) {
            NEW_CONTEXT = new KeyMapping(
                    "key.ingameinfo.new_context",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_F8,
                    "key.categories.ingameinfo");
        }
        if (LINE_INSERT_BELOW == null) {
            LINE_INSERT_BELOW = new KeyMapping(
                    "key.ingameinfo.line_insert_below",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_F6,
                    "key.categories.ingameinfo");
        }
        if (LINE_DELETE == null) {
            LINE_DELETE = new KeyMapping(
                    "key.ingameinfo.line_delete",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_DELETE,
                    "key.categories.ingameinfo");
        }
        e.register(EDIT_TOGGLE);
        e.register(EDIT_SETTINGS);
        e.register(NEW_CONTEXT);
        e.register(LINE_INSERT_BELOW);
        e.register(LINE_DELETE);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        // F9 toggle handled in onKeyInput to work even when screens are open
        if (dragging && mode == Mode.POSITION) {
            var screen = Minecraft.getInstance().screen;
            if (screen != null && !(screen instanceof HudEditScreen)) return;
            // update preview using current mouse position
            Minecraft mc = Minecraft.getInstance();
            var window = mc.getWindow();
            double mx = mc.mouseHandler.xpos() * (double) window.getGuiScaledWidth() / (double) window.getWidth();
            double my = mc.mouseHandler.ypos() * (double) window.getGuiScaledHeight() / (double) window.getHeight();
            previewDx = (int) Math.round(mx - mouseDownX);
            previewDy = (int) Math.round(my - mouseDownY);
        }
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton e) {
        if (mode == Mode.NONE) return;
        var currentScreen = Minecraft.getInstance().screen;
        if (currentScreen != null && !(currentScreen instanceof HudEditScreen)) return; // allow when our edit screen is open

        Minecraft mc = Minecraft.getInstance();
        var window = mc.getWindow();
        double mx = mc.mouseHandler.xpos() * (double) window.getGuiScaledWidth() / (double) window.getWidth();
        double my = mc.mouseHandler.ypos() * (double) window.getGuiScaledHeight() / (double) window.getHeight();
        int button = e.getButton();
        int action = e.getAction();

        if (action == GLFW.GLFW_PRESS) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                // Select context/line
                String ctxName = findContextAt(mx, my);
                if (ctxName != null) {
                    selectedContextName = ctxName;
                    if (mode == Mode.TEXT) {
                        selectedLineIndex = findLineAt(ctxName, mx, my);
                        if (selectedLineIndex >= 0) {
                            // Open line edit screen with raw text
                            HudContext ctx = getContextByName(ctxName);
                            if (ctx != null && selectedLineIndex < ctx.lines().size()) {
                                String raw = ctx.lines().get(selectedLineIndex);
                                mc.setScreen(new HudLineEditScreen(Component.translatable("ingameinfo.line_edit.title", ctxName, selectedLineIndex + 1), ctxName, selectedLineIndex, raw));
                                e.setCanceled(true);
                                return;
                            }
                        }
                    } else if (mode == Mode.POSITION) {
                        // Begin dragging
                        Rect r = contextRects.get(ctxName);
                        if (r != null) {
                            dragging = true;
                            mouseDownX = mx;
                            mouseDownY = my;
                            contextStartX = r.x1;
                            contextStartY = r.y1;
                            previewDx = 0;
                            previewDy = 0;
                            e.setCanceled(true);
                            return;
                        }
                    }
                }
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && mode == Mode.POSITION) {
                // cycle anchor for selected context
                if (selectedContextName == null) {
                    selectedContextName = findContextAt(mx, my);
                }
                if (selectedContextName != null) {
                    HudContext ctx = getContextByName(selectedContextName);
                    if (ctx != null) {
                        HudPosition next = cyclePosition(ctx.position());
                        // Save position change immediately (margins kept)
                        HudContextIO.savePosition(selectedContextName, next,
                                ctx.marginTop(), ctx.marginBottom(), ctx.marginLeft(), ctx.marginRight());
                        // Reload handled by IO, update selection rects later
                        e.setCanceled(true);
                        return;
                    }
                }
            }
        } else if (action == GLFW.GLFW_RELEASE) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && dragging) {
                // Commit drag
                int finalX = contextStartX + previewDx;
                int finalY = contextStartY + previewDy;
                commitDrag(finalX, finalY);
                dragging = false;
                previewDx = previewDy = 0;
                e.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key e) {
        // Handle keybinds even when a Screen is open
        if (e.getAction() != GLFW.GLFW_PRESS) return;
        if (EDIT_TOGGLE == null) return;
        try {
            if (EDIT_TOGGLE.matches(e.getKey(), e.getScanCode())) {
                cycleMode();
                return;
            }
        } catch (Throwable ignore) {
            if (e.getKey() == GLFW.GLFW_KEY_F9) { cycleMode(); return; }
        }
        // Settings key
        try {
            if (EDIT_SETTINGS != null && EDIT_SETTINGS.matches(e.getKey(), e.getScanCode())) {
                openSettingsIfAvailable();
                return;
            }
        } catch (Throwable ignore) {
            if (e.getKey() == GLFW.GLFW_KEY_F10) { openSettingsIfAvailable(); return; }
        }

        // New: quick actions for line insert/delete and new context
        if (mode != Mode.NONE) {
            // Insert line below (F6 by default)
            try {
                if (LINE_INSERT_BELOW != null && LINE_INSERT_BELOW.matches(e.getKey(), e.getScanCode())) {
                    getSelectedContextName().ifPresent(name -> {
                        int idx = getSelectedLineIndex();
                        if (idx >= 0) HudContextIO.insertLine(name, idx + 1, ""); else HudContextIO.addLine(name, "");
                    });
                    return;
                }
            } catch (Throwable ignore) {
                if (e.getKey() == GLFW.GLFW_KEY_F6) {
                    getSelectedContextName().ifPresent(name -> {
                        int idx = getSelectedLineIndex();
                        if (idx >= 0) HudContextIO.insertLine(name, idx + 1, ""); else HudContextIO.addLine(name, "");
                    });
                    return;
                }
            }
            // Delete selected line (Delete by default)
            try {
                if (LINE_DELETE != null && LINE_DELETE.matches(e.getKey(), e.getScanCode())) {
                    getSelectedContextName().ifPresent(name -> { int idx = getSelectedLineIndex(); if (idx >= 0) HudContextIO.deleteLine(name, idx); });
                    return;
                }
            } catch (Throwable ignore) {
                if (e.getKey() == GLFW.GLFW_KEY_DELETE) {
                    getSelectedContextName().ifPresent(name -> { int idx = getSelectedLineIndex(); if (idx >= 0) HudContextIO.deleteLine(name, idx); });
                    return;
                }
            }
            // Create new context (F8 by default)
            try {
                if (NEW_CONTEXT != null && NEW_CONTEXT.matches(e.getKey(), e.getScanCode())) {
                    var mc = net.minecraft.client.Minecraft.getInstance();
                    mc.setScreen(new HudNewContextScreen(mc.screen));
                    return;
                }
            } catch (Throwable ignore) {
                if (e.getKey() == GLFW.GLFW_KEY_F8) {
                    var mc = net.minecraft.client.Minecraft.getInstance();
                    mc.setScreen(new HudNewContextScreen(mc.screen));
                    return;
                }
            }
        }
    }

    public static void beginFrame(int sw, int sh, Font f) {
        screenWidth = sw;
        screenHeight = sh;
        Objects.requireNonNull(f); // keep parameter used to avoid unused-parameter warning
        contextRects.clear();
        lineRects.clear();
    }

    public static void registerContextRect(String name, int x, int y, int w, int h) {
        contextRects.put(name, new Rect(x, y, x + w, y + h));
    }

    public static void registerLineRect(String name, int lineIndex, int x, int y, int w, int h) {
        lineRects.computeIfAbsent(name, k -> new ArrayList<>());
        List<Rect> list = lineRects.get(name);
        while (list.size() <= lineIndex) list.add(null);
        list.set(lineIndex, new Rect(x, y, x + w, y + h));
    }

    public static boolean isEditMode() { return mode != Mode.NONE; }
    public static boolean isPositionMode() { return mode == Mode.POSITION; }
    public static boolean isTextMode() { return mode == Mode.TEXT; }

    public static Optional<int[]> getPreviewTranslation(String ctxName) {
        if (mode == Mode.POSITION && dragging && Objects.equals(ctxName, selectedContextName)) {
            return Optional.of(new int[]{previewDx, previewDy});
        }
        return Optional.empty();
    }

    public static Optional<String> getSelectedContextName() {
        return Optional.ofNullable(selectedContextName);
    }

    public static int getSelectedLineIndex() { return selectedLineIndex; }

    public static void renderOverlayHints(net.minecraft.client.gui.GuiGraphics g) {
        if (mode == Mode.NONE) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        // Draw a small hint at top
        Component modeTextComp = switch (mode) {
            case POSITION -> Component.translatable("ingameinfo.edit.hint.position");
            case TEXT -> Component.translatable("ingameinfo.edit.hint.text");
            default -> Component.literal("");
        };
        int color = 0xFFFFFF;
        g.drawString(mc.font, modeTextComp.getString(), 8, 8, color, true);

        // Draw selection info
        if (selectedContextName != null) {
            HudContext ctx = getContextByName(selectedContextName);
            if (ctx != null) {
                String anchor = ctx.position().name();
                String sel = Component.translatable("ingameinfo.edit.selection_info", selectedContextName, anchor).getString();
                g.drawString(mc.font, sel, 8, 8 + mc.font.lineHeight + 2, 0xFFFF55, true);
            }
        }

        // Draw rectangles
        for (Map.Entry<String, Rect> e : contextRects.entrySet()) {
            Rect r = e.getValue();
            int argb = 0x40FFFFFF;
            g.fill(r.x1, r.y1, r.x2, r.y2, argb);
            // border
            int border = 0xAAFFFFFF;
            drawBorder(g, r, border);
        }

        if (mode == Mode.TEXT && selectedContextName != null) {
            int idx = selectedLineIndex;
            List<Rect> list = lineRects.get(selectedContextName);
            if (list != null && idx >= 0 && idx < list.size()) {
                Rect r = list.get(idx);
                if (r != null) {
                    int border = 0xAA55FFFF;
                    drawBorder(g, r, border);
                }
            }
        }
    }

    private static void drawBorder(net.minecraft.client.gui.GuiGraphics g, Rect r, int color) {
        // top
        g.fill(r.x1, r.y1, r.x2, r.y1 + 1, color);
        // bottom
        g.fill(r.x1, r.y2 - 1, r.x2, r.y2, color);
        // left
        g.fill(r.x1, r.y1, r.x1 + 1, r.y2, color);
        // right
        g.fill(r.x2 - 1, r.y1, r.x2, r.y2, color);
    }

    private static void cycleMode() {
        // rotate mode
        switch (mode) {
            case NONE -> mode = Mode.POSITION;
            case POSITION -> mode = Mode.TEXT;
            case TEXT -> mode = Mode.NONE;
        }
        // Screen management to show/hide mouse pointer
        var mc = Minecraft.getInstance();
        var current = mc.screen;
        if (mode == Mode.NONE) {
            // Reset selection when leaving modes
            selectedContextName = null;
            selectedLineIndex = -1;
            dragging = false;
            previewDx = previewDy = 0;
            // Close our edit screens if open
            if (current instanceof HudEditScreen || current instanceof HudLineEditScreen) {
                mc.setScreen(null);
            }
        } else {
            // Entering an edit mode: ensure a cursor is visible when no other screen is open
            if (current == null) {
                mc.setScreen(new HudEditScreen());
            }
            // If a text line editor is open but we switched away from TEXT, close it
            if (!(mode == Mode.TEXT) && current instanceof HudLineEditScreen) {
                mc.setScreen(new HudEditScreen());
            }
        }
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.translatable("ingameinfo.edit.mode_message", mode.name()), true);
        }
    }

    private static void openSettingsIfAvailable() {
        if (mode == Mode.NONE) {
            var mc = Minecraft.getInstance();
            if (mc.player != null) mc.player.displayClientMessage(Component.translatable("ingameinfo.edit.not_in_mode"), true);
            return;
        }
        if (selectedContextName == null) {
            var mc = Minecraft.getInstance();
            if (mc.player != null) mc.player.displayClientMessage(Component.translatable("ingameinfo.edit.select_first"), true);
            return;
        }
        var mc = Minecraft.getInstance();
        Screen parentForSettings = mc.screen;
        if (!(parentForSettings instanceof HudEditScreen) && !(parentForSettings instanceof HudLineEditScreen)) parentForSettings = new HudEditScreen();
        mc.setScreen(new HudContextSettingsScreen(selectedContextName, parentForSettings));
    }

    private static HudContext getContextByName(String name) {
        for (HudContext ctx : HudContextManager.getContexts()) {
            if (Objects.equals(ctx.name(), name)) return ctx;
        }
        return null;
    }

    private static String findContextAt(double x, double y) {
        for (Map.Entry<String, Rect> e : contextRects.entrySet()) {
            if (e.getValue().contains(x, y)) return e.getKey();
        }
        return null;
    }

    private static int findLineAt(String ctxName, double x, double y) {
        List<Rect> list = lineRects.getOrDefault(ctxName, Collections.emptyList());
        for (int i = 0; i < list.size(); i++) {
            Rect r = list.get(i);
            if (r != null && r.contains(x, y)) return i;
        }
        return -1;
    }

    private static void commitDrag(int finalX, int finalY) {
        if (selectedContextName == null) return;
        HudContext ctx = getContextByName(selectedContextName);
        if (ctx == null) return;
        // We need width/height to compute margins
        Rect r = contextRects.get(selectedContextName);
        if (r == null) return;
        int totalWidth = r.width();
        int totalHeight = r.height();

        // Preserve current anchor; update corresponding margins only
        HudPosition pos = ctx.position();
        int marginTop = ctx.marginTop();
        int marginBottom = ctx.marginBottom();
        int marginLeft = ctx.marginLeft();
        int marginRight = ctx.marginRight();
        switch (pos) {
            case TOP_LEFT -> {
                marginLeft = Math.max(0, finalX);
                marginTop = Math.max(0, finalY);
            }
            case TOP_RIGHT -> {
                marginRight = Math.max(0, screenWidth - (finalX + totalWidth));
                marginTop = Math.max(0, finalY);
            }
            case BOTTOM_LEFT -> {
                marginLeft = Math.max(0, finalX);
                marginBottom = Math.max(0, screenHeight - (finalY + totalHeight));
            }
            case BOTTOM_RIGHT -> {
                marginRight = Math.max(0, screenWidth - (finalX + totalWidth));
                marginBottom = Math.max(0, screenHeight - (finalY + totalHeight));
            }
            case CENTER_LEFT -> {
                marginLeft = Math.max(0, finalX);
            }
            case CENTER_RIGHT -> {
                marginRight = Math.max(0, screenWidth - (finalX + totalWidth));
            }
            case CENTER_TOP -> {
                marginTop = Math.max(0, finalY);
            }
            case CENTER_BOTTOM -> {
                marginBottom = Math.max(0, screenHeight - (finalY + totalHeight));
            }
        }

        HudContextIO.savePosition(selectedContextName, pos, marginTop, marginBottom, marginLeft, marginRight);
        // On save, contexts reload via IO; selection remains by name
    }

    private static HudPosition cyclePosition(HudPosition p) {
        HudPosition[] vals = HudPosition.values();
        int idx = p.ordinal();
        idx = (idx + 1) % vals.length;
        return vals[idx];
    }

    public record Rect(int x1, int y1, int x2, int y2) {
        boolean contains(double x, double y) { return x >= x1 && x <= x2 && y >= y1 && y <= y2; }
        int width() { return Math.max(0, x2 - x1); }
        int height() { return Math.max(0, y2 - y1); }
    }
}
