package com.vinplay.dal.service;

import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * SUN-1255 Phase 1 — auto-propagation of commission rates across an
 * agency tree.
 *
 * <p>Two flows live here:
 *
 * <ol>
 *   <li>{@link #seedRatesForNewAgent} — when ops creates a new
 *       sub-agent, copy every game_key the immediate parent has and
 *       set rate = max(floor, parent_rate - step). Step + floor come
 *       from {@code commission_rate_policy} for the master TĐL.
 *   <li>{@link #cascadeMasterRateChange} — when ops bumps a master's
 *       rate (or any non-leaf rate), walk every descendant top-down
 *       and apply the same step-down rule. The legacy guard triggers
 *       block out-of-order writes; this method writes top-down so the
 *       guard never trips.
 * </ol>
 *
 * <p>Both flows write through the existing audit triggers (no new
 * audit plumbing). They DO NOT temporarily disable any guard — by the
 * time a row is written, the policy guarantees it will satisfy the
 * pool-aware invariant set by {@code tg_gcr_guard_*} (Phase 0c).
 *
 * <p>The legacy {@code AdminMakeAgentProcessor.seedDefaultGameCommissionRates}
 * stays as a thin wrapper that delegates here so existing call sites
 * (AddNewUserAgentProcessor, AddNewUserAgentChildProcessor, AdminMake
 * AgentProcessor itself) keep working.
 */
public final class CommissionPropagator {

    private static final Logger logger = Logger.getLogger("backend");

    private static final BigDecimal DEFAULT_STEP  = new BigDecimal("0.25");
    private static final BigDecimal DEFAULT_FLOOR = new BigDecimal("0.00");

    private CommissionPropagator() {}

    /**
     * Snapshot of the policy that applies to a given agent. Master
     * TĐL is the policy owner; descendants inherit unless ops sets a
     * per-master override (future work).
     */
    public static final class Policy {
        public final int masterId;
        public final BigDecimal stepPct;
        public final BigDecimal floorPct;

        Policy(int masterId, BigDecimal stepPct, BigDecimal floorPct) {
            this.masterId = masterId;
            this.stepPct = stepPct;
            this.floorPct = floorPct;
        }
    }

    /**
     * Resolve the master TĐL for an agent and read its
     * commission_rate_policy. Falls back to the platform-wide defaults
     * (step=0.25, floor=0.00) if no policy row exists yet.
     */
    public static Policy resolvePolicy(Connection conn, int agentUserId) throws Exception {
        // Master id = agent if it has no parent, else first ancestor.
        int masterId = agentUserId;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT parentid, ancestors FROM vinplay_admin.useragent WHERE id = ?")) {
            ps.setInt(1, agentUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int parentId = rs.getInt("parentid");
                    String ancestors = rs.getString("ancestors");
                    if (parentId > 0) {
                        if (ancestors != null && !ancestors.isEmpty()) {
                            masterId = Integer.parseInt(ancestors.split(",")[0]);
                        } else {
                            masterId = parentId;
                        }
                    }
                }
            }
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT step_pct, floor_pct FROM vinplay.commission_rate_policy WHERE master_id = ?")) {
            ps.setInt(1, masterId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal step  = rs.getBigDecimal("step_pct");
                    BigDecimal floor = rs.getBigDecimal("floor_pct");
                    return new Policy(masterId,
                            step != null ? step : DEFAULT_STEP,
                            floor != null ? floor : DEFAULT_FLOOR);
                }
            }
        }
        return new Policy(masterId, DEFAULT_STEP, DEFAULT_FLOOR);
    }

    /**
     * Insert a {@code game_commission_rate} row per parent game_key
     * with rate = max(floor, parent_rate - step). Idempotent — uses
     * the (agent_user_id, game_key) UNIQUE key (Phase 0b) so re-running
     * is a no-op.
     *
     * @param conn         active connection on the user/game DB
     * @param childAgentId useragent.id of the new sub-agent
     */
    public static int seedRatesForNewAgent(Connection conn, int childAgentId) throws Exception {
        int parentId;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT parentid FROM vinplay_admin.useragent WHERE id = ?")) {
            ps.setInt(1, childAgentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || (parentId = rs.getInt("parentid")) <= 0) {
                    return 0;
                }
            }
        }
        Policy policy = resolvePolicy(conn, childAgentId);
        String childNickname;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT nickname FROM vinplay_admin.useragent WHERE id = ?")) {
            ps.setInt(1, childAgentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return 0;
                childNickname = rs.getString("nickname");
            }
        }

        List<String[]> rows = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT game_key, rate FROM vinplay.game_commission_rate WHERE agent_user_id = ?")) {
            ps.setInt(1, parentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String gameKey = rs.getString("game_key");
                    BigDecimal parentRate = rs.getBigDecimal("rate");
                    BigDecimal childRate = parentRate.subtract(policy.stepPct).max(policy.floorPct)
                            .setScale(2, RoundingMode.DOWN);
                    rows.add(new String[]{ gameKey, childRate.toPlainString() });
                }
            }
        }
        if (rows.isEmpty()) {
            logger.info("CommissionPropagator: no parent rates to seed for agent_id=" + childAgentId);
            return 0;
        }

        int inserted = 0;
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO vinplay.game_commission_rate (agent_user_id, agent_nickname, game_key, rate) "
              + "VALUES (?, ?, ?, ?) "
              + "ON DUPLICATE KEY UPDATE rate = VALUES(rate)")) {
            for (String[] row : rows) {
                ps.setInt(1, childAgentId);
                ps.setString(2, childNickname);
                ps.setString(3, row[0]);
                ps.setBigDecimal(4, new BigDecimal(row[1]));
                ps.addBatch();
            }
            int[] r = ps.executeBatch();
            for (int x : r) inserted += (x > 0 ? 1 : 0);
        }
        logger.info("CommissionPropagator: seeded " + inserted + " rates for agent_id="
                + childAgentId + " (parent_id=" + parentId
                + ", step=" + policy.stepPct + ", floor=" + policy.floorPct + ")");
        return inserted;
    }

    /**
     * After ops changes a non-leaf agent's rate for one {@code gameKey},
     * walk descendants top-down and rewrite their rate to
     * max(floor, ancestor_rate - step). The descendant subtree is
     * resolved via {@code useragent.ancestors} (FIND_IN_SET).
     *
     * <p>Top-down ordering keeps the existing pool-aware guard happy —
     * each row's new rate is only written after its parent's row has
     * already been lowered.
     *
     * @param conn      active connection on the user/game DB
     * @param parentId  useragent.id of the row whose rate just changed
     * @param gameKey   the game_key whose rate was updated
     * @return rows updated downstream
     */
    public static int cascadeRateChange(Connection conn, int parentId, String gameKey) throws Exception {
        Policy policy = resolvePolicy(conn, parentId);
        BigDecimal step  = policy.stepPct;
        BigDecimal floor = policy.floorPct;

        // Pull descendants ORDER BY useragent.level ASC so we go closest-first.
        // Each row's effective_parent_rate = MAX(rate of ANY direct ancestor row).
        // Approach: iterate level-by-level. For each descendant level, compute
        // rate = max(floor, parent.rate - step) and UPDATE/INSERT.
        List<int[]> descendantsByLevel = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, parentid, level FROM vinplay_admin.useragent "
              + "WHERE FIND_IN_SET(?, IFNULL(ancestors, '')) > 0 "
              + "ORDER BY level ASC, id ASC")) {
            ps.setInt(1, parentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    descendantsByLevel.add(new int[]{ rs.getInt("id"), rs.getInt("parentid") });
                }
            }
        }
        if (descendantsByLevel.isEmpty()) return 0;

        int updated = 0;
        try (PreparedStatement get = conn.prepareStatement(
                "SELECT rate FROM vinplay.game_commission_rate WHERE agent_user_id = ? AND game_key = ?");
             PreparedStatement upsert = conn.prepareStatement(
                "INSERT INTO vinplay.game_commission_rate (agent_user_id, agent_nickname, game_key, rate) "
              + "VALUES (?, (SELECT nickname FROM vinplay_admin.useragent WHERE id = ?), ?, ?) "
              + "ON DUPLICATE KEY UPDATE rate = VALUES(rate)")) {
            for (int[] node : descendantsByLevel) {
                int childId = node[0];
                int parentOfChildId = node[1];
                BigDecimal parentRate = null;
                get.setInt(1, parentOfChildId);
                get.setString(2, gameKey);
                try (ResultSet rs = get.executeQuery()) {
                    if (rs.next()) parentRate = rs.getBigDecimal("rate");
                }
                if (parentRate == null) continue;
                BigDecimal newRate = parentRate.subtract(step).max(floor)
                        .setScale(2, RoundingMode.DOWN);
                upsert.setInt(1, childId);
                upsert.setInt(2, childId);
                upsert.setString(3, gameKey);
                upsert.setBigDecimal(4, newRate);
                upsert.executeUpdate();
                updated++;
            }
        }
        logger.info("CommissionPropagator: cascadeRateChange parentId=" + parentId
                + " gameKey=" + gameKey + " descendants_updated=" + updated);
        return updated;
    }
}
