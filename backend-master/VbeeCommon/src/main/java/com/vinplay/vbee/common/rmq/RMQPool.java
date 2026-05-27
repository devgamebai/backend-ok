package com.vinplay.vbee.common.rmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.apache.log4j.Logger;

public class RMQPool {
    private static final Logger logger = Logger.getLogger("rmq");
    private Connection connection = RMQConnectionFactory.newConnection();
    private static RMQPool instance;

    private RMQPool() throws IOException, TimeoutException {
    }

    public static RMQPool getInstance() throws IOException, TimeoutException {
        if (instance == null) {
            instance = new RMQPool();
        }
        return instance;
    }

    public Channel getChannel(String queueName) throws IOException {
        Channel channel = this.connection.createChannel();
        try {
            channel.queueDeclarePassive(queueName);
            return channel;
        } catch (IOException e) {
            // Queue doesn't exist — create it WITH DLX so failures route to
            // <queueName>_dlq instead of disappearing. Channel is closed
            // after PRECONDITION_FAILED so re-create.
            channel = this.connection.createChannel();
        }

        String dlqName = queueName + "_dlq";
        try {
            channel.queueDeclare(dlqName, true, false, false, null);
        } catch (Exception dlqErr) {
            channel = this.connection.createChannel();
        }

        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "");
        args.put("x-dead-letter-routing-key", dlqName);
        try {
            channel.queueDeclare(queueName, true, false, false, args);
        } catch (Exception declareErr) {
            // Pre-existing queue without DLX — ops must apply RMQ policy
            // (scripts/rmq-policy-bootstrap.sh) to retrofit. Fall back to
            // passive so producer can still publish.
            logger.warn("Queue " + queueName + " exists without DLX args; "
                + "run scripts/rmq-policy-bootstrap.sh to retrofit. "
                + "Continuing with passive declare.");
            channel = this.connection.createChannel();
            try {
                channel.queueDeclarePassive(queueName);
            } catch (IOException retryErr) {
                channel = this.connection.createChannel();
                channel.queueDeclare(queueName, true, false, false, null);
            }
        }
        return channel;
    }

    public void releaseChannel(Channel channel) throws InterruptedException, IOException, TimeoutException {
        channel.close();
    }
}
