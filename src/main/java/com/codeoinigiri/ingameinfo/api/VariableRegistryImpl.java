package com.codeoinigiri.ingameinfo.api;

import com.codeoinigiri.ingameinfo.variable.CustomVariable;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 🔧 VariableRegistry 実装クラス（内部用）
 * VariableAPI の実装を担当するクラス。
 * 外部コードからは VariableAPI を使用してください。
 */
public class VariableRegistryImpl {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, CustomVariable> customVariables = new ConcurrentHashMap<>();

    public static void register(String key, String value) {
        if (key == null || key.isEmpty()) {
            LOGGER.error("Variable key cannot be null or empty");
            return;
        }
        customVariables.put(key, new CustomVariable(key, value, null));
        LOGGER.info("Registered custom variable: {} = {}", key, value);
    }

    public static void register(String key, Supplier<String> supplier) {
        if (key == null || key.isEmpty()) {
            LOGGER.error("Variable key cannot be null or empty");
            return;
        }
        if (supplier == null) {
            LOGGER.error("Supplier cannot be null");
            return;
        }
        customVariables.put(key, new CustomVariable(key, null, supplier));
        LOGGER.info("Registered dynamic custom variable: {}", key);
    }

    public static void update(String key, String newValue) {
        if (!customVariables.containsKey(key)) {
            LOGGER.warn("Variable not found: {}", key);
            return;
        }
        customVariables.put(key, new CustomVariable(key, newValue, null));
        LOGGER.info("Updated custom variable: {} = {}", key, newValue);
    }

    public static void unregister(String key) {
        if (customVariables.remove(key) != null) {
            LOGGER.info("Unregistered custom variable: {}", key);
        } else {
            LOGGER.warn("Variable not found: {}", key);
        }
    }

    public static String get(String key) {
        CustomVariable var = customVariables.get(key);
        if (var == null) return null;
        return var.getValue();
    }

    public static Map<String, String> getAll() {
        Map<String, String> result = new HashMap<>();
        for (var entry : customVariables.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getValue());
        }
        return result;
    }

    public static boolean contains(String key) {
        return customVariables.containsKey(key);
    }

    public static int size() {
        return customVariables.size();
    }

    public static void clear() {
        customVariables.clear();
        LOGGER.info("Cleared all custom variables");
    }
}

