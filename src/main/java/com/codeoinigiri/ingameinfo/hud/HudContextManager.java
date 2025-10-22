package com.codeoinigiri.ingameinfo.hud;

import com.codeoinigiri.ingameinfo.config.HudPosition;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
                            int backgroundColor = config.getOrElse("background_color", 0x55000000);
                            int backgroundPadding = config.getOrElse("background_padding", 4);

                            contexts.add(new HudContext(name, position, color, lines, align, scale, shadow, background, backgroundColor, backgroundPadding));

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}