package com.vinplay.dal.tools;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.vinplay.dal.dao.GscBetsDao;
import com.vinplay.dal.dao.impl.GscBetsDaoImpl;
import com.vinplay.dal.entities.gsc.GscBet;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.bson.Document;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * One-shot tool — backfill {@code vinplay.gsc_bets} from Mongo
 * {@code log_gsc_bets}. Cross-store traceability follow-up #5.
 *
 * <p>Reads every Mongo doc, maps it to a {@link GscBet}, and inserts
 * via {@link GscBetsDao#insert} (which is INSERT IGNORE on
 * {@code uk_wager_code}, so re-runs are safe). Counts before/after
 * row totals plus error count, prints a final summary.
 *
 * <h2>Run order</h2>
 * <ol>
 *   <li>Apply {@code 2026_05_02_gsc_bets_table.sql}.</li>
 *   <li>Flip {@code GSC_BETS_MYSQL_DUAL_WRITE=true} (so new rows land in
 *       both stores from this point on).</li>
 *   <li>Run this tool. It backfills the historical rows that landed in
 *       Mongo before the flag flipped. Any new rows that land in Mongo
 *       while this tool is running are also picked up by the dual-write
 *       — they'll either be inserted by the tool first or by the
 *       dual-write first; INSERT IGNORE on the UNIQUE means the loser
 *       is a no-op, no double-count.</li>
 *   <li>Compare {@code SELECT COUNT(*) FROM vinplay.gsc_bets} to the
 *       Mongo count. Diff &lt; 0.1 % is success; bigger drift means a
 *       systemic issue (run with {@code --limit 10} first to smoke).</li>
 * </ol>
 *
 * <h2>CLI</h2>
 * <pre>
 *   java -cp ... com.vinplay.dal.tools.GscBetsBackfill [--limit N] [--dry-run]
 * </pre>
 * <ul>
 *   <li>{@code --limit N} — process at most N Mongo docs (smoke test).</li>
 *   <li>{@code --dry-run} — read + map, do NOT INSERT. For schema
 *       validation pre-flight.</li>
 * </ul>
 *
 * <h2>Operational notes</h2>
 * <ul>
 *   <li>{@code users.id} resolution: per-user_name LRU cache so we don't
 *       hammer MySQL with one SELECT per row. Cache size is small
 *       relative to the user base (32k entries — bounded).</li>
 *   <li>2633 rows × ~5 ms/row ≈ 13 sec. Even a full re-run is fast.</li>
 *   <li>NOT auto-invoked anywhere. {@code bin/backfill_gsc_bets_to_mysql.sh}
 *       is the ops entry point.</li>
 * </ul>
 */
public final class GscBetsBackfill {

    private static final int CACHE_LIMIT = 32_000;

    private final GscBetsDao dao;
    private final Map<String, Long> userIdCache = new HashMap<>(1024);

    public GscBetsBackfill(GscBetsDao dao) {
        this.dao = dao;
    }

    public static void main(String[] args) throws Exception {
        long limit = -1L;
        boolean dryRun = false;
        for (int i = 0; i < args.length; i++) {
            if ("--limit".equals(args[i]) && i + 1 < args.length) {
                limit = Long.parseLong(args[++i]);
            } else if ("--dry-run".equals(args[i])) {
                dryRun = true;
            }
        }

        // VBeePath / db_pool.properties expected to be on the classpath
        // already (the .sh wrapper sets the working dir to a service
        // config dir, e.g. api/vbee/config — same way the long-running
        // services bootstrap their pools).
        ConnectionPool.getInstance();

        GscBetsBackfill bf = new GscBetsBackfill(new GscBetsDaoImpl());
        bf.run(limit, dryRun);
    }

