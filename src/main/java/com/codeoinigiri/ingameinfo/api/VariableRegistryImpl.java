package com.codeoinigiri.ingameinfo.api;

import com.codeoinigiri.ingameinfo.variable.CustomVariable;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 🔧 VariableRegistry 実装クラス（内部用）
 * Single Responsibility Principle: カスタム変数の管理のみに責任を持つ
 * VariableAPI の実装を担当するクラス。
 * 外部コードからは VariableAPI を使用してください。
 */
public class VariableRegistryImpl {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, CustomVariable> customVariables = new ConcurrentHashMap<>();

    private VariableRegistryImpl() {
        // ユーティリティクラスのため、インスタンス化を防止
    }

    /**
     * 静的な値を持つ変数を登録
     */
    public static void register(String key, String value) {
        if (!validateKey(key)) return;

        customVariables.put(key, new CustomVariable(key, value, null));
        LOGGER.debug("Registered custom variable: {} = {}", key, value);
    }

    /**
     * 動的な値を持つ変数を登録
     */
    public static void register(String key, Supplier<String> supplier) {
        if (!validateKey(key)) return;
        if (!validateSupplier(supplier)) return;

        customVariables.put(key, new CustomVariable(key, null, supplier));
        LOGGER.debug("Registered dynamic custom variable: {}", key);
    }

    /**
     * 変数の値を更新
     */
    public static void update(String key, String newValue) {
        if (!customVariables.containsKey(key)) {
            LOGGER.warn("Attempted to update non-existent variable: {}", key);
            return;
        }
        customVariables.put(key, new CustomVariable(key, newValue, null));
        LOGGER.debug("Updated custom variable: {} = {}", key, newValue);
    }

    /**
     * 変数を登録解除
     */
    public static void unregister(String key) {
        CustomVariable removed = customVariables.remove(key);
        if (removed != null) {
            LOGGER.debug("Unregistered custom variable: {}", key);
        } else {
            LOGGER.warn("Attempted to unregister non-existent variable: {}", key);
        }
    }

    /**
     * 変数の値を取得
     */
    public static String get(String key) {
        CustomVariable var = customVariables.get(key);
        return var != null ? var.getValue() : null;
    }

    /**
     * すべての変数を取得
     */
    public static Map<String, String> getAll() {
        Map<String, String> result = new HashMap<>();
        customVariables.forEach((key, var) -> {
            try {
                result.put(key, var.getValue());
            } catch (Exception e) {
                LOGGER.error("Failed to get value for variable: {}", key, e);
            }
        });
        return result;
    }

    /**
     * 変数が存在するか確認
     */
    public static boolean contains(String key) {
        return customVariables.containsKey(key);
    }

    /**
     * 変数の数を取得
     */
    public static int size() {
        return customVariables.size();
    }

    /**
     * すべての変数をクリア
     */
    public static void clear() {
        int count = customVariables.size();
        customVariables.clear();
        LOGGER.info("Cleared {} custom variables", count);
    }

    /**
     * キーのバリデーション
     */
    private static boolean validateKey(String key) {
        if (key == null || key.isEmpty()) {
            LOGGER.error("Variable key cannot be null or empty");
            return false;
        }
        return true;
    }

    /**
     * Supplierのバリデーション
     */
    private static boolean validateSupplier(Supplier<String> supplier) {
        if (supplier == null) {
            LOGGER.error("Supplier cannot be null");
            return false;
        }
        return true;
    }
}

