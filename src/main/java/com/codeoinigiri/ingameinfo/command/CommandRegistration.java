package com.codeoinigiri.ingameinfo.command;

import com.codeoinigiri.ingameinfo.InGameInfo;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = InGameInfo.MOD_ID)
public class CommandRegistration {

    @SubscribeEvent
    public static void onCommandsRegister(RegisterCommandsEvent event) {
        ListVariablesCommand.register(event.getDispatcher());
    }
}
