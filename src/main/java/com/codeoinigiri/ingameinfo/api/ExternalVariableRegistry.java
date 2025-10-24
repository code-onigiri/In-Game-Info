package com.codeoinigiri.ingameinfo.api;

import com.codeoinigiri.ingameinfo.variable.VariableManager;

/**
 * 他のMODや外部コンポーネントが変数を登録・更新するためのシンプルなAPIです。
 */
public class ExternalVariableRegistry {
    /**
     * 指定されたキーと値で変数を登録または更新します。
     * @param key 変数キー (例: "mymod.myvar")
     * @param value 変数の文字列表現
     */
    public static void update(String key, String value) {
        VariableManager.getInstance().update(key, value);
    }
}
