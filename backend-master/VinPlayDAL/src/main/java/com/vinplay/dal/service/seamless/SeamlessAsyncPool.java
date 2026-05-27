package com.vinplay.dal.service.seamless;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SUN-1236: shared daemon thread pool for non-critical post-fact bookkeeping
 * triggered from seamless-wallet processors (AWC, GSC, future SBO/IBC).
 *
 * <p>Use this for work that should not gate the response time:
 * <ul>
 *   <li>MongoDB history writes ({@code log_awc_bets}, {@code log_gsc_bets})</li>
 *   <li>RMQ commission publish ({@code LogMoneyUserMessage})</li>
 *   <li>Side-effect cache invalidations downstream of MoneyGateway</li>
 * </ul>
 *
 * <p><b>Why a shared pool instead of per-processor pools?</b> AWC's
 * {@code BATCH_ASYNC} pool was added during SXB cert (8 daemon threads).
 * GSC has its own equivalent for similar reasons. As more aggregators land
 * the JVM accumulates duplicate fixed thread pools — wasteful and harder to
 * monitor. One shared pool with explicit naming makes thread dumps and
 * metrics easier to read.
 *
 * <p>Threads are daemon so they don't block JVM shutdown. Naming
 * convention: {@code seamless-async-N} for easy filtering in logs.
 *
 * <p>Sized for the worst-case batch: AWC SXB-22 sends 50-txn batches
 * across 3 players, each per-txn submitting one Mongo + one RMQ task =
 * up to ~100 in-flight tasks. 16 threads handles that with headroom.
 */
public final class SeamlessAsyncPool {

    private static final ExecutorService POOL =
            Executors.newFixedThreadPool(16, r -> {
                Thread t = new Thread(r, "seamless-async");
                t.setDaemon(true);
                return t;
            });

    private SeamlessAsyncPool() {}

    /**
     * Submit a fire-and-forget task. Throwables inside the runnable are
     * NOT rethrown to the caller — caller should wrap in its own try/catch
     * if it needs to log specific context.
     */
    public static void submit(Runnable r) {
        POOL.submit(r);
    }

    /** Return the underlying executor for callers that need futures. */
    public static ExecutorService executor() {
        return POOL;
    }
}
