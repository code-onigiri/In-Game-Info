package com.codeoinigiri.ingameinfo.variable;

import com.codeoinigiri.ingameinfo.api.VariableRegistryImpl;
import com.codeoinigiri.ingameinfo.variable.provider.*;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 変数を一元管理する中心クラス。
 * Single Responsibility Principle: 変数の管理とプロバイダーの登録のみに責任を持つ
 * Dependency Inversion Principle: 具象クラスではなくIVariableProviderインターフェースに依存
 * Open/Closed Principle: 新しいプロバイダーを追加しやすい設計
 */
public class VariableManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final VariableManager INSTANCE = new VariableManager();

    private final Map<String, String> variables = new ConcurrentHashMap<>();
    private final List<IVariableProvider> providers = new ArrayList<>();

    private VariableManager() {}

    public static VariableManager getInstance() {
        return INSTANCE;
    }

    /**
     * 初期化処理: すべてのプロバイダーを登録
     */
    public void initialize() {
        cleanup();
        registerProviders();
        LOGGER.info("VariableManager initialized with {} providers", providers.size());
    }

    /**
     * プロバイダーの登録
     * Open/Closed Principle: 新しいプロバイダーはここに追加するだけ
     */
    private void registerProviders() {
        registerProvider(new PlayerProvider());
        registerProvider(new WorldProvider());
        registerProvider(new SystemProvider());

        // イベントマッピングプロバイダーは独自の初期化が必要
        EventMappingProvider eventMappingProvider = new EventMappingProvider();
        registerProvider(eventMappingProvider);
        eventMappingProvider.initialize();
    }

    /**
     * プロバイダーを登録してEvent Busに追加
     * Dependency Inversion Principle: インターフェースに依存
     */
    public void registerProvider(IVariableProvider provider) {
        if (provider == null) {
            LOGGER.warn("Attempted to register null provider");
            return;
        }

        providers.add(provider);
        MinecraftForge.EVENT_BUS.register(provider);
        LOGGER.debug("Registered provider: {}", provider.getName());
    }

    /**
     * 変数を更新（プロバイダーから呼ばれる）
     */
    public void update(String key, String value) {
        if (key == null || value == null) return;
        variables.put(key, value);
    }

    /**
     * すべての解決済み変数を取得
     * 内部変数と外部API変数をマージして返す
     */
    public Map<String, String> getResolvedVariables() {
        Map<String, String> resolved = new HashMap<>(variables);
        resolved.putAll(VariableRegistryImpl.getAll());
        return Collections.unmodifiableMap(resolved);
    }

    /**
     * 特定の変数を取得
     */
    public Optional<String> getVariable(String key) {
        String value = variables.get(key);
        if (value == null) {
            value = VariableRegistryImpl.get(key);
        }
        return Optional.ofNullable(value);
    }

    /**
     * クリーンアップ処理
     */
    private void cleanup() {
        variables.clear();

        // 既存のプロバイダーをクリーンアップ
        for (IVariableProvider provider : providers) {
            try {
                MinecraftForge.EVENT_BUS.unregister(provider);
                provider.cleanup();
            } catch (Exception e) {
                LOGGER.error("Failed to cleanup provider: {}", provider.getName(), e);
            }
        }
        providers.clear();
    }

    /**
     * 登録されているプロバイダーの数を取得
     */
    public int getProviderCount() {
        return providers.size();
    }

    /**
     * 登録されている変数の数を取得
     */
    public int getVariableCount() {
        return variables.size() + VariableRegistryImpl.size();
    }
}
