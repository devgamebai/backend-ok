package com.vinplay.vbee.common.messagebus.redis;

import com.vinplay.vbee.common.messagebus.MessageBus;
import com.vinplay.vbee.common.messagebus.MessageBusBackend;
import com.vinplay.vbee.common.messagebus.QueueRouter;
import com.vinplay.vbee.common.messagebus.QueueRouter.Hop;
import com.vinplay.vbee.common.messagebus.audit.MessageBusAuditWriter;
import com.vinplay.vbee.common.messages.BaseMessage;
import io.lettuce.core.RedisException;
import io.lettuce.core.XAddArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisStreamCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Redis-Streams-backed {@link MessageBus} adapter &mdash; the producer side of
 * the new transport.
 *
 * <h2>Wire format</h2>
 * Each {@code XADD} writes a single stream entry with two named fields:
 * <ul>
 *   <li>field name <code>"b"</code> (1 byte, UTF-8) &rarr; the raw bytes of
 *       {@link BaseMessage#toBytes()} (Java {@code ObjectOutputStream}
 *       serialization).</li>
 *   <li>field name <code>"c"</code> (1 byte, UTF-8) &rarr; the legacy
 *       dispatcher command id, encoded as a UTF-8 string of decimal digits
 *       (e.g. {@code 30} &rarr; the bytes {@code 0x33 0x30}).</li>
 * </ul>
 * The consumer side ({@code F4}) reads these by NAME, not by position &mdash;
 * Redis does <em>not</em> guarantee field-iteration order on read, so a
 * positional read would be unstable across server versions.
 *
 * <h2>Stream naming</h2>
 * Stream keys come from {@link StreamNames#stream(String)} which wraps the
 * queue name in a Redis Cluster hashtag (so primary stream and DLQ co-locate
 * on the same slot). See {@link StreamNames} for full naming conventions.
 *
 * <h2>Routing</h2>
 * {@link #publish} delegates to {@link QueueRouter#route(String, int)} so
 * cmd-keyed payment-queue selection and the {@code queue_log_money} triple
 * fan-out behave identically to the RMQ adapter. This means the producer
 * call site does NOT need to know whether it is publishing to RMQ or Redis
 * &mdash; the routing decision is the same on both backends.
 *
 * <h2>Trimming</h2>
 * Each {@code XADD} carries {@code MAXLEN ~ 200_000} (approximate trimming).
 * The {@code ~} is mandatory for throughput: exact trimming forces a rebalance
 * of the radix-tree index on every write and tanks throughput by 10x+ at
 * scale. Approximate trimming caps the stream at "around" the limit (within
 * a node-block boundary, ~100 entries slack) which is fine for an async
 * work-queue where consumers acknowledge with XACK and trimming is just a
 * safety net against unbounded growth from a stuck consumer.
 *
 * <h2>Failure semantics</h2>
 * Per {@link MessageBus#publish}, transport-level errors (Redis unreachable,
 * AUTH failure, OOM on the server, connection drop mid-XADD, etc.) are
 * caught, logged at WARN, and swallowed. They are NOT thrown to the caller
 * &mdash; producers are fire-and-forget. Per-hop failures do not abort the
 * remaining hops in a fan-out.
 *
 * <h2>Thread safety</h2>
 * Stateless beyond the constants. Lettuce's
 * {@link StatefulRedisConnection} (held by {@link RedisConnectionHolder}) is
 * thread-safe by design, so concurrent {@code publish} calls share the one
 * multiplexed connection without external synchronization.
 *
 * <h2>Lifecycle</h2>
 * {@link #close()} delegates to {@link RedisConnectionHolder#shutdown()}.
 * {@link RedisConnectionHolder} is process-wide, so calling close on one
 * adapter instance tears down the connection for all of them. The factory
 * (F5) calls this from a single JVM shutdown hook; application code should
 * not call it directly.
 */
public final class RedisStreamMessageBus implements MessageBus {

    private static final Logger logger = LoggerFactory.getLogger(RedisStreamMessageBus.class);

    /** MAXLEN cap per stream. ~200k entries is roughly 10 minutes of peak
     *  payment traffic; lets a stuck consumer recover without losing days
     *  of backlog while still bounding RAM. */
    private static final long DEFAULT_MAXLEN = 200_000L;

    /** Field name carrying {@link BaseMessage#toBytes()}. */
    private static final byte[] FIELD_BODY = "b".getBytes(StandardCharsets.UTF_8);

    /** Field name carrying the command id as UTF-8 decimal digits. */
    private static final byte[] FIELD_COMMAND = "c".getBytes(StandardCharsets.UTF_8);

    public RedisStreamMessageBus() {
    }

    @Override
    public void publish(String queueName, BaseMessage message, int command) {
        if (message == null) {
            throw new IllegalArgumentException("message must be non-null");
        }
        if (queueName == null || queueName.trim().isEmpty()) {
            throw new IllegalArgumentException("queueName must be non-null and non-blank");
        }

        // Reuse F2's router so cmd→queue rewrites stay identical between
        // backends. A queue rerouted at the producer level (cmd 16 →
        // queue_payment_minigame, cmd 30) lands on the
        // `{queue_payment_minigame}.stream` stream with cmd 30, not on the
        // sentinel `queue_payment` stream.
        List<Hop> hops = QueueRouter.route(queueName, command);

        // Serialize once; reuse across all fan-out hops.
        byte[] body = message.toBytes();

        for (Hop hop : hops) {
            byte[] streamKey = StreamNames.stream(hop.queueName).getBytes(StandardCharsets.UTF_8);
            byte[] cmdBytes = Integer.toString(hop.command).getBytes(StandardCharsets.UTF_8);

            // 2-field map: { "b" → body, "c" → cmd }.
            // Order doesn't matter on the wire — consumer reads by name.
            Map<byte[], byte[]> entry = new HashMap<>(4);
            entry.put(FIELD_BODY, body);
            entry.put(FIELD_COMMAND, cmdBytes);

            XAddArgs args = XAddArgs.Builder.maxlen(DEFAULT_MAXLEN).approximateTrimming();

            boolean success = false;
            try {
                StatefulRedisConnection<byte[], byte[]> conn = RedisConnectionHolder.get();
                RedisStreamCommands<byte[], byte[]> commands = conn.sync();
                commands.xadd(streamKey, args, entry);
                success = true;
            } catch (RedisException e) {
                // Covers RedisConnectionException, RedisCommandException,
                // RedisCommandTimeoutException — all subclasses of
                // RedisException. Log and swallow per MessageBus contract.
                logger.warn("RedisStreamMessageBus.publish failed queue={} cmd={}: {}",
                        hop.queueName, hop.command, e.getMessage());
            } catch (RuntimeException e) {
                // Defensive — unexpected error from connection holder (e.g.
                // env misconfiguration) should not break a fire-and-forget
                // producer call site.
                logger.warn("RedisStreamMessageBus.publish unexpected error queue={} cmd={}: {}",
                        hop.queueName, hop.command, e.getMessage(), e);
            } finally {
                // O1: record the publish outcome to the message_bus_audit
                // queue. Best-effort; the writer drops on overflow and never
                // blocks. Records the post-routing hop so a queue rerouted
                // by QueueRouter is audited under its actual destination —
                // matches what the RMQ adapter records on the same publish.
                MessageBusAuditWriter.getInstance()
                        .record(hop.queueName, hop.command, MessageBusBackend.REDIS, success);
            }
        }
    }

    @Override
    public void publishOrThrow(String queueName, BaseMessage message, int command) throws Exception {
        if (message == null) {
            throw new IllegalArgumentException("message must be non-null");
        }
        if (queueName == null || queueName.trim().isEmpty()) {
            throw new IllegalArgumentException("queueName must be non-null and non-blank");
        }
        List<Hop> hops = QueueRouter.route(queueName, command);
        byte[] body = message.toBytes();
        for (Hop hop : hops) {
            byte[] streamKey = StreamNames.stream(hop.queueName).getBytes(StandardCharsets.UTF_8);
            byte[] cmdBytes = Integer.toString(hop.command).getBytes(StandardCharsets.UTF_8);
            Map<byte[], byte[]> entry = new HashMap<>(4);
            entry.put(FIELD_BODY, body);
            entry.put(FIELD_COMMAND, cmdBytes);
            XAddArgs args = XAddArgs.Builder.maxlen(DEFAULT_MAXLEN).approximateTrimming();
            boolean success = false;
            try {
                StatefulRedisConnection<byte[], byte[]> conn = RedisConnectionHolder.get();
                RedisStreamCommands<byte[], byte[]> commands = conn.sync();
                commands.xadd(streamKey, args, entry);
                success = true;
            } finally {
                MessageBusAuditWriter.getInstance()
                        .record(hop.queueName, hop.command, MessageBusBackend.REDIS, success);
            }
        }
    }

    @Override
    public MessageBusBackend backend() {
        return MessageBusBackend.REDIS;
    }

    @Override
    public void close() {
        RedisConnectionHolder.shutdown();
    }
}
