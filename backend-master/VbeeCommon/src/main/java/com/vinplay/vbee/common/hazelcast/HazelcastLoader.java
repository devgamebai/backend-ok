/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.hazelcast;

import com.vinplay.vbee.common.config.VBeePath;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class HazelcastLoader {
    public static void start() throws IOException {
        String configFile = VBeePath.basePath.concat("config/hazelcast.properties");
        Properties prop = new Properties();
        FileInputStream in = new FileInputStream(configFile);
        prop.load(in);
        HazelcastClientFactory.ADDRESS = prop.getProperty("address");
        HazelcastClientFactory.GROUP_NAME = prop.getProperty("group_name");
        HazelcastClientFactory.GROUP_PASS = prop.getProperty("group_pass");
        HazelcastClientFactory.initDefault();
        ((InputStream)in).close();
    }
}

