package game.queue;

import com.rabbitmq.client.*;
import com.vinplay.vbee.common.rmq.RMQConnectionFactory;
import org.apache.log4j.Logger;

/**
 * Self-contained RabbitMQ consumer for {@code queue_force_logout} (SUN-767).
 *
 * <p>Receives {@code ForceLogoutMessage} published by
 * {@code com.vinplay.api.utils.PortalUtils#loginSuccess} (and any future
 * force-logout caller), delegates to {@link ForceLogoutProcessor} which
 * disconnects the target bitzero sessions on this game server.
 *
 * <p>Mirrors {@link BalanceUpdateConsumer} for consistency with the existing
 * Minigame RMQ consumer pattern.
 */
public class ForceLogoutConsumer {

    private static final Logger logger = Logger.getLogger("game");
    private static final String QUEUE_NAME = "queue_force_logout";

    private volatile boolean running = false;

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

                    try {
                        channel.queueDeclarePassive(QUEUE_NAME);
                    } catch (Exception e) {
                        channel = conn.createChannel();
                        channel.queueDeclare(QUEUE_NAME, true, false, false, null);
                    }

                    running = true;
                    logger.info("ForceLogoutConsumer: started on " + QUEUE_NAME);

                    final ForceLogoutProcessor processor = new ForceLogoutProcessor();

                    channel.basicConsume(QUEUE_NAME, false, new DefaultConsumer(channel) {
                        @Override
                        public void handleDelivery(String consumerTag, Envelope envelope,
                                AMQP.BasicProperties properties, byte[] body) throws java.io.IOException {
                            try {
                                com.vinplay.vbee.common.cp.Param<byte[]> p = new com.vinplay.vbee.common.cp.Param<>();
                                p.set(body);
                                processor.execute(p);
                                getChannel().basicAck(envelope.getDeliveryTag(), false);
                            } catch (Exception e) {
                                logger.warn("ForceLogoutConsumer: process error: " + e.getMessage());
                                // Ack to avoid retry storm on poison messages.
                                try { getChannel().basicAck(envelope.getDeliveryTag(), false); } catch (Exception ackErr) {}
                            }
                        }
                    });

                    while (conn.isOpen()) {
                        Thread.sleep(10000);
                    }
                } catch (Exception e) {
                    logger.error("ForceLogoutConsumer: connection lost, reconnecting in 5s: " + e.getMessage());
                    running = false;
                    try { Thread.sleep(5000); } catch (InterruptedException ie) { return; }
                }
            }
        }, "force-logout-consumer");
        t.setDaemon(true);
        t.start();
    }
}
