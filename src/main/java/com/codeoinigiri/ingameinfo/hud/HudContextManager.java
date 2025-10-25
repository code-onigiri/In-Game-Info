package com.codeoinigiri.ingameinfo.hud;

import com.codeoinigiri.ingameinfo.config.HudPosition;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class HudContextManager {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final List<HudContext> contexts = Collections.synchronizedList(new ArrayList<>());
    private static final File CONTEXT_DIR = new File("config/ingameinfo/context");

    public static List<HudContext> getContexts() {
        return new ArrayList<>(contexts);
    }

    public static synchronized void loadContexts() {
        contexts.clear();
        if (!CONTEXT_DIR.exists()) {
            CONTEXT_DIR.mkdirs();
        }

        try {
            Files.list(CONTEXT_DIR.toPath())
                .filter(p -> p.toString().endsWith(".toml"))
                .forEach(path -> {
                    try (CommentedFileConfig config = CommentedFileConfig.builder(path)
                            .sync()
                            .autosave()
                            .writingMode(WritingMode.REPLACE)
                            .build()) {

                        config.load();
                        LOGGER.info("[Debug] Loading context from: {}", path.getFileName());

                        // General settings
                        String name = config.getOrElse("name", path.getFileName().toString().replace(".toml", ""));
                        String posStr = config.getOrElse("position", "top_right").toUpperCase();
                        HudPosition position = HudPosition.TOP_RIGHT;
                        try { position = HudPosition.valueOf(posStr); } catch (Exception ignored) {}

                        // Text settings
                        int color = config.getOrElse("color", 0xFFFFFF);
                        String textRaw = config.getOrElse("text", "");
                        List<String> lines = textRaw.strip().lines().toList();

                        // Style settings
                        String alignStr = config.getOrElse("align", "left").toUpperCase();
                        HudContext.Align align = HudContext.Align.LEFT;
                        try { align = HudContext.Align.valueOf(alignStr); } catch (Exception ignored) {}
                        float scale = config.getOrElse("scale", 1.0).floatValue();
                        boolean shadow = config.getOrElse("shadow", true);

                        // Background settings
                        boolean background = config.getOrElse("background", false);
                        int backgroundPadding = config.getOrElse("background_padding", 4);

                        // --- Background Color Logic (using Optional) ---
                        Optional<Object> rgbOptional = config.getOptional("background_rgb");
                        LOGGER.info("[Debug] background_rgb raw value: {} (Type: {})",
                            rgbOptional.map(String::valueOf).orElse("not present"),
                            rgbOptional.map(o -> o.getClass().getName()).orElse("N/A"));

                        int backgroundRgb = rgbOptional
                            .filter(Number.class::isInstance)
                            .map(Number.class::cast)
                            .map(Number::intValue)
                            .orElse(0x000000);

                        Optional<Object> alphaOptional = config.getOptional("background_alpha");
                        LOGGER.info("[Debug] background_alpha raw value: {} (Type: {})",
                            alphaOptional.map(String::valueOf).orElse("not present"),
                            alphaOptional.map(o -> o.getClass().getName()).orElse("N/A"));

                        double backgroundAlpha = alphaOptional
                            .filter(Number.class::isInstance)
                            .map(Number.class::cast)
                            .map(Number::doubleValue)
                            .orElse(0.33);
                        // --- End of Background Color Logic ---

                        LOGGER.info("[Debug] Using RGB: {}, Alpha: {}", backgroundRgb, backgroundAlpha);

                        // Create and add context
                        contexts.add(new HudContext(name, position, color, lines, align, scale, shadow, background, backgroundRgb, backgroundAlpha, backgroundPadding));

                    } catch (Exception e) {
                        LOGGER.error("Failed to load HUD context from: " + path, e);
                    }
                });
        } catch (Exception e) {
            LOGGER.error("Failed to list HUD context files in: " + CONTEXT_DIR.getAbsolutePath(), e);
        }
    }
}