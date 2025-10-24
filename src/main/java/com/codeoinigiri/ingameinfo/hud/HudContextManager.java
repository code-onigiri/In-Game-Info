package com.codeoinigiri.ingameinfo.hud;

import com.codeoinigiri.ingameinfo.config.HudPosition;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.codeoinigiri.ingameinfo.InGameInfo.LOGGER;

public class HudContextManager {
    private static final List<HudContext> contexts = Collections.synchronizedList(new ArrayList<>());
    private static final File CONTEXT_DIR = new File("config/ingameinfo/context");

    public static List<HudContext> getContexts() {
        return new ArrayList<>(contexts);
    }

    public static synchronized void loadContexts() {
        contexts.clear();
        if (!CONTEXT_DIR.exists()) CONTEXT_DIR.mkdirs();

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

                            String name = config.getOrElse("name", path.getFileName().toString().replace(".toml", ""));
                            String posStr = config.getOrElse("position", "top_right").toUpperCase();
                            HudPosition position = HudPosition.TOP_RIGHT;
                            try { position = HudPosition.valueOf(posStr); } catch (Exception ignored) {}

                            int color = config.getOrElse("color", 0xFFFFFF);
                            String textRaw = config.getOrElse("text", "");
                            List<String> lines = textRaw.strip().lines().toList();

                            // 拡張設定
                            String alignStr = config.getOrElse("align", "left").toUpperCase();
                            HudContext.Align align;
                            try { align = HudContext.Align.valueOf(alignStr); } catch (Exception e) { align = HudContext.Align.LEFT; }

                            float scale = config.getOrElse("scale", 1.0).floatValue();
                            boolean shadow = config.getOrElse("shadow", true);

                            boolean background = config.getOrElse("background", false);
                            int backgroundPadding = config.getOrElse("background_padding", 4);

                            int backgroundRgb;
                            double backgroundAlpha;

                            // New separate RGB and Alpha
                            Object rgbValue = config.get("background_rgb");
                            Object alphaValue = config.get("background_alpha");
                            LOGGER.info("[Debug] background_rgb raw value: {} (Type: {})", rgbValue, rgbValue != null ? rgbValue.getClass().getName() : "null");
                            LOGGER.info("[Debug] background_alpha raw value: {} (Type: {})", alphaValue, alphaValue != null ? alphaValue.getClass().getName() : "null");

                            // Robustly get RGB value
                            Object rgbObj = config.get("background_rgb");
                            if (rgbObj instanceof Number) {
                                backgroundRgb = ((Number) rgbObj).intValue();
                            } else {
                                backgroundRgb = 0x000000; // default
                            }

                            // Robustly get alpha value
                            Object alphaObj = config.get("background_alpha");
                            if (alphaObj instanceof Number) {
                                backgroundAlpha = ((Number) alphaObj).doubleValue();
                            } else {
                                backgroundAlpha = 0.33; // default
                            }

                            LOGGER.info("[Debug] Using RGB: {}, Alpha: {}", backgroundRgb, backgroundAlpha);

                            contexts.add(new HudContext(name, position, color, lines, align, scale, shadow, background, backgroundRgb, backgroundAlpha, backgroundPadding));

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}