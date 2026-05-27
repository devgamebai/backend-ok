package com.vinplay.vbee.common.messagebus.redis;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

/**
 * Naming conventions for Redis Streams used by the new transport.
 *
 * <h2>Streams &amp; DLQs</h2>
 * Each logical queue gets:
 * <ul>
 *   <li>a primary stream &mdash; {@code "{queueName}.stream"}</li>
 *   <li>a dead-letter stream &mdash; {@code "{queueName}.dlq"} (consumer-side
 *       use; declared here so producer/consumer agree on the convention).</li>
 * </ul>
 *
 * <h2>Hashtag &mdash; cluster safety</h2>
 * The {@code {queueName}} braces are a Redis Cluster <em>hashtag</em>: when
 * Redis hashes a key to a slot it considers only the substring inside the
 * first {@code {...}} pair. By wrapping just the queue name we guarantee that
 * a queue's primary stream and its DLQ always hash to the <em>same</em> slot,
 * which is required for any future multi-key Lua / transaction operations
 * that touch both (e.g. atomic re-queue from DLQ on operator command). Even
 * though we run single-node Redis today, paying the 2-byte tax now keeps the
 * door open for cluster mode without a key-rewrite migration later.
 *
 * <h2>Consumer groups</h2>
 * One group per queue, named {@code "<queueName>.grp"} (no hashtag &mdash;
 * group names are scoped to a stream and are not separate keys). Work-queue
 * semantics: each message delivered to exactly one consumer in the group.
 *
 * <h2>Consumer names</h2>
 * Each JVM gets one stable consumer name: {@code "<host>-<pid>-<uuid>"}.
 * Computed once at static-init so a process keeps the same name across all
 * its consumer-group calls (required by Redis &mdash; XACK / pending-list
 * tracking is keyed on consumer name). The UUID suffix avoids collisions
 * between two JVMs on the same host with the same PID after a container
 * restart loop, which would otherwise inherit each other's pending entries.
 */
public final class StreamNames {

    /** Per-process consumer name. Computed exactly once at class load. */
    private static final String CONSUMER_NAME = computeConsumerName();

    /**
     * Stream key for the given logical queue name.
     *
     * <p>Format: {@code {queueName}.stream}. The braces are a Redis Cluster
     * hashtag &mdash; see class javadoc.
     *
     * @param queueName the logical queue identifier (e.g. {@code "queue_pot"});
     *                  not validated here because callers (the message-bus
     *                  adapters) already validate before calling.
     * @return the stream key as a {@code String}; the adapter encodes it to
     *         UTF-8 bytes when handing it to Lettuce.
     */
    public static String stream(String queueName) {
        return "{" + queueName + "}.stream";
    }

    /**
     * Dead-letter stream key for the given logical queue name.
     *
     * <p>Format: {@code {queueName}.dlq}. Same hashtag as
     * {@link #stream(String)} so primary and DLQ co-locate on the same slot.
     *
     * @param queueName the logical queue identifier.
     * @return the DLQ stream key.
     */
    public static String dlq(String queueName) {
        return "{" + queueName + "}.dlq";
    }

    /**
     * Consumer-group name for the given logical queue.
     *
     * <p>Format: {@code <queueName>.grp}. No hashtag &mdash; consumer-group
     * names are scoped to a single stream key, they are not standalone keys
     * routed by the cluster.
     *
     * @param queueName the logical queue identifier.
     * @return the consumer-group name.
     */
    public static String group(String queueName) {
        return queueName + ".grp";
    }

    /**
     * The stable per-JVM consumer name &mdash; {@code <host>-<pid>-<uuid>}.
     *
     * <p>Computed once at class load and reused for the life of the process.
     * Required by Redis consumer-group bookkeeping: pending-entry-list (PEL)
     * tracking is keyed on the consumer name, so the name must not change
     * across XREADGROUP / XACK calls in the same process.
     *
     * @return the consumer name string.
     */
    public static String consumerName() {
        return CONSUMER_NAME;
    }

    private static String computeConsumerName() {
        String host;
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            host = "unknown-host";
        }
        if (host == null || host.isEmpty()) {
            host = "unknown-host";
        }

        String pid;
        try {
            // ManagementFactory.getRuntimeMXBean().getName() returns
            // "<pid>@<host>" on every Hotspot JVM we run; split on '@' for the
            // pid. (Java 8 has no Process.pid(); ProcessHandle is Java 9+.)
            String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
            int at = runtimeName.indexOf('@');
            pid = (at > 0) ? runtimeName.substring(0, at) : runtimeName;
        } catch (Throwable t) {
            pid = "0";
        }

        // Short UUID suffix — 8 hex chars is plenty to disambiguate restarts
        // on the same host/pid pair without bloating the consumer name.
        String uuidSuffix = UUID.randomUUID().toString().substring(0, 8);
        return host + "-" + pid + "-" + uuidSuffix;
    }

    private StreamNames() {
        // utility class — no instances
    }
}
