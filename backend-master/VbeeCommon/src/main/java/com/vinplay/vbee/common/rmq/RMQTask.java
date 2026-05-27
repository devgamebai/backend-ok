package com.vinplay.vbee.common.rmq;

import com.rabbitmq.client.Channel;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Base class for RMQ publish tasks.
 *
 * <p>SUN-1108/1110 follow-up — publisher confirms.
 *
 * <p>Before this change, {@link Channel#basicPublish} was called without
 * any acknowledgement check. The driver would return as soon as the
 * message was queued in its outbound TCP buffer; if the broker dropped
 * the message (network blip between us and RabbitMQ, broker not yet
 * persisted, etc.) the caller had no way to know. For the GSC bet path
 * — where {@link com.vinplay.vbee.common.rmq.RMQApi#publishMessageLogMoney}
 * fires the commission cascade — a silently-lost message means the
 * agent's commission is never credited.
 *
 * <p>With confirms, every channel calls {@link Channel#confirmSelect()}
 * before publish and {@link Channel#waitForConfirmsOrDie(long)} after.
 * If the broker NACKs or times out, the exception bubbles up to the
 * caller — for GSC withdraw that's the Hazelcast transaction in
 * {@code UserServiceImpl.updateMoney}, which rolls back the wallet
 * update. The bet is rejected to GSC and the player is not charged.
 *
 * <p>Toggle: env {@code RMQ_PUBLISHER_CONFIRMS=false} reverts to the
 * pre-fix fire-and-forget mode if confirms misbehave under specific
 * brokers or loads.
 */
public abstract class RMQTask {

    private static final Logger logger = Logger.getLogger("rmq");
    private static final long CONFIRM_TIMEOUT_MS = 5_000L;

    protected String queueName;

    public RMQTask(String queueName) {
        this.queueName = queueName;
    }

    public abstract void run(Channel var1) throws IOException;

    public void start() throws IOException, TimeoutException, InterruptedException {
        RMQPool pool = RMQPool.getInstance();
        Channel channel = null;
        try {
            channel = pool.getChannel(this.queueName);
            boolean confirmsEnabled = isConfirmsEnabled();
            if (confirmsEnabled) {
                // Per-channel: idempotent on already-confirmed channels.
                channel.confirmSelect();
            }
            this.run(channel);
            if (confirmsEnabled) {
                // Throws IOException on broker NACK, TimeoutException on
                // unreachable. Caller (HZ transaction in updateMoney)
                // catches and rolls back the wallet update.
                channel.waitForConfirmsOrDie(CONFIRM_TIMEOUT_MS);
            }
        } finally {
            if (channel != null) {
                try { pool.releaseChannel(channel); } catch (Exception e) {
                    logger.warn("RMQTask.releaseChannel failed: " + e.getMessage());
                }
            }
        }
    }

    private static boolean isConfirmsEnabled() {
        String v = System.getenv("RMQ_PUBLISHER_CONFIRMS");
        if (v == null) return true;
        return !"false".equalsIgnoreCase(v) && !"0".equals(v);
    }
}
