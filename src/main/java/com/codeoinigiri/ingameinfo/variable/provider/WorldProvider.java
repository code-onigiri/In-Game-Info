package com.codeoinigiri.ingameinfo.variable.provider;

import com.codeoinigiri.ingameinfo.variable.VariableManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * ワールド関連（時間、天候、バイオームなど）の変数を提供します。
 */
public class WorldProvider {
    private final Minecraft mc = Minecraft.getInstance();
    private final VariableManager manager = VariableManager.getInstance();

    private void updateAll() {
        Level level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null) return;

        long dayTime = level.getDayTime() % 24000;
        manager.update("world.time", String.valueOf(dayTime));
        manager.update("world.day", String.valueOf(level.getDayTime() / 24000));

        int hour = (int) ((dayTime + 6000) % 24000) / 1000;
        int minute = (int) ((dayTime % 1000) * 60 / 1000);
        manager.update("world.time_str", String.format("%02d:%02d", hour, minute));

        String weather = level.isThundering() ? "thunder" : (level.isRaining() ? "rain" : "clear");
        manager.update("world.weather", weather);
        manager.update("world.is_day", String.valueOf(level.isDay()));

        level.getBiome(player.blockPosition()).unwrapKey().ifPresent(biomeKey ->
            manager.update("world.biome", biomeKey.location().getPath())
        );

        manager.update("world.dimension", level.dimension().location().toString());
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (mc.level == null) return;

        // 10tickごとに更新
        if (mc.level.getGameTime() % 10 == 0) {
            updateAll();
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (mc.player != null && event.getEntity().getId() == mc.player.getId()) {
            String dimension = event.getTo().location().toString();
            manager.update("world.dimension", dimension);
        }
    }

    @SubscribeEvent
    public void onWorldJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof LocalPlayer) {
            updateAll();
        }
    }
}