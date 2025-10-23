package com.codeoinigiri.ingameinfo.hud.variable;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

public class PlayerVariableProvider implements VariableProvider {

    @Override
    public Map<String, String> getVariables(Minecraft mc) {
        Map<String, String> vars = new HashMap<>();
        Player player = mc.player;
        if (player == null) return vars;

        vars.put("player.name", player.getName().getString());
        vars.put("player.health", String.format("%.1f", player.getHealth()));
        vars.put("player.max_health", String.format("%.1f", player.getMaxHealth()));
        vars.put("player.x", String.format("%.0f", player.getX()));
        vars.put("player.y", String.format("%.0f", player.getY()));
        vars.put("player.z", String.format("%.0f", player.getZ()));
        vars.put("player.dimension", player.level().dimension().location().toString());

        return vars;
    }
}