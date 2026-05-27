package com.vinplay.dal.monitoring;

import com.vinplay.vbee.common.pools.ConnectionPool;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.apache.log4j.Logger;

/**
 * Phase 5 prep gate 5p4 — periodic snapshot of HikariCP pool utilization.
 *
 * <p>Static utility (no state) used by
 * {@code com.vinplay.api.backend.perf.ConnectionPoolMonitorScheduler}.
 * One method, {@link #sampleOnce()}, returns the live active/idle/total
 * counts plus utilization% for the user pool ({@code mysqlpoolname}).
 *
 * <p>The pool was bumped from {@code maxpool=10} to {@code maxpool=30} in
 * 5p1; without active monitoring, slow exhaustion (a leaking handler, a
 * stuck Mongo retry chain, a misbehaving query) would only become visible
 * once users started seeing connection-timeout errors. This monitor
 * surfaces that pressure before it becomes user-visible.
 *
 * <p>Defensive contract: never throws. If the named pool is missing or
 * the underlying datasource is not a HikariCP {@link HikariDataSource},
 * returns a placeholder with {@code active = -1} so the caller can
 * WARN-log and skip the tick. The caller's scheduler must keep running.
 */
public final class ConnectionPoolMonitor {

    /** The pool we care about for ledger / wallet traffic. */
    public static final String DEFAULT_POOL_NAME = ConnectionPool.USER_POOL;

    private static final Logger logger = Logger.getLogger("pool-monitor");

    private ConnectionPoolMonitor() {}

    /**
     * Snapshot the default user pool ({@code mysqlpoolname}) once.
     * Equivalent to {@code sampleOnce(DEFAULT_POOL_NAME)}.
     */
    public static SampleResult sampleOnce() {
        return sampleOnce(DEFAULT_POOL_NAME);
    }

    /**
     * Snapshot the named pool once. Returns a placeholder
     * ({@code active = -1}) on any failure path — never throws.
     */
    public static SampleResult sampleOnce(String poolName) {
        try {
            HikariDataSource ds = ConnectionPool.getInstance().getDataSource(poolName);
            if (ds == null) {
                logger.warn("sampleOnce: pool [" + poolName + "] not found — skipping");
                return SampleResult.unavailable(poolName);
            }
            HikariPoolMXBean bean = ds.getHikariPoolMXBean();
            if (bean == null) {
                logger.warn("sampleOnce: pool [" + poolName + "] has no MXBean — skipping");
                return SampleResult.unavailable(poolName);
            }
            int active = bean.getActiveConnections();
            int idle = bean.getIdleConnections();
            int total = bean.getTotalConnections();
            int maxPoolSize = ds.getMaximumPoolSize();
            double utilizationPct = maxPoolSize > 0
                    ? ((double) active / (double) maxPoolSize) * 100.0
                    : 0.0;
            return new SampleResult(poolName, active, idle, total, maxPoolSize, utilizationPct);
        } catch (Throwable t) {
            // Any cast/access failure → placeholder. The scheduler keeps running.
            logger.warn("sampleOnce: pool [" + poolName + "] read failed: " + t.getMessage());
            return SampleResult.unavailable(poolName);
        }
    }

    /**
     * Immutable snapshot of one pool sample.
     *
     * <p>{@code active = -1} signals "couldn't read" — caller should
     * WARN-log and skip alerting logic for this tick.
     */
    public static final class SampleResult {
        public final String poolName;
        public final int active;
        public final int idle;
        public final int total;
        public final int maxPoolSize;
        public final double utilizationPct;

        public SampleResult(String poolName, int active, int idle, int total,
                            int maxPoolSize, double utilizationPct) {
            this.poolName = poolName;
            this.active = active;
            this.idle = idle;
            this.total = total;
            this.maxPoolSize = maxPoolSize;
            this.utilizationPct = utilizationPct;
        }

        static SampleResult unavailable(String poolName) {
            return new SampleResult(poolName, -1, -1, -1, -1, 0.0);
        }

        /** True if {@link #active} is a real reading (i.e. not the -1 sentinel). */
        public boolean isAvailable() {
            return active >= 0;
        }

        @Override
        public String toString() {
            if (!isAvailable()) {
                return "SampleResult{pool=" + poolName + " UNAVAILABLE}";
            }
            return "SampleResult{pool=" + poolName
                    + " active=" + active
                    + " idle=" + idle
                    + " total=" + total
                    + " max=" + maxPoolSize
                    + " util=" + String.format("%.1f", utilizationPct) + "%}";
        }
    }
}
