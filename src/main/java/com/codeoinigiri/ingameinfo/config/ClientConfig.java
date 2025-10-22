package com.codeoinigiri.ingameinfo.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ClientConfig {
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ClientConfig INSTANCE;

    public final ForgeConfigSpec.BooleanValue hudEnabled;
    public final ForgeConfigSpec.ConfigValue<String> hudText;
    public final ForgeConfigSpec.IntValue hudColor;
    public final ForgeConfigSpec.EnumValue<HudPosition> hudPosition;
    public final ForgeConfigSpec.IntValue hudOffsetX;
    public final ForgeConfigSpec.IntValue hudOffsetY;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        INSTANCE = new ClientConfig(builder);
        CLIENT_SPEC = builder.build();
    }

    ClientConfig(ForgeConfigSpec.Builder builder) {
        builder.push("hud_settings");

        hudEnabled = builder.comment("HUDを表示するかどうか")
                .define("hudEnabled", true);

        hudText = builder.comment("HUDに表示するテキスト")
                .define("hudText", "サンプルテキスト");

        hudColor = builder.comment("HUDテキストの色 (16進数RGB)")
                .defineInRange("hudColor", 0xFFFFFF, 0x000000, 0xFFFFFF);

        hudPosition = builder.comment("""
                HUDの位置を指定:
                TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT,
                CENTER_LEFT, CENTER_RIGHT, CENTER_TOP, CENTER_BOTTOM
                """)
                .defineEnum("hudPosition", HudPosition.TOP_RIGHT);

        hudOffsetX = builder.comment("HUDのX方向余白（ピクセル）")
                .defineInRange("hudOffsetX", 10, 0, 1000);

        hudOffsetY = builder.comment("HUDのY方向余白（ピクセル）")
                .defineInRange("hudOffsetY", 10, 0, 1000);

        builder.pop();
    }
}
