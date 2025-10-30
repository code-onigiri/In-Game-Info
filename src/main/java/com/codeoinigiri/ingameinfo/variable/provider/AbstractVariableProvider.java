package com.codeoinigiri.ingameinfo.variable.provider;

import com.codeoinigiri.ingameinfo.variable.VariableManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 変数プロバイダーの抽象基底クラス
 * Single Responsibility Principle (単一責任の原則): 共通処理のみを担当
 * Open/Closed Principle (開放/閉鎖の原則): 拡張に対して開いている
 */
public abstract class AbstractVariableProvider implements IVariableProvider {
    protected final Minecraft mc;
    protected final VariableManager manager;
    protected final DecimalFormat df;
    private final AtomicLong lastUpdateTick;
    private final long updateInterval;

    protected AbstractVariableProvider(long updateInterval) {
        this.mc = Minecraft.getInstance();
        this.manager = VariableManager.getInstance();
        this.df = new DecimalFormat("0.##");
        this.lastUpdateTick = new AtomicLong(0);
        this.updateInterval = updateInterval;
    }

    /**
     * 変数の更新を行う抽象メソッド
     * サブクラスで実装が必要
     */
    protected abstract void updateVariables();

    /**
     * ワールド参加時の初期更新
     */
    protected void onWorldJoin() {
        updateVariables();
    }

    /**
     * プレイヤーとレベルの有効性をチェック
     */
    protected boolean isValid() {
        return mc.player != null && mc.level != null;
    }

    /**
     * 現在のプレイヤーを取得
     */
    protected LocalPlayer getPlayer() {
        return mc.player;
    }

    /**
     * 現在のレベルを取得
     */
    protected Level getLevel() {
        return mc.level;
    }

    /**
     * 更新間隔をチェックして必要に応じて更新
     */
    protected boolean shouldUpdate(long currentTick) {
        long last = lastUpdateTick.get();
        if (currentTick - last >= updateInterval) {
            lastUpdateTick.set(currentTick);
            return true;
        }
        return false;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!isValid()) return;

        long currentTick = getLevel().getGameTime();
        if (shouldUpdate(currentTick)) {
            updateVariables();
        }
    }

    @SubscribeEvent
    public void onWorldJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof LocalPlayer) {
            onWorldJoin();
        }
    }

    /**
     * 変数を安全に更新するヘルパーメソッド
     */
    protected void safeUpdate(String key, String value) {
        if (key != null && value != null) {
            manager.update(key, value);
        }
    }

    /**
     * 数値を安全にフォーマットするヘルパーメソッド
     */
    protected String formatNumber(double value) {
        return df.format(value);
    }
}

