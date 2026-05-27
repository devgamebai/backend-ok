package com.vinplay.api.ws;

import com.hazelcast.core.IMap;
import com.rabbitmq.client.*;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.NotiGameMessage;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.rmq.RMQConnectionFactory;
import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RabbitMQ consumer for queue_action_portal.
 * Receives NotiGameMessage, reads fresh balance from Hazelcast,
 * and pushes JSON update to the user's WebSocket session.
 *
 * Follows the same pattern as game/Minigame's BalanceUpdateConsumer.
 */
public class PortalBalanceConsumer {

    private static final Logger logger = Logger.getLogger("portal");
    private static final String QUEUE_NAME = "queue_action_portal";

    private volatile boolean running = false;

    /**
     * Start consuming in a daemon thread. Non-blocking.
     * Must be called after RMQApi.start() so RMQConnectionFactory has credentials.
     */
    public void start() {
        if (running) return;
        Thread t = new Thread(() -> {
            while (true) {
                try {
                    ConnectionFactory factory = new ConnectionFactory();
                    factory.setHost(RMQConnectionFactory.SERVER_ADR);
                    factory.setPort(RMQConnectionFactory.SERVER_PORT);
                    factory.setUsername(RMQConnectionFactory.USERNAME);
                    factory.setPassword(RMQConnectionFactory.PASSWORD);
                    factory.setAutomaticRecoveryEnabled(true);
                    factory.setNetworkRecoveryInterval(5000);

                    Connection conn = factory.newConnection();
                    Channel channel = conn.createChannel();
                    channel.basicQos(1);

                    // Declare queue (create if not exists)
                    try {
                        channel.queueDeclarePassive(QUEUE_NAME);
                    } catch (Exception e) {
                        channel = conn.createChannel();
                        channel.queueDeclare(QUEUE_NAME, true, false, false, null);
                    }

                    running = true;
                    logger.info("PortalBalanceConsumer: started on " + QUEUE_NAME);

                    channel.basicConsume(QUEUE_NAME, false, new DefaultConsumer(channel) {
                        @Override
                        public void handleDelivery(String consumerTag, Envelope envelope,
                                AMQP.BasicProperties properties, byte[] body) throws java.io.IOException {
                            try {
                                NotiGameMessage msg = (NotiGameMessage) BaseMessage.fromBytes(body);

                                if (msg.nicknames == null || msg.nicknames.isEmpty()) {
                                    getChannel().basicAck(envelope.getDeliveryTag(), false);
                                    return;
                                }

                                IMap<String, UserCacheModel> userMap =
                                        HazelcastClientFactory.getInstance().getMap("users");

                                long timestamp = System.currentTimeMillis();

                                for (String nickname : msg.nicknames) {
                                    try {
                                        UserCacheModel cache = userMap.get(nickname);
                                        long vin, xu;
                                        if (cache != null) {
                                            vin = cache.getVin();
                                            xu  = cache.getXu();
                                        } else {
                                            // SUN-1219: Hazelcast cache miss (expired or split-brain).
                                            // Fall back to DB so balance_update is still pushed.
                                            // Without this, the client's balance stays at 0 (its
                                            // "loading" placeholder) until the user reloads the page.
                                            long[] dbBalance = queryBalanceFromDb(nickname);
                                            if (dbBalance == null) continue;
                                            vin = dbBalance[0];
                                            xu  = dbBalance[1];
                                        }

                                        String json = "{\"type\":\"balance_update\",\"vin\":"
                                                + vin + ",\"xu\":" + xu
                                                + ",\"timestamp\":" + timestamp + "}";

                                        boolean sent = BalanceWebSocketServlet.sendToUser(nickname, json);
                                        if (sent) {
                                            logger.info("Portal balance push to " + nickname
                                                    + " vin=" + vin + " xu=" + xu);
                                        }
                                    } catch (Exception userErr) {
                                        logger.warn("Portal balance push failed for "
                                                + nickname + ": " + userErr.getMessage());
                                    }
                                }

                                getChannel().basicAck(envelope.getDeliveryTag(), false);
                            } catch (Exception e) {
                                logger.warn("PortalBalanceConsumer: process error: " + e.getMessage());
                                try {
                                    getChannel().basicAck(envelope.getDeliveryTag(), false);
                                } catch (Exception ackErr) { /* best-effort */ }
                            }
                        }
                    });

                    // Block — connection recovery handles reconnects
                    while (conn.isOpen()) {
                        Thread.sleep(10000);
                    }
                } catch (Exception e) {
                    logger.error("PortalBalanceConsumer: connection lost, reconnecting in 5s: "
                            + e.getMessage());
                    running = false;
                    try { Thread.sleep(5000); } catch (InterruptedException ie) { return; }
                }
            }
        }, "portal-balance-consumer");
        t.setDaemon(true);
        t.start();
    }

    private static long[] queryBalanceFromDb(String nickname) {
        try (java.sql.Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT vin, 0 AS xu FROM users WHERE nick_name = ? LIMIT 1")) {
            ps.setString(1, nickname);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new long[]{rs.getLong("vin"), 0L};
            }
        } catch (Exception e) {
            logger.warn("PortalBalanceConsumer: DB fallback failed for " + nickname + ": " + e.getMessage());
        }
        return null;
    }
}
