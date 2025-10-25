package com.codeoinigiri.ingameinfo.api;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Public interface for the variable registry that other mods can rely on.
 *
 * This interface abstracts the registry that stores custom variables exposed
 * by InGameInfo. Use {@link VariableAPI} to access a singleton implementation
 * safely.
 */
public interface IVariableRegistry {
    void register(String key, String value);
    void register(String key, Supplier<String> supplier);
    void update(String key, String newValue);
    void unregister(String key);
    String get(String key);
    Map<String, String> getAll();
    boolean contains(String key);
    int size();
    void clear();
}
