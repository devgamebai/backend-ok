/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.client.HazelcastClient
 *  com.hazelcast.client.config.ClientConfig
 *  com.hazelcast.client.config.ClientNetworkConfig
 *  com.hazelcast.config.GroupConfig
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.LifecycleService
 */
package com.vinplay.vbee.common.hazelcast;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.ClientNetworkConfig;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.LifecycleEvent;
import com.hazelcast.core.LifecycleService;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HazelcastClientFactory {
    private static final Logger logger = LoggerFactory.getLogger("hazelcast");
    public static String ADDRESS = "127.0.0.1";
    public static String GROUP_NAME = "vinplay";
    public static String GROUP_PASS = "vinplay@123";
    private static HazelcastInstance instance;
    private static ClientConfig cfg;
    private static volatile boolean initialConnectDone = false;

    // Touch GameHealthServer so its static block runs (auto-starts /health
    // listener). Every game server / backend API loads HazelcastClientFactory
    // during bootstrap, making this a universal hook. Card games (lieng, tlmn,
    // sam) that don't touch ConnectionPool directly still load this class.
    static {
        try { Class.forName("com.vinplay.vbee.common.utils.GameHealthServer"); }
        catch (Throwable ignored) {}
    }

    public static void initDefault() {
        ArrayList<String> address = new ArrayList<String>();
        address.add(ADDRESS);
        HazelcastClientFactory.init(address, GROUP_NAME, GROUP_PASS);
    }

    public static void init(List<String> address, String groupName, String groupPassword) {
        GroupConfig groupConfig = new GroupConfig();
        ClientNetworkConfig clientNetworkConfig = new ClientNetworkConfig();
        groupConfig.setName(groupName);
        groupConfig.setPassword(groupPassword);
        cfg.setGroupConfig(groupConfig);
        for (String addr : address) {
            clientNetworkConfig.addAddress(new String[]{addr});
        }
        cfg.setNetworkConfig(clientNetworkConfig);
        instance = HazelcastClient.newHazelcastClient((ClientConfig)cfg);
        registerReconnectListener(instance);
    }

    private static void registerReconnectListener(HazelcastInstance hz) {
        hz.getLifecycleService().addLifecycleListener(event -> {
            if (event.getState() == LifecycleEvent.LifecycleState.CLIENT_CONNECTED) {
                if (!initialConnectDone) {
                    initialConnectDone = true;
                    return;
                }
                logger.info("Hazelcast CLIENT_CONNECTED (reconnect) — reloading GameCommon config");
                try {
                    Class<?> gc = Class.forName("com.vinplay.usercore.utils.GameCommon");
                    gc.getMethod("init").invoke(null);
                    logger.info("GameCommon reloaded successfully after reconnect");
                } catch (ClassNotFoundException e) {
                    logger.debug("GameCommon not on classpath — skipping reload");
                } catch (Exception e) {
                    logger.error("GameCommon reload failed after reconnect: " + e.getMessage(), e);
                }
            }
        });
    }

    public static void reconnect() {
        instance = HazelcastClient.newHazelcastClient((ClientConfig)cfg);
        registerReconnectListener(instance);
    }

    public static HazelcastInstance getInstance() {
        if (!instance.getLifecycleService().isRunning()) {
            HazelcastClientFactory.reconnect();
        }
        return instance;
    }

    static {
        cfg = new ClientConfig();
    }
}

