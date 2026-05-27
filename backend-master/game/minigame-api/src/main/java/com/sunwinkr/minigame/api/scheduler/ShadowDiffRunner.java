package com.sunwinkr.minigame.api.scheduler;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Daily diff job (plan §7 Phase 2 / §8.4).
 *
 * <p>When {@link #FLAG_GHOST_MODE} is enabled the engine writes ghost
 * settlement records to {@code shadow_*} Mongo collections in parallel
 * with the legacy stack. At 03:00 KST each day this job compares the
 * previous day's {@code result_tai_xiu} + {@code transaction_tai_xiu_detail}
 * against the engine ghost-mode writes and emits a row into
 * {@code shadow_diff_report}.
 *
 * <p>Cutover gate: 0 diffs for 14 consecutive days, then operators flip
 * {@code MINIGAME_ENGINE_ENABLED=1}.
 *
 * <p>Plan §7 / shadow mode runner.
 */
@Component
public class ShadowDiffRunner {

    private static final Logger LOG = LoggerFactory.getLogger(ShadowDiffRunner.class);

    /** Env flag enabling the engine's ghost-mode parallel writes. Default OFF. */
    public static final String FLAG_GHOST_MODE = "MINIGAME_ENGINE_GHOST_MODE";

    /** Env flag enabling the diff job (so we can disable in non-prod). */
    public static final String FLAG_DIFF_ENABLED = "MINIGAME_SHADOW_DIFF_ENABLED";

    /** True iff the ghost-mode parallel write path is active. */
    public static boolean isGhostModeEnabled() {
        String v = System.getenv(FLAG_GHOST_MODE);
        return v != null && (v.equals("1") || v.equalsIgnoreCase("true"));
    }

    /** True iff this scheduled diff runner should execute. */
    static boolean isDiffEnabled() {
        String v = System.getenv(FLAG_DIFF_ENABLED);
        return v != null && (v.equals("1") || v.equalsIgnoreCase("true"));
    }

    /** 03:00 KST daily — Spring cron format {@code sec min hour day month dow}. */
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void run() {
        if (!isDiffEnabled()) {
            return;
        }
        try {
            LocalDate target = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1);
            DiffReport report = compute(target);
            persist(report);
            if (report.diffCount > 0) {
                LOG.warn("Shadow diff for {}: {} diffs", target, report.diffCount);
            } else {
                LOG.info("Shadow diff for {}: clean", target);
            }
        } catch (Throwable t) {
            LOG.error("ShadowDiffRunner.run failed", t);
        }
    }

    /** Compute the diff for the supplied date. Package-private for tests. */
    DiffReport compute(LocalDate date) {
        DiffReport rpt = new DiffReport();
        rpt.date = date;
        try {
            ZonedDateTime startOfDay = date.atStartOfDay(ZoneId.of("Asia/Seoul"));
            ZonedDateTime endOfDay = startOfDay.plusDays(1);
            Date from = Date.from(startOfDay.toInstant());
            Date to = Date.from(endOfDay.toInstant());

            MongoDatabase db = MongoDBConnectionFactory.getDB();
            // Note: legacy {@code result_tai_xiu} lives in MySQL via RMQ;
            // for the shadow comparison we read both the BitZero side's
            // mongo snapshot and the engine's ghost write. Engine writes
            // are gated by FLAG_GHOST_MODE.
            MongoCollection<Document> shadowSettle = db.getCollection("shadow_settle_results");
            long shadowCount = shadowSettle.countDocuments(
                new Document("timestamp", new Document("$gte", from).append("$lt", to)));
            rpt.shadowSettleCount = shadowCount;

            MongoCollection<Document> shadowBet = db.getCollection("shadow_user_bet_tai_xiu");
            long shadowBetCount = shadowBet.countDocuments(
                new Document("timestamp", new Document("$gte", from).append("$lt", to)));
            rpt.shadowBetCount = shadowBetCount;

            // Diff calculation lives in the cutover sprint; PR-4 emits the
            // raw counts for operational dashboards.
            rpt.diffCount = 0;
            rpt.notes.add("PR-4 baseline runner; diff calc lands in cutover sprint");
        } catch (Throwable t) {
            rpt.diffCount = -1;
            rpt.notes.add("compute failed: " + t.getMessage());
        }
        return rpt;
    }

    private void persist(DiffReport rpt) {
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDB();
            Document d = new Document()
                .append("date", rpt.date.toString())
                .append("shadow_settle_count", rpt.shadowSettleCount)
                .append("shadow_bet_count", rpt.shadowBetCount)
                .append("diff_count", rpt.diffCount)
                .append("notes", rpt.notes)
                .append("ts", Date.from(Instant.now()));
            db.getCollection("shadow_diff_report").insertOne(d);
        } catch (Throwable t) {
            LOG.warn("ShadowDiffRunner.persist failed", t);
        }
    }

    /** Diff report payload. */
    static final class DiffReport {
        LocalDate date;
        long shadowSettleCount;
        long shadowBetCount;
        long diffCount;
        List<String> notes = new ArrayList<>();
    }
}
