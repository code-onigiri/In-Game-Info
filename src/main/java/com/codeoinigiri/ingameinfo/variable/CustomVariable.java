package com.codeoinigiri.ingameinfo.variable;

import java.util.function.Supplier;

/**
 * A container for custom variables provided through the API.
 * It can hold either a static string value or a dynamic supplier.
 */
public class CustomVariable {
    private final String key;
    private final String staticValue;
    private final Supplier<String> supplier;

    public CustomVariable(String key, String staticValue, Supplier<String> supplier) {
        this.key = key;
        this.staticValue = staticValue;
        this.supplier = supplier;
    }

    public String getValue() {
        if (supplier != null) {
            try {
                String value = supplier.get();
                return value != null ? value : "";
            } catch (Exception e) {
                System.out.println("[IngameInfo] Error evaluating supplier for key: " + key);
                return "ERROR";
            }
        }
        return staticValue != null ? staticValue : "";
    }

    public String getKey() {
        return key;
    }

    public boolean isDynamic() {
        return supplier != null;
    }

    @Override
    public String toString() {
        return "CustomVariable{"
                + "key='" + key + "'" +
                ", dynamic=" + isDynamic() +
                ", value='" + getValue() + "'" +
                '}';
    }
}
