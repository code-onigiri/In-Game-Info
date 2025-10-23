package com.codeoinigiri.ingameinfo.hud.variable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;

import java.text.DecimalFormat;
import java.util.*;

public class VariableResolver {
    private static final Minecraft mc = Minecraft.getInstance();

    // 🕒 デフォルト TTL 値
    private static final long PLAYER_TTL_MS = 200;
    private static final long WORLD_TTL_MS = 1000;
    private static final long ENV_TTL_MS = 500;
    private static final long SYSTEM_TTL_MS = 1000;

    private static final Map<String, CachedValue> cache = new HashMap<>();
    private static final Map<String, Long> categoryTimestamp = new HashMap<>();

    private final DecimalFormat df = new DecimalFormat("0.##");

    // ===============================
    // 🧭 主要メソッド
    // ===============================
    public Map<String, String> collectVariables() {
        LocalPlayer player = mc.player;
        Level level = mc.level;
        Map<String, String> vars = new HashMap<>();
        if (player == null || level == null) return vars;

        long now = System.currentTimeMillis();

        updatePlayer(player, now);
        updateWorld(level, player, now);
        updateEnvironment(level, player, now);
        updateSystem(now);

        for (var e : cache.entrySet())
            vars.put(e.getKey(), e.getValue().getValue());
        return vars;
    }

    // ===============================
    // 👤 Player
    // ===============================
    private void updatePlayer(LocalPlayer player, long now) {
        if (!needsUpdate("player", now, PLAYER_TTL_MS)) return;

        putDiff("player.name", player.getName().getString(), now);
        putDiff("player.health", fmt(player.getHealth()), now);
        putDiff("player.max_health", fmt(player.getMaxHealth()), now);
        putDiff("player.food", fmt(player.getFoodData().getFoodLevel()), now);
        putDiff("player.saturation", fmt(player.getFoodData().getSaturationLevel()), now);
        putDiff("player.xp", fmt(player.experienceLevel), now);
        putDiff("player.posX", fmt(player.getX()), now);
        putDiff("player.posY", fmt(player.getY()), now);
        putDiff("player.posZ", fmt(player.getZ()), now);
        putDiff("player.yaw", fmt(player.getYRot()), now);
        putDiff("player.pitch", fmt(player.getXRot()), now);
        putDiff("player.is_flying", String.valueOf(player.getAbilities().flying), now);
        putDiff("player.is_swimming", String.valueOf(player.isSwimming()), now);
        putDiff("player.item.mainhand", getItemName(player.getMainHandItem()), now);
        putDiff("player.item.offhand", getItemName(player.getOffhandItem()), now);
        putDiff("player.item.helmet", getItemName(player.getInventory().armor.get(3)), now);
        putDiff("player.item.chestplate", getItemName(player.getInventory().armor.get(2)), now);
        putDiff("player.item.leggings", getItemName(player.getInventory().armor.get(1)), now);
        putDiff("player.item.boots", getItemName(player.getInventory().armor.get(0)), now);

        categoryTimestamp.put("player", now);
    }

    // ===============================
    // 🌍 World
    // ===============================
    private void updateWorld(Level level, LocalPlayer player, long now) {
        if (!needsUpdate("world", now, WORLD_TTL_MS)) return;

        long dayTime = level.getDayTime() % 24000;
        putDiff("world.time", fmt(dayTime), now);
        putDiff("world.day", fmt(level.getDayTime() / 24000), now);

        int hour = (int) ((dayTime + 6000) % 24000) / 1000;
        int minute = (int) ((dayTime % 1000) * 60 / 1000);
        putDiff("world.time_str", String.format("%02d:%02d", hour, minute), now);

        if (level.isThundering()) putDiff("world.weather", "thunder", now);
        else if (level.isRaining()) putDiff("world.weather", "rain", now);
        else putDiff("world.weather", "clear", now);

        putDiff("world.is_day", String.valueOf(level.isDay()), now);
        putDiff("world.dimension", level.dimension().location().toString(), now);

        try {
            ResourceKey<Biome> biomeKey = level.getBiome(player.blockPosition()).unwrapKey().orElse(null);
            if (biomeKey != null)
                putDiff("world.biome", biomeKey.location().getPath(), now);
        } catch (Exception ignored) {
            putDiff("world.biome", "unknown", now);
        }

        categoryTimestamp.put("world", now);
    }

    // ===============================
    // 🌡 Environment
    // ===============================
    private void updateEnvironment(Level level, LocalPlayer player, long now) {
        if (!needsUpdate("environment", now, ENV_TTL_MS)) return;

        double temperature = level.getBiome(player.blockPosition()).value().getBaseTemperature();
        int light = level.getBrightness(LightLayer.BLOCK, player.blockPosition());
        double altitude = player.getY();

        putDiff("environment.temperature", fmt(temperature), now);
        putDiff("environment.light", String.valueOf(light), now);
        putDiff("environment.altitude", fmt(altitude), now);

        categoryTimestamp.put("environment", now);
    }

    // ===============================
    // ⚙ System
    // ===============================
    private void updateSystem(long now) {
        if (!needsUpdate("system", now, SYSTEM_TTL_MS)) return;

        putDiff("system.fps", fmt(mc.getFps()), now);
        putDiff("system.language", mc.options.languageCode, now);

        categoryTimestamp.put("system", now);
    }

    // ===============================
    // 🔄 差分 & TTL適用
    // ===============================
    private void putDiff(String key, String newValue, long now) {
        String category = key.contains(".") ? key.split("\\.")[0] : "misc";
        long ttl = CacheConfig.getTTL(key, category, getCategoryTTL(category)); // ✅ 修正: 3引数に

        CachedValue old = cache.get(key);
        if (old == null || !old.getValue().equals(newValue) ||
                (now - old.getTimestamp()) > ttl) {
            cache.put(key, new CachedValue(newValue, now));
        }
    }

    private boolean needsUpdate(String category, long now, long baseTtl) {
        long ttl = CacheConfig.getTTL(category, category, baseTtl); // ✅ 修正: 3引数に
        long last = categoryTimestamp.getOrDefault(category, 0L);
        return (now - last) > ttl;
    }

    private long getCategoryTTL(String category) {
        return switch (category) {
            case "player" -> PLAYER_TTL_MS;
            case "world" -> WORLD_TTL_MS;
            case "environment" -> ENV_TTL_MS;
            case "system" -> SYSTEM_TTL_MS;
            default -> 1000;
        };
    }

    // ===============================
    // 🎁 ユーティリティ
    // ===============================
    public List<String> resolveLines(List<String> lines) {
        List<String> out = new ArrayList<>();
        if (lines == null) return out;
        Map<String, String> vars = collectVariables();
        for (String line : lines)
            out.add(ExpressionUtils.evaluateEmbedded(line, vars));
        return out;
    }

    private String getItemName(ItemStack stack) {
        return stack.isEmpty() ? "(なし)" : stack.getHoverName().getString();
    }

    private String fmt(double v) { return df.format(v); }
    private String fmt(long v) { return String.valueOf(v); }
}