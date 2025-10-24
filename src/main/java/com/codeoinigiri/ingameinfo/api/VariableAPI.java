package com.codeoinigiri.ingameinfo.api;

import java.util.Map;
import java.util.function.Supplier;

/**
 * 🌐 IngameInfo Variable API - 公開インターフェース
 * 
 * 外部Modやプラグインから変数を動的に登録・管理するための公開API
 * 
 * 使用例：
 *   VariableAPI.register("custom.myvar", "value");
 *   VariableAPI.register("custom.dynamic", () -> getCurrentValue());
 */
public class VariableAPI {

    // ===============================
    // 📝 変数登録
    // ===============================

    /**
     * 静的な値を持つカスタム変数を登録します
     * @param key 変数キー (推奨形式: "namespace.key")
     * @param value 値
     * 
     * 例: VariableAPI.register("mymod.level", "10");
     */
    public static void register(String key, String value) {
        VariableRegistryImpl.register(key, value);
    }

    /**
     * 動的な値（毎回最新値を生成）を持つ変数を登録します
     * @param key 変数キー
     * @param supplier 値を生成する関数
     * 
     * 例: VariableAPI.register("mymod.time", () -> System.currentTimeMillis());
     */
    public static void register(String key, Supplier<String> supplier) {
        VariableRegistryImpl.register(key, supplier);
    }

    // ===============================
    // ♻️ 変数更新
    // ===============================

    /**
     * 既存の静的変数の値を更新します
     * @param key 変数キー
     * @param newValue 新しい値
     * 
     * 例: VariableAPI.update("mymod.level", "20");
     */
    public static void update(String key, String newValue) {
        VariableRegistryImpl.update(key, newValue);
    }

    // ===============================
    // 🗑 変数削除
    // ===============================

    /**
     * カスタム変数を削除します
     * @param key 変数キー
     * 
     * 例: VariableAPI.unregister("mymod.oldvar");
     */
    public static void unregister(String key) {
        VariableRegistryImpl.unregister(key);
    }

    // ===============================
    // 📖 変数取得
    // ===============================

    /**
     * 登録されたカスタム変数の現在の値を取得します
     * @param key 変数キー
     * @return 値、またはnull（存在しない場合）
     */
    public static String get(String key) {
        return VariableRegistryImpl.get(key);
    }

    /**
     * すべてのカスタム変数をマップで取得します
     * @return キー → 値のマップ
     */
    public static Map<String, String> getAll() {
        return VariableRegistryImpl.getAll();
    }

    // ===============================
    // ❓ 存在確認
    // ===============================

    /**
     * 指定のキーが登録されているか確認します
     * @param key 変数キー
     * @return true if registered
     */
    public static boolean contains(String key) {
        return VariableRegistryImpl.contains(key);
    }

    // ===============================
    // 📊 サイズ・クリア
    // ===============================

    /**
     * 登録されているカスタム変数の数を取得します
     * @return 変数の数
     */
    public static int size() {
        return VariableRegistryImpl.size();
    }

    /**
     * すべてのカスタム変数をクリアします
     */
    public static void clear() {
        VariableRegistryImpl.clear();
    }

    // ===============================
    // 🔍 デバッグ用メソッド
    // ===============================

    /**
     * 登録済みの全変数を表示（デバッグ用）
     */
    public static void debugPrintAll() {
        System.out.println("[IngameInfo] ===== Registered Custom Variables =====");
        int count = 0;
        for (var entry : getAll().entrySet()) {
            System.out.println(String.format("[IngameInfo]   %s = %s", entry.getKey(), entry.getValue()));
            count++;
        }
        System.out.println(String.format("[IngameInfo] Total: %d variables", count));
    }

    /**
     * 特定の変数の値を表示（デバッグ用）
     * @param key 変数キー
     */
    public static void debugGet(String key) {
        String value = get(key);
        System.out.println(String.format("[IngameInfo] %s = %s", key, value != null ? value : "NOT FOUND"));
    }
}

