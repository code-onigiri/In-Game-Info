package com.codeoinigiri.ingameinfo.api;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Internal facade that adapts {@link VariableRegistryImpl} to the public
 * {@link IVariableRegistry} interface. Other mods should obtain an instance via
 * {@link VariableAPI#registry()} instead of constructing this directly.
 */
class VariableRegistryFacade implements IVariableRegistry {
    @Override
    public void register(String key, String value) {
        VariableRegistryImpl.register(key, value);
    }

    @Override
    public void register(String key, Supplier<String> supplier) {
        VariableRegistryImpl.register(key, supplier);
    }

    @Override
    public void update(String key, String newValue) {
        VariableRegistryImpl.update(key, newValue);
    }

    @Override
    public void unregister(String key) {
        VariableRegistryImpl.unregister(key);
    }

    @Override
    public String get(String key) {
        return VariableRegistryImpl.get(key);
    }

    @Override
    public Map<String, String> getAll() {
        return VariableRegistryImpl.getAll();
    }

    @Override
    public boolean contains(String key) {
        return VariableRegistryImpl.contains(key);
    }

    @Override
    public int size() {
        return VariableRegistryImpl.size();
    }

    @Override
    public void clear() {
        VariableRegistryImpl.clear();
    }
}
