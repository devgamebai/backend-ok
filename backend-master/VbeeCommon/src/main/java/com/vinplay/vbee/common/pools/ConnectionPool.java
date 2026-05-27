package com.vinplay.vbee.common.pools;

import com.vinplay.vbee.common.config.VBeePath;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Centralized database connection pool — Facade over HikariCP.
 *
 * <p>Replaces the legacy snaq.db DBPool 7.0.1 (abandoned 2016) with HikariCP
 * while keeping the exact same external API ({@link #getConnection(String)},
 * {@link #releaseConnection(Connection)}) so zero caller changes are needed
 * across the 1,089 call sites in the codebase.
 *
 * <p>Key improvements over snaq.db:
 * <ul>
 *   <li>Connection validation on borrow ({@code SELECT 1}) — eliminates stale connections
 *   <li>Leak detection (30s threshold) — logs stack trace for zombie connections
 *   <li>maxLifetime (30min) — forces recycling before MySQL wait_timeout
 *   <li>Fail-fast: throws RuntimeException instead of returning null on exhaustion
 *   <li>Thread-safe singleton via double-checked locking + volatile
 *   <li>Per-pool metrics available via {@link #getPoolStats()}
 * </ul>
 *
 * <p>Design patterns: Connection Pool, RAII, Thread-Safe Singleton, Facade, Fail-Fast.
 *
 * @see <a href="docs/DATABASE_CONNECTION_POOL_OPTIMIZATION.md">Full architecture doc</a>
 */
public class ConnectionPool {

    public static final String USER_POOL = "mysqlpoolname";
    public static final String MINIGAME_POOL = "mysqlpool_minigame";
    public static final String ADMIN_POOL = "mysqlpool_admin";
    public static final String GAMEBAI_POOL = "mysqlpool_gamebai";
    public static final String XC_GIFTCODE_POOL = "mysql_xc_giftcode";

    private static final Logger logger = Logger.getLogger("ConnectionPool");
    private static String CONFIG_FILE = "config/db_pool.properties";
    private static final int DEFAULT_TIMEOUT_MS = 5000;
    private static final int DEFAULT_MIN_IDLE = 5;
    private static final int DEFAULT_MAX_POOL = 20;
    private static final long DEFAULT_IDLE_TIMEOUT_MS = 600_000L;   // 10 min
    private static final long MAX_LIFETIME_MS = 1_800_000L;         // 30 min
    private static final long LEAK_DETECTION_MS = 30_000L;          // 30 sec
    private static final long VALIDATION_TIMEOUT_MS = 3000L;        // 3 sec

    private final Map<String, HikariDataSource> pools = new ConcurrentHashMap<>();
    private static volatile ConnectionPool _instance;

    // Touch GameHealthServer so its static initializer fires.
    static {
        try { Class.forName("com.vinplay.vbee.common.utils.GameHealthServer"); }
        catch (Throwable ignored) {}
    }

    /**
     * Thread-safe lazy singleton via double-checked locking.
     */
    public static ConnectionPool getInstance() {
        if (_instance == null) {
            synchronized (ConnectionPool.class) {
                if (_instance == null) {
                    _instance = new ConnectionPool();
                }
            }
        }
        return _instance;
    }

    public static void start(String configFile) {
        CONFIG_FILE = configFile;
    }

    private ConnectionPool() {
        try {
            this.init(CONFIG_FILE);
        } catch (Exception e) {
            logger.error("ConnectionPool init failed", e);
            throw new RuntimeException("ConnectionPool init failed: " + e.getMessage(), e);
        }
    }

    public void init(String configFile) throws Exception {
        Properties prop = new Properties();
        String fullPath = VBeePath.basePath.concat(configFile);
        try (InputStream input = new FileInputStream(fullPath)) {
            prop.load(input);
        }

        Set<String> poolNames = extractPoolNames(prop);
        logger.info("ConnectionPool initializing " + poolNames.size() + " pools from " + fullPath
                + ": " + poolNames);

        for (String poolName : poolNames) {
            String url = prop.getProperty(poolName + ".url");
            String user = prop.getProperty(poolName + ".user");
            String password = prop.getProperty(poolName + ".password");

            if (url == null || url.isEmpty()) {
                logger.warn("Skipping pool " + poolName + " — no URL configured");
                continue;
            }

            HikariConfig hc = new HikariConfig();
            hc.setPoolName(poolName);
            hc.setJdbcUrl(url);
            hc.setUsername(user);
            hc.setPassword(password);

            // Pool sizing
            int minPool = intProp(prop, poolName + ".minpool", DEFAULT_MIN_IDLE);
            int maxPool = intProp(prop, poolName + ".maxpool", DEFAULT_MAX_POOL);
            if (minPool > maxPool) minPool = maxPool;
            hc.setMinimumIdle(minPool);
            hc.setMaximumPoolSize(maxPool);

            // Timeouts
            long idleTimeout = intProp(prop, poolName + ".idleTimeout", 600) * 1000L;
            hc.setIdleTimeout(idleTimeout);
            hc.setConnectionTimeout(DEFAULT_TIMEOUT_MS);
            hc.setMaxLifetime(MAX_LIFETIME_MS);
            hc.setValidationTimeout(VALIDATION_TIMEOUT_MS);

            // Validation — eliminates stale connections
            hc.setConnectionTestQuery("SELECT 1");

            // Leak detection — logs stack trace when connection held > 30s
            hc.setLeakDetectionThreshold(LEAK_DETECTION_MS);

            // Driver (MySQL 8 auto-registers via SPI, but explicit for clarity)
            hc.setDriverClassName("com.mysql.cj.jdbc.Driver");

            // SUN-1xxx (2026-05-12): bypass Hikari's checkFailFast at init.
            //
            // Previously: if MySQL was briefly slow during container startup
            // (handshake EOF, TLS handshake retry, network blip, mysqld
            // mid-restart), new HikariDataSource(hc) would throw inside
            // checkFailFast. Our catch block below logged the error but did
            // NOT register the pool — `pools` stayed empty for that pool name,
            // every subsequent getConnection("mysqlpool_X") returned
            // "Unknown pool: X — available: []", and the container went
            // "silent half-down" until manual restart. BitZero's healthcheck
            // doesn't probe DB so docker reported the container as healthy.
            //
            // Incident on game-minigame 2026-05-12 07:52 UTC (~1948 errors in
            // 10 min, every TaiXiu.init failing). Fixed by force-recreate; this
            // change prevents the next occurrence.
            //
            // With initializationFailTimeout=-1 the pool is registered
            // immediately and Hikari attempts connections in the background.
            // The first real getConnection() call validates connectivity —
            // and if MySQL is genuinely down, the caller gets a normal
            // "Cannot get connection" exception (via the existing
            // connectionTimeout governance), not "Unknown pool". Pool
            // self-heals once MySQL recovers — no container restart needed.
            hc.setInitializationFailTimeout(-1);

            try {
                HikariDataSource ds = new HikariDataSource(hc);
                pools.put(poolName, ds);
                logger.info("Pool [" + poolName + "] initialized: min=" + minPool
                        + " max=" + maxPool + " idleTimeout=" + (idleTimeout/1000) + "s"
                        + " url=" + url.replaceAll("password=[^&]*", "password=***"));
            } catch (Exception e) {
                // With setInitializationFailTimeout(-1) this catch should now
                // only fire for genuinely-bad config (malformed JDBC URL,
                // missing driver, illegal config combo). Log loudly — the
                // pool is unregistered and that DB will be unreachable until
                // the config is fixed and the container restarts.
                logger.error("Failed to create pool [" + poolName + "] — POOL NOT REGISTERED, "
                        + "this DB will be unreachable until the config is fixed and the container is restarted: "
                        + e.getMessage(), e);
            }
        }
    }

    /**
     * Get a connection from the named pool.
     *
     * <p><b>Fail-fast:</b> throws RuntimeException if pool is exhausted or
     * pool name unknown. Never returns null — callers no longer need null checks.
     *
     * <p><b>RAII:</b> always use with try-with-resources:
     * <pre>
     * try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
     *     // use conn
     * }
     * </pre>
     */
    public Connection getConnection(String poolName) {
        HikariDataSource ds = pools.get(poolName);
        if (ds == null) {
            // Fallback: try resolving pool name variants for backward compat
            // Some old code passes "mysqlpool_admin" from a different config
            logger.error("Unknown pool: " + poolName + " — available: " + pools.keySet());
            throw new RuntimeException("Unknown connection pool: " + poolName);
        }
        try {
            return ds.getConnection();
        } catch (SQLException e) {
            logger.error("Pool [" + poolName + "] exhausted or unavailable: " + e.getMessage());
            throw new RuntimeException("Cannot get connection from pool [" + poolName + "]: " + e.getMessage(), e);
        }
    }

    /**
     * Backward-compatible overload with timeout parameter (ignored — HikariCP
     * uses connectionTimeout from config).
     */
    public Connection getConnection(String poolName, long timeoutMs) {
        return getConnection(poolName);
    }

    /**
     * Return a connection to the pool. With HikariCP, {@code conn.close()} on
     * the proxied connection already returns it — this method is a safe no-op
     * wrapper for backward compatibility with legacy callers.
     */
    public static void releaseConnection(Connection conn) {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            logger.warn("releaseConnection error: " + e.getMessage());
        }
    }

    /**
     * Expose the underlying HikariCP {@link HikariDataSource} for monitoring
     * and metrics. Returns {@code null} when the pool name is unknown so
     * callers can WARN-log and skip rather than crash.
     *
     * <p>Used by {@code com.vinplay.dal.monitoring.ConnectionPoolMonitor}
     * (Phase 5 prep gate 5p4) to read {@code getHikariPoolMXBean()} every
     * 30s for utilization alerting. Do not use for {@code getConnection()}
     * — go through {@link #getConnection(String)} so the singleton's
     * fail-fast and leak-detection behavior are preserved.
     */
    public HikariDataSource getDataSource(String poolName) {
        return pools.get(poolName);
    }

    /**
     * Get pool statistics for monitoring ({@code /pool-stats} endpoint).
     * Returns a map of pool name → {active, idle, waiting, total, maxSize}.
     */
    public Map<String, Map<String, Object>> getPoolStats() {
        Map<String, Map<String, Object>> stats = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, HikariDataSource> entry : pools.entrySet()) {
            HikariDataSource ds = entry.getValue();
            Map<String, Object> s = new java.util.LinkedHashMap<>();
            try {
                com.zaxxer.hikari.HikariPoolMXBean bean = ds.getHikariPoolMXBean();
                s.put("active", bean.getActiveConnections());
                s.put("idle", bean.getIdleConnections());
                s.put("waiting", bean.getThreadsAwaitingConnection());
                s.put("total", bean.getTotalConnections());
            } catch (Exception e) {
                s.put("error", e.getMessage());
            }
            s.put("maxSize", ds.getMaximumPoolSize());
            stats.put(entry.getKey(), s);
        }
        return stats;
    }

    /**
     * Graceful shutdown — close all pools. Called on JVM shutdown.
     */
    public void shutdown() {
        for (Map.Entry<String, HikariDataSource> entry : pools.entrySet()) {
            try {
                entry.getValue().close();
                logger.info("Pool [" + entry.getKey() + "] closed");
            } catch (Exception e) {
                logger.warn("Error closing pool [" + entry.getKey() + "]: " + e.getMessage());
            }
        }
        pools.clear();
    }

    /**
     * Parse snaq.db-format property file to extract pool names.
     * Format: multiple {@code name=poolName} lines declare pool names;
     * then {@code poolName.url}, {@code poolName.user}, etc. for each.
     */
    private Set<String> extractPoolNames(Properties prop) {
        Set<String> names = new LinkedHashSet<>();
        // snaq.db format uses repeated "name=xxx" lines
        // Properties only keeps the LAST one, so we also scan for *.url keys
        for (String key : prop.stringPropertyNames()) {
            if (key.endsWith(".url")) {
                names.add(key.substring(0, key.length() - 4));
            }
        }
        // Also check explicit "name" values
        String directName = prop.getProperty("name");
        if (directName != null && !directName.isEmpty()
                && prop.getProperty(directName + ".url") != null) {
            names.add(directName);
        }
        return names;
    }

    private int intProp(Properties prop, String key, int defaultVal) {
        String val = prop.getProperty(key);
        if (val == null || val.trim().isEmpty()) return defaultVal;
        try { return Integer.parseInt(val.trim()); }
        catch (NumberFormatException e) { return defaultVal; }
    }
}
