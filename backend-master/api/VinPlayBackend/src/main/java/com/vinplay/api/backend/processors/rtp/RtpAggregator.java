package com.vinplay.api.backend.processors.rtp;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.bson.Document;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * RTP aggregator — rolls per-round game logs in Mongo into
 * user_pnl_daily, user_pnl_summary, game_heatmap_cache collections.
 *
 * Convention: "house_net" = total_bet - total_payout (positive = house profit).
 * We store per user_name × game_code × day: total_bet, total_payout,
 * net (payout - bet, user POV), bet_count.
 *
 * Sources (incremental, watermark-driven):
 *   log_KhoBau        → slot_khobau      (bet=bet_value, payout=prize)
 *   log_VuongQuocVin  → slot_vqv         (bet=bet_value, payout=prize)
 *   log_SieuAnhHung   → slot_sah         (bet=bet_value, payout=prize)
 *   log_NuDiepVien    → slot_ndv         (bet=bet_value, payout=prize)
 *   log_mini_poker    → minipoker        (bet=bet_value, payout=prize)
 *   log_no_hu_slot    → slot_<game_name> (bet=bet_value, payout=prize, user=nick_name)
 *   log_cao_thap      → caothap          (bet=bet_value, payout=bet_value+prize, user=nick_name, prize is NET)
 *   bau_cua_transaction → baucua         (bet=sum(bet_*), payout=sum(prize_*))
 *
 * Each source maintains a watermark row in vinplay.rtp_aggregator_watermark.
 * Call {@link #runIncremental()} to process all sources.
 */
public final class RtpAggregator {
    private static final Logger logger = Logger.getLogger("backend");

    public static class Source {
        public final String collection;
        public final String gameCode;          // or null to read from field
        public final String gameCodeField;     // e.g. "game_name" for log_no_hu_slot
        public final String gameCodePrefix;    // prefix applied to field value (e.g. "slot_")
        public final String userField;
        public final String betExpr;           // Mongo expression (as $field string or Document)
        public final String payoutExpr;
        public final boolean prizeIsNet;       // if true: payout = bet + prize

        public Source(String collection, String gameCode, String gameCodeField, String gameCodePrefix,
                      String userField, String betExpr, String payoutExpr, boolean prizeIsNet) {
            this.collection = collection;
            this.gameCode = gameCode;
            this.gameCodeField = gameCodeField;
            this.gameCodePrefix = gameCodePrefix;
            this.userField = userField;
            this.betExpr = betExpr;
            this.payoutExpr = payoutExpr;
            this.prizeIsNet = prizeIsNet;
        }
    }

    public static final List<Source> SOURCES = Arrays.asList(
            new Source("log_KhoBau",        "slot_khobau", null, null,     "user_name", "$bet_value", "$prize", false),
            new Source("log_VuongQuocVin",  "slot_vqv",    null, null,     "user_name", "$bet_value", "$prize", false),
            new Source("log_SieuAnhHung",   "slot_sah",    null, null,     "user_name", "$bet_value", "$prize", false),
            new Source("log_NuDiepVien",    "slot_ndv",    null, null,     "user_name", "$bet_value", "$prize", false),
            new Source("log_mini_poker",    "minipoker",   null, null,     "user_name", "$bet_value", "$prize", false),
            // log_no_hu_slot excluded — overlaps with individual slot log collections (jackpot rounds are already counted there).
            new Source("log_cao_thap",      "caothap",     null, null,     "nick_name", "$bet_value", "$prize", true),
            // baucua: bet = sum bet_bau..bet_huou, payout = sum prize_bau..prize_huou
            new Source("bau_cua_transaction","baucua",     null, null,     "user_name",
                    "{$add:[\"$bet_bau\",\"$bet_cua\",\"$bet_tom\",\"$bet_ca\",\"$bet_ga\",\"$bet_huou\"]}",
                    "{$add:[\"$prize_bau\",\"$prize_cua\",\"$prize_tom\",\"$prize_ca\",\"$prize_ga\",\"$prize_huou\"]}",
                    false)
    );

    /** Runs incremental aggregation for all sources. Returns total rounds ingested. */
    public static long runIncremental() {
        long total = 0;
        for (Source s : SOURCES) {
            try {
                total += runOne(s);
            } catch (Exception e) {
                logger.error("RtpAggregator: source failed: " + s.collection, e);
            }
        }
        try {
            rebuildSummaries();
            rebuildHeatmapCache();
        } catch (Exception e) {
            logger.error("RtpAggregator: rebuild failed", e);
        }
        return total;
    }

    /** Process one source collection incrementally. */
    @SuppressWarnings("unchecked")
    static long runOne(Source s) throws Exception {
        Date watermark = readWatermark(s.collection);
        MongoDatabase db = MongoDBConnectionFactory.getDB();

        // Build pipeline
        List<Document> pipeline = new ArrayList<>();
        pipeline.add(new Document("$match", new Document("create_time", new Document("$gt", watermark))));

        // Project unified shape
        Object betExpr = parseExpr(s.betExpr);
        Object payoutExpr = parseExpr(s.payoutExpr);
        Document proj = new Document()
                .append("user_name", "$" + s.userField)
                .append("ts", "$create_time")
                .append("bet", betExpr);

        if (s.prizeIsNet) {
            proj.append("payout", new Document("$add", Arrays.asList(betExpr, payoutExpr)));
        } else {
            proj.append("payout", payoutExpr);
        }

        // game_code: fixed string or derived from field with optional prefix
        if (s.gameCode != null) {
            proj.append("game_code", s.gameCode);
        } else {
            if (s.gameCodePrefix != null) {
                proj.append("game_code", new Document("$concat", Arrays.asList(
                        s.gameCodePrefix,
                        new Document("$toLower", "$" + s.gameCodeField))));
            } else {
                proj.append("game_code", "$" + s.gameCodeField);
            }
        }
        proj.append("day", new Document("$dateToString", new Document("format", "%Y-%m-%d").append("date", "$create_time")));

        pipeline.add(new Document("$project", proj));

        // Group by composite key (string _id for simple $merge)
        pipeline.add(new Document("$group", new Document()
                .append("_id", new Document("$concat", Arrays.asList(
                        "$user_name", ":", "$game_code", ":", "$day")))
                .append("user_name", new Document("$first", "$user_name"))
                .append("game_code", new Document("$first", "$game_code"))
                .append("day", new Document("$first", "$day"))
                .append("total_bet", new Document("$sum", "$bet"))
                .append("total_payout", new Document("$sum", "$payout"))
                .append("bet_count", new Document("$sum", 1))));

        // Merge into user_pnl_daily (upsert, add to existing counters)
        pipeline.add(new Document("$merge", new Document()
                .append("into", "user_pnl_daily")
                .append("on", "_id")
                .append("whenMatched", Arrays.asList(
                        new Document("$set", new Document()
                                .append("total_bet", new Document("$add",
                                        Arrays.asList("$total_bet", "$$new.total_bet")))
                                .append("total_payout", new Document("$add",
                                        Arrays.asList("$total_payout", "$$new.total_payout")))
                                .append("bet_count", new Document("$add",
                                        Arrays.asList("$bet_count", "$$new.bet_count")))
                                .append("user_name", "$$new.user_name")
                                .append("game_code", "$$new.game_code")
                                .append("day", "$$new.day")
                                .append("updated_at", "$$NOW"))))
                .append("whenNotMatched", "insert")));

        MongoCollection<Document> col = db.getCollection(s.collection);
        // Execute — use toCollection via $merge
        long maxTs = 0;
        long countRounds = 0;
        try {
            col.aggregate(pipeline).allowDiskUse(true).toCollection();
            // Count how many raw rounds this pulled (approx via separate count)
            countRounds = col.countDocuments(new Document("create_time", new Document("$gt", watermark)));
            // Find max create_time we just ingested
            Document maxDoc = col.find(new Document("create_time", new Document("$gt", watermark)))
                    .sort(new Document("create_time", -1))
                    .limit(1).first();
            if (maxDoc != null && maxDoc.get("create_time") instanceof Date) {
                maxTs = ((Date) maxDoc.get("create_time")).getTime();
            }
        } catch (Exception e) {
            logger.error("RtpAggregator: pipeline failed for " + s.collection, e);
            throw e;
        }

        if (maxTs > 0) {
            writeWatermark(s.collection, new Date(maxTs), countRounds);
        }
        logger.info("RtpAggregator: " + s.collection + " ingested " + countRounds + " rounds");
        return countRounds;
    }

    /** Rebuild user_pnl_summary from user_pnl_daily for D1/D7/D30/ALL. */
    static void rebuildSummaries() {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        long nowMs = System.currentTimeMillis();
        String dayD1 = dayAgo(0);
        String dayD7 = dayAgo(6);
        String dayD30 = dayAgo(29);

        rebuildSummaryWindow(db, "D1", dayD1);
        rebuildSummaryWindow(db, "D7", dayD7);
        rebuildSummaryWindow(db, "D30", dayD30);
        rebuildSummaryWindow(db, "ALL", "0000-00-00");
        logger.info("RtpAggregator: summaries rebuilt at " + new Date(nowMs));
    }

    @SuppressWarnings("unchecked")
    static void rebuildSummaryWindow(MongoDatabase db, String window, String dayFloor) {
        List<Document> pipeline = new ArrayList<>();
        pipeline.add(new Document("$match", new Document("day", new Document("$gte", dayFloor))));
        pipeline.add(new Document("$group", new Document()
                .append("_id", new Document()
                        .append("user_name", "$user_name")
                        .append("game_code", "$game_code"))
                .append("total_bet", new Document("$sum", "$total_bet"))
                .append("total_payout", new Document("$sum", "$total_payout"))
                .append("bet_count", new Document("$sum", "$bet_count"))
                .append("first_seen", new Document("$min", "$day"))
                .append("last_seen", new Document("$max", "$day"))));
        pipeline.add(new Document("$project", new Document()
                .append("_id", new Document("$concat", Arrays.asList(
                        "$_id.user_name", ":", "$_id.game_code", ":", window)))
                .append("user_name", "$_id.user_name")
                .append("game_code", "$_id.game_code")
                .append("window", window)
                .append("total_bet", "$total_bet")
                .append("total_payout", "$total_payout")
                .append("net", new Document("$subtract", Arrays.asList("$total_payout", "$total_bet")))
                .append("bet_count", "$bet_count")
                .append("first_seen", "$first_seen")
                .append("last_seen", "$last_seen")
                .append("updated_at", "$$NOW")));
        pipeline.add(new Document("$merge", new Document()
                .append("into", "user_pnl_summary")
                .append("on", "_id")
                .append("whenMatched", "replace")
                .append("whenNotMatched", "insert")));
        db.getCollection("user_pnl_daily").aggregate(pipeline).allowDiskUse(true).toCollection();
    }

    /** Rebuild game_heatmap_cache from user_pnl_summary — top 200 by |net| per window. */
    static void rebuildHeatmapCache() {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        // Drop existing cache and rebuild (simpler than diff)
        db.getCollection("game_heatmap_cache").deleteMany(new Document());

        for (String window : Arrays.asList("D1", "D7", "D30", "ALL")) {
            List<Document> pipeline = new ArrayList<>();
            pipeline.add(new Document("$match", new Document("window", window)));
            pipeline.add(new Document("$addFields", new Document("abs_net",
                    new Document("$abs", "$net"))));
            pipeline.add(new Document("$sort", new Document("abs_net", -1)));
            pipeline.add(new Document("$limit", 200));
            pipeline.add(new Document("$project", new Document()
                    .append("_id", new Document("$concat", Arrays.asList(
                            window, ":", "$user_name", ":", "$game_code")))
                    .append("window", window)
                    .append("user_name", "$user_name")
                    .append("game_code", "$game_code")
                    .append("net", "$net")
                    .append("total_bet", "$total_bet")
                    .append("bet_count", "$bet_count")
                    .append("updated_at", "$$NOW")));
            pipeline.add(new Document("$merge", new Document()
                    .append("into", "game_heatmap_cache")
                    .append("on", "_id")
                    .append("whenMatched", "replace")
                    .append("whenNotMatched", "insert")));
            db.getCollection("user_pnl_summary").aggregate(pipeline).allowDiskUse(true).toCollection();
        }
    }

    static Date readWatermark(String source) throws Exception {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT last_processed_ts FROM rtp_aggregator_watermark WHERE source_collection=?")) {
            ps.setString(1, source);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp ts = rs.getTimestamp("last_processed_ts");
                    if (ts != null) return new Date(ts.getTime());
                }
            }
        }
        return new Date(0L);
    }

    static void writeWatermark(String source, Date ts, long rowsIngested) throws Exception {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO rtp_aggregator_watermark (source_collection, last_processed_ts, rows_ingested) " +
                     "VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE last_processed_ts=VALUES(last_processed_ts), " +
                     "rows_ingested = rows_ingested + VALUES(rows_ingested)")) {
            ps.setString(1, source);
            ps.setTimestamp(2, new Timestamp(ts.getTime()));
            ps.setLong(3, rowsIngested);
            ps.executeUpdate();
        }
    }

    /** Parse an expression string: "$field" → literal string; "{...}" → Document. */
    static Object parseExpr(String expr) {
        if (expr == null) return null;
        String t = expr.trim();
        if (t.startsWith("{")) return Document.parse(t);
        return t;
    }

    static String dayAgo(int days) {
        long ms = System.currentTimeMillis() - (days * 86400000L);
        return new java.text.SimpleDateFormat("yyyy-MM-dd").format(new Date(ms));
    }

    private RtpAggregator() {}
}
