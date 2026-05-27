package game.queue;

import com.rabbitmq.client.*;
import com.vinplay.vbee.common.rmq.RMQConnectionFactory;
import org.apache.log4j.Logger;

/**
 * Lightweight RabbitMQ consumer for queue_action_minigame.
 * Receives NotiGameMessage and delegates to BalanceUpdateProcessor
 * to push real-time balance updates to connected game/lobby clients.
 *
 * Separate from vbee's RMQConsumer — this is self-contained for the
 * Minigame classpath (no dependency on vbee module).
 */
public class BalanceUpdateConsumer {

    private static final Logger logger = Logger.getLogger("game");
    private static final String QUEUE_NAME = "queue_action_minigame";

    private volatile boolean running = false;

    /**
     * Start consuming in a daemon thread. Non-blocking.
     * Uses RMQConnectionFactory credentials (already initialized by RMQApi.start()).
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

                    // Declare queue passively (must already exist)
                    try {
                        channel.queueDeclarePassive(QUEUE_NAME);
                    } catch (Exception e) {
                        // Queue doesn't exist yet — declare it
                        channel = conn.createChannel();
                        channel.queueDeclare(QUEUE_NAME, true, false, false, null);
                    }

                    running = true;
                    logger.info("BalanceUpdateConsumer: started on " + QUEUE_NAME);

                    final BalanceUpdateProcessor processor = new BalanceUpdateProcessor();

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
                                logger.warn("BalanceUpdateConsumer: process error: " + e.getMessage());
                                // Ack anyway to avoid infinite loop — balance push is best-effort
                                try { getChannel().basicAck(envelope.getDeliveryTag(), false); } catch (Exception ackErr) {}
                            }
                        }
                    });

                    // Block this thread — connection recovery handles reconnects
                    while (conn.isOpen()) {
                        Thread.sleep(10000);
                    }
                } catch (Exception e) {
                    logger.error("BalanceUpdateConsumer: connection lost, reconnecting in 5s: " + e.getMessage());
                    running = false;
                    try { Thread.sleep(5000); } catch (InterruptedException ie) { return; }
                }
            }
        }, "balance-update-consumer");
        t.setDaemon(true);
        t.start();
    }
}
