/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.rabbitmq.client.Connection
 *  com.rabbitmq.client.ConnectionFactory
 */
package com.vinplay.vbee.common.rmq;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class RMQConnectionFactory {
    public static String USERNAME = "vinplay";
    public static String PASSWORD = "vinplay@123";
    public static String SERVER_ADR = "localhost";
    public static int SERVER_PORT = 5672;
    public static int RECONNECT_DELAY = 5000;
    public static int CONNECTION_TIMEOUT = 1000;
    public static int HANDSHAKE_TIMEOUT = 1000;

    public static void init(String username, String password, String serverAddr, int port, int reconnectDelay, int conenctionTimeout, int handshakeTimeout) {
        USERNAME = username;
        PASSWORD = password;
        SERVER_ADR = serverAddr;
        SERVER_PORT = port;
        RECONNECT_DELAY = reconnectDelay;
        CONNECTION_TIMEOUT = conenctionTimeout;
        HANDSHAKE_TIMEOUT = handshakeTimeout;
    }

    public static ConnectionFactory newFactory() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername(USERNAME);
        factory.setPassword(PASSWORD);
        factory.setHost(SERVER_ADR);
        factory.setPort(SERVER_PORT);
        factory.setAutomaticRecoveryEnabled(true);
        factory.setTopologyRecoveryEnabled(true);
        factory.setNetworkRecoveryInterval(RECONNECT_DELAY);
        factory.setVirtualHost("/");
        factory.setConnectionTimeout(CONNECTION_TIMEOUT);
        factory.setHandshakeTimeout(HANDSHAKE_TIMEOUT);
        return factory;
    }

    public static Connection newConnection() throws IOException, TimeoutException {
        return RMQConnectionFactory.newFactory().newConnection();
    }
}

