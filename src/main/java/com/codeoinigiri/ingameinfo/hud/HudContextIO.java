package com.codeoinigiri.ingameinfo.hud;

import com.codeoinigiri.ingameinfo.InGameInfo;
import com.codeoinigiri.ingameinfo.config.HudPosition;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Utilities to save HUD context changes back to TOML and reload.
 */
public class HudContextIO {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final File CONTEXT_DIR = new File("config/ingameinfo/context");

    /** DTO for bulk saving of context settings */
    public static class Settings {
        public int color;
        public boolean shadow;
        public float scale;
        public HudContext.Align align;
        public boolean background;
        public int backgroundRgb;
        public double backgroundAlpha;
        public boolean backgroundPerLine;
        public int paddingTop;
        public int paddingBottom;
        public int paddingLeft;
        public int paddingRight;
        public int lineSpacing;
        public int lineSpacingPaddingTop;
        public int lineSpacingPaddingBottom;
        public int marginTop;
        public int marginBottom;
        public int marginLeft;
        public int marginRight;
    }

    public static void ensureDir() {
        if (!CONTEXT_DIR.exists()) {
            if (!CONTEXT_DIR.mkdirs()) {
                LOGGER.warn("Failed to create HUD context directory: {}", CONTEXT_DIR.getAbsolutePath());
            }
        }
    }

    public static void savePosition(String contextName, HudPosition position,
                                    int marginTop, int marginBottom, int marginLeft, int marginRight) {
        File file = findFileByContextName(contextName);
        if (file == null) {
            LOGGER.warn("savePosition: Context not found: {}", contextName);
            return;
        }
        try (CommentedFileConfig config = CommentedFileConfig.builder(file)
                .sync()
                .autosave()
                .writingMode(WritingMode.REPLACE)
                .build()) {
            config.load();
            config.set("position", position.name().toLowerCase(Locale.ROOT));
            config.set("margin_top", marginTop);
            config.set("margin_bottom", marginBottom);
            config.set("margin_left", marginLeft);
            config.set("margin_right", marginRight);
            config.save();
            HudContextManager.loadContexts();
            LOGGER.info("Saved position for '{}': {} (margins T:{} B:{} L:{} R:{})",
                    contextName, position, marginTop, marginBottom, marginLeft, marginRight);
        } catch (Exception e) {
            LOGGER.error("Failed saving HUD position for {}", contextName, e);
        }
    }

    public static void saveLine(String contextName, int lineIndex, String newLine) {
        File file = findFileByContextName(contextName);
        if (file == null) {
            LOGGER.warn("saveLine: Context not found: {}", contextName);
            return;
        }
        try (CommentedFileConfig config = CommentedFileConfig.builder(file)
                .sync()
                .autosave()
                .writingMode(WritingMode.REPLACE)
                .build()) {
            config.load();
            String textRaw = config.getOrElse("text", "");
            List<String> lines = textRaw.strip().lines().toList();
            if (lineIndex < 0 || lineIndex >= lines.size()) {
                LOGGER.warn("saveLine: line index out of bounds: {} for '{}'", lineIndex, contextName);
                return;
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < lines.size(); i++) {
                if (i > 0) sb.append('\n');
                if (i == lineIndex) sb.append(newLine);
                else sb.append(lines.get(i));
            }
            config.set("text", sb.toString());
            config.save();
            HudContextManager.loadContexts();
            LOGGER.info("Saved text line {} for '{}'", lineIndex, contextName);
        } catch (Exception e) {
            LOGGER.error("Failed saving HUD text for {}", contextName, e);
        }
    }

    public static void addLine(String contextName, String newLine) {
        insertLine(contextName, Integer.MAX_VALUE, newLine);
    }

    public static void insertLine(String contextName, int index, String newLine) {
        File file = findFileByContextName(contextName);
        if (file == null) {
            LOGGER.warn("insertLine: Context not found: {}", contextName);
            return;
        }
        try (CommentedFileConfig config = CommentedFileConfig.builder(file)
                .sync()
                .autosave()
                .writingMode(WritingMode.REPLACE)
                .build()) {
            config.load();
            String textRaw = config.getOrElse("text", "");
            List<String> lines = textRaw.isBlank() ? java.util.Collections.emptyList() : textRaw.strip().lines().toList();
            java.util.ArrayList<String> out = new java.util.ArrayList<>(lines);
            int pos = Math.max(0, Math.min(index, out.size()));
            out.add(pos, newLine);
            String joined = String.join("\n", out);
            config.set("text", joined);
            config.save();
            HudContextManager.loadContexts();
            LOGGER.info("Inserted line at {} for '{}'", pos, contextName);
        } catch (Exception e) {
            LOGGER.error("Failed inserting HUD text for {}", contextName, e);
        }
    }

