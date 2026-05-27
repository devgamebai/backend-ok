/*
 * Decompiled with CFR 0.144.
 */
package com.vbee.security.config;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class FWConfig {
    private static final String CONFIG_GAME_PATH = "config/firewall.properties";
    private static Properties prop = new Properties();
    public static long RANGE_2_REQUEST = 5000L;
    public static long logLevel = 2L;
    public static boolean enableFW = false;

    public static void readConfig() {
        try {
            FileInputStream input = new FileInputStream(CONFIG_GAME_PATH);
            prop.load(input);
            logLevel = Long.parseLong(prop.getProperty("log_level"));
            RANGE_2_REQUEST = Long.parseLong(prop.getProperty("range_2_request"));
            enableFW = Boolean.parseBoolean(prop.getProperty("enable"));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

