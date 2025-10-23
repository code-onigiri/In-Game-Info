package com.codeoinigiri.ingameinfo.hud.variable;

/** 変数キャッシュを表す小さなデータクラス */
public class CachedValue {
    private final String value;
    private final long timestamp; // キャッシュされた時刻

    public CachedValue(String value, long timestamp) {
        this.value = value;
        this.timestamp = timestamp;
    }

    public String getValue() {
        return value;
    }

    public long getTimestamp() {
        return timestamp;
    }
}