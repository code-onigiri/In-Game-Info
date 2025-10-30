package com.codeoinigiri.ingameinfo.variable.provider;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * プレイヤー関連の変数を提供します。
 * Single Responsibility Principle: プレイヤー情報の収集のみに責任を持つ
 * Open/Closed Principle: AbstractVariableProviderを拡張して機能を追加
 */
public class PlayerProvider extends AbstractVariableProvider {
    private static final long HIGH_FREQ_INTERVAL = 1L;    // 毎tick (高頻度)
    private static final long MID_FREQ_INTERVAL = 5L;     // 5tick毎 (中頻度)
    private static final long LOW_FREQ_INTERVAL = 20L;    // 20tick毎 (低頻度)

    public PlayerProvider() {
        super(HIGH_FREQ_INTERVAL);
    }

    @Override
    public String getName() {
        return "PlayerProvider";
    }

    @Override
    protected void updateVariables() {
        if (!isValid()) return;
        LocalPlayer player = getPlayer();
        long gameTick = getLevel().getGameTime();

        // 高頻度更新（毎tick）
        updateHighFrequency(player);

        // 中頻度更新（5tick毎）
        if (gameTick % MID_FREQ_INTERVAL == 0) {
            updateMidFrequency(player);
        }

        // 低頻度更新（20tick毎）
        if (gameTick % LOW_FREQ_INTERVAL == 0) {
            updateLowFrequency(player);
        }
    }

    /**
     * 高頻度更新（位置・体力など）
     */
    private void updateHighFrequency(LocalPlayer player) {
        safeUpdate("player.posX", formatNumber(player.getX()));
        safeUpdate("player.posY", formatNumber(player.getY()));
        safeUpdate("player.posZ", formatNumber(player.getZ()));
        safeUpdate("player.health", formatNumber(player.getHealth()));
    }

    /**
     * 中頻度更新（食料・向きなど）
     */
    private void updateMidFrequency(LocalPlayer player) {
        safeUpdate("player.name", player.getName().getString());
        safeUpdate("player.food", String.valueOf(player.getFoodData().getFoodLevel()));
        safeUpdate("player.saturation", formatNumber(player.getFoodData().getSaturationLevel()));
        safeUpdate("player.yaw", formatNumber(player.getYRot()));
        safeUpdate("player.pitch", formatNumber(player.getXRot()));
    }

    /**
     * 低頻度更新（装備・ステータスなど）
     */
    private void updateLowFrequency(LocalPlayer player) {
        safeUpdate("player.max_health", formatNumber(player.getMaxHealth()));
        safeUpdate("player.xp_level", String.valueOf(player.experienceLevel));
        safeUpdate("player.is_flying", String.valueOf(player.getAbilities().flying));
        safeUpdate("player.is_swimming", String.valueOf(player.isSwimming()));

        updateEquipment(player);
    }

    /**
     * 装備情報の更新
     */
    private void updateEquipment(LocalPlayer player) {
        safeUpdate("player.item.mainhand", getItemName(player.getMainHandItem()));
        safeUpdate("player.item.offhand", getItemName(player.getOffhandItem()));
        safeUpdate("player.item.helmet", getItemName(player.getInventory().armor.get(3)));
        safeUpdate("player.item.chestplate", getItemName(player.getInventory().armor.get(2)));
        safeUpdate("player.item.leggings", getItemName(player.getInventory().armor.get(1)));
        safeUpdate("player.item.boots", getItemName(player.getInventory().armor.get(0)));
    }

    /**
     * アイテム名を取得
     */
    private String getItemName(ItemStack stack) {
        return stack.isEmpty() ? "" : stack.getHoverName().getString();
    }

    /**
     * ダメージイベント発生時に体力を即時更新
     */
    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof LocalPlayer player) {
            safeUpdate("player.health", formatNumber(player.getHealth()));
        }
    }
}
