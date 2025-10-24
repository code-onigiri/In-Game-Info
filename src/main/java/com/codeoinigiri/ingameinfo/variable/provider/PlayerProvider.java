package com.codeoinigiri.ingameinfo.variable.provider;

import com.codeoinigiri.ingameinfo.variable.VariableManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.text.DecimalFormat;

/**
 * プレイヤー関連の変数を提供します。
 * Tickイベントで定期的に更新し、関連イベントで即時更新します。
 */
public class PlayerProvider {
    private final Minecraft mc = Minecraft.getInstance();
    private final VariableManager manager = VariableManager.getInstance();
    private final DecimalFormat df = new DecimalFormat("0.##");

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        LocalPlayer player = mc.player;
        if (player == null) return;

        // 高頻度更新
        manager.update("player.posX", df.format(player.getX()));
        manager.update("player.posY", df.format(player.getY()));
        manager.update("player.posZ", df.format(player.getZ()));
        manager.update("player.health", df.format(player.getHealth()));

        // 中頻度更新
        if (mc.level != null && mc.level.getGameTime() % 5 == 0) {
            manager.update("player.name", player.getName().getString());
            manager.update("player.food", String.valueOf(player.getFoodData().getFoodLevel()));
            manager.update("player.saturation", df.format(player.getFoodData().getSaturationLevel()));
            manager.update("player.yaw", df.format(player.getYRot()));
            manager.update("player.pitch", df.format(player.getXRot()));
        }

        // 低頻度更新
        if (mc.level != null && mc.level.getGameTime() % 20 == 0) {
            manager.update("player.max_health", df.format(player.getMaxHealth()));
            manager.update("player.xp_level", String.valueOf(player.experienceLevel));
            manager.update("player.is_flying", String.valueOf(player.getAbilities().flying));
            manager.update("player.is_swimming", String.valueOf(player.isSwimming()));
            manager.update("player.item.mainhand", getItemName(player.getMainHandItem()));
            manager.update("player.item.offhand", getItemName(player.getOffhandItem()));
            manager.update("player.item.helmet", getItemName(player.getInventory().armor.get(3)));
            manager.update("player.item.chestplate", getItemName(player.getInventory().armor.get(2)));
            manager.update("player.item.leggings", getItemName(player.getInventory().armor.get(1)));
            manager.update("player.item.boots", getItemName(player.getInventory().armor.get(0)));
        }
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof LocalPlayer player) {
            // ダメージイベント発生時に体力を即時更新
            manager.update("player.health", df.format(player.getHealth()));
        }
    }

    private String getItemName(ItemStack stack) {
        return stack.isEmpty() ? "" : stack.getHoverName().getString();
    }

    private void updateAll() {
        LocalPlayer player = mc.player;
        if (player == null) return;

        manager.update("player.posX", df.format(player.getX()));
        manager.update("player.posY", df.format(player.getY()));
        manager.update("player.posZ", df.format(player.getZ()));
        manager.update("player.health", df.format(player.getHealth()));
        manager.update("player.name", player.getName().getString());
        manager.update("player.food", String.valueOf(player.getFoodData().getFoodLevel()));
        manager.update("player.saturation", df.format(player.getFoodData().getSaturationLevel()));
        manager.update("player.yaw", df.format(player.getYRot()));
        manager.update("player.pitch", df.format(player.getXRot()));
        manager.update("player.max_health", df.format(player.getMaxHealth()));
        manager.update("player.xp_level", String.valueOf(player.experienceLevel));
        manager.update("player.is_flying", String.valueOf(player.getAbilities().flying));
        manager.update("player.is_swimming", String.valueOf(player.isSwimming()));
        manager.update("player.item.mainhand", getItemName(player.getMainHandItem()));
        manager.update("player.item.offhand", getItemName(player.getOffhandItem()));
        manager.update("player.item.helmet", getItemName(player.getInventory().armor.get(3)));
        manager.update("player.item.chestplate", getItemName(player.getInventory().armor.get(2)));
        manager.update("player.item.leggings", getItemName(player.getInventory().armor.get(1)));
        manager.update("player.item.boots", getItemName(player.getInventory().armor.get(0)));
    }

    @SubscribeEvent
    public void onWorldJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof LocalPlayer) {
            updateAll();
        }
    }
}
