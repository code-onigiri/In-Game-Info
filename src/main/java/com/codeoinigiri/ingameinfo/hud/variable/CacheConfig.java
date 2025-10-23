package com.codeoinigiri.ingameinfo.hud.variable;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🔧 cache_ttl.toml 管理クラス（リファクタリング版）
 * - スレッドセーフな TTL マップ
 * - デフォルト設定を定数化
 * - WatchService の安全な再起動 / 停止
 * - 冗長なコードの整理
 */
public class CacheConfig {
    private static final Map<String, Long> TTL_MAP = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, Long>> DEFAULT_SECTION_ENTRIES;
    private static final Map<String, Long> DEFAULT_CATEGORY_TTLS;

    private static File CONFIG_FILE;
    private static WatchService watcher;
    private static Thread watchThread;

    static {
        // カテゴリのデフォルト TTL
        Map<String, Long> cat = new HashMap<>();
        cat.put("player", 200L);
        cat.put("world", 1000L);
        cat.put("environment", 500L);
        cat.put("system", 1000L);
        DEFAULT_CATEGORY_TTLS = Collections.unmodifiableMap(cat);

        // セクションごとのデフォルトキー
        Map<String, Map<String, Long>> defs = new HashMap<>();

        Map<String, Long> player = Map.ofEntries(
                Map.entry("health", 200L),
                Map.entry("max_health", 200L),
                Map.entry("food", 300L),
                Map.entry("saturation", 500L),
                Map.entry("posX", 200L),
                Map.entry("posY", 200L),
                Map.entry("posZ", 200L),
                Map.entry("yaw", 500L),
                Map.entry("pitch", 500L),
                Map.entry("item.mainhand", 800L),
                Map.entry("item.offhand", 800L)
        );

        Map<String, Long> world = Map.ofEntries(
                Map.entry("time", 500L),
                Map.entry("day", 1000L),
                Map.entry("weather", 2000L),
                Map.entry("biome", 1500L),
                Map.entry("is_day", 1000L),
                Map.entry("dimension", 2000L)
        );

        Map<String, Long> env = Map.ofEntries(
                Map.entry("temperature", 800L),
                Map.entry("light", 500L),
                Map.entry("altitude", 1000L)
        );

        Map<String, Long> system = Map.ofEntries(
                Map.entry("fps", 1000L),
                Map.entry("language", 2000L)
        );

        defs.put("player", player);
        defs.put("world", world);
        defs.put("environment", env);
        defs.put("system", system);

        DEFAULT_SECTION_ENTRIES = Collections.unmodifiableMap(defs);
    }

    // ===============================
    // 🚀 初期ロード
    // ===============================
    public static void load(File configDir) {
        File ingameInfoDir = new File(configDir, "ingameinfo");
        if (!ingameInfoDir.exists()) {
            boolean ok = ingameInfoDir.mkdirs();
            if (!ok) {
                System.out.println("[IngameInfo] Warning: Failed to create directory: " + ingameInfoDir.getAbsolutePath());
            }
        }

        CONFIG_FILE = new File(ingameInfoDir, "cache_ttl.toml");

        if (!CONFIG_FILE.exists()) {
            createDefaultConfig(CONFIG_FILE);
        }

        reload();
        startWatcher();
    }

    // ===============================
    // 🔁 再読込
    // ===============================
    public static synchronized void reload() {
        TTL_MAP.clear();
        try (CommentedFileConfig cfg = CommentedFileConfig.builder(CONFIG_FILE)
                .autosave()
                .build()) {

            cfg.load();

            // --- グローバルカテゴリ TTL ---
            for (var entry : DEFAULT_CATEGORY_TTLS.entrySet()) {
                String key = entry.getKey() + "_ttl_ms";
                Object v = cfg.get(key);
                long ttl = (v instanceof Number) ? ((Number) v).longValue() : entry.getValue();
                TTL_MAP.put(entry.getKey(), ttl);
            }

            // --- 個別キー読み込み ---
            cfg.valueMap().forEach((k, v) -> {
                if (v instanceof Map<?, ?> section) {
                    section.forEach((subK, subV) -> {
                        if (subV instanceof Number num) {
                            TTL_MAP.put(k + "." + subK.toString(), num.longValue());
                        }
                    });
                }
            });

            // --- 欠損キーを自動追加 ---
            addMissingDefaults(cfg);
            cfg.save();

            System.out.println("[IngameInfo] TTL Config loaded: " + TTL_MAP.size() + " entries");
        } catch (Exception e) {
            System.out.println("[IngameInfo] Error loading TTL config: " + e);
        }
    }

