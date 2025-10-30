package com.codeoinigiri.ingameinfo.api;

/**
 * 📚 VariableAPI 使用例集
 * 
 * IngameInfo Variable API の具体的な使用パターンを示します。
 * このクラスはドキュメント用です。実装は VariableAPI を使用してください。
 */
public class VariableAPIExamples {

    // ===============================
    // 📖 基本的な使用例
    // ===============================

    /**
     * 例1: 静的な定数値を登録
     * HUD内で ${custom.message} として使用可能
     */
    public static void example_1_staticVariable() {
        VariableAPI.register("custom.message", "Hello, World!");
        VariableAPI.register("custom.version", "1.0.0");
        VariableAPI.register("custom.author", "code-onigiri");
    }

    /**
     * 例2: 動的な値（毎回最新値を生成）を登録
     * 呼び出すたびに現在のタイムスタンプが表示される
     */
    public static void example_2_dynamicVariable() {
        VariableAPI.register("custom.timestamp", 
            () -> String.valueOf(System.currentTimeMillis()));
        
        VariableAPI.register("custom.random", 
            () -> String.valueOf((int)(Math.random() * 100)));
    }

    /**
     * 例3: 条件分岐付きの動的変数
     */
    public static void example_3_conditionalVariable() {
        VariableAPI.register("custom.status", 
            () -> {
                long now = System.currentTimeMillis();
                int second = (int)((now / 1000) % 60);
                return second % 2 == 0 ? "ON" : "OFF";
            });
    }

    /**
     * 例4: 外部イベント時に変数を更新
     */
    public static void example_4_eventUpdate() {
        // キーバインドが押された時
        VariableAPI.register("custom.keybind_pressed", "false");
        
        // イベント時に更新
        VariableAPI.update("custom.keybind_pressed", "true");
        
        // 一定時間後にリセット
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                VariableAPI.update("custom.keybind_pressed", "false");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * 例5: 複数の変数を一括登録
     */
    public static void example_5_batchRegister() {
        VariableAPI.register("mymod.name", "MyMod");
        VariableAPI.register("mymod.version", "2.0.0");
        VariableAPI.register("mymod.enabled", "true");
        VariableAPI.register("mymod.debug", "false");
    }

    /**
     * 例6: 計算結果を変数として登録
     */
    public static void example_6_computedVariable() {
        VariableAPI.register("custom.calculation", 
            () -> {
                int a = 10, b = 20;
                return String.valueOf(a + b);
            });
    }

    /**
     * 例7: 変数の削除
     */
    public static void example_7_unregister() {
        VariableAPI.register("custom.temp", "temporary");
        // ... 使用 ...
        VariableAPI.unregister("custom.temp");
    }

    /**
     * 例8: すべての変数をクリア
     */
    public static void example_8_clearAll() {
        VariableAPI.clear();
    }

    // ===============================
    // 🎯 実践的なパターン
    // ===============================

    /**
     * パターン1: Mod間連携
     * 他のModの情報をHUDに表示する場合
     */
    public static void pattern_1_modIntegration() {
        // 他Modの機能を呼び出して値を登録
        VariableAPI.register("integration.other_mod", "some_data");
        VariableAPI.register("integration.mana", 
            () -> String.valueOf(100)); // 例
    }

    /**
     * パターン2: ゲームイベント連動
     * プレイヤーの死亡時、スコア変更時など
     */
    public static void pattern_2_gameEventIntegration() {
        VariableAPI.register("event.boss_defeated", "false");
        VariableAPI.register("event.level_up", "0");
        
        // イベント発火時：
        // VariableAPI.update("event.boss_defeated", "true");
    }

    /**
     * パターン3: リアルタイムモニタリング
     * メモリ使用量、アップタイム、パフォーマンスなど
     */
    public static void pattern_3_realtimeMonitoring() {
        VariableAPI.register("system.memory", 
            () -> {
                long used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                long max = Runtime.getRuntime().maxMemory();
                return (used / 1024 / 1024) + "MB / " + (max / 1024 / 1024) + "MB";
            });

        VariableAPI.register("system.uptime", 
            () -> {
                long uptime = System.currentTimeMillis() / 1000;
                long hours = uptime / 3600;
                long minutes = (uptime % 3600) / 60;
                return String.format("%dh %dm", hours, minutes);
            });
    }

    /**
     * パターン4: ユーザー設定の反映
     * 設定ファイルから読み込んだ値を変数として登録
     */
    public static void pattern_4_configIntegration() {
        String configValue = "from_config"; // 実際には設定ファイルから読む
        VariableAPI.register("config.theme", configValue);
        VariableAPI.register("config.language", "ja_JP");
    }

    /**
     * パターン5: キャッシュ付き動的値
     * 頻繁に計算してほしくない値の場合
     */
    public static void pattern_5_cachedDynamicValue() {
        final long[] lastUpdate = {0};
        final String[] cachedValue = {""};
        final long CACHE_TTL = 1000; // 1秒キャッシュ
        
        VariableAPI.register("custom.expensive_calc",
            () -> {
                long now = System.currentTimeMillis();
                if (now - lastUpdate[0] > CACHE_TTL) {
                    // 重い計算をここで実行
                    cachedValue[0] = "result_" + (now / 1000);
                    lastUpdate[0] = now;
                }
                return cachedValue[0];
            });
    }

    // ===============================
    // 🔍 デバッグ・確認用メソッド
    // ===============================

    /**
     * 登録済みの変数をすべて表示（デバッグ用）
     */
    public static void debug_printAllVariables() {
        VariableAPI.debugPrintAll();
    }

    /**
     * 特定の変数の値を確認（デバッグ用）
     */
    public static void debug_getVariable(String key) {
        VariableAPI.debugGet(key);
    }

    /**
     * 変数が存在するか確認
     */
    public static void debug_checkExists(String key) {
        boolean exists = VariableAPI.contains(key);
        System.out.println("[IngameInfo] Variable '" + key + "' exists: " + exists);
    }

    /**
     * 登録済み変数の数を表示
     */
    public static void debug_printCount() {
        System.out.println("[IngameInfo] Total registered variables: " + VariableAPI.size());
    }

    // ===============================
    // 📋 HUD設定ファイルでの利用例
    // ===============================

    /*
    # config/ingameinfo/context/custom.toml の例：

    name = "custom_hud"
    position = "top-left"
    color = 0xFFFFFF
    scale = 1.0
    shadow = true

    text = """
    === MyMod Info ===
    Version: ${custom.version}
    Status: ${custom.status}
    
    === System ===
    Memory: ${system.memory}
    Uptime: ${system.uptime}
    
    === Events ===
    Boss Defeated: ${event.boss_defeated}
    Level: ${event.level_up}
    
    === Custom ===
    Timestamp: ${custom.timestamp}
    Calculation: ${custom.calculation}
    """
    */
}

