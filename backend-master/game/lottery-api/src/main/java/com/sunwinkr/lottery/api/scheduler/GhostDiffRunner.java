package com.sunwinkr.lottery.api.scheduler;

import com.sunwinkr.lottery.engine.clock.LotteryClock;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Daily ghost-mode diff runner. Plan §7 cutover gate.
 *
 * <p>When {@link #FLAG_GHOST_MODE} is enabled, the engine settles into
 * shadow table {@code lode_shadow} in parallel with the legacy
 * {@code lode} table. This job runs at 03:00 KST (ops-time in Korea)
 * the day after, compares prize totals per ticket, and emits diff
 * counts to log (and in PR-3 baseline, persists to
 * {@code lottery_shadow_diff_report}).
 *
 * <p>Cutover gate: 0 diffs for 14 consecutive days → flip
 * {@code LOTTERY_ENGINE_ENABLED=1}.
 *
 * <h3>Cron zone choice</h3>
 * Unlike {@link DrawScheduler} which is Vietnamese product timing, this
 * is an internal ops job. We anchor it to ops timezone (KST) so the diff
 * report is fresh in the operators' morning. The diff CONTENT still
 * uses VN wall date as the partition key (yesterday in Vietnam).
 */
@Component
public class GhostDiffRunner {

    private static final Logger LOG = LoggerFactory.getLogger(GhostDiffRunner.class);

    /** Env flag enabling the engine's ghost-mode parallel writes. */
    public static final String FLAG_GHOST_MODE = "LOTTERY_ENGINE_GHOST_MODE";

    /** Env flag enabling THIS diff runner (so we can disable in non-prod). */
    public static final String FLAG_DIFF_ENABLED = "LOTTERY_SHADOW_DIFF_ENABLED";

    private static final String POOL = "mysqlpool_minigame";

    public static boolean isGhostModeEnabled() {
        String v = System.getenv(FLAG_GHOST_MODE);
        return v != null && (v.equals("1") || v.equalsIgnoreCase("true"));
    }

    static boolean isDiffEnabled() {
        String v = System.getenv(FLAG_DIFF_ENABLED);
        return v != null && (v.equals("1") || v.equalsIgnoreCase("true"));
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void diffYesterday() {
        if (!isDiffEnabled()) {
            return;
        }
        LocalDate vnYesterday = LocalDate.now(LotteryClock.VN).minusDays(1);
        try {
            DiffReport report = compute(vnYesterday);
            if (report.diffCount > 0) {
                LOG.warn("GhostDiffRunner vnDate={} legacyCount={} shadowCount={} diffs={} details={}",
                    vnYesterday, report.legacyCount, report.shadowCount, report.diffCount, report.details);
            } else {
                LOG.info("GhostDiffRunner vnDate={} clean legacyCount={} shadowCount={}",
                    vnYesterday, report.legacyCount, report.shadowCount);
            }
            persist(report);
        } catch (Throwable t) {
            LOG.error("GhostDiffRunner.diffYesterday failed for vnDate={}", vnYesterday, t);
        }
    }

    /** Compare {@code lode.prize} vs {@code lode_shadow.prize}. Package-private for tests. */
    DiffReport compute(LocalDate vnDate) {
        DiffReport rpt = new DiffReport();
        rpt.vnDate = vnDate;
        String sql = "SELECT l.id, l.prize AS legacy_prize, s.prize AS shadow_prize "
                   + "FROM lode l LEFT JOIN lode_shadow s ON s.id = l.id "
                   + "WHERE DATE(l.created_date) = ? AND l.settled_at IS NOT NULL";
        try (Connection conn = ConnectionPool.getInstance().getConnection(POOL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(vnDate));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rpt.legacyCount++;
                    Object legacyObj = rs.getObject("legacy_prize");
                    Object shadowObj = rs.getObject("shadow_prize");
                    Long legacy = legacyObj == null ? null : ((Number) legacyObj).longValue();
                    Long shadow = shadowObj == null ? null : ((Number) shadowObj).longValue();
                    if (shadow != null) rpt.shadowCount++;
                    if (legacy != null && shadow != null && !legacy.equals(shadow)) {
                        rpt.diffCount++;
                        if (rpt.details.size() < 20) {
                            rpt.details.add("ticket=" + rs.getLong("id")
                                + " legacy=" + legacy + " shadow=" + shadow);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            // lode_shadow may not exist yet — ghost-mode opt-in by migration.
            LOG.debug("GhostDiffRunner.compute SQL failed (lode_shadow may be absent): {}", e.getMessage());
            rpt.diffCount = -1;
        }
        return rpt;
    }

    private void persist(DiffReport rpt) {
        String sql = "INSERT INTO lottery_shadow_diff_report (vn_date, legacy_count, shadow_count, diff_count, details, ts) "
                   + "VALUES (?, ?, ?, ?, ?, NOW())";
        try (Connection conn = ConnectionPool.getInstance().getConnection(POOL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(rpt.vnDate));
            ps.setLong(2, rpt.legacyCount);
            ps.setLong(3, rpt.shadowCount);
            ps.setLong(4, rpt.diffCount);
            ps.setString(5, String.join("\n", rpt.details));
            ps.executeUpdate();
        } catch (SQLException e) {
            // diff_report table may not exist in non-prod — log + drop.
            LOG.debug("GhostDiffRunner.persist failed: {}", e.getMessage());
        }
    }

    /** Diff report payload. */
    static final class DiffReport {
        LocalDate vnDate;
        long legacyCount;
        long shadowCount;
        long diffCount;
        List<String> details = new ArrayList<>();
    }
}
