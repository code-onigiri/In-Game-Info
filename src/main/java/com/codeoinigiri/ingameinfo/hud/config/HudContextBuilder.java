package com.codeoinigiri.ingameinfo.hud.config;

import com.codeoinigiri.ingameinfo.config.HudPosition;
import com.codeoinigiri.ingameinfo.hud.HudContext;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;

/**
 * TOML設定ファイルからHudContextを構築するクラス
 * Single Responsibility Principle: 設定の読み込みと解析のみに責任を持つ
 */
public class HudContextBuilder {
    private static final Logger LOGGER = LogUtils.getLogger();

    // デフォルト値の定数化
    private static final int DEFAULT_COLOR = 0xFFFFFF;
    private static final int DEFAULT_PADDING = 4;
    private static final int DEFAULT_LINE_SPACING = 0;
    private static final int DEFAULT_MARGIN = 0;
    private static final int DEFAULT_BACKGROUND_RGB = 0x000000;
    private static final double DEFAULT_BACKGROUND_ALPHA = 0.33;
    private static final float DEFAULT_SCALE = 1.0f;
    private static final boolean DEFAULT_SHADOW = true;
    private static final boolean DEFAULT_BACKGROUND = false;
    private static final boolean DEFAULT_BACKGROUND_PER_LINE = false;

    /**
     * 設定ファイルからHudContextを構築
     */
    public static Optional<HudContext> buildFromConfig(CommentedFileConfig config, String fileName) {
        try {
            String name = config.getOrElse("name", fileName.replace(".toml", ""));
            HudPosition position = parsePosition(config.getOrElse("position", "top_right"));
            
            // テキスト設定
            int color = config.getOrElse("color", DEFAULT_COLOR);
            String textRaw = config.getOrElse("text", "");
            List<String> lines = textRaw.strip().lines().toList();

            // スタイル設定
            HudContext.Align align = parseAlign(config.getOrElse("align", "left"));
            float scale = ((Number) config.getOrElse("scale", DEFAULT_SCALE)).floatValue();
            boolean shadow = config.getOrElse("shadow", DEFAULT_SHADOW);

            // 背景設定
            boolean background = config.getOrElse("background", DEFAULT_BACKGROUND);
            boolean backgroundPerLine = config.getOrElse("background_per_line", DEFAULT_BACKGROUND_PER_LINE);
            
            // パディング設定
            PaddingConfig padding = parsePadding(config);
            
            // 行間設定
            LineSpacingConfig lineSpacing = parseLineSpacing(config);
            
            // マージン設定
            MarginConfig margin = parseMargin(config);
            
            // 背景色設定
            BackgroundColorConfig bgColor = parseBackgroundColor(config);

            LOGGER.debug("Built HudContext: {} (position={}, lines={})", name, position, lines.size());

            return Optional.of(new HudContext(
                name, position, color, lines, align, scale, shadow,
                background, bgColor.rgb, bgColor.alpha, backgroundPerLine,
                padding.top, padding.bottom, padding.left, padding.right,
                lineSpacing.spacing, lineSpacing.paddingTop, lineSpacing.paddingBottom,
                margin.top, margin.bottom, margin.left, margin.right
            ));
        } catch (Exception e) {
            LOGGER.error("Failed to build HudContext from config: {}", fileName, e);
            return Optional.empty();
        }
    }

    /**
     * 位置設定のパース
     */
    private static HudPosition parsePosition(String posStr) {
        try {
            return HudPosition.valueOf(posStr.toUpperCase());
        } catch (Exception e) {
            LOGGER.warn("Invalid position '{}', using TOP_RIGHT", posStr);
            return HudPosition.TOP_RIGHT;
        }
    }

    /**
     * アライメント設定のパース
     */
    private static HudContext.Align parseAlign(String alignStr) {
        try {
            return HudContext.Align.valueOf(alignStr.toUpperCase());
        } catch (Exception e) {
            LOGGER.warn("Invalid align '{}', using LEFT", alignStr);
            return HudContext.Align.LEFT;
        }
    }

    /**
     * パディング設定のパース
     */
    private static PaddingConfig parsePadding(CommentedFileConfig config) {
        int defaultPadding = config.getOrElse("padding",
            config.getOrElse("background_padding",
            config.getOrElse("backgroundPadding", DEFAULT_PADDING)));

        int top = config.getOrElse("padding_top",
            config.getOrElse("padding_y", defaultPadding));
        int bottom = config.getOrElse("padding_bottom",
            config.getOrElse("padding_y", defaultPadding));
        int left = config.getOrElse("padding_left",
            config.getOrElse("padding_x", defaultPadding));
        int right = config.getOrElse("padding_right",
            config.getOrElse("padding_x", defaultPadding));

        return new PaddingConfig(top, bottom, left, right);
    }

    /**
     * 行間設定のパース
     */
    private static LineSpacingConfig parseLineSpacing(CommentedFileConfig config) {
        int spacing = config.getOrElse("line_spacing", DEFAULT_LINE_SPACING);
        int defaultPadding = config.getOrElse("line_spacing_padding", DEFAULT_LINE_SPACING);
        
        int paddingTop = config.getOrElse("line_spacing_padding_top",
            config.getOrElse("line_spacing_padding_y", defaultPadding));
        int paddingBottom = config.getOrElse("line_spacing_padding_bottom",
            config.getOrElse("line_spacing_padding_y", defaultPadding));

        return new LineSpacingConfig(spacing, paddingTop, paddingBottom);
    }

    /**
     * マージン設定のパース
     */
    private static MarginConfig parseMargin(CommentedFileConfig config) {
        int defaultMargin = config.getOrElse("margin", DEFAULT_MARGIN);
        
        int top = config.getOrElse("margin_top",
            config.getOrElse("margin_y", defaultMargin));
        int bottom = config.getOrElse("margin_bottom",
            config.getOrElse("margin_y", defaultMargin));
        int left = config.getOrElse("margin_left",
            config.getOrElse("margin_x", defaultMargin));
        int right = config.getOrElse("margin_right",
            config.getOrElse("margin_x", defaultMargin));

        return new MarginConfig(top, bottom, left, right);
    }

    /**
     * 背景色設定のパース
     */
    private static BackgroundColorConfig parseBackgroundColor(CommentedFileConfig config) {
        Optional<Object> rgbOptional = config.getOptional("background_rgb");
        int rgb = rgbOptional
            .filter(Number.class::isInstance)
            .map(Number.class::cast)
            .map(Number::intValue)
            .orElse(DEFAULT_BACKGROUND_RGB);

        Optional<Object> alphaOptional = config.getOptional("background_alpha");
        double alpha = alphaOptional
            .filter(Number.class::isInstance)
            .map(Number.class::cast)
            .map(Number::doubleValue)
            .orElse(DEFAULT_BACKGROUND_ALPHA);

        LOGGER.debug("Parsed background color: RGB={}, Alpha={}", rgb, alpha);
        return new BackgroundColorConfig(rgb, alpha);
    }

    // 内部データクラス
    private record PaddingConfig(int top, int bottom, int left, int right) {}
    private record LineSpacingConfig(int spacing, int paddingTop, int paddingBottom) {}
    private record MarginConfig(int top, int bottom, int left, int right) {}
    private record BackgroundColorConfig(int rgb, double alpha) {}
}

