package com.vinplay.vbee.common.ledger;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.TimeUnit;

/**
 * Single read API for player balances. Scaffold for the planned migration
 * away from {@code users.vin / users.xu / ...} as the source of truth.
 *
 * <p><b>Status: SCAFFOLD ONLY.</b> No callers yet. Keeping this in the tree
 * so a future engineer can drop into the read-side cutover (Phase B of the
 * Ledger Hardening Roadmap) without bootstrapping. See
 * {@code docs/architecture/LEDGER_HARDENING_ROADMAP.md} fix #9.
 *
 * <p><b>Three-tier read</b> (per the spec's CQRS read-side):
 * <ol>
 *   <li>Hazelcast cache hit ({@code balance:{type}:{userId}:{currency}}) — &lt;1 ms.</li>
 *   <li>{@code money_account.balance} cache column — ~5 ms (still inside DB).</li>
 *   <li>{@code SUM(money_entry.amount * direction-sign)} — ~50 ms cold;
 *       authoritative.</li>
 * </ol>
 *
 * <p>Tier-3 fallback runs only when tier-2 returns NULL (account doesn't
 * exist) or {@link #strictMode} is enabled (development / shadow runs).
 *
 * <p><b>Why this matters:</b> today every game/api reads {@code users.vin}
 * directly. That column is the source of truth, but it drifts silently
 * from the ledger (we have 80 drifted rows as of 2026-05-06). Routing
 * reads through this service lets us:
 * <ul>
 *   <li>Cut over to the ledger as truth without touching every game.</li>
 *   <li>Run shadow-mode comparisons (cache vs ledger sum) and alert on diff.</li>
 *   <li>Eventually delete {@code users.vin} once all writers post through
 *       the ledger SP.</li>
 * </ul>
 *
 * <p>Latency target for tier-1 hit: under 2 ms p99. Tier-2: under 10 ms.
 * Tier-3 should NEVER be the steady-state path.
 */
public final class BalanceService {

    private static final Logger logger = Logger.getLogger("BalanceService");

    private static final String CACHE_NAME = "balance_cache";
    private static final long CACHE_TTL_SECONDS = 60L;

    /** When true, skip tier-2 and always derive from {@code SUM(money_entry)}.
     *  Use ONLY in shadow-mode validation runs — slow and not for hot paths. */
    private static volatile boolean strictMode = false;

    private BalanceService() {}

    public static void setStrictMode(boolean strict) { strictMode = strict; }
    public static boolean isStrictMode() { return strictMode; }

    /** Reads player balance for the given currency. Currency strings:
     *  "vin", "xu", "vp", "safe" — same as the {@code account_type}
     *  suffix on {@link com.vinplay.vbee.common.ledger.MoneyLedger}. */
    public static long get(int userId, String currency) {
        if (userId <= 0 || currency == null) return 0L;
        String accountType = currencyToAccountType(currency);
        if (accountType == null) {
            logger.warn("BalanceService.get: unknown currency=" + currency);
            return 0L;
        }
        String cacheKey = cacheKey(accountType, userId, currency);

        // Tier 1: Hazelcast.
        if (!strictMode) {
            Long cached = readCache(cacheKey);
            if (cached != null) return cached;
        }

        // Tier 2: money_account.balance column.
        Long balanceColumn = readBalanceColumn(userId, accountType, currency);

        // Tier 3: SUM(money_entry) — only if tier 2 is NULL (no account row)
        // OR strictMode is on (forced comparison).
        if (balanceColumn == null || strictMode) {
            Long ledgerSum = readLedgerSum(userId, accountType, currency);
            if (strictMode && balanceColumn != null && !balanceColumn.equals(ledgerSum)) {
                logger.warn("BalanceService strict-mode drift: userId=" + userId
                        + " currency=" + currency
                        + " balance_cached=" + balanceColumn
                        + " ledger_sum=" + ledgerSum);
            }
            if (ledgerSum != null) {
                writeCache(cacheKey, ledgerSum);
                return ledgerSum;
            }
        }

        long value = balanceColumn != null ? balanceColumn : 0L;
        writeCache(cacheKey, value);
        return value;
    }

    /** Invalidate the cache entry for one (userId, currency). Callers MUST
     *  invoke this after any out-of-band balance change (e.g. a legacy
     *  direct-SQL wallet write that bypasses MoneyGateway). Once the
     *  read-side cutover is complete, MoneyGateway will own this. */
    public static void invalidate(int userId, String currency) {
        String accountType = currencyToAccountType(currency);
        if (accountType == null) return;
        String key = cacheKey(accountType, userId, currency);
        try {
            HazelcastInstance hz = HazelcastClientFactory.getInstance();
            if (hz == null) return;
            IMap<String, Long> cache = hz.getMap(CACHE_NAME);
            cache.remove(key);
        } catch (Exception e) {
            logger.warn("BalanceService.invalidate failed key=" + key + " err=" + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Internals
    // ─────────────────────────────────────────────────────────────────────

    private static String currencyToAccountType(String currency) {
        switch (currency.toLowerCase()) {
            case "vin":  return "PLAYER_VIN";
            case "xu":   return "PLAYER_XU";
            case "vp":   return "PLAYER_VP";
            case "safe": return "PLAYER_SAFE";
            default:     return null;
        }
    }

    private static String cacheKey(String accountType, int userId, String currency) {
        return accountType + ":" + userId + ":" + currency.toUpperCase();
    }

    private static Long readCache(String key) {
        try {
            HazelcastInstance hz = HazelcastClientFactory.getInstance();
            if (hz == null) return null;
            IMap<String, Long> cache = hz.getMap(CACHE_NAME);
            return cache.get(key);
        } catch (Exception e) {
            logger.debug("BalanceService.readCache miss/error: " + e.getMessage());
            return null;
        }
    }

    private static void writeCache(String key, long value) {
        try {
            HazelcastInstance hz = HazelcastClientFactory.getInstance();
            if (hz == null) return;
            IMap<String, Long> cache = hz.getMap(CACHE_NAME);
            cache.put(key, value, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.debug("BalanceService.writeCache failed: " + e.getMessage());
        }
    }

    private static Long readBalanceColumn(int userId, String accountType, String currency) {
        String sql = "SELECT balance FROM money_account "
                   + " WHERE owner_user_id = ? AND account_type = ? AND currency = ? "
                   + " LIMIT 1";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, accountType);
            ps.setString(3, currency.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long v = rs.getLong(1);
                    return rs.wasNull() ? null : v;
                }
                return null;
            }
        } catch (Exception e) {
            logger.warn("BalanceService.readBalanceColumn error userId=" + userId
                    + " type=" + accountType + ": " + e.getMessage());
            return null;
        }
    }

    private static Long readLedgerSum(int userId, String accountType, String currency) {
        // Authoritative: SUM(entries) for the account. Cold path; expensive.
        String sql = "SELECT COALESCE(SUM(IF(e.direction='CREDIT', e.amount, -e.amount)), 0) "
                   + "  FROM money_entry e "
                   + "  JOIN money_account a ON a.account_id = e.account_id "
                   + " WHERE a.owner_user_id = ? AND a.account_type = ? AND a.currency = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, accountType);
            ps.setString(3, currency.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        } catch (Exception e) {
            logger.warn("BalanceService.readLedgerSum error userId=" + userId
                    + " type=" + accountType + ": " + e.getMessage());
            return null;
        }
    }
}
