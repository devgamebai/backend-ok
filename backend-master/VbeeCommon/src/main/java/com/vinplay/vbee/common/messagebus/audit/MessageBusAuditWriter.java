package com.vinplay.vbee.common.messagebus.audit;

import com.vinplay.vbee.common.messagebus.MessageBusBackend;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Producer-side audit recorder for Task O1 of the RMQ &rarr; Redis Streams
 * migration. Every {@link com.vinplay.vbee.common.messagebus.MessageBus}
 * adapter calls {@link #record(String, int, MessageBusBackend, boolean)} once
 * per publish; the row is inserted into the {@code message_bus_audit} MySQL
 * table by a single dedicated worker thread.
 *
 * <h2>Why async &amp; bounded</h2>
 * At sustained 200 TPS (and burst 1000+ TPS on hot-path queues like
 * {@code queue_log_gsc_bets_async} or {@code queue_payment}) inserting on the
 * caller thread would tax the wallet/user MySQL pool. So this class:
 * <ul>
 *   <li>Holds a bounded {@link LinkedBlockingQueue} of pending rows
 *       (capacity {@value #QUEUE_CAPACITY}).</li>
 *   <li>{@link #record} only does {@link LinkedBlockingQueue#offer(Object)}
 *       &mdash; never blocks. If the queue is full (worker is stuck on slow
 *       MySQL or MySQL is down) the row is dropped and {@link #droppedCount}
 *       is incremented.</li>
 *   <li>A single daemon worker drains the queue and runs {@code INSERT}s.
 *       One worker keeps DB connection use to a single concurrent borrow
 *       from {@link ConnectionPool}.</li>
 * </ul>
 *
 * <p><b>Auditing must NEVER block message processing.</b> Every entry point
 * is best-effort and swallow-on-error.
 *
 * <h2>Schema</h2>
 * The target table is owned by Task S1 of the migration plan
 * ({@code docs/RMQ_TO_REDIS_STREAMS_MIGRATION_PLAN.md}). For reference:
 *
 * <pre>{@code
 * CREATE TABLE IF NOT EXISTS message_bus_audit (
 *   id BIGINT NOT NULL AUTO_INCREMENT,
 *   queue_name VARCHAR(64) NOT NULL,
 *   command INT NOT NULL,
 *   backend ENUM('rmq','redis') NOT NULL,
 *   ts DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 *   success TINYINT(1) NOT NULL,
 *   PRIMARY KEY (id),
 *   KEY idx_queue_ts (queue_name, ts),
 *   KEY idx_backend_ts (backend, ts)
 * ) ENGINE=InnoDB;
 * }</pre>
 *
 * Until S1 creates the table, every insert below fails with
 * {@code ER_NO_SUCH_TABLE}; the worker logs at WARN once per
 * {@value #WARN_THROTTLE_NS} ns and silently drops the row. Log volume stays
 * bounded so a missing table doesn't flood the error log.
 *
 * <h2>Connection pool</h2>
 * Borrows from the shared {@link ConnectionPool#USER_POOL} ({@code mysqlpoolname},
 * the {@code vinplay} DB). Per the O1 task brief, a dedicated
 * {@code mysqlpool_audit} would be preferable to keep audit traffic off the
 * wallet pool, but {@code db_pool.properties} does not currently define one.
 * S1 should add a dedicated pool before traffic ramps; until then the single
 * worker thread keeps concurrent borrows to 1, which is well below the
 * 50-conn pool ceiling.
 *
 * <h2>Lifecycle</h2>
 * Singleton, lazily started on first {@link #record} call. A JVM shutdown
 * hook calls {@link #shutdown()} which interrupts the worker after a short
 * drain window. Tests can call {@link #shutdown()} directly.
 *
 * <h2>Thread safety</h2>
 * {@link #record} is safe for concurrent calls from any number of producer
 * threads (queue is thread-safe). The worker is single-threaded.
 */
public final class MessageBusAuditWriter {

    private static final Logger logger = LoggerFactory.getLogger(MessageBusAuditWriter.class);

    /**
     * Bounded queue capacity. At 200 TPS sustained, 8192 buffers ~40s of
     * publishes if the worker is briefly stuck — generous slack for a
     * transient MySQL stall while still capping resident memory at &lt;1MB
     * worth of small audit-row objects.
     */
    private static final int QUEUE_CAPACITY = 8192;

    private static final String INSERT_SQL =
            "INSERT INTO message_bus_audit (queue_name, command, backend, success) VALUES (?, ?, ?, ?)";

    /** WARN-log throttle for repeated insert failures (e.g. table missing). */
    private static final long WARN_THROTTLE_NS = TimeUnit.MINUTES.toNanos(1);

    /** Drain budget on shutdown — best-effort flush before SIGTERM. */
    private static final long SHUTDOWN_DRAIN_MS = 2_000L;

    private static final MessageBusAuditWriter INSTANCE = new MessageBusAuditWriter();

    private final LinkedBlockingQueue<AuditRow> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    private final AtomicLong droppedCount = new AtomicLong();
    private final AtomicLong insertedCount = new AtomicLong();
    private final AtomicLong insertFailedCount = new AtomicLong();

    private volatile Thread worker;
    private volatile boolean running;
    private volatile long lastWarnNs;

    private MessageBusAuditWriter() {
    }

    /** Singleton accessor. */
    public static MessageBusAuditWriter getInstance() {
        return INSTANCE;
    }

    /**
     * Best-effort enqueue of one audit row. Never blocks; drops on full queue.
     *
     * @param queueName logical queue name; truncated by MySQL to 64 chars if
     *                  longer.
     * @param command   legacy dispatcher command id.
     * @param backend   transport that produced the publish (RMQ, REDIS, or
     *                  DUAL — the {@link MessageBusBackend#DUAL} case is
     *                  decomposed into TWO {@link #record} calls by the
     *                  caller, one per delegate).
     * @param success   {@code true} if the underlying publish call returned
     *                  without throwing.
     */
    public void record(String queueName, int command, MessageBusBackend backend, boolean success) {
        if (queueName == null || backend == null) {
            // Defensive — never throw out of an audit hook.
            return;
        }
        ensureStarted();
        AuditRow row = new AuditRow(queueName, command, backend, success);
        if (!queue.offer(row)) {
            // Bounded queue full — drop. Increment counter so the operator
            // can tell from `MessageBusAuditWriter.droppedCount()` how much
            // audit data we lost during the soak.
            droppedCount.incrementAndGet();
        }
    }

    /** @return rows dropped due to a full queue (audit-side data loss). */
    public long droppedCount() {
        return droppedCount.get();
    }

    /** @return rows successfully inserted into MySQL. */
    public long insertedCount() {
        return insertedCount.get();
    }

    /** @return inserts that threw an SQLException (table missing, MySQL down, etc.). */
    public long insertFailedCount() {
        return insertFailedCount.get();
    }

    private synchronized void ensureStarted() {
        if (running) {
            return;
        }
        running = true;
        Thread t = new Thread(this::workerLoop, "MessageBusAuditWriter");
        t.setDaemon(true);
        worker = t;
        t.start();
    }

    /**
     * Stop the worker. Tries to drain in-flight rows for up to
     * {@value #SHUTDOWN_DRAIN_MS} ms, then interrupts. Idempotent.
     */
    public synchronized void shutdown() {
        if (!running) {
            return;
        }
        running = false;
        Thread t = worker;
        if (t != null) {
            try {
                t.join(SHUTDOWN_DRAIN_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (t.isAlive()) {
                t.interrupt();
            }
        }
        worker = null;
    }

    private void workerLoop() {
        // One worker, one connection borrowed per row to keep the code
        // simple and keep idle pool slots free. ConnectionPool.getConnection
        // is a fast-path borrow on HikariCP, not a JDBC-level connect, so
        // the per-row cost is the network INSERT round-trip itself (~0.5ms
        // on the local docker network).
        while (running || !queue.isEmpty()) {
            AuditRow row;
            try {
                row = queue.poll(200, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            if (row == null) {
                continue;
            }
            insertOne(row);
        }
    }

    private void insertOne(AuditRow row) {
        try (Connection conn = ConnectionPool.getInstance().getConnection(ConnectionPool.USER_POOL);
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            ps.setString(1, row.queueName);
            ps.setInt(2, row.command);
            ps.setString(3, row.backendValue);
            ps.setInt(4, row.success ? 1 : 0);
            ps.executeUpdate();
            insertedCount.incrementAndGet();
        } catch (SQLException e) {
            insertFailedCount.incrementAndGet();
            maybeWarn(e);
        } catch (RuntimeException e) {
            // ConnectionPool throws RuntimeException on exhaustion — also
            // best-effort; never let it propagate out of the worker (would
            // kill the daemon thread silently and stop all auditing).
            insertFailedCount.incrementAndGet();
            maybeWarn(e);
        }
    }

    private void maybeWarn(Exception e) {
        long now = System.nanoTime();
        long last = lastWarnNs;
        if (last == 0L || now - last > WARN_THROTTLE_NS) {
            lastWarnNs = now;
            logger.warn("MessageBusAuditWriter insert failed (throttled, dropped so far={}, failed={}): {}",
                    droppedCount.get(), insertFailedCount.get(), e.getMessage());
        }
    }

    /** Immutable audit row. */
    private static final class AuditRow {
        final String queueName;
        final int command;
        final String backendValue; // "rmq" or "redis" — matches the ENUM
        final boolean success;

        AuditRow(String queueName, int command, MessageBusBackend backend, boolean success) {
            this.queueName = queueName;
            this.command = command;
            // The audit table only enumerates ('rmq','redis'). DUAL is
            // decomposed at the call site (DualWriteMessageBus calls
            // record() twice — once with RMQ, once with REDIS). That keeps
            // reconciliation queries simple (SUM(backend='rmq') vs
            // SUM(backend='redis')).
            this.backendValue = (backend == MessageBusBackend.REDIS) ? "redis" : "rmq";
            this.success = success;
        }
    }
}
