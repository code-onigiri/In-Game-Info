package com.codeoinigiri.ingameinfo.variable.provider;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * ワールド関連（時間、天候、バイオームなど）の変数を提供します。
 * Single Responsibility Principle: ワールド情報の収集のみに責任を持つ
 */
public class WorldProvider extends AbstractVariableProvider {
    private static final long UPDATE_INTERVAL = 10L; // 10tick毎に更新

    public WorldProvider() {
        super(UPDATE_INTERVAL);
    }

    @Override
    public String getName() {
        return "WorldProvider";
    }

    @Override
    protected void updateVariables() {
        if (!isValid()) return;

        Level level = getLevel();
        LocalPlayer player = getPlayer();

        updateTime(level);
        updateWeather(level);
        updateBiome(level, player);
        updateDimension(level);
    }

    /**
     * 時間情報の更新
     */
    private void updateTime(Level level) {
        long dayTime = level.getDayTime() % 24000;
        safeUpdate("world.time", String.valueOf(dayTime));
        safeUpdate("world.day", String.valueOf(level.getDayTime() / 24000));

        int hour = (int) ((dayTime + 6000) % 24000) / 1000;
        int minute = (int) ((dayTime % 1000) * 60 / 1000);
        safeUpdate("world.time_str", String.format("%02d:%02d", hour, minute));
        safeUpdate("world.is_day", String.valueOf(level.isDay()));
    }

    /**
     * 天候情報の更新
     */
    private void updateWeather(Level level) {
        String weather = level.isThundering() ? "thunder"
                        : level.isRaining() ? "rain"
                        : "clear";
        safeUpdate("world.weather", weather);
    }

    /**
     * バイオーム情報の更新
     */
    private void updateBiome(Level level, LocalPlayer player) {
        level.getBiome(player.blockPosition())
             .unwrapKey()
             .ifPresent(biomeKey ->
                 safeUpdate("world.biome", biomeKey.location().getPath())
             );
    }

    /**
     * ディメンション情報の更新
     */
    private void updateDimension(Level level) {
        safeUpdate("world.dimension", level.dimension().location().toString());
    }

    /**
     * ディメンション変更時に即時更新
     */
    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (mc.player != null && event.getEntity().getId() == mc.player.getId()) {
            safeUpdate("world.dimension", event.getTo().location().toString());
        }
    }
}