    // ===============================
    // 📜 デフォルト生成
    // ===============================
    private static void createDefaultConfig(File file) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("""
                # IngameInfo cache TTL configuration (milliseconds)
                # 各カテゴリや個別キーのキャッシュ更新間隔を設定できます
                # 低い値ほど頻繁に更新されます（単位: ミリ秒）

                [player]
                health = 200
                max_health = 200
                food = 300
                saturation = 500
                posX = 200
                posY = 200
                posZ = 200
                yaw = 500
                pitch = 500
                item.mainhand = 800
                item.offhand = 800

                [world]
                time = 500
                day = 1000
                weather = 2000
                biome = 1500
                is_day = 1000
                dimension = 2000

                [environment]
                temperature = 800
                light = 500
                altitude = 1000

                [system]
                fps = 1000
                language = 2000
                """);
            System.out.println("[IngameInfo] Created default cache_ttl.toml");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ===============================
            System.out.println("[IngameInfo] Failed to create default cache_ttl.toml: " + e);
    // ===============================
    private static void addMissingDefaults(CommentedFileConfig cfg) {
        // デフォルトカテゴリキー
        Map<String, Map<String, Long>> defaults = new HashMap<>();

        Map<String, Long> player = Map.ofEntries(
                Map.entry("health", 200L),
        // デフォルトカテゴリキーの追加
        for (var catEntry : DEFAULT_SECTION_ENTRIES.entrySet()) {
            String section = catEntry.getKey();
            for (var entry : catEntry.getValue().entrySet()) {
        }
    }

    // ===============================
    // 👀 WatchService 自動リロード
    // ===============================

        // カテゴリTTLがなければ追加
        for (var cat : DEFAULT_CATEGORY_TTLS.entrySet()) {
            String cfgKey = cat.getKey() + "_ttl_ms";
            if (!cfg.contains(cfgKey)) cfg.set(cfgKey, cat.getValue());
        }
    public static void startWatcher() {

        if (watchThread != null && watchThread.isAlive()) return;

        try {
    public static synchronized void startWatcher() {
            Path dir = CONFIG_FILE.getParentFile().toPath();

        // 既存の watcher を安全に停止
        stopWatcher();

            watchThread = new Thread(() -> {
                while (true) {
                    try {
            dir.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);
                        for (WatchEvent<?> event : key.pollEvents()) {
                            Path changed = (Path) event.context();
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                                reload();
                            }
                            WatchEvent.Kind<?> kind = event.kind();
                            if (kind == StandardWatchEventKinds.OVERFLOW) continue;

                        }
                            if (changed != null && changed.getFileName().toString().equals(CONFIG_FILE.getName())) {
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, "IngameInfo-ConfigWatcher");
                    }
                } catch (InterruptedException e) {
                    // 期待される停止経路
                } catch (ClosedWatchServiceException cwse) {
                    // 正常にクローズされた
                } catch (Exception e) {
                    System.out.println("[IngameInfo] Config watcher error: " + e);

            System.out.println("[IngameInfo] Config watcher started.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ===============================
            System.out.println("[IngameInfo] Failed to start config watcher: " + e);
        }
    }

    public static synchronized void stopWatcher() {
        if (watchThread != null && watchThread.isAlive()) {
            watchThread.interrupt();
            watchThread = null;
        }
        if (watcher != null) {
            try {
                watcher.close();
            } catch (IOException ignored) {
            }
            watcher = null;
    // ===============================
    public static long getTTL(String key, String category, long defaultMs) {
        if (TTL_MAP.containsKey(key)) return TTL_MAP.get(key);
        if (TTL_MAP.containsKey(category)) return TTL_MAP.get(category);
        return defaultMs;
    }
}
        Long v = TTL_MAP.get(key);
        if (v != null) return v;
        v = TTL_MAP.get(category);
        if (v != null) return v;
