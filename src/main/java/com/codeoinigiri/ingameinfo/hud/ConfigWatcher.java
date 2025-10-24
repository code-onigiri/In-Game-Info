package com.codeoinigiri.ingameinfo.hud;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

/**
 * 🔍 Config監視クラス（リファクタリング版）
 * - config/ingameinfo/context/*.toml のファイル変更を監視
 * - 変更検出時に HudContextManager.loadContexts() を呼び出し
 * - スレッドセーフな停止処理
 */
public class ConfigWatcher {
    private static final File CONTEXT_DIR = new File("config/ingameinfo/context");
    private static Thread watcherThread;
    private static volatile boolean running = false;

    // ===============================
    // 🚀 監視開始
    // ===============================
    public static synchronized void startWatching() {
        // 既に実行中なら何もしない
        if (watcherThread != null && watcherThread.isAlive()) {
            return;
        }

        // ディレクトリを作成
        if (!CONTEXT_DIR.exists()) {
            boolean ok = CONTEXT_DIR.mkdirs();
            if (!ok) {
                System.out.println("[IngameInfo] Warning: Failed to create context directory: " + CONTEXT_DIR.getAbsolutePath());
            }
        }

        running = true;
        Path path = CONTEXT_DIR.toPath();

        watcherThread = new Thread(() -> {
            try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
                path.register(
                        watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE,
                        StandardWatchEventKinds.ENTRY_MODIFY
                );

                System.out.println("[IngameInfo] Config watcher started: " + path);

                // ===============================
                // 📡 イベントループ
                // ===============================
                while (running) {
                    WatchKey key = watchService.take(); // ブロッキング待機
                    boolean shouldReload = false;

                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        Path changed = (Path) event.context();

                        // .tomlファイルのみ監視
                        if (changed != null && changed.toString().endsWith(".toml")) {
                            shouldReload = true;
                            System.out.println("[IngameInfo] Detected: " + kind.name() + " -> " + changed);
                        }
                    }

                    if (shouldReload) {
                        try {
                            HudContextManager.loadContexts();
                            System.out.println("[IngameInfo] HUD contexts reloaded.");
                        } catch (Exception e) {
                            System.out.println("[IngameInfo] Error reloading contexts: " + e);
                        }
                    }

                    key.reset();
                }

            } catch (InterruptedException e) {
                System.out.println("[IngameInfo] Config watcher thread interrupted.");
            } catch (ClosedWatchServiceException cwse) {
                System.out.println("[IngameInfo] Config watcher service closed.");
            } catch (IOException e) {
                System.out.println("[IngameInfo] Config watcher I/O error: " + e);
            } finally {
                running = false;
            }
        }, "IngameInfo-ConfigWatcher");

        watcherThread.setDaemon(true); // ゲーム終了時に自動停止
        watcherThread.start();
    }

    // ===============================
    // ⏹ 監視停止
    // ===============================
    public static synchronized void stopWatching() {
        running = false;
        if (watcherThread != null && watcherThread.isAlive()) {
            watcherThread.interrupt();
            try {
                watcherThread.join(2000); // 最大2秒待機
            } catch (InterruptedException e) {
                System.out.println("[IngameInfo] Config watcher stop interrupted.");
            }
            watcherThread = null;
        }
    }
}