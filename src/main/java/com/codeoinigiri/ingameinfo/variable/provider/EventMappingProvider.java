package com.codeoinigiri.ingameinfo.variable.provider;

import com.codeoinigiri.ingameinfo.variable.VariableManager;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * event_mappings.toml に基づいて、
 * カスタムイベントと変数のマッピングを処理します。
 * Single Responsibility Principle: イベントマッピングの管理のみに責任を持つ
 */
public class EventMappingProvider implements IVariableProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, List<EventMapping>> MAPPINGS = new ConcurrentHashMap<>();
    private static final File CONFIG_FILE = new File("config/ingameinfo/event_mappings.toml");
    private static final String LITERAL_PREFIX = "literal:";
    
    private final VariableManager manager;

    public EventMappingProvider() {
        this.manager = VariableManager.getInstance();
    }

    @Override
    public String getName() {
        return "EventMappingProvider";
    }

    /**
     * イベントマッピング設定のデータクラス
     */
    public record EventMapping(String event, String key, String source) {}

    @Override
    public void initialize() {
        ensureConfigFileExists();
        reload();
        LOGGER.info("EventMappingProvider initialized with {} mappings", MAPPINGS.size());
    }

    /**
     * 設定ファイルの存在を確認し、なければ作成
     */
    private void ensureConfigFileExists() {
        File dir = CONFIG_FILE.getParentFile();
        if (!dir.exists() && !dir.mkdirs()) {
            LOGGER.error("Failed to create config directory: {}", dir);
            return;
        }
        
        if (!CONFIG_FILE.exists()) {
            createDefaultConfig();
        }
    }

    /**
     * 設定を再読み込み
     */
    public void reload() {
        MAPPINGS.clear();
        
        try (CommentedFileConfig cfg = CommentedFileConfig.builder(CONFIG_FILE)
                .autosave()
                .build()) {
            cfg.load();
            loadMappings(cfg);
            LOGGER.debug("Loaded {} event mappings", MAPPINGS.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load event mappings", e);
        }
    }

    /**
     * 設定からマッピングを読み込む
     */
    private void loadMappings(CommentedFileConfig cfg) {
        Object mappingObj = cfg.get("mapping");
        if (!(mappingObj instanceof List<?> list)) {
            return;
        }

        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                parseMapping(map);
            }
        }
    }

    /**
     * マッピングエントリをパース
     */
    private void parseMapping(Map<?, ?> map) {
        String event = Objects.toString(map.get("event"), "");
        String key = Objects.toString(map.get("key"), "");
        String source = Objects.toString(map.get("source"), LITERAL_PREFIX);
        
        if (!event.isEmpty() && !key.isEmpty()) {
            EventMapping em = new EventMapping(event, key, source);
            MAPPINGS.computeIfAbsent(event, k -> new ArrayList<>()).add(em);
            LOGGER.debug("Registered event mapping: {} -> {} (source: {})", event, key, source);
        }
    }

    /**
     * デフォルト設定を作成
     */
    private void createDefaultConfig() {
        try (FileWriter w = new FileWriter(CONFIG_FILE)) {
            writeDefaultConfig(w);
            LOGGER.info("Created default event_mappings.toml");
        } catch (IOException e) {
            LOGGER.error("Failed to create default event_mappings.toml", e);
        }
    }

    /**
     * デフォルト設定を書き込み
     */
    private void writeDefaultConfig(FileWriter w) throws IOException {
        w.write("# Event mappings for IngameInfo HUD (toml)\n");
        w.write("# Define [[mapping]] entries with: event, key, source\n");
        w.write("# source can be 'message', 'health', 'damage', 'dimension', or literal:<value>\n\n");
        w.write("[[mapping]]\n event = \"chat\"\n key = \"system.last_chat\"\n source = \"message\"\n\n");
        w.write("[[mapping]]\n event = \"hurt\"\n key = \"player.last_damage\"\n source = \"damage\"\n\n");
        w.write("[[mapping]]\n event = \"dimension_change\"\n key = \"world.previous_dimension\"\n source = \"dimension\"\n\n");
    }

    /**
     * イベントコンテキストからマッピングを適用
     */
    private void applyMappings(String event, Map<String, String> context) {
        List<EventMapping> mappings = MAPPINGS.getOrDefault(event, Collections.emptyList());
        if (mappings.isEmpty()) return;

        for (EventMapping mapping : mappings) {
            String value = extractValue(mapping.source, context);
            manager.update(mapping.key, value);
        }
    }

    /**
     * ソース指定から値を抽出
     */
    private String extractValue(String source, Map<String, String> context) {
        if (source.startsWith(LITERAL_PREFIX)) {
            return source.substring(LITERAL_PREFIX.length());
        }
        return context.getOrDefault(source, "");
    }

    // === イベントハンドラー ===

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer)) return;
        
        Map<String, String> ctx = Map.of(
            "health", String.valueOf(event.getEntity().getHealth()),
            "damage", String.valueOf(event.getAmount())
        );
        applyMappings("hurt", ctx);
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        Map<String, String> ctx = Map.of(
            "dimension", event.getTo().location().toString()
        );
        applyMappings("dimension_change", ctx);
    }

    @SubscribeEvent
    public void onClientChat(ClientChatEvent event) {
        Map<String, String> ctx = Map.of(
            "message", event.getMessage()
        );
        applyMappings("chat", ctx);
    }
}