    public static void deleteLine(String contextName, int lineIndex) {
        File file = findFileByContextName(contextName);
        if (file == null) {
            LOGGER.warn("deleteLine: Context not found: {}", contextName);
            return;
        }
        try (CommentedFileConfig config = CommentedFileConfig.builder(file)
                .sync()
                .autosave()
                .writingMode(WritingMode.REPLACE)
                .build()) {
            config.load();
            String textRaw = config.getOrElse("text", "");
            List<String> lines = textRaw.strip().lines().toList();
            if (lineIndex < 0 || lineIndex >= lines.size()) {
                LOGGER.warn("deleteLine: line index out of bounds: {} for '{}'", lineIndex, contextName);
                return;
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < lines.size(); i++) {
                if (i == lineIndex) continue;
                if (!sb.isEmpty()) sb.append('\n');
                sb.append(lines.get(i));
            }
            config.set("text", sb.toString());
            config.save();
            HudContextManager.loadContexts();
            LOGGER.info("Deleted text line {} for '{}'", lineIndex, contextName);
        } catch (Exception e) {
            LOGGER.error("Failed deleting HUD text for {}", contextName, e);
        }
    }

    public static String createNewContext(String desiredName) {
        ensureDir();
        String safe = desiredName == null || desiredName.isBlank() ? "new_context" : desiredName.trim();
        safe = safe.replaceAll("[^a-zA-Z0-9_-]", "_");
        // Find unique filename
        String base = safe;
        int counter = 0;
        File file;
        do {
            String fname = counter == 0 ? base + ".toml" : base + "_" + counter + ".toml";
            file = new File(CONTEXT_DIR, fname);
            counter++;
        } while (file.exists());

        try (CommentedFileConfig config = CommentedFileConfig.builder(file)
                .sync()
                .autosave()
                .writingMode(WritingMode.REPLACE)
                .build()) {
            config.load();
            config.set("name", safe);
            config.set("position", HudPosition.TOP_RIGHT.name().toLowerCase(Locale.ROOT));
            config.set("color", 0xFFFFFF);
            config.set("align", HudContext.Align.LEFT.name().toLowerCase(Locale.ROOT));
            config.set("scale", 1.0);
            config.set("shadow", true);
            config.set("background", false);
            config.set("background_rgb", 0x000000);
            config.set("background_alpha", 0.33);
            config.set("background_per_line", false);
            config.set("padding", 4);
            config.set("line_spacing", 0);
            config.set("margin", 0);
            config.set("text", "New HUD context");
            config.save();
        } catch (Exception e) {
            LOGGER.error("Failed to create new context file", e);
            return null;
        }
        HudContextManager.loadContexts();
        LOGGER.info("Created new HUD context: {}", safe);
        return safe;
    }

    public static void saveContextSettings(String contextName, Settings s) {
        File file = findFileByContextName(contextName);
        if (file == null) {
            LOGGER.warn("saveContextSettings: Context not found: {}", contextName);
            return;
        }
        try (CommentedFileConfig config = CommentedFileConfig.builder(file)
                .sync()
                .autosave()
                .writingMode(WritingMode.REPLACE)
                .build()) {
            config.load();
            // Basic appearance
            config.set("color", s.color);
            config.set("shadow", s.shadow);
            config.set("scale", (double)s.scale);
            config.set("align", s.align.name().toLowerCase(java.util.Locale.ROOT));
            // Background
            config.set("background", s.background);
            config.set("background_rgb", s.backgroundRgb);
            config.set("background_alpha", s.backgroundAlpha);
            config.set("background_per_line", s.backgroundPerLine);
            // Padding
            config.set("padding_top", s.paddingTop);
            config.set("padding_bottom", s.paddingBottom);
            config.set("padding_left", s.paddingLeft);
            config.set("padding_right", s.paddingRight);
            // Line spacing
            config.set("line_spacing", s.lineSpacing);
            config.set("line_spacing_padding_top", s.lineSpacingPaddingTop);
            config.set("line_spacing_padding_bottom", s.lineSpacingPaddingBottom);
            // Margins
            config.set("margin_top", s.marginTop);
            config.set("margin_bottom", s.marginBottom);
            config.set("margin_left", s.marginLeft);
            config.set("margin_right", s.marginRight);

            config.save();
            HudContextManager.loadContexts();
            LOGGER.info("Saved context settings for '{}'.", contextName);
        } catch (Exception e) {
            LOGGER.error("Failed saving HUD settings for {}", contextName, e);
        }
    }

    private static File findFileByContextName(String name) {
        try {
            if (!CONTEXT_DIR.exists()) return null;
            Path dir = CONTEXT_DIR.toPath();
            try (var paths = Files.list(dir)) {
                Optional<File> match = paths
                        .filter(p -> p.toString().endsWith(".toml"))
                        .map(Path::toFile)
                        .filter(f -> {
                            String fileName = f.getName();
                            String base = fileName.endsWith(".toml") ? fileName.substring(0, fileName.length() - 5) : fileName;
                            if (base.equals(name)) return true;
                            try (CommentedFileConfig cfg = CommentedFileConfig.builder(f)
                                    .sync()
                                    .autosave()
                                    .writingMode(WritingMode.REPLACE)
                                    .build()) {
                                cfg.load();
                                String n = cfg.getOrElse("name", base);
                                return name.equals(n);
                            } catch (Exception e) {
                                return false;
                            }
                        })
                        .findFirst();
                return match.orElse(null);
            }
        } catch (Exception e) {
            LOGGER.error("findFileByContextName failed for '{}'", name, e);
            return null;
        }
    }
}
