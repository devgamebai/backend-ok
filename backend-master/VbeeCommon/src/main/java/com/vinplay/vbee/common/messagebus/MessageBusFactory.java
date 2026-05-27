package com.vinplay.vbee.common.messagebus;

import com.vinplay.vbee.common.messagebus.audit.MessageBusAuditWriter;
import com.vinplay.vbee.common.messagebus.dual.DualWriteMessageBus;
import com.vinplay.vbee.common.messagebus.redis.RedisStreamMessageBus;
import com.vinplay.vbee.common.messagebus.rmq.RmqMessageBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Producer-side entry point for the staged RMQ &rarr; Redis Streams migration.
 *
 * <p>Returns the {@link MessageBus} implementation configured for a given
 * logical queue. Producer call sites converted by the M-* tasks do:
 *
 * <pre>
 *     MessageBusFactory.get(queueName).publish(queueName, msg, cmd);
 * </pre>
 *
 * and the resolved transport (RMQ, Redis, or dual-write) depends on the
 * environment variables described below.
 *
 * <h2>Resolution rules</h2>
 * <ol>
 *   <li>Read the per-queue env var
 *       {@code MESSAGE_BUS_<QUEUE_UPPER>}, where {@code <QUEUE_UPPER>} is the
 *       queue name uppercased with every character outside
 *       {@code [A-Z0-9_]} replaced with {@code _}. Examples:
 *       <ul>
 *         <li>{@code queue_payment}            &rarr; {@code MESSAGE_BUS_QUEUE_PAYMENT}</li>
 *         <li>{@code deposit_bank_queue}       &rarr; {@code MESSAGE_BUS_DEPOSIT_BANK_QUEUE}</li>
 *         <li>{@code queue_slotExtend}         &rarr; {@code MESSAGE_BUS_QUEUE_SLOTEXTEND}</li>
 *         <li>{@code queue_log_gsc_bets_async} &rarr; {@code MESSAGE_BUS_QUEUE_LOG_GSC_BETS_ASYNC}</li>
 *         <li>hypothetical {@code queue-foo.bar} &rarr; {@code MESSAGE_BUS_QUEUE_FOO_BAR}</li>
 *       </ul>
 *       Recognized values (case-insensitive): {@code "rmq"}, {@code "redis"},
 *       {@code "dual"}. Anything else (typo, unset, blank) falls through to
 *       step 2.</li>
 *   <li>Fallback: read {@code MESSAGE_BUS_DEFAULT}; if unset or unrecognized,
 *       default to {@code "rmq"} (preserves legacy behavior on any uncovered
 *       call site).</li>
 * </ol>
 *
 * <h2>Caching</h2>
 * The factory caches the resolved bus per queue name forever &mdash; subsequent
 * {@link #get(String)} calls for the same queue return the same instance. The
 * {@code MESSAGE_BUS_<QUEUE>} value is read <strong>once</strong> at first
 * {@code get()} for that queue and is never re-read; the migration plan
 * deliberately rejects live reconfiguration to avoid hot-path-taxing the ~376
 * producer call sites with an {@code System.getenv} call per publish.
 * Operators flip a queue's backend by editing {@code .env} and restarting the
 * producer container.
 *
 * <h2>Lifecycle</h2>
 * {@link #shutdown()} calls {@link MessageBus#close()} on every cached bus and
 * then drops the cache so a subsequent {@link #get(String)} can rebuild from
 * a fresh env (mainly useful in tests). Wiring a JVM shutdown hook that calls
 * {@code shutdown()} is a C-bootstrap concern, not F5's responsibility.
 *
 * <h2>Thread safety</h2>
 * All public methods are {@code synchronized} on the class monitor. The cache
 * is small (one entry per logical queue, &lt; 30 queues across the platform)
 * so contention on the synchronized block is negligible compared with the
 * cost of an actual publish.
 */
public final class MessageBusFactory {

    private static final Logger logger = LoggerFactory.getLogger(MessageBusFactory.class);

    private static final String DEFAULT_ENV_VAR = "MESSAGE_BUS_DEFAULT";
    private static final String DEFAULT_BACKEND = "rmq";

    private static final String VALUE_RMQ = "rmq";
    private static final String VALUE_REDIS = "redis";
    private static final String VALUE_DUAL = "dual";

    /** Per-queue cache: queueName &rarr; resolved bus. */
    private static final Map<String, MessageBus> CACHE = new HashMap<>();

    /** Cached single instances of each underlying bus, lazily built so a JVM
     *  that never publishes to e.g. Redis never opens a Lettuce connection. */
    private static MessageBus sharedRmq;
    private static MessageBus sharedRedis;
    private static MessageBus sharedDual;

    private MessageBusFactory() {
    }

    /**
     * Resolve the {@link MessageBus} bound to {@code queueName} per the rules
     * in this class's javadoc. The first call for a given queue reads the env;
     * every subsequent call returns the cached instance.
     *
     * @param queueName the logical queue name (must be non-null and non-blank);
     *                  not the env-var name &mdash; the factory derives that.
     * @return the resolved bus, never null.
     * @throws IllegalArgumentException if {@code queueName} is null or blank.
     */
    public static synchronized MessageBus get(String queueName) {
        if (queueName == null || queueName.trim().isEmpty()) {
            throw new IllegalArgumentException("queueName must be non-null and non-blank");
        }

        MessageBus cached = CACHE.get(queueName);
        if (cached != null) {
            return cached;
        }

        String resolved = resolveValueForQueue(queueName);
        MessageBus bus = busFor(resolved);
        CACHE.put(queueName, bus);

        logger.info("MessageBusFactory: queue={} resolved to backend={} (env={}, value=\"{}\")",
                queueName, bus.backend(), envVarFor(queueName), resolved);

        return bus;
    }

    /**
     * Close every cached {@link MessageBus} and clear the cache. Idempotent:
     * subsequent calls with no intervening {@link #get(String)} are no-ops.
     *
     * <p>Each {@code close()} is wrapped in its own try/catch so a misbehaving
     * adapter cannot prevent the others from shutting down.
     */
    public static synchronized void shutdown() {
        // Walk the per-queue cache first because that is what the public
        // get() contract returned to callers; the shared singletons are an
        // internal implementation detail and may be referenced by multiple
        // cache entries (so closing them via the cache walk would be a
        // duplicate close — which is fine because MessageBus.close() is
        // documented as idempotent).
        for (Map.Entry<String, MessageBus> e : CACHE.entrySet()) {
            try {
                e.getValue().close();
            } catch (RuntimeException ex) {
                logger.warn("MessageBusFactory.shutdown: close failed for queue={}: {}",
                        e.getKey(), ex.getMessage(), ex);
            }
        }
        CACHE.clear();

        // Also close any shared bus singletons that were instantiated but
        // never bound to a queue (defensive — shouldn't happen, but cheap).
        closeQuietly(sharedRmq, "rmq");
        closeQuietly(sharedRedis, "redis");
        closeQuietly(sharedDual, "dual");
        sharedRmq = null;
        sharedRedis = null;
        sharedDual = null;

        // O1: drain the audit writer's bounded queue + stop the worker.
        // Idempotent; safe to call even if the writer never started (no
        // publish ever happened).
        try {
            MessageBusAuditWriter.getInstance().shutdown();
        } catch (RuntimeException e) {
            logger.warn("MessageBusFactory.shutdown: audit writer shutdown failed: {}",
                    e.getMessage(), e);
        }
    }

    /**
     * Convert a logical queue name to the env-var key used to override its
     * backend. Visible for testing &mdash; the mapping is documented in the
     * class javadoc and exercised by the producer-migration tooling.
     *
     * <p>Pattern: {@code "MESSAGE_BUS_" + queueName.toUpperCase().replaceAll("[^A-Z0-9_]", "_")}.
     *
     * @param queueName the logical queue identifier; null/blank is a
     *                  programmer error and surfaces as
     *                  {@link IllegalArgumentException}.
     * @return the env-var name (never null).
     */
    static String envVarFor(String queueName) {
        if (queueName == null || queueName.trim().isEmpty()) {
            throw new IllegalArgumentException("queueName must be non-null and non-blank");
        }
        return "MESSAGE_BUS_" + queueName.toUpperCase().replaceAll("[^A-Z0-9_]", "_");
    }

    /**
     * Look up the per-queue env var, falling back to {@code MESSAGE_BUS_DEFAULT}
     * and finally to {@code "rmq"}. The returned value is always one of
     * {@code "rmq"}, {@code "redis"}, or {@code "dual"} (lower-cased).
     */
    private static String resolveValueForQueue(String queueName) {
        String perQueue = trimToNull(System.getenv(envVarFor(queueName)));
        if (perQueue != null) {
            String normalized = perQueue.toLowerCase();
            if (isRecognized(normalized)) {
                return normalized;
            }
            logger.warn("MessageBusFactory: ignoring unrecognized value \"{}\" for env={} "
                    + "(expected rmq|redis|dual); falling back to MESSAGE_BUS_DEFAULT",
                    perQueue, envVarFor(queueName));
        }

        String fallback = trimToNull(System.getenv(DEFAULT_ENV_VAR));
        if (fallback != null) {
            String normalized = fallback.toLowerCase();
            if (isRecognized(normalized)) {
                return normalized;
            }
            logger.warn("MessageBusFactory: ignoring unrecognized value \"{}\" for env={} "
                    + "(expected rmq|redis|dual); using built-in default \"rmq\"",
                    fallback, DEFAULT_ENV_VAR);
        }

        return DEFAULT_BACKEND;
    }

    private static boolean isRecognized(String normalizedValue) {
        return VALUE_RMQ.equals(normalizedValue)
                || VALUE_REDIS.equals(normalizedValue)
                || VALUE_DUAL.equals(normalizedValue);
    }

    /**
     * Return (lazily building) the shared bus singleton for a normalized value.
     * Multiple queues that map to the same backend share one underlying
     * adapter, which is correct because the adapters are stateless and
     * thread-safe (the transport-pool state is held in process-wide singletons:
     * {@code RMQPool} / {@code RedisConnectionHolder}).
     */
    private static MessageBus busFor(String normalizedValue) {
        if (VALUE_REDIS.equals(normalizedValue)) {
            if (sharedRedis == null) {
                sharedRedis = new RedisStreamMessageBus();
            }
            return sharedRedis;
        }
        if (VALUE_DUAL.equals(normalizedValue)) {
            if (sharedDual == null) {
                if (sharedRmq == null) {
                    sharedRmq = new RmqMessageBus();
                }
                if (sharedRedis == null) {
                    sharedRedis = new RedisStreamMessageBus();
                }
                sharedDual = new DualWriteMessageBus(sharedRmq, sharedRedis);
            }
            return sharedDual;
        }
        // Default branch: rmq (also the only branch reachable when neither
        // env var was set, per resolveValueForQueue).
        if (sharedRmq == null) {
            sharedRmq = new RmqMessageBus();
        }
        return sharedRmq;
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static void closeQuietly(MessageBus bus, String label) {
        if (bus == null) {
            return;
        }
        try {
            bus.close();
        } catch (RuntimeException e) {
            logger.warn("MessageBusFactory.shutdown: close failed for shared {} bus: {}",
                    label, e.getMessage(), e);
        }
    }
}
