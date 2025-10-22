package com.codeoinigiri.ingameinfo.hud;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

public class ConfigWatcher {
    private static Thread watcherThread;

    public static void startWatching() {
        File contextDir = new File("config/ingameinfo/context");
        if (!contextDir.exists()) contextDir.mkdirs();

        Path path = contextDir.toPath();

        watcherThread = new Thread(() -> {
            try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
                path.register(
                        watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE,
                        StandardWatchEventKinds.ENTRY_MODIFY
                );

                System.out.println("[InGameInfo] ファイル監視を開始しました: " + path);

                while (true) {
                    WatchKey key = watchService.take(); // ブロッキング待機
                    boolean shouldReload = false;

                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        Path changed = (Path) event.context();

                        // .tomlファイルのみ監視
                        if (changed.toString().endsWith(".toml")) {
                            shouldReload = true;
                            System.out.println("[InGameInfo] 検出: " + kind.name() + " -> " + changed);
                        }
                    }

                    if (shouldReload) {
                        HudContextManager.loadContexts();
                        System.out.println("[InGameInfo] コンテキストを再読み込みしました。");
                    }

                    key.reset();
                }

            } catch (InterruptedException e) {
                System.out.println("[InGameInfo] ファイル監視スレッドが終了しました。");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        watcherThread.setDaemon(true); // ゲーム終了時に自動停止
        watcherThread.start();
    }

    public static void stopWatching() {
        if (watcherThread != null && watcherThread.isAlive()) {
            watcherThread.interrupt();
        }
    }
}