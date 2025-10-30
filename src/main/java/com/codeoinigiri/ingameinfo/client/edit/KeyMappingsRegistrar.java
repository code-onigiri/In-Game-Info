package com.codeoinigiri.ingameinfo.client.edit;

import com.codeoinigiri.ingameinfo.InGameInfo;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = InGameInfo.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class KeyMappingsRegistrar {

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent e) {
        HudEditManager.registerKeyMappings(e);
    }
}
