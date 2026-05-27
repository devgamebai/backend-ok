/*
 * Decompiled with CFR 0.144.
 * 
 * Enhanced with:
 *   - Proper nack when processor returns false (retry/DLQ support)
 *   - In-memory retry counter (x-death headers NOT populated on requeue)
 *   - Dead letter queue (DLQ) routing when max retries exceeded
 *   - Configurable prefetch count
 *   - Non-blocking retry delay via ScheduledExecutorService (no Thread.sleep)
 *   - Periodic cleanup of stale retry counters to prevent memory leak
 */
package com.vinplay.vbee.rmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.vinplay.vbee.common.cp.BaseController;
import com.vinplay.vbee.common.cp.NoCommandRegistered;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.rmq.RMQConnectionFactory;
import com.vinplay.vbee.logger.HandleMessageLogger;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.log4j.Logger;

public class RMQConsumer {
    private static final int DEFAULT_PREFETCH_COUNT = 20;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2000; // 2 second delay between retries
    private static final long RETRY_ENTRY_EXPIRE_MS = 10 * 60 * 1000; // 10 minutes TTL for retry entries
    private static final long CLEANUP_INTERVAL_MIN = 5; // cleanup runs every 5 minutes
    private static final Logger logger = Logger.getLogger((String)"vbee");
    private BaseController<byte[], Boolean> controller;
    private String queueName;
    private int numConsumer = 1;
    private int prefetchCount = DEFAULT_PREFETCH_COUNT;

