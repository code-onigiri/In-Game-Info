package com.codeoinigiri.ingameinfo.variable.provider;

import com.codeoinigiri.ingameinfo.variable.VariableManager;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * event_mappings.toml に基づいて、
 * カスタムイベントと変数のマッピングを処理します。
 */
public class EventMappingProvider {
    private static final Map<String, List<EventMapping>> MAPPINGS = new ConcurrentHashMap<>();
    private static final File CONFIG_FILE = new File("config/ingameinfo/event_mappings.toml");
    private final VariableManager manager = VariableManager.getInstance();

    public record EventMapping(String event, String key, String source) {}

    public void initialize() {
        File dir = CONFIG_FILE.getParentFile();
        if (!dir.exists()) dir.mkdirs();
        if (!CONFIG_FILE.exists()) createDefaultConfig();
        reload();
    }

    public void reload() {
        MAPPINGS.clear();
        try (CommentedFileConfig cfg = CommentedFileConfig.builder(CONFIG_FILE).autosave().build()) {
            cfg.load();
            Object o = cfg.get("mapping");
            if (o instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> m) {
                        String event = Objects.toString(m.get("event"), "");
                        String key = Objects.toString(m.get("key"), "");
                        String source = Objects.toString(m.get("source"), "literal:");
                        if (!event.isEmpty() && !key.isEmpty()) {
                            EventMapping em = new EventMapping(event, key, source);
                            MAPPINGS.computeIfAbsent(event, k -> new ArrayList<>()).add(em);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[IngameInfo] Failed to load event mappings: " + e);
        }
    }

    private void createDefaultConfig() {
        try (FileWriter w = new FileWriter(CONFIG_FILE)) {
            w.write("# Event mappings for IngameInfo HUD (toml)\n");
            w.write("# Define [[mapping]] entries with: event, key, source\n");
            w.write("# source can be 'message', 'health', 'damage', 'dimension', or literal:<value>\n\n");
            w.write("[[mapping]]\n event = \"chat\"\n key = \"system.last_chat\"\n source = \"message\"\n\n");
            w.write("[[mapping]]\n event = \"hurt\"\n key = \"player.last_damage\"\n source = \"damage\"\n\n");
            w.write("[[mapping]]\n event = \"dimension_change\"\n key = \"world.previous_dimension\"\n source = \"dimension\"\n\n");
        } catch (IOException e) {
            System.out.println("[IngameInfo] Failed to create default event_mappings.toml: " + e);
        }
    }

    private void applyMappings(String event, Map<String, String> ctx) {
        var list = MAPPINGS.getOrDefault(event, Collections.emptyList());
        if (list.isEmpty()) return;
        for (EventMapping m : list) {
            String value;
            if (m.source.startsWith("literal:")) {
                value = m.source.substring("literal:".length());
            } else {
                value = ctx.getOrDefault(m.source, "");
            }
            manager.update(m.key, value);
        }
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof LocalPlayer) {
            Map<String, String> ctx = new HashMap<>();
            ctx.put("health", String.valueOf(event.getEntity().getHealth()));
            ctx.put("damage", String.valueOf(event.getAmount()));
            applyMappings("hurt", ctx);
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        Map<String, String> ctx = new HashMap<>();
        ctx.put("dimension", event.getTo().location().toString());
        applyMappings("dimension_change", ctx);
    }

    @SubscribeEvent
    public void onClientChat(ClientChatEvent event) {
        Map<String, String> ctx = new HashMap<>();
        ctx.put("message", event.getMessage());
        applyMappings("chat", ctx);
    }
}
