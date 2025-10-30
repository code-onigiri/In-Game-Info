package com.codeoinigiri.ingameinfo.variable.provider;

/**
 * システム関連（FPSなど）の変数を提供します。
 * Single Responsibility Principle: システム情報の収集のみに責任を持つ
 */
public class SystemProvider extends AbstractVariableProvider {
    private static final long UPDATE_INTERVAL = 20L; // 1秒(20tick)ごとに更新

    public SystemProvider() {
        super(UPDATE_INTERVAL);
    }

    @Override
    public String getName() {
        return "SystemProvider";
    }

    @Override
    protected void updateVariables() {
        safeUpdate("system.fps", String.valueOf(mc.getFps()));
        safeUpdate("system.language", mc.options.languageCode);
    }
}