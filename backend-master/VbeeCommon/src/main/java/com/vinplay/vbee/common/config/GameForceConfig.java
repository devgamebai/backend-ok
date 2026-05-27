package com.vinplay.vbee.common.config;

/**
 * Game force/house-edge configuration loaded from environment variables.
 * All flags default to enabled (house advantage is core to casino operations).
 */
public class GameForceConfig {
    public static final boolean GAME_FORCE_ENABLED =
        Boolean.parseBoolean(System.getenv().getOrDefault("GAME_FORCE_ENABLED", "true"));

    public static final boolean XOCDIA_FORCE_ENABLED =
        Boolean.parseBoolean(System.getenv().getOrDefault("XOCDIA_FORCE_ENABLED", "true"));

    public static final int XOCDIA_DEFAULT_FORCE_TYPE =
        Integer.parseInt(System.getenv().getOrDefault("XOCDIA_DEFAULT_FORCE_TYPE", "-1"));

    public static final boolean BOT_FUND_MANIPULATION_ENABLED =
        Boolean.parseBoolean(System.getenv().getOrDefault("BOT_FUND_MANIPULATION_ENABLED", "true"));

    public static final boolean BOT_AUTO_JOIN_ENABLED =
        Boolean.parseBoolean(System.getenv().getOrDefault("BOT_AUTO_JOIN_ENABLED", "true"));

    public static final long BOT_DEFAULT_BALANCE =
        Long.parseLong(System.getenv().getOrDefault("BOT_DEFAULT_BALANCE", "1000000"));

    public static final int SLOT_RTP_PERCENTAGE =
        Integer.parseInt(System.getenv().getOrDefault("SLOT_RTP_PERCENTAGE", "48"));

    public static final boolean TAIXIU_HOUSE_EDGE_ENABLED =
        Boolean.parseBoolean(System.getenv().getOrDefault("TAIXIU_HOUSE_EDGE_ENABLED", "true"));
}
