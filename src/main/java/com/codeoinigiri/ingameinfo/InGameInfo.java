package com.codeoinigiri.ingameinfo;

import com.codeoinigiri.ingameinfo.config.ClientConfig;
import com.codeoinigiri.ingameinfo.hud.ConfigWatcher;
import com.codeoinigiri.ingameinfo.hud.HudContextManager;
import com.codeoinigiri.ingameinfo.variable.VariableManager;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod("ingameinfo")
public class InGameInfo {
    public static final String MOD_ID = "ingameinfo";
    public static final Logger LOGGER = LogUtils.getLogger();

    public InGameInfo(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the setup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the config
        context.registerConfig(ModConfig.Type.CLIENT, ClientConfig.CLIENT_SPEC);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("InGameInfo: Common Setup");

        // Initialize VariableManager which sets up all variable providers
        VariableManager.getInstance().initialize();

        // Load HUD contexts and start watching for config changes
        HudContextManager.loadContexts();
        ConfigWatcher.startWatching();
    }
}