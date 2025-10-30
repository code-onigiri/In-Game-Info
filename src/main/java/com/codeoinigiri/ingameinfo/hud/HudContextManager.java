package com.codeoinigiri.ingameinfo.hud;

import com.codeoinigiri.ingameinfo.hud.config.HudContextBuilder;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * HUDコンテキストの管理クラス
 * Single Responsibility Principle: コンテキストの読み込みと管理のみに責任を持つ
 * Open/Closed Principle: 新しい設定形式への対応が容易
 */
public class HudContextManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final List<HudContext> contexts = Collections.synchronizedList(new ArrayList<>());
    private static final File CONTEXT_DIR = new File("config/ingameinfo/context");

    /**
     * すべてのHUDコンテキストを取得
     */
    public static List<HudContext> getContexts() {
        return new ArrayList<>(contexts);
    }

    /**
     * HUDコンテキストをファイルから読み込む
     */
    public static synchronized void loadContexts() {
        contexts.clear();
        ensureDirectoryExists();

        try (Stream<Path> paths = Files.list(CONTEXT_DIR.toPath())) {
            paths.filter(HudContextManager::isTomlFile)
                 .forEach(HudContextManager::loadContextFromFile);

            LOGGER.info("Loaded {} HUD contexts", contexts.size());
        } catch (Exception e) {
            LOGGER.error("Failed to list HUD context files in: {}", CONTEXT_DIR.getAbsolutePath(), e);
        }
    }

    /**
     * ディレクトリが存在することを確認
     */
    private static void ensureDirectoryExists() {
        if (!CONTEXT_DIR.exists() && !CONTEXT_DIR.mkdirs()) {
            LOGGER.warn("Failed to create HUD context directory: {}", CONTEXT_DIR.getAbsolutePath());
        }
    }

    /**
     * TOMLファイルかどうかを判定
     */
    private static boolean isTomlFile(Path path) {
        return path.toString().endsWith(".toml");
    }

    /**
     * ファイルからコンテキストを読み込む
     */
    private static void loadContextFromFile(Path path) {
        try (CommentedFileConfig config = createConfig(path)) {
            config.load();
            String fileName = path.getFileName().toString();
            LOGGER.debug("Loading context from: {}", fileName);

            HudContextBuilder.buildFromConfig(config, fileName)
                .ifPresentOrElse(
                    contexts::add,
                    () -> LOGGER.warn("Failed to build context from: {}", fileName)
                );
        } catch (Exception e) {
            LOGGER.error("Failed to load HUD context from: {}", path, e);
        }
    }

    /**
     * 設定ファイルを作成
     */
    private static CommentedFileConfig createConfig(Path path) {
        return CommentedFileConfig.builder(path)
            .sync()
            .autosave()
            .writingMode(WritingMode.REPLACE)
            .build();
    }

    /**
     * 特定の名前のコンテキストを取得
     */
    public static HudContext getContext(String name) {
        return contexts.stream()
            .filter(ctx -> ctx.name().equals(name))
            .findFirst()
            .orElse(null);
    }

    /**
     * コンテキストが存在するかを確認
     */
    public static boolean hasContext(String name) {
        return contexts.stream().anyMatch(ctx -> ctx.name().equals(name));
    }

    /**
     * コンテキストの数を取得
     */
    public static int getContextCount() {
        return contexts.size();
    }
}