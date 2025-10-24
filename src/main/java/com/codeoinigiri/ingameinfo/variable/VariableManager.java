package com.codeoinigiri.ingameinfo.variable;

import com.codeoinigiri.ingameinfo.api.VariableRegistryImpl;
import com.codeoinigiri.ingameinfo.variable.provider.EventMappingProvider;
import com.codeoinigiri.ingameinfo.variable.provider.PlayerProvider;
import com.codeoinigiri.ingameinfo.variable.provider.SystemProvider;
import com.codeoinigiri.ingameinfo.variable.provider.WorldProvider;
import net.minecraftforge.common.MinecraftForge;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 変数を一元管理する新しい中心クラス。
 * イベント駆動を基本とし、各種プロバイダーから値を受け取って状態を更新します。
 */
public class VariableManager {
    private static final VariableManager INSTANCE = new VariableManager();
    private final Map<String, String> variables = new ConcurrentHashMap<>();

    private VariableManager() {}

    public static VariableManager getInstance() {
        return INSTANCE;
    }

    public void initialize() {
        // 古い状態をクリア
        variables.clear();

        // プロバイダーを初期化してEvent Busに登録
        EventMappingProvider eventMappingProvider = new EventMappingProvider();
        eventMappingProvider.initialize();

        Object[] providers = {
            new PlayerProvider(),
            new WorldProvider(),
            new SystemProvider(),
            eventMappingProvider
        };

        for (Object provider : providers) {
            MinecraftForge.EVENT_BUS.register(provider);
        }
    }

    public void update(String key, String value) {
        if (key == null || value == null) return;
        variables.put(key, value);
    }

    public Map<String, String> getResolvedVariables() {
        Map<String, String> resolved = new HashMap<>(variables);
        // 外部APIからの変数を追加
        resolved.putAll(VariableRegistryImpl.getAll());
        return Collections.unmodifiableMap(resolved);
    }
}