    /**
     * Scheduled executor for non-blocking retry delays.
     * Instead of Thread.sleep() which blocks consumer threads, we schedule
     * the nack+requeue to happen after a delay on a separate thread.
     */
    private final ScheduledExecutorService retryScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "rmq-retry-scheduler");
        t.setDaemon(true);
        return t;
    });

    /**
     * In-memory retry counter per message with timestamp for expiry.
     * Key: unique message identifier (body hashCode to survive requeue).
     * Value: RetryEntry containing count and creation timestamp.
     * 
     * Why not use x-death header? Because basicNack(requeue=true) does NOT
     * populate x-death — that only happens on dead-lettering (reject without requeue).
     * So we must track retries ourselves.
     *
     * Entries are periodically cleaned up (every 5 min) to prevent memory leak
     * in case consumer crashes before cleanup or messages are silently dropped.
     */
    private final ConcurrentHashMap<String, RetryEntry> retryCounters = new ConcurrentHashMap<String, RetryEntry>();

    /**
     * Retry entry that tracks both the count and creation time.
     * Entries older than RETRY_ENTRY_EXPIRE_MS are eligible for cleanup.
     */
    private static class RetryEntry {
        final AtomicInteger count;
        volatile long lastAccessTime;

        RetryEntry() {
            this.count = new AtomicInteger(0);
            this.lastAccessTime = System.currentTimeMillis();
        }

        int incrementAndGet() {
            this.lastAccessTime = System.currentTimeMillis();
            return count.incrementAndGet();
        }

        boolean isExpired() {
            return (System.currentTimeMillis() - lastAccessTime) > RETRY_ENTRY_EXPIRE_MS;
        }
    }

    public RMQConsumer(String queueName, int numConsumer) {
        this.queueName = queueName;
        if (numConsumer > 1) {
            this.numConsumer = numConsumer;
        }
        // Periodic cleanup of stale retry entries to prevent memory leak.
        // If a message is nacked but consumer crashes before cleanup, entries
        // would remain forever. This task evicts entries older than 10 minutes.
        retryScheduler.scheduleAtFixedRate(this::cleanupStaleRetryEntries,
                CLEANUP_INTERVAL_MIN, CLEANUP_INTERVAL_MIN, TimeUnit.MINUTES);
    }

    public RMQConsumer(String queueName, int numConsumer, int prefetchCount) {
        this(queueName, numConsumer);
        if (prefetchCount > 0) {
            this.prefetchCount = prefetchCount;
        }
    }

    public void start(Map<Integer, String> commandMap) {
        try {
            this.controller = new BaseController();
            for (Map.Entry<Integer, String> entry : commandMap.entrySet()) {
                logger.debug((Object)("  " + entry.getKey() + " - " + entry.getValue()));
            }
            try {
                this.controller.initCommands(commandMap);
            }
            catch (Exception e) {
                e.printStackTrace();
                logger.error((Object)e.getMessage());
            }
            // RabbitMQ may not be accepting connections yet when vbee starts
            // (compose 'depends_on: service_healthy' gates on rabbitmqctl status,
            // which flips true a few seconds BEFORE the AMQP port is ready for
            // authenticated clients). Without a retry loop, the first
            // ConnectException bubbles to the outer catch at line 166, is
            // silently swallowed, and this consumer never binds — container
            // appears healthy while messages pile up. See root-cause note in
            // issue tracker; symptom has recurred after every rebuild.
            //
            // Retry up to 10× @ 3s = 30s window, then exit(1) so Docker's
            // restart policy respawns us once RMQ is genuinely ready.
            Connection connection = null;
            int maxAttempts = 10;
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    connection = RMQConnectionFactory.newConnection();
                    if (attempt > 1) {
                        logger.info((Object)("RMQ connected on attempt " + attempt + " for queue " + this.queueName));
                    }
                    break;
                } catch (Exception connErr) {
                    if (attempt < maxAttempts) {
                        logger.warn((Object)("RMQ connect attempt " + attempt + "/" + maxAttempts
                                + " failed for queue " + this.queueName + ": " + connErr.getMessage()
                                + " — retrying in 3s"));
                        try { Thread.sleep(3000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                    } else {
                        logger.error((Object)("RMQ unreachable after " + maxAttempts + " attempts for queue "
                                + this.queueName + " — exiting JVM so Docker restart policy can retry"));
                        System.exit(1);
                    }
                }
            }
            Channel channel = connection.createChannel();

            // Declare DLQ for this queue (convention: queueName + "_dlq")
            String dlqName = this.queueName + "_dlq";
            try {
                channel.queueDeclare(dlqName, true, false, false, null);
                logger.info((Object)("DLQ declared: " + dlqName));
            } catch (Exception dlqErr) {
                // Channel may be closed after PRECONDITION_FAILED — re-create
                logger.warn((Object)("Could not declare DLQ " + dlqName + ": " + dlqErr.getMessage()));
                try { channel = connection.createChannel(); } catch (Exception ignored) {}
            }

            // Declare main queue with DLQ routing
            Map<String, Object> queueArgs = new HashMap<String, Object>();
            queueArgs.put("x-dead-letter-exchange", "");
            queueArgs.put("x-dead-letter-routing-key", dlqName);
            try {
                channel.queueDeclare(this.queueName, true, false, false, queueArgs);
            } catch (Exception e) {
                // Queue already exists without DLQ args — channel is now closed by RabbitMQ
                logger.warn((Object)("Queue " + this.queueName + " already exists, using passive declaration"));
                try {
                    // Re-create channel (old one is dead after PRECONDITION_FAILED)
                    channel = connection.createChannel();
                    channel.queueDeclarePassive(this.queueName);
                } catch (Exception e2) {
                    // Last resort: re-create channel and declare without special args
                    channel = connection.createChannel();
                    channel.queueDeclare(this.queueName, true, false, false, (Map)null);
                }
            }

            this.run(channel);

        }
        catch (Exception e) {
            e.printStackTrace();
            logger.error((Object)e.getMessage());
        }
    }

    private void run(final Channel channel) throws IOException {
        channel.basicQos(this.prefetchCount);
        DefaultConsumer consumer = new DefaultConsumer(channel){

            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                long startHandleTime = System.currentTimeMillis();
                int command = Integer.valueOf(properties.getMessageId());
                // Generate a stable message key for retry tracking.
                // Use body hash so the same message gets the same key even after requeue.
                final String retryKey = RMQConsumer.this.queueName + ":" + command + ":" + java.util.Arrays.hashCode(body);
                try {
                    logger.debug((Object)(String.valueOf(RMQConsumer.this.queueName) + " HANDLE MESSAGE: " + command));
                    Param p = new Param();
                    p.set((Object)body);
                    Object result = RMQConsumer.this.controller.processCommand(Integer.valueOf(command), p);

                    // Check processor return value for nack support
                    if (result instanceof Boolean && !((Boolean) result)) {
                        // Processor explicitly returned false — nack with retry logic
                        handleNack(channel, envelope, retryKey, command);
                    } else {
                        // Success — ack and clean up retry counter
                        channel.basicAck(envelope.getDeliveryTag(), false);
                        retryCounters.remove(retryKey);
                    }
                }
                catch (NoCommandRegistered e2) {
                    logger.error((Object)("COMMAND " + command + " NOT FOUND IN QUEUE: " + RMQConsumer.this.queueName));
                    // Unknown command — ack to prevent infinite loop
                    channel.basicAck(envelope.getDeliveryTag(), false);
                    retryCounters.remove(retryKey);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    logger.error((Object)(String.valueOf(RMQConsumer.this.queueName) + " HANDLE MESSAGE: " + command + " ERROR: "), (Throwable)e);
                    // Exception — nack with requeue for retry
                    try {
                        handleNack(channel, envelope, retryKey, command);
                    } catch (Exception ackErr) {
                        logger.error((Object)("Error during nack for " + RMQConsumer.this.queueName), (Throwable)ackErr);
                    }
                }
                long endHandleTime = System.currentTimeMillis();
                long handleTime = endHandleTime - startHandleTime;
                HandleMessageLogger.log(RMQConsumer.this.queueName, command, handleTime, new Date());
            }
        };
        for (int i = 0; i < this.numConsumer; ++i) {
            channel.basicConsume(this.queueName, false, (Consumer)consumer);
        }
    }

    /**
     * Handle a failed message: increment retry counter, nack with requeue or reject to DLQ.
     * Uses ScheduledExecutorService for non-blocking retry delay instead of Thread.sleep()
     * to avoid blocking the consumer thread.
     */
    private void handleNack(final Channel channel, final Envelope envelope, final String retryKey, final int command) throws IOException {
        RetryEntry entry = retryCounters.get(retryKey);
        if (entry == null) {
            entry = new RetryEntry();
            RetryEntry existing = retryCounters.putIfAbsent(retryKey, entry);
            if (existing != null) {
                entry = existing;
            }
        }
        int retryCount = entry.incrementAndGet();

        if (retryCount >= MAX_RETRIES) {
            // Max retries exceeded — reject without requeue (goes to DLQ if configured)
            logger.error((Object)(this.queueName + " message REJECTED to DLQ after "
                    + retryCount + " retries, command=" + command));
            channel.basicReject(envelope.getDeliveryTag(), false);
            retryCounters.remove(retryKey); // clean up
        } else {
            // Retry: non-blocking delay then nack with requeue.
            // Uses ScheduledExecutorService instead of Thread.sleep() to free the consumer thread.
            final int currentRetry = retryCount;
            logger.warn((Object)(this.queueName + " message NACK (retry "
                    + currentRetry + "/" + MAX_RETRIES + "), command=" + command
                    + " — scheduling requeue after " + RETRY_DELAY_MS + "ms"));
            retryScheduler.schedule(() -> {
                try {
                    channel.basicNack(envelope.getDeliveryTag(), false, true);
                } catch (Exception e) {
                    logger.error((Object)(queueName + " error during scheduled nack for command=" + command), e);
                }
            }, RETRY_DELAY_MS, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Periodic cleanup of stale retry entries.
     * Removes entries that haven't been accessed for more than RETRY_ENTRY_EXPIRE_MS (10 minutes).
     * This prevents memory leak when messages are nacked but never cleaned up
     * (e.g. consumer crash, message dropped by broker, etc.).
     */
    private void cleanupStaleRetryEntries() {
        try {
            int removed = 0;
            Iterator<Map.Entry<String, RetryEntry>> it = retryCounters.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, RetryEntry> entry = it.next();
                if (entry.getValue().isExpired()) {
                    it.remove();
                    removed++;
                }
            }
            if (removed > 0) {
                logger.info((Object)("RMQConsumer cleanup: removed " + removed + " stale retry entries"));
            }
        } catch (Exception e) {
            logger.warn((Object)"RMQConsumer cleanup error", e);
        }
    }
}
