package com.cdjedai.wide_ae2_terminals;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue COLUMN_COUNT;
    public static final ModConfigSpec.IntValue MAX_COLUMNS;
    public static final ModConfigSpec.IntValue SIDE_MARGIN;

    static {
        var builder = new ModConfigSpec.Builder();
        COLUMN_COUNT = builder
                .comment("Number of storage columns. 0 = adapt to the window width. Never less than 9.")
                .translation(key("columnCount"))
                .defineInRange("columnCount", 0, 0, 100);
        MAX_COLUMNS = builder
                .comment("Upper limit for the adaptive column count (not used when columnCount is set). 0 = unlimited.")
                .translation(key("maxColumns"))
                .defineInRange("maxColumns", 18, 0, 100);
        SIDE_MARGIN = builder
                .comment("Space (in GUI pixels) kept free on each side of the terminal (for JEI and the side buttons) when adapting the column count.")
                .translation(key("sideMargin"))
                .defineInRange("sideMargin", 120, 0, 1000);
        SPEC = builder.build();
    }

    private static String key(String name) {
        return WideAE2Terminals.MODID + ".configuration." + name;
    }
}
