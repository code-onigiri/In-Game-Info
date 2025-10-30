package com.codeoinigiri.ingameinfo.variable.provider;
/**
 * 変数プロバイダーの共通インターフェース
 * Interface Segregation Principle (インターフェース分離の原則) を適用
 */
public interface IVariableProvider {
    /**
     * プロバイダーの初期化処理
     */
    default void initialize() {}
    /**
     * プロバイダーのクリーンアップ処理
     */
    default void cleanup() {}
    /**
     * プロバイダーの名前を取得
     * @return プロバイダー名
     */
    String getName();
}
