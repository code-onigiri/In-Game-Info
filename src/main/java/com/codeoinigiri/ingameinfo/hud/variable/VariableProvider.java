package com.codeoinigiri.ingameinfo.hud.variable;
import net.minecraft.client.Minecraft;

import java.util.Map;

public interface VariableProvider {
    /**
     * 現在のゲーム状態から変数を返す。
     * 例: "player.health" -> "18.0"
     */
    Map<String, String> getVariables(Minecraft mc);
}