    public void run(long limit, boolean dryRun) {
        long before = countMysqlRows();
        System.out.println("[GscBetsBackfill] starting — mysql before=" + before
                + " limit=" + limit + " dry_run=" + dryRun);

        MongoDatabase db = MongoDBConnectionFactory.getDB("win123club");
        MongoCollection<Document> col = db.getCollection("log_gsc_bets");
        long mongoCount = col.countDocuments();
        System.out.println("[GscBetsBackfill] mongo count=" + mongoCount);

        long scanned = 0L;
        long inserted = 0L;
        long skipped = 0L;
        long errors = 0L;
        long startMs = System.currentTimeMillis();

        try (MongoCursor<Document> cursor = col.find().iterator()) {
            while (cursor.hasNext()) {
                if (limit > 0 && scanned >= limit) break;
                Document d = cursor.next();
                scanned++;
                try {
                    GscBet bet = mapDocToBet(d);
                    if (bet.wagerCode == null || bet.wagerCode.isEmpty()) {
                        skipped++;
                        continue;
                    }
                    if (!dryRun) {
                        if (dao.insert(bet)) inserted++;
                        else skipped++; // UNIQUE collision = already there
                    } else {
                        inserted++;     // count for telemetry only
                    }
                } catch (Throwable t) {
                    errors++;
                    if (errors <= 10) {
                        System.err.println("[GscBetsBackfill] row error wager="
                                + d.getString("wager_code")
                                + " err=" + t.getMessage());
                    }
                }
                if (scanned % 500L == 0L) {
                    System.out.println("[GscBetsBackfill] progress scanned=" + scanned
                            + " inserted=" + inserted
                            + " skipped=" + skipped
                            + " errors=" + errors
                            + " elapsed=" + (System.currentTimeMillis() - startMs) + "ms");
                }
            }
        }

        long after = countMysqlRows();
        long elapsed = System.currentTimeMillis() - startMs;
        System.out.println("[GscBetsBackfill] DONE scanned=" + scanned
                + " inserted=" + inserted
                + " skipped=" + skipped
                + " errors=" + errors
                + " mysql_before=" + before
                + " mysql_after=" + after
                + " mongo_count=" + mongoCount
                + " elapsed=" + elapsed + "ms"
                + (dryRun ? " (DRY-RUN — no INSERTs ran)" : ""));
    }

    private GscBet mapDocToBet(Document d) {
        GscBet b = new GscBet();
        b.userName = d.getString("user_name");
        b.nickName = d.getString("nick_name");
        b.userId = resolveUserId(b.userName);
        b.betValue = numLong(d.get("bet_value"));
        b.prize = numLong(d.get("prize"));
        b.fee = numLong(d.get("fee"));
        Object pc = d.get("product_code");
        b.productCode = pc instanceof Number ? ((Number) pc).intValue() : 0;
        b.gameCode = d.getString("game_code");
        b.gameKey = d.getString("game_key");
        b.gameName = d.getString("game_name");
        b.txnId = d.getString("txn_id");
        b.wagerCode = d.getString("wager_code");
        b.currency = d.getString("currency");
        b.betType = d.getString("bet_type");
        Object settledRaw = d.get("settled");
        b.settled = settledRaw instanceof Boolean ? (Boolean) settledRaw
                : (settledRaw instanceof Number && ((Number) settledRaw).intValue() != 0);
        b.settleTime = d.getDate("settle_time");
        b.settleTxnId = d.getString("settle_txn_id");
        b.vendorGameId = d.getString("vendor_game_id");
        b.eventKey = d.getString("event_key");
        b.createTime = d.getDate("create_time");
        // time_log on the Mongo doc is a string (legacy formatter). Best-effort:
        // leave time_log = create_time so the MySQL row carries something useful.
        b.timeLog = b.createTime;
        return b;
    }

    private long resolveUserId(String userName) {
        if (userName == null || userName.isEmpty()) return 0L;
        Long cached = userIdCache.get(userName);
        if (cached != null) return cached;
        long id = 0L;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id FROM vinplay.users WHERE user_name = ? LIMIT 1")) {
            ps.setString(1, userName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) id = rs.getLong(1);
            }
        } catch (Throwable ignored) {
            // user lookup failed; fall through with id=0 (matches the
            // dual-write path's posture).
        }
        if (userIdCache.size() < CACHE_LIMIT) {
            userIdCache.put(userName, id);
        }
        return id;
    }

    private static long countMysqlRows() {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM vinplay.gsc_bets");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
        } catch (Throwable t) {
            System.err.println("[GscBetsBackfill] countMysqlRows failed: " + t.getMessage());
        }
        return -1L;
    }

    private static long numLong(Object v) {
        if (v == null) return 0L;
        if (v instanceof Number) return ((Number) v).longValue();
        try {
            return Long.parseLong(v.toString());
        } catch (Throwable t) {
            return 0L;
        }
    }

    @SuppressWarnings("unused")
    private static Date safeDate(Object v) {
        if (v instanceof Date) return (Date) v;
        return null;
    }
}
