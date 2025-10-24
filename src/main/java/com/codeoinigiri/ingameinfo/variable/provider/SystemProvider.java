package com.codeoinigiri.ingameinfo.variable.provider;

import com.codeoinigiri.ingameinfo.variable.VariableManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * システム関連（FPSなど）の変数を提供します。
 */
public class SystemProvider {
    private final Minecraft mc = Minecraft.getInstance();
    private final VariableManager manager = VariableManager.getInstance();

    private void updateAll() {
        manager.update("system.fps", String.valueOf(mc.getFps()));
        manager.update("system.language", mc.options.languageCode);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // 1秒ごとに更新
        if (mc.level != null && mc.level.getGameTime() % 20 == 0) {
            updateAll();
        }
    }

    @SubscribeEvent
    public void onWorldJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof LocalPlayer) {
            updateAll();
        }
    }
}