package com.vinplay.dal.rebate;

import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Unified service for all rebate module database operations.
 * Handles rebate_config, rebate_logs, rebate_payout tables.
 * Also queries useragent (F0-F1 hierarchy) and user_volume_tracking.
 */
public class RebateService {

    private static final Logger logger = Logger.getLogger("dal");
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final int BOT_NICKNAME_WARN_CAP = 50_000;
    private static final long BOT_NICKNAME_CACHE_TTL_MS = 5 * 60_000L;
    private static volatile Set<String> cachedBotNicknames = Collections.emptySet();
    private static volatile long cachedBotNicknamesAt = 0L;

    // ─── Connection helpers ─────────────────────────────────────────────

    private static Connection getMainConn() throws Exception {
        return ConnectionPool.getInstance().getConnection("mysqlpoolname");
    }

    private static Connection getAdminConn() throws Exception {
        return ConnectionPool.getInstance().getConnection("mysqlpool_admin");
    }

    private static void close(AutoCloseable... resources) {
        for (AutoCloseable r : resources) {
            if (r != null) try { r.close(); } catch (Exception ignored) {}
        }
    }

    private static Set<String> loadBotNicknamesForFilter() {
        long now = System.currentTimeMillis();
        Set<String> cached = cachedBotNicknames;
        if (cachedBotNicknamesAt > 0L && now - cachedBotNicknamesAt < BOT_NICKNAME_CACHE_TTL_MS) {
            return cached;
        }

        synchronized (RebateService.class) {
            now = System.currentTimeMillis();
            cached = cachedBotNicknames;
            if (cachedBotNicknamesAt > 0L && now - cachedBotNicknamesAt < BOT_NICKNAME_CACHE_TTL_MS) {
                return cached;
            }

            Set<String> bots = new LinkedHashSet<>();
            boolean capped = false;
            Connection conn = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                conn = getMainConn();
                ps = conn.prepareStatement("SELECT nick_name FROM users WHERE is_bot = 1 ORDER BY nick_name LIMIT ?");
                ps.setInt(1, BOT_NICKNAME_WARN_CAP + 1);
                rs = ps.executeQuery();
                while (rs.next()) {
                    String nn = rs.getString("nick_name");
                    if (nn == null || nn.trim().isEmpty()) continue;
                    if (bots.size() >= BOT_NICKNAME_WARN_CAP) {
                        capped = true;
                        break;
                    }
                    bots.add(nn.trim());
                }
                if (capped) {
                    logger.warn("RebateService.loadBotNicknamesForFilter: bot nickname list reached cap "
                            + BOT_NICKNAME_WARN_CAP + "; remaining bot accounts fall back to NOT EXISTS filtering");
                    cachedBotNicknames = null;
                    cachedBotNicknamesAt = System.currentTimeMillis();
                    return null;
                }
                Set<String> immutable = Collections.unmodifiableSet(bots);
                cachedBotNicknames = immutable;
                cachedBotNicknamesAt = System.currentTimeMillis();
                return immutable;
            } catch (Exception e) {
                logger.warn("RebateService.loadBotNicknamesForFilter error: " + e.getMessage());
                cachedBotNicknames = null;
                cachedBotNicknamesAt = System.currentTimeMillis();
                return null;
            } finally {
                close(rs, ps, conn);
            }
        }
    }

    private static void appendBotFilter(StringBuilder sql, List<Object> params) {
        Set<String> bots = loadBotNicknamesForFilter();
        if (bots == null) {
            // SUN-1226: also exclude rows where player_nickname IS NULL — those are
            // legacy rows inserted before the V011 migration column was added. They
            // cannot be identified as non-bot, so hide them from the agency LSR view.
            sql.append(" AND player_nickname IS NOT NULL"
                     + " AND NOT EXISTS (SELECT 1 FROM users WHERE nick_name = player_nickname AND is_bot = 1)");
            return;
        }
        if (bots.isEmpty()) {
            // No bots in the system, but still require player_nickname to be non-NULL
            // so legacy unidentifiable rows are excluded from the agency view.
            sql.append(" AND player_nickname IS NOT NULL");
            return;
        }

        // SUN-1226: must NOT allow player_nickname IS NULL to bypass the bot filter.
        // Legacy rows (pre-V011 migration) have player_nickname=NULL and could be bot
        // bets — they are excluded from the agency LSR view for the same reason.
        sql.append(" AND player_nickname IS NOT NULL AND player_nickname NOT IN (");
        boolean first = true;
        for (String bot : bots) {
            if (!first) sql.append(",");
            sql.append("?");
            params.add(bot);
            first = false;
        }
        sql.append(")");
    }

    /**
     * GitLab #46 / SUN-1095: read a DECIMAL percentage column as a fixed
     * 2-decimal string. JSONObject.numberToString() unconditionally
     * trims trailing zeros from any Number subtype, so emitting
     * percentages as `double` round-trips "0.10" → "0.1", "1.20" → "1.2",
     * "0.00" → "0". Using BigDecimal + setScale(2, HALF_UP) +
     * toPlainString preserves exactly what the QC team sees on the
     * DECIMAL(5,2) column.
     *
     * Returns "0.00" for SQL NULL / missing column. Caller writes a
     * String into the response map, so org.json.JSONObject leaves it
     * alone.
     */
    private static String pct(ResultSet rs, String column) {
        try {
            java.math.BigDecimal v = rs.getBigDecimal(column);
            if (v == null) return "0.00";
            return v.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
        } catch (SQLException ignored) {
            return "0.00";
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  REBATE CONFIG
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Get all active rebate configs.
     */
    public static List<Map<String, Object>> listConfigs(boolean activeOnly) {
        List<Map<String, Object>> results = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getMainConn();
            String sql = "SELECT * FROM rebate_config" +
                    (activeOnly ? " WHERE is_active = 1" : "") +
                    " ORDER BY agent_nickname";
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                results.add(mapConfig(rs));
            }
        } catch (Exception e) {
            logger.error("RebateService.listConfigs error", e);
        } finally {
            close(rs, ps, conn);
        }
        return results;
    }

    /**
     * Get config for a specific agent.
     */
    public static Map<String, Object> getConfig(int agentUserId) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getMainConn();
            ps = conn.prepareStatement("SELECT * FROM rebate_config WHERE agent_user_id = ?");
            ps.setInt(1, agentUserId);
            rs = ps.executeQuery();
            if (rs.next()) return mapConfig(rs);
        } catch (Exception e) {
            logger.error("RebateService.getConfig error agentUserId=" + agentUserId, e);
        } finally {
            close(rs, ps, conn);
        }
        return null;
    }

    /**
     * Insert or update rebate config (upsert).
     */
    public static boolean upsertConfig(int agentUserId, String nickname, double rebatePct,
                                       double sharePct, String periodType, long minVolume, boolean active) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getMainConn();
            String sql = "INSERT INTO rebate_config (agent_user_id, agent_nickname, rebate_percentage, " +
                    "share_percentage, period_type, min_volume_threshold, is_active) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE agent_nickname=VALUES(agent_nickname), " +
                    "rebate_percentage=VALUES(rebate_percentage), share_percentage=VALUES(share_percentage), " +
                    "period_type=VALUES(period_type), min_volume_threshold=VALUES(min_volume_threshold), " +
                    "is_active=VALUES(is_active)";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, agentUserId);
            ps.setString(2, nickname);
            ps.setDouble(3, rebatePct);
            ps.setDouble(4, sharePct);
            ps.setString(5, periodType);
            ps.setLong(6, minVolume);
            ps.setInt(7, active ? 1 : 0);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("RebateService.upsertConfig error agentUserId=" + agentUserId, e);
            return false;
        } finally {
            close(ps, conn);
        }
    }

    /**
     * Agent (F0) sets share percentage (from Portal API).
     */
    public static boolean setSharePercentage(int agentUserId, double sharePct) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getMainConn();
            // Validate: share_percentage must be <= rebate_percentage
            ps = conn.prepareStatement(
                    "UPDATE rebate_config SET share_percentage = ? " +
                    "WHERE agent_user_id = ? AND is_active = 1 AND ? <= rebate_percentage");
            ps.setDouble(1, sharePct);
            ps.setInt(2, agentUserId);
            ps.setDouble(3, sharePct);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("RebateService.setSharePercentage error agentUserId=" + agentUserId, e);
            return false;
        } finally {
            close(ps, conn);
        }
    }

    private static Map<String, Object> mapConfig(ResultSet rs) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", rs.getInt("id"));
        m.put("agent_user_id", rs.getInt("agent_user_id"));
        m.put("agent_nickname", rs.getString("agent_nickname"));
        m.put("rebate_percentage", pct(rs, "rebate_percentage"));
        m.put("share_percentage", pct(rs, "share_percentage"));
        m.put("period_type", rs.getString("period_type"));
        m.put("min_volume_threshold", rs.getLong("min_volume_threshold"));
        m.put("is_active", rs.getInt("is_active"));
        m.put("created_at", rs.getString("created_at"));
        m.put("updated_at", rs.getString("updated_at"));
        return m;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  AGENT HIERARCHY (F0 → F1)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Get all F1 nicknames belonging to an F0 agent.
     * Uses useragent table from mysqlpool_admin.
     *
     * Legacy data used status='D', while newer provisioning flows insert
     * numeric status values ('1'/1). Rebate calculation must support both,
     * otherwise newly created agent trees never generate rebate logs.
     */
    public static List<String> getF1Nicknames(int f0AgentId) {
        List<String> nicknames = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getAdminConn();
            ps = conn.prepareStatement(
                    "SELECT nickname FROM useragent " +
                    "WHERE parentid = ? AND active = 1 " +
                    "AND COALESCE(CAST(status AS CHAR), '1') IN ('D', '1')");
            ps.setInt(1, f0AgentId);
            rs = ps.executeQuery();
            while (rs.next()) {
                nicknames.add(rs.getString("nickname"));
            }
        } catch (Exception e) {
            logger.error("RebateService.getF1Nicknames error f0AgentId=" + f0AgentId, e);
        } finally {
            close(rs, ps, conn);
        }
        return nicknames;
    }

    /**
     * Get F0 agent ID by nickname (from useragent).
     * Accept both legacy status='D' and current status='1'.
     */
    public static int getAgentIdByNickname(String nickname) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getAdminConn();
            ps = conn.prepareStatement(
                    "SELECT id FROM useragent " +
                    "WHERE nickname = ? AND parentid = -1 " +
                    "AND COALESCE(CAST(status AS CHAR), '1') IN ('D', '1') " +
                    "AND active = 1 LIMIT 1");
            ps.setString(1, nickname);
            rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (Exception e) {
            logger.error("RebateService.getAgentIdByNickname error nickname=" + nickname, e);
        } finally {
            close(rs, ps, conn);
        }
        return -1;
    }

    /**
     * Check if user is an F0 agent (parentid = -1).
     */
    public static boolean isF0Agent(String nickname) {
        return getAgentIdByNickname(nickname) > 0;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  VOLUME CALCULATION
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Calculate total bet volume for a list of F1 nicknames within a period.
     * Queries user_volume_tracking for accumulated volume.
     *
     * For more accurate period-based tracking, this queries the raw volume
     * from user_volume_tracking. In a future enhancement, per-period
     * volume snapshots can be added.
     */
    public static long calculateF1Volume(List<String> f1Nicknames, String periodStart, String periodEnd) {
        if (f1Nicknames == null || f1Nicknames.isEmpty()) return 0;
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getMainConn();
            // Build IN clause
            StringBuilder placeholders = new StringBuilder();
            for (int i = 0; i < f1Nicknames.size(); i++) {
                if (i > 0) placeholders.append(",");
                placeholders.append("?");
            }
            String sql = "SELECT COALESCE(SUM(total_commission_volume), 0) as total_vol " +
                    "FROM user_volume_tracking WHERE nick_name IN (" + placeholders + ")";
            ps = conn.prepareStatement(sql);
            for (int i = 0; i < f1Nicknames.size(); i++) {
                ps.setString(i + 1, f1Nicknames.get(i));
            }
            rs = ps.executeQuery();
            if (rs.next()) return rs.getLong("total_vol");
        } catch (Exception e) {
            logger.error("RebateService.calculateF1Volume error", e);
        } finally {
            close(rs, ps, conn);
        }
        return 0;
    }

    /**
     * Get volume details per F1 user for a specific F0 agent.
     */
    public static List<Map<String, Object>> getF1VolumeDetails(int f0AgentId) {
        List<Map<String, Object>> results = new ArrayList<>();
        List<String> f1Nicks = getF1Nicknames(f0AgentId);
        if (f1Nicks.isEmpty()) return results;

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getMainConn();
            StringBuilder placeholders = new StringBuilder();
            for (int i = 0; i < f1Nicks.size(); i++) {
                if (i > 0) placeholders.append(",");
                placeholders.append("?");
            }
            ps = conn.prepareStatement(
                    "SELECT user_id, nick_name, total_actual_volume, total_required_volume, COALESCE(total_commission_volume, total_actual_volume) as total_commission_volume " +
                    "FROM user_volume_tracking WHERE nick_name IN (" + placeholders + ") " +
                    "ORDER BY total_commission_volume DESC");
            for (int i = 0; i < f1Nicks.size(); i++) {
                ps.setString(i + 1, f1Nicks.get(i));
            }
            rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("user_id", rs.getInt("user_id"));
                m.put("nick_name", rs.getString("nick_name"));
                m.put("total_actual_volume", rs.getLong("total_commission_volume"));
                m.put("total_required_volume", rs.getLong("total_required_volume"));
                results.add(m);
            }
        } catch (Exception e) {
            logger.error("RebateService.getF1VolumeDetails error f0AgentId=" + f0AgentId, e);
        } finally {
            close(rs, ps, conn);
        }
        return results;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  REBATE LOGS
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Create a rebate log entry (called by scheduled job or manual calculation).
     * Returns the generated ID, or -1 on failure.
     */
    public static long createRebateLog(int agentUserId, String nickname, String periodStart,
                                        String periodEnd, String periodType, long totalF1Volume,
                                        double rebatePct, long rebateAmount, double sharePct,
                                        long shareAmount, long netRebate) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getMainConn();
            ps = conn.prepareStatement(
                    "INSERT INTO rebate_logs (agent_user_id, agent_nickname, period_start, period_end, " +
                    "period_type, total_f1_volume, rebate_percentage, rebate_amount, share_percentage, " +
                    "share_amount, net_rebate, status) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, agentUserId);
            ps.setString(2, nickname);
            ps.setString(3, periodStart);
            ps.setString(4, periodEnd);
            ps.setString(5, periodType);
            ps.setLong(6, totalF1Volume);
            ps.setDouble(7, rebatePct);
            ps.setLong(8, rebateAmount);
            ps.setDouble(9, sharePct);
            ps.setLong(10, shareAmount);
            ps.setLong(11, netRebate);
            ps.setString(12, "PENDING");
            ps.executeUpdate();
            rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getLong(1);
        } catch (Exception e) {
            logger.error("RebateService.createRebateLog error agentUserId=" + agentUserId, e);
        } finally {
            close(rs, ps, conn);
        }
        return -1;
    }

    /**
     * Check for duplicate rebate log (same agent, same period).
     */
    public static boolean hasExistingLog(int agentUserId, String periodStart, String periodEnd) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getMainConn();
            ps = conn.prepareStatement(
                    "SELECT 1 FROM rebate_logs WHERE agent_user_id = ? " +
                    "AND period_start = ? AND period_end = ? LIMIT 1");
            ps.setInt(1, agentUserId);
            ps.setString(2, periodStart);
            ps.setString(3, periodEnd);
            rs = ps.executeQuery();
            return rs.next();
        } catch (Exception e) {
            logger.error("RebateService.hasExistingLog error", e);
        } finally {
            close(rs, ps, conn);
        }
        return false;
    }

    /**
     * Query rebate logs with filters. Supports pagination.
     */
    /** Backward-compatible overload — no bot exclusion (audit/admin paths). */
    public static List<Map<String, Object>> queryLogs(Integer agentUserId, String status,
                                                       String dateFrom, String dateTo,
                                                       String sort, String dir,
                                                       int page, int limit) {
        return queryLogs(agentUserId, status, dateFrom, dateTo, sort, dir, page, limit, false, false);
    }

    /** Backward-compatible 9-arg overload — no REVERSED/REJECTED exclusion. */
    public static List<Map<String, Object>> queryLogs(Integer agentUserId, String status,
                                                       String dateFrom, String dateTo,
                                                       String sort, String dir,
                                                       int page, int limit,
                                                       boolean excludeBots) {
        return queryLogs(agentUserId, status, dateFrom, dateTo, sort, dir, page, limit, excludeBots, false);
    }

    /**
     * @param excludeBots     when true, rows whose player_nickname matches a user
     *                        with is_bot=1 are excluded (used by agency LSR view).
     *                        Pass false for admin/audit paths that need all records.
     * @param excludeReversed when true, rows with status REVERSED or REJECTED are
     *                        excluded — i.e. cancelled bets whose rebate was
     *                        compensated, and rebates an admin rejected. Used by
     *                        agency LSR (SUN-1240) so cancelled-bet rebates do
     *                        not count toward Rolling totals or commission.
     */
    public static List<Map<String, Object>> queryLogs(Integer agentUserId, String status,
                                                       String dateFrom, String dateTo,
                                                       String sort, String dir,
                                                       int page, int limit,
                                                       boolean excludeBots,
                                                       boolean excludeReversed) {
        List<Map<String, Object>> results = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getMainConn();
            StringBuilder sql = new StringBuilder("SELECT * FROM rebate_logs WHERE 1=1");
            List<Object> params = new ArrayList<>();

            if (agentUserId != null && agentUserId > 0) {
                sql.append(" AND agent_user_id = ?");
                params.add(agentUserId);
            }
            if (status != null && !status.isEmpty()) {
                sql.append(" AND status = ?");
                params.add(status);
            }
            if (dateFrom != null && !dateFrom.isEmpty()) {
                sql.append(" AND period_start >= ?");
                // FE sends bare date 'YYYY-MM-DD'; MySQL compares as 'YYYY-MM-DD 00:00:00' which is correct for >=
                params.add(dateFrom.contains(" ") ? dateFrom : dateFrom + " 00:00:00");
            }
            if (dateTo != null && !dateTo.isEmpty()) {
                sql.append(" AND period_end <= ?");
                // RealTimeCommission writes period_end as 'YYYY-MM-DD 23:59:59'.
                // FE sends bare date 'YYYY-MM-DD' which MySQL treats as '00:00:00',
                // so '23:59:59 <= 00:00:00' is false and every REALTIME row was filtered out.
                // Append 23:59:59 when the caller sends a bare date.
                params.add(dateTo.contains(" ") ? dateTo : dateTo + " 23:59:59");
            }
            if (excludeBots) {
                appendBotFilter(sql, params);
            }
            if (excludeReversed) {
                sql.append(" AND status NOT IN ('REVERSED','REJECTED')");
            }
            if (dir == null || (!dir.equalsIgnoreCase("asc") && !dir.equalsIgnoreCase("desc"))) {
                dir = "desc";
            } else {
                dir = dir.toLowerCase();
            }

            String sortCol = "created_at"; // default
            if ("id".equals(sort)) sortCol = "id";
            else if ("bet_value".equals(sort) || "base_bet".equals(sort)) sortCol = "total_f1_volume";
            else if ("rebate_rate".equals(sort) || "commission_rate".equals(sort)) sortCol = "rebate_percentage";
            else if ("rebate_amount".equals(sort) || "commission_earned".equals(sort)) sortCol = "rebate_amount";
            else if ("differential_pct".equals(sort) || "settlement_rate".equals(sort)) sortCol = "differential_pct";
            else if ("status".equals(sort)) sortCol = "status";
            else if ("create_time".equals(sort) || "time".equals(sort) || "created_at".equals(sort)) sortCol = "created_at";

            sql.append(" ORDER BY ").append(sortCol).append(" ").append(dir).append(" LIMIT ? OFFSET ?");
            params.add(limit);
            params.add((page - 1) * limit);

            ps = conn.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Integer) ps.setInt(i + 1, (Integer) p);
                else if (p instanceof Long) ps.setLong(i + 1, (Long) p);
                else ps.setString(i + 1, p.toString());
            }
            rs = ps.executeQuery();
            while (rs.next()) {
                results.add(mapLog(rs));
            }
        } catch (Exception e) {
            logger.error("RebateService.queryLogs error", e);
        } finally {
            close(rs, ps, conn);
        }
        return results;
    }

    /**
     * SUN-1201 — agency-LSR-only variant of {@link #queryLogs} that collapses
     * click-events into one display row per (agent, player, game, period,
     * status) tuple. Required because the underlying RealTimeCommission /
     * LogMoneyUserExtraProcessor write one INSERT per bet click, so a
     * multi-side SicBo round used to render N rows in the rolling history
     * (QC SUN-1201: "1 bet 5k và 1 bet 1k và 1 bet 2k" for one 8k round).
     *
     * <p>Schema-first design (V011 migration): {@code player_nickname} and
     * {@code game_action} are real columns with a covering index
     * {@code (agent_user_id, period_start, player_nickname, game_action)}.
     * The GROUP BY is index-supported and pagination is done at SQL level —
     * no in-memory full-set scan, scales linearly with distinct group count.
     *
     * <p>Aggregation rules per group:
     * <ul>
     *   <li>{@code total_f1_volume}, {@code rebate_amount}, {@code net_rebate},
     *       {@code share_amount} — SUM</li>
     *   <li>{@code rebate_percentage}, {@code differential_pct},
     *       {@code own_percentage}, {@code child_percentage} — MAX (rates
     *       are constant within a round; MAX collapses safely)</li>
     *   <li>{@code created_at} — MIN (round-start time)</li>
     *   <li>{@code id} — MIN (visual stable id for "view detail" link)</li>
     *   <li>{@code note} — MIN (representative; the parsed player/action are
     *       authoritative anyway)</li>
     * </ul>
     *
     * <p>Other paths ({@link #queryLogs}, the player rebate history endpoint,
     * admin detail) keep raw click-event rows for audit purposes.
     */
    public static List<Map<String, Object>> queryLogsAggregated(Integer agentUserId, String status,
                                                                 String dateFrom, String dateTo,
                                                                 String sort, String dir,
                                                                 int page, int limit) {
        return queryLogsAggregated(agentUserId, status, dateFrom, dateTo, sort, dir, page, limit, false);
    }

    /**
     * SUN-1240 / SUN-1248 — REVERSED/REJECTED-aware overload.
     *
     * <p>Mirrors the {@code excludeReversed} flag the {@code queryLogs} /
     * {@code countLogs} / {@code summarizeLogs} family already exposes
     * (added in !371). Without it, after the SUN-1248 cutover from
     * {@code queryLogs} → {@code queryLogsAggregated}, the agency LSR
     * list shows REVERSED rows again (regression of !371) while the
     * count/summary still hide them — the two views go out of sync.
     *
     * @param excludeReversed when true, rows with status REVERSED or REJECTED
     *                        are excluded BEFORE the GROUP BY runs.
     */
    public static List<Map<String, Object>> queryLogsAggregated(Integer agentUserId, String status,
                                                                 String dateFrom, String dateTo,
                                                                 String sort, String dir,
                                                                 int page, int limit,
                                                                 boolean excludeReversed) {
        return queryLogsAggregated(agentUserId, status, dateFrom, dateTo, sort, dir, page, limit, excludeReversed, null);
    }

    public static List<Map<String, Object>> queryLogsAggregated(Integer agentUserId, String status,
                                                                 String dateFrom, String dateTo,
                                                                 String sort, String dir,
                                                                 int page, int limit,
                                                                 boolean excludeReversed,
                                                                 String playerFilter) {
        List<Map<String, Object>> results = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getMainConn();
            // Group key: agent_user_id, period_start (day bucket), status,
            // player_nickname, game_action. Rows with NULL player/action
            // (legacy / non-AUTO formats not yet backfilled) fall into a
            // single-row group keyed on id, preserving 1-row-per-event for
            // those — i.e. no incorrect collapse of distinct events.
            StringBuilder sql = new StringBuilder(
                "SELECT MIN(id) AS id, " +
                "       agent_user_id, MIN(agent_nickname) AS agent_nickname, MIN(agent_level) AS agent_level, " +
                "       MIN(period_start) AS period_start, MIN(period_end) AS period_end, " +
                "       MIN(period_type) AS period_type, " +
                "       SUM(total_f1_volume) AS total_f1_volume, " +
                "       MAX(rebate_percentage) AS rebate_percentage, " +
                "       MAX(share_percentage) AS share_percentage, " +
                "       MAX(own_percentage) AS own_percentage, " +
                "       MAX(child_percentage) AS child_percentage, " +
                "       MAX(differential_pct) AS differential_pct, " +
                // SUN-1209: aggregate at full storage precision (DECIMAL(20,4))
                // and round once at display so per-bet rounding errors don't
                // accumulate into the LSR sums. The underlying SUM stays
                // exact (no per-row rounding); ROUND(.., 2) is applied here
                // so the response always has cents-precision regardless of
                // how many bets contributed.
                // SUN-1255: drop ROUND(_, 2). Storage is DECIMAL(20,4) and
                // the agency display is "trim to 3" via PctFormatter.formatMoney.
                // Rounding to 2 here turned 0.735 into 0.74 (HALF_UP) and
                // 2.5025 into 2.50, defeating the trim semantics that QC
                // explicitly asked for. Carry the full storage precision and
                // let the formatter trim at render time.
                "       SUM(rebate_amount) AS rebate_amount, " +
                "       SUM(share_amount) AS share_amount, " +
                "       SUM(net_rebate) AS net_rebate, " +
                "       status, MIN(created_at) AS created_at, " +
                "       MIN(rebate_type) AS rebate_type, " +
                "       MIN(note) AS note, " +
                // SUN-1201: use MIN() for player/game columns instead of
                // bare references — MySQL 8 only_full_group_by mode
                // rejects bare columns when GROUP BY uses an expression
                // (COALESCE) over them. MIN() is value-equivalent within a
                // group (all rows share the same player+game by definition
                // of the grouping key).
                "       MIN(player_nickname) AS player_nickname, " +
                "       MIN(game_action) AS game_action, " +
                // SUN-1248: surface wager_code so the FE can show "round X
                // (wager Y)" if it wants to drill down. MIN() is safe — all
                // grouped rows share the same wager_code by construction
                // (or all NULL).
                "       MIN(wager_code) AS wager_code " +
                "FROM rebate_logs WHERE 1=1");
            List<Object> params = new ArrayList<>();
            if (agentUserId != null && agentUserId > 0) {
                sql.append(" AND agent_user_id = ?"); params.add(agentUserId);
            }
            if (status != null && !status.isEmpty()) {
                sql.append(" AND status = ?"); params.add(status);
            }
            if (dateFrom != null && !dateFrom.isEmpty()) {
                sql.append(" AND period_start >= ?");
                params.add(dateFrom.contains(" ") ? dateFrom : dateFrom + " 00:00:00");
            }
            if (dateTo != null && !dateTo.isEmpty()) {
                sql.append(" AND period_end <= ?");
                params.add(dateTo.contains(" ") ? dateTo : dateTo + " 23:59:59");
            }
            appendBotFilter(sql, params);
            if (excludeReversed) {
                // Filter at the row level (BEFORE GROUP BY) so REVERSED /
                // REJECTED sub-bets don't pull a whole multi-bet round into
                // the result with a partial volume sum.
                sql.append(" AND status NOT IN ('REVERSED','REJECTED')");
            }
            if (playerFilter != null && !playerFilter.trim().isEmpty()) {
                sql.append(" AND player_nickname LIKE ?");
                params.add("%" + playerFilter.trim() + "%");
            }
            // GROUP BY natural keys. period_start is the day bucket — using
            // the raw timestamp would make every distinct-second a new
            // group; use DATE() to bucket per calendar day.
            //
            // SUN-1250 follow-up 2026-05-14: GROUP BY uses source_key (one row
            // per sub-bet, e.g. awc:BAC-386349255 vs awc:BAC-386349256) instead
            // of wager_code (one row per round). Matches the per-sub-bet grain
            // we restored in LS Cược (GameHistoryService.gscHistoryGroupKey)
            // — operator audits Rolling vs LS Cược row-for-row again, instead
            // of seeing a merged Rolling total against split LS Cược rows.
            // Legacy rows (NULL source_key, pre-SUN-1248) keep their
            // per-(player,action,day) collapse via CONCAT NULL fallback.
            sql.append(" GROUP BY agent_user_id, DATE(period_start), status, " +
                       " COALESCE(player_nickname, CONCAT('NULL_PNICK_', id)), " +
                       " COALESCE(game_action,     CONCAT('NULL_GACT_',  id)), " +
                       " COALESCE(source_key,      CONCAT('NULL_SRCKEY_', id))");

            // Sortable column whitelist (mirrors queryLogs).
            if (dir == null || (!dir.equalsIgnoreCase("asc") && !dir.equalsIgnoreCase("desc"))) {
                dir = "desc";
            } else {
                dir = dir.toLowerCase();
            }
            String sortCol = "created_at";
            if ("id".equals(sort)) sortCol = "id";
            else if ("bet_value".equals(sort) || "base_bet".equals(sort)) sortCol = "total_f1_volume";
            else if ("rebate_rate".equals(sort) || "commission_rate".equals(sort)) sortCol = "rebate_percentage";
            else if ("rebate_amount".equals(sort) || "commission_earned".equals(sort)) sortCol = "rebate_amount";
            else if ("differential_pct".equals(sort) || "settlement_rate".equals(sort)) sortCol = "differential_pct";
            else if ("status".equals(sort)) sortCol = "status";
            else if ("create_time".equals(sort) || "time".equals(sort) || "created_at".equals(sort)) sortCol = "created_at";

            sql.append(" ORDER BY ").append(sortCol).append(" ").append(dir).append(" LIMIT ? OFFSET ?");
            params.add(limit);
            params.add((page - 1) * limit);

            ps = conn.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Integer) ps.setInt(i + 1, (Integer) p);
                else if (p instanceof Long) ps.setLong(i + 1, (Long) p);
                else ps.setString(i + 1, p.toString());
            }
            rs = ps.executeQuery();
            while (rs.next()) {
                results.add(mapLog(rs));
            }
        } catch (Exception e) {
            logger.error("RebateService.queryLogsAggregated error", e);
        } finally {
            close(rs, ps, conn);
        }
        return results;
    }

    /** SUN-1201 — distinct-group count matching {@link #queryLogsAggregated}'s
     *  filter, used by the FE for pagination. Counts groups (not raw rows)
     *  using the same {@code GROUP BY} natural-key tuple. */
    public static long countLogsAggregated(Integer agentUserId, String status,
                                            String dateFrom, String dateTo) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getMainConn();
            // COUNT(DISTINCT) over the same natural-key tuple. Faster and
            // simpler than wrapping the GROUP BY query in a derived-table
            // count: MySQL's optimizer handles COUNT(DISTINCT col_list)
            // well when the columns are indexed.
            // SUN-1250 follow-up: include source_key so pagination count
            // matches queryLogsAggregated's per-sub-bet group grain.
            StringBuilder sql = new StringBuilder(
                "SELECT COUNT(DISTINCT agent_user_id, DATE(period_start), status, " +
                "       COALESCE(player_nickname, CONCAT('NULL_PNICK_', id)), " +
                "       COALESCE(game_action,     CONCAT('NULL_GACT_',  id)), " +
                "       COALESCE(source_key,      CONCAT('NULL_SRCKEY_', id))) " +
                "FROM rebate_logs WHERE 1=1");
            List<Object> params = new ArrayList<>();
            if (agentUserId != null && agentUserId > 0) {
                sql.append(" AND agent_user_id = ?"); params.add(agentUserId);
            }
            if (status != null && !status.isEmpty()) {
                sql.append(" AND status = ?"); params.add(status);
            }
            if (dateFrom != null && !dateFrom.isEmpty()) {
                sql.append(" AND period_start >= ?");
                params.add(dateFrom.contains(" ") ? dateFrom : dateFrom + " 00:00:00");
            }
            if (dateTo != null && !dateTo.isEmpty()) {
                sql.append(" AND period_end <= ?");
                params.add(dateTo.contains(" ") ? dateTo : dateTo + " 23:59:59");
            }
            appendBotFilter(sql, params);
            ps = conn.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Integer) ps.setInt(i + 1, (Integer) p);
                else if (p instanceof Long) ps.setLong(i + 1, (Long) p);
                else ps.setString(i + 1, p.toString());
            }
            rs = ps.executeQuery();
            if (rs.next()) return rs.getLong(1);
        } catch (Exception e) {
            logger.error("RebateService.countLogsAggregated error", e);
        } finally {
            close(rs, ps, conn);
        }
        return 0;
    }

    /**
     * Count rebate logs matching the same filters as {@link #queryLogs}.
     * Used to return a total count for FE pagination (without this, FE can't
     * navigate past page 1 — SUN-QC: LS Rolling historical data unreachable).
     */
    public static long countLogs(Integer agentUserId, String status,
                                  String dateFrom, String dateTo) {
        return countLogs(agentUserId, status, dateFrom, dateTo, false, false);
    }

    /** Backward-compatible 5-arg overload — no REVERSED/REJECTED exclusion. */
    public static long countLogs(Integer agentUserId, String status,
                                  String dateFrom, String dateTo, boolean excludeBots) {
        return countLogs(agentUserId, status, dateFrom, dateTo, excludeBots, false);
    }

    /** @param excludeBots     mirrors the same flag on {@link #queryLogs}.
     *  @param excludeReversed mirrors the same flag on {@link #queryLogs}. */
    public static long countLogs(Integer agentUserId, String status,
                                  String dateFrom, String dateTo,
                                  boolean excludeBots, boolean excludeReversed) {
        return countLogs(agentUserId, status, dateFrom, dateTo, excludeBots, excludeReversed, null);
    }

    public static long countLogs(Integer agentUserId, String status,
                                  String dateFrom, String dateTo,
                                  boolean excludeBots, boolean excludeReversed,
                                  String playerFilter) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getMainConn();
            StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM rebate_logs WHERE 1=1");
            List<Object> params = new ArrayList<>();

            if (agentUserId != null && agentUserId > 0) {
                sql.append(" AND agent_user_id = ?");
                params.add(agentUserId);
            }
            if (status != null && !status.isEmpty()) {
                sql.append(" AND status = ?");
                params.add(status);
            }
            if (dateFrom != null && !dateFrom.isEmpty()) {
                sql.append(" AND period_start >= ?");
                params.add(dateFrom.contains(" ") ? dateFrom : dateFrom + " 00:00:00");
            }
            if (dateTo != null && !dateTo.isEmpty()) {
                sql.append(" AND period_end <= ?");
                params.add(dateTo.contains(" ") ? dateTo : dateTo + " 23:59:59");
            }
            if (excludeBots) {
                appendBotFilter(sql, params);
            }
            if (excludeReversed) {
                sql.append(" AND status NOT IN ('REVERSED','REJECTED')");
            }
            if (playerFilter != null && !playerFilter.trim().isEmpty()) {
                sql.append(" AND player_nickname LIKE ?");
                params.add("%" + playerFilter.trim() + "%");
            }

            ps = conn.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Integer) ps.setInt(i + 1, (Integer) p);
                else if (p instanceof Long) ps.setLong(i + 1, (Long) p);
                else ps.setString(i + 1, p.toString());
            }
            rs = ps.executeQuery();
            if (rs.next()) return rs.getLong(1);
        } catch (Exception e) {
            logger.error("RebateService.countLogs error", e);
        } finally {
            close(rs, ps, conn);
        }
        return 0;
    }

    /**
     * Aggregate rebate logs using the same filters as {@link #queryLogs}.
     * `commission_earned` on the agency API maps to `rebate_amount`, so the
     * summary intentionally sums that column to match the list rows exactly.
     *
     * <p>Backward-compatible 4-arg overload — applies neither bot exclusion
     * nor REVERSED/REJECTED exclusion. Used by audit/admin paths.
     */
    public static Map<String, Object> summarizeLogs(Integer agentUserId, String status,
                                                    String dateFrom, String dateTo) {
        return summarizeLogs(agentUserId, status, dateFrom, dateTo, false, false);
    }

    /** Backward-compatible 5-arg overload — no REVERSED/REJECTED exclusion. */
    public static Map<String, Object> summarizeLogs(Integer agentUserId, String status,
                                                    String dateFrom, String dateTo,
                                                    boolean excludeBots) {
        return summarizeLogs(agentUserId, status, dateFrom, dateTo, excludeBots, false);
    }

    /**
     * @param excludeBots     when true, rows whose player_nickname matches a
     *                        user with is_bot=1 are excluded from the sums.
     *                        Mirrors {@link #queryLogs}.
     * @param excludeReversed when true, rows with status REVERSED or REJECTED
     *                        are excluded. Mirrors {@link #queryLogs}.
     */
    public static Map<String, Object> summarizeLogs(Integer agentUserId, String status,
                                                    String dateFrom, String dateTo,
                                                    boolean excludeBots, boolean excludeReversed) {
        return summarizeLogs(agentUserId, status, dateFrom, dateTo, excludeBots, excludeReversed, null);
    }

    public static Map<String, Object> summarizeLogs(Integer agentUserId, String status,
                                                    String dateFrom, String dateTo,
                                                    boolean excludeBots, boolean excludeReversed,
                                                    String playerFilter) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total_count", 0L);
        summary.put("sum_bet_amount", 0L);
        summary.put("sum_commission_earned", 0L);

        // SUN-1108 Tier 3: split the date range into "rollup eligible" (days
        // already aggregated into rebate_daily_rollup, i.e. < CURDATE()) and
        // "live tail" (today). Read pre-aggregated days from the rollup
        // table — O(days × agents) rows — and only the live tail from
        // rebate_logs. Sum on Java side. Falls back to original full scan
        // if the rollup path errors or rollup table is empty for the range.
        boolean rollupEnabled = isRollupEnabled();
        // Rollup table is pre-aggregated without bot filtering — disable until
        // the rollup is rebuilt with the bot-exclusion fix applied at write time.
        boolean canUseRollup = false && !excludeBots && rollupEnabled
                && status != null && "PAID".equals(status)
                && agentUserId != null && agentUserId > 0;

        if (canUseRollup) {
            Map<String, Object> rollupSum = trySummarizeFromRollup(agentUserId, dateFrom, dateTo);
            if (rollupSum != null) return rollupSum;
            // fall through on null (e.g. range edge case) → full scan below
        }

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getMainConn();
            // SUN-1226 + SUN-1233: agency LSR now shows individual bet records
            // (not grouped). total_count must be raw COUNT(*) with bot filter so
            // "Lượt" in the summary header matches the actual number of visible rows.
            // COUNT(DISTINCT groups) was used while the list was aggregated; after
            // SUN-1233 switched to raw records it no longer matches.
            StringBuilder sql = new StringBuilder(
                    "SELECT COUNT(*) AS total_count, " +
                    "COALESCE(SUM(" +
                    ((agentUserId != null && agentUserId > 0) ? "total_f1_volume" :
                    "CASE WHEN rebate_type = 'SELF' OR (rebate_type = 'DOWNLINE' AND child_percentage = 0) THEN total_f1_volume ELSE 0 END") +
                    "), 0) AS sum_bet_amount, " +
                    // SUN-1209 / SUN-1255: keep the aggregated sum at full
                    // storage precision (DECIMAL(20,4)). Java side renders
                    // via PctFormatter.formatMoney which truncates to 3
                    // decimals (DOWN), so values like 1.5750 surface as
                    // "1.575" instead of being half-up rounded to "1.58".
                    "COALESCE(SUM(rebate_amount), 0) AS sum_commission_earned " +
                    "FROM rebate_logs WHERE 1=1");
            List<Object> params = new ArrayList<>();

            if (agentUserId != null && agentUserId > 0) {
                sql.append(" AND agent_user_id = ?");
                params.add(agentUserId);
            }
            if (status != null && !status.isEmpty()) {
                sql.append(" AND status = ?");
                params.add(status);
            }
            if (dateFrom != null && !dateFrom.isEmpty()) {
                sql.append(" AND period_start >= ?");
                params.add(dateFrom.contains(" ") ? dateFrom : dateFrom + " 00:00:00");
            }
            if (dateTo != null && !dateTo.isEmpty()) {
                sql.append(" AND period_end <= ?");
                params.add(dateTo.contains(" ") ? dateTo : dateTo + " 23:59:59");
            }
            if (excludeBots) {
                appendBotFilter(sql, params);
            }
            if (excludeReversed) {
                sql.append(" AND status NOT IN ('REVERSED','REJECTED')");
            }
            if (playerFilter != null && !playerFilter.trim().isEmpty()) {
                sql.append(" AND player_nickname LIKE ?");
                params.add("%" + playerFilter.trim() + "%");
            }

            ps = conn.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Integer) ps.setInt(i + 1, (Integer) p);
                else if (p instanceof Long) ps.setLong(i + 1, (Long) p);
                else ps.setString(i + 1, p.toString());
            }
            rs = ps.executeQuery();
            if (rs.next()) {
                summary.put("total_count", rs.getLong("total_count"));
                summary.put("sum_bet_amount", rs.getLong("sum_bet_amount"));
                // SUN-1150 read-side: SUM(DECIMAL(20,2)) → BigDecimal.
                java.math.BigDecimal sumCom = rs.getBigDecimal("sum_commission_earned");
                // SUN-1255: 3-decimal truncate. See per-row comment in mapLog.
                summary.put("sum_commission_earned",
                        com.vinplay.dal.utils.PctFormatter.formatMoney(
                                sumCom != null ? sumCom : java.math.BigDecimal.ZERO));
            }
        } catch (Exception e) {
            logger.error("RebateService.summarizeLogs error", e);
        } finally {
            close(rs, ps, conn);
        }
        return summary;
    }

    /**
     * Supervisor-view summary helper. Computes total commission LIABILITY
     * for the filter scope as Σ(total_f1_volume × rebate_percentage / 100)
     * — i.e. the full configured rate per row, NOT the cascade-allocated
     * differential. Used by the agency Rolling endpoint when the resolved
     * agent is a SpecialAccount-class observer (useragent.code='0') so
     * the dashboard total reflects what the platform paid out per bet,
     * not the observer's own (always-zero) cascade slice.
     */
    public static java.math.BigDecimal supervisorSumCommission(Integer agentUserId,
                                                               String dateFrom, String dateTo,
                                                               boolean excludeBots, boolean excludeReversed) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getMainConn();
            StringBuilder sql = new StringBuilder(
                    "SELECT COALESCE(ROUND(SUM(total_f1_volume * rebate_percentage / 100), 2), 0) AS s " +
                    "FROM rebate_logs WHERE 1=1");
            List<Object> params = new ArrayList<>();
            if (agentUserId != null && agentUserId > 0) {
                sql.append(" AND agent_user_id = ?");
                params.add(agentUserId);
            }
            if (dateFrom != null && !dateFrom.isEmpty()) {
                sql.append(" AND period_start >= ?");
                params.add(dateFrom.contains(" ") ? dateFrom : dateFrom + " 00:00:00");
            }
            if (dateTo != null && !dateTo.isEmpty()) {
                sql.append(" AND period_end <= ?");
                params.add(dateTo.contains(" ") ? dateTo : dateTo + " 23:59:59");
            }
            if (excludeBots) {
                appendBotFilter(sql, params);
            }
            if (excludeReversed) {
                sql.append(" AND status NOT IN ('REVERSED','REJECTED')");
            }
            ps = conn.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Integer) ps.setInt(i + 1, (Integer) p);
                else if (p instanceof Long) ps.setLong(i + 1, (Long) p);
                else ps.setString(i + 1, p.toString());
            }
            rs = ps.executeQuery();
            if (rs.next()) {
                java.math.BigDecimal v = rs.getBigDecimal("s");
                if (v != null) return v;
            }
        } catch (Exception e) {
            logger.error("RebateService.supervisorSumCommission error", e);
        } finally {
            close(rs, ps, conn);
        }
        return java.math.BigDecimal.ZERO;
    }

    private static boolean isRollupEnabled() {
        String v = System.getenv("REBATE_ROLLUP_ENABLED");
        if (v == null) return true;
        return !"false".equalsIgnoreCase(v) && !"0".equals(v);
    }

    /**
     * SUN-1108 Tier 3: read pre-aggregated rebate_daily_rollup for any
     * day already closed (< CURDATE()), live rebate_logs for today.
     * Returns null on any error / unexpected shape so the caller falls
     * back to the legacy full-scan path.
     */
    private static Map<String, Object> trySummarizeFromRollup(Integer agentUserId, String dateFrom, String dateTo) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total_count", 0L);
        out.put("sum_bet_amount", 0L);
        out.put("sum_commission_earned", "0.000");
        long count = 0L, sumBet = 0L;
        // SUN-1150 read-side: rebate_amount is DECIMAL(20,2); rollup
        // sum_commission will be widened to DECIMAL by V17. Use BigDecimal so
        // partial-day live tail + closed-day rollup both add cleanly.
        java.math.BigDecimal sumCommission = java.math.BigDecimal.ZERO;
        Connection conn = null;
        try {
            conn = getMainConn();
            // Historical pre-aggregated portion.
            String rollupSql =
                    "SELECT COALESCE(SUM(row_count), 0) AS c, " +
                    "       COALESCE(SUM(sum_bet_amount), 0) AS b, " +
                    "       COALESCE(SUM(sum_commission), 0) AS r " +
                    "  FROM rebate_daily_rollup " +
                    " WHERE agent_user_id = ? " +
                    "   AND rollup_date < CURDATE() " +
                    (dateFrom != null && !dateFrom.isEmpty() ? "AND rollup_date >= ? " : "") +
                    (dateTo   != null && !dateTo.isEmpty()   ? "AND rollup_date <= ? " : "");
            try (PreparedStatement ps = conn.prepareStatement(rollupSql)) {
                int idx = 1;
                ps.setInt(idx++, agentUserId);
                if (dateFrom != null && !dateFrom.isEmpty()) {
                    ps.setString(idx++, dateFrom.length() >= 10 ? dateFrom.substring(0, 10) : dateFrom);
                }
                if (dateTo != null && !dateTo.isEmpty()) {
                    ps.setString(idx++, dateTo.length() >= 10 ? dateTo.substring(0, 10) : dateTo);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        count         += rs.getLong("c");
                        sumBet        += rs.getLong("b");
                        java.math.BigDecimal r = rs.getBigDecimal("r");
                        if (r != null) sumCommission = sumCommission.add(r);
                    }
                }
            }
            // Live tail — rows from today only.
            String liveSql =
                    "SELECT COUNT(*) AS c, " +
                    "       COALESCE(SUM(total_f1_volume), 0) AS b, " +
                    "       COALESCE(SUM(rebate_amount), 0) AS r " +
                    "  FROM rebate_logs " +
                    " WHERE agent_user_id = ? " +
                    "   AND DATE(created_at) = CURDATE() " +
                    "   AND status = 'PAID' " +
                    (dateFrom != null && !dateFrom.isEmpty() ? "AND period_start >= ? " : "") +
                    (dateTo   != null && !dateTo.isEmpty()   ? "AND period_end   <= ? " : "");
            try (PreparedStatement ps = conn.prepareStatement(liveSql)) {
                int idx = 1;
                ps.setInt(idx++, agentUserId);
                if (dateFrom != null && !dateFrom.isEmpty()) {
                    ps.setString(idx++, dateFrom.contains(" ") ? dateFrom : dateFrom + " 00:00:00");
                }
                if (dateTo != null && !dateTo.isEmpty()) {
                    ps.setString(idx++, dateTo.contains(" ") ? dateTo : dateTo + " 23:59:59");
                }
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        count         += rs.getLong("c");
                        sumBet        += rs.getLong("b");
                        java.math.BigDecimal r = rs.getBigDecimal("r");
                        if (r != null) sumCommission = sumCommission.add(r);
                    }
                }
            }
            out.put("total_count", count);
            out.put("sum_bet_amount", sumBet);
            // SUN-1255: 3-decimal truncate for the rollup summary too —
            // matches the live-path summary in summarizeLogs so both
            // entry points emit consistent formatting.
            out.put("sum_commission_earned",
                    com.vinplay.dal.utils.PctFormatter.formatMoney(sumCommission));
            return out;
        } catch (Exception e) {
            logger.warn("RebateService.trySummarizeFromRollup failed, falling back: " + e.getMessage());
            return null;
        } finally {
            close(conn);
        }
    }

    /**
     * Get a single rebate log by ID.
     */
    public static Map<String, Object> getLog(long logId) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getMainConn();
            ps = conn.prepareStatement("SELECT * FROM rebate_logs WHERE id = ?");
            ps.setLong(1, logId);
            rs = ps.executeQuery();
            if (rs.next()) return mapLog(rs);
        } catch (Exception e) {
            logger.error("RebateService.getLog error logId=" + logId, e);
        } finally {
            close(rs, ps, conn);
        }
        return null;
    }

    /**
     * Claim a PENDING log for payout processing.
     * Moves status PENDING -> APPROVED atomically.
     */
    public static boolean claimForPayout(long logId, String approvedBy) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getMainConn();
            ps = conn.prepareStatement(
                    "UPDATE rebate_logs SET status = 'APPROVED', approved_by = ?, approved_at = NOW() " +
                    "WHERE id = ? AND status = 'PENDING'");
            ps.setString(1, approvedBy);
            ps.setLong(2, logId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("RebateService.claimForPayout error logId=" + logId, e);
            return false;
        } finally {
            close(ps, conn);
        }
    }

    /**
     * Revert payout claim if crediting failed.
     * Moves status APPROVED -> PENDING.
     */
    public static boolean revertPayoutClaim(long logId) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getMainConn();
            ps = conn.prepareStatement(
                    "UPDATE rebate_logs SET status = 'PENDING', approved_by = NULL, approved_at = NULL " +
                            "WHERE id = ? AND status = 'APPROVED'");
            ps.setLong(1, logId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("RebateService.revertPayoutClaim error logId=" + logId, e);
            return false;
        } finally {
            close(ps, conn);
        }
    }

    /**
     * Update rebate log status to PAID after claim.
     * Moves status APPROVED -> PAID.
     */
    public static boolean markPaid(long logId, String approvedBy) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getMainConn();
            ps = conn.prepareStatement(
                    "UPDATE rebate_logs SET status = 'PAID', approved_by = ?, approved_at = NOW() " +
                            "WHERE id = ? AND status = 'APPROVED'");
            ps.setString(1, approvedBy);
            ps.setLong(2, logId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("RebateService.markPaid error logId=" + logId, e);
            return false;
        } finally {
            close(ps, conn);
        }
    }

    /**
     * Reject a rebate log.
     */
    public static boolean rejectLog(long logId, String rejectedBy, String reason) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getMainConn();
            ps = conn.prepareStatement(
                    "UPDATE rebate_logs SET status = 'REJECTED', approved_by = ?, approved_at = NOW(), " +
                    "note = ? WHERE id = ? AND status = 'PENDING'");
            ps.setString(1, rejectedBy);
            ps.setString(2, reason);
            ps.setLong(3, logId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("RebateService.rejectLog error logId=" + logId, e);
            return false;
        } finally {
            close(ps, conn);
        }
    }

    private static Map<String, Object> mapLog(ResultSet rs) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", rs.getLong("id"));
        m.put("bet_amount", rs.getLong("total_f1_volume"));
        m.put("commission_rate", pct(rs, "rebate_percentage"));
        // Stable mirror of the agent's configured rate. Kept distinct from
        // commission_rate so a future remap of commission_rate (e.g. to
        // differential_pct) doesn't strand callers that need the configured
        // rate — notably GetRebateLogs4AgencyProcessor's supervisor view
        // override which reads own_rate_pct to compute per-bet liability.
        m.put("own_rate_pct", pct(rs, "rebate_percentage"));
        // Re-applies QC fix from 533a8b7d (Apr 25) — got reverted during
        // SUN-1115 cashback routing merge. Emit as 2-decimal String for
        // visual parity with commission_rate / differential_pct (both Strings).
        // SUN-1150 read-side: rebate_amount is DECIMAL(20,2). getLong drops
        // the fractional part; read as BigDecimal so 8.40 stays 8.40 in the
        // API response. Without this, all the precision SUN-1150 added on
        // the write side gets thrown away here.
        java.math.BigDecimal earned = rs.getBigDecimal("rebate_amount");
        // SUN-1255: render money at 3 decimals with TRUNCATE (DOWN), never
        // round-up. Tester saw 150 × 1.05% = 1.575 displayed as "1.58";
        // ops policy is to trim. Storage is DECIMAL(20,4) so the cut at
        // scale=3 is lossless for any rate ≤ 99.9% × bet ≤ 1e15.
        m.put("commission_earned",
                com.vinplay.dal.utils.PctFormatter.formatMoney(
                        earned != null ? earned : java.math.BigDecimal.ZERO));
        m.put("differential_pct", pct(rs, "differential_pct"));
        m.put("status", rs.getString("status"));
        m.put("created_at", rs.getString("created_at"));
        try { m.put("rebate_type", rs.getString("rebate_type")); } catch (Exception e) { m.put("rebate_type", "DOWNLINE"); }

        // Parse player nickname and game from note
        // Old/Downline format: "Realtime from <nick> game=<action>"
        // New/Self PENDING format: "Cashback <action> bet=<amount>"
        String note = rs.getString("note");
        String playerNick = "";
        String gameAction = "";
        try {
            String columnPlayer = rs.getString("player_nickname");
            if (columnPlayer != null && !columnPlayer.trim().isEmpty()) {
                playerNick = columnPlayer.trim();
            }
        } catch (Exception ignored) {}
        try {
            String columnAction = rs.getString("game_action");
            if (columnAction != null && !columnAction.trim().isEmpty()) {
                gameAction = columnAction.trim();
            }
        } catch (Exception ignored) {}
        if (note != null) {
            if (note.startsWith("Realtime from ")) {
                String rest = note.substring("Realtime from ".length());
                int gameIdx = rest.indexOf(" game=");
                if (gameIdx > 0 && (playerNick.isEmpty() || gameAction.isEmpty())) {
                    playerNick = rest.substring(0, gameIdx);
                    gameAction = rest.substring(gameIdx + " game=".length());
                }
            } else if (note.startsWith("Cashback ")) {
                // "Cashback <action> bet=<amount>"
                String rest = note.substring("Cashback ".length());
                int betIdx = rest.indexOf(" bet=");
                if (betIdx > 0 && gameAction.isEmpty()) {
                    gameAction = rest.substring(0, betIdx);
                    // For SELF PENDING, the player is the agent themselves
                    if (playerNick.isEmpty()) {
                        try { playerNick = rs.getString("agent_nickname"); } catch (Exception e) {}
                    }
                }
            } else if (note.startsWith("AUTO_COMMISSION ")) {
                // "AUTO_COMMISSION source=... type=DOWNLINE user=Kwon_DL2 action=TaiXiu service=2"
                int actionIdx = note.indexOf(" action=");
                int userIdx = note.indexOf(" user=");
                if (actionIdx > 0 && gameAction.isEmpty()) {
                    int end = note.indexOf(' ', actionIdx + 8);
                    gameAction = note.substring(actionIdx + 8, end > 0 ? end : note.length());
                }
                if (userIdx > 0 && playerNick.isEmpty()) {
                    int end = note.indexOf(' ', userIdx + 6);
                    playerNick = note.substring(userIdx + 6, end > 0 ? end : note.length());
                }
            } else if (note.startsWith("Auto-cron from user=")) {
                // "Auto-cron from user=<nick>"
                if (playerNick.isEmpty()) playerNick = note.substring("Auto-cron from user=".length());
                if (gameAction.isEmpty()) gameAction = "T\u1ED5ng h\u1EE3p"; // "Tổng hợp" (All/Aggregate)
            }
        }
        m.put("player_nickname", playerNick);
        m.put("game_action", gameAction);

        // Resolve gameAction → display name (game_name + game_key) in the
        // same place the gameAction was parsed. Earlier the processor layer
        // (GetRebateLogs4AgencyProcessor / GetRebateLogsProcessor) tried to
        // do this from row.get("note"), but mapLog never put the raw note
        // in the row — so game_name was always missing in the API response,
        // causing the FE Game column to render blank and QC to think rows
        // were missing.
        if (gameAction != null && !gameAction.isEmpty()) {
            String gameName = gameAction;
            String gameKey  = gameAction;
            try {
                if (gameAction.startsWith("gsc_")) {
                    String[] parts = gameAction.split("_", 3);
                    if (parts.length == 3) {
                        int pc = Integer.parseInt(parts[1]);
                        String gc = parts[2];
                        gameName = com.vinplay.dal.service.GscGameNameResolver.displayName(pc, gc);
                        if (gameName == null || gameName.isEmpty()) gameName = gameAction;
                    }
                } else if (gameAction.startsWith("awc_")) {
                    String[] parts = gameAction.split("_", 3);
                    if (parts.length == 3) {
                        String platform     = parts[1];
                        String gc           = parts[2];
                        // SUN-1252 reopen: pass wager_code (= AWC roundId per SUN-1248
                        // / SUN-1250 backfill) so the resolver can append the parsed
                        // table tag — Sexy Live Baccarat tables share game_code
                        // MX-LIVE-001 and only differ by the round_id prefix
                        // (Mexico-NN-... → "Sexy Baccarat MNN", RNN-... → "...CNN").
                        // Without this, LS Rolling shows "Sexy Baccarat (SEXYBCRT)"
                        // while LS Cược shows "Sexy Baccarat M01 (SEXYBCRT)" —
                        // QC's "naming không match" reopen.
                        String wagerCode = null;
                        try { wagerCode = rs.getString("wager_code"); } catch (Exception ignored) {}
                        String resolved     = com.vinplay.dal.service.AwcGameNameResolver.displayName(platform, gc, wagerCode);
                        String platformUpper= platform == null ? "" : platform.toUpperCase();
                        // Use the curated catalog name as-is; the platform tag
                        // already lives in the catalog row (e.g. "Sexy Baccarat M09")
                        // so appending "(SEXYBCRT)" was redundant noise QC asked
                        // to drop. LS Cược renderer below uses the same shape.
                        if (resolved != null && !resolved.isEmpty()) {
                            gameName = resolved;
                        } else if (!platformUpper.isEmpty()) {
                            gameName = platformUpper;
                        } else {
                            gameName = gameAction;
                        }
                    }
                }
            } catch (Exception ignore) {
                gameName = gameAction;
            }
            m.put("game_name", gameName);
            m.put("game_key", gameKey);
        }

        return m;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  REBATE PAYOUT
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Record a payout transaction.
     */
    public static long createPayout(long rebateLogId, int agentUserId, String nickname,
                                     long payoutAmount, long balanceBefore, long balanceAfter,
                                     String triggeredBy, String note) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getMainConn();
            ps = conn.prepareStatement(
                    "INSERT INTO rebate_payout (rebate_log_id, agent_user_id, agent_nickname, " +
                    "payout_amount, balance_before, balance_after, triggered_by, note) " +
                    "VALUES (?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, rebateLogId);
            ps.setInt(2, agentUserId);
            ps.setString(3, nickname);
            ps.setLong(4, payoutAmount);
            ps.setLong(5, balanceBefore);
            ps.setLong(6, balanceAfter);
            ps.setString(7, triggeredBy);
            ps.setString(8, note);
            ps.executeUpdate();
            rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getLong(1);
        } catch (Exception e) {
            logger.error("RebateService.createPayout error rebateLogId=" + rebateLogId, e);
        } finally {
            close(rs, ps, conn);
        }
        return -1;
    }

    /**
     * Get payout history for an agent.
     */
    public static List<Map<String, Object>> getPayouts(int agentUserId, int page, int limit) {
        List<Map<String, Object>> results = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getMainConn();
            String sql = "SELECT p.*, l.period_start, l.period_end, l.period_type, " +
                    "l.total_f1_volume, l.rebate_percentage " +
                    "FROM rebate_payout p " +
                    "JOIN rebate_logs l ON l.id = p.rebate_log_id " +
                    "WHERE p.agent_user_id = ? ORDER BY p.created_at DESC LIMIT ? OFFSET ?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, agentUserId);
            ps.setInt(2, limit);
            ps.setInt(3, (page - 1) * limit);
            rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", rs.getLong("id"));
                m.put("rebate_log_id", rs.getLong("rebate_log_id"));
                m.put("payout_amount", rs.getLong("payout_amount"));
                m.put("balance_before", rs.getLong("balance_before"));
                m.put("balance_after", rs.getLong("balance_after"));
                m.put("triggered_by", rs.getString("triggered_by"));
                m.put("note", rs.getString("note"));
                m.put("created_at", rs.getString("created_at"));
                m.put("period_start", rs.getString("period_start"));
                m.put("period_end", rs.getString("period_end"));
                m.put("period_type", rs.getString("period_type"));
                m.put("total_f1_volume", rs.getLong("total_f1_volume"));
                m.put("rebate_percentage", pct(rs, "rebate_percentage"));
                results.add(m);
            }
        } catch (Exception e) {
            logger.error("RebateService.getPayouts error agentUserId=" + agentUserId, e);
        } finally {
            close(rs, ps, conn);
        }
        return results;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  DASHBOARD AGGREGATION
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Get dashboard data: for each agent with config, aggregate volume/rebate stats.
     */
    public static List<Map<String, Object>> getDashboard(String dateFrom, String dateTo,
                                                          int page, int limit) {
        List<Map<String, Object>> results = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getMainConn();
            StringBuilder sql = new StringBuilder(
                    "SELECT c.agent_user_id, c.agent_nickname, c.rebate_percentage, c.share_percentage, " +
                    "c.period_type, c.is_active, " +
                    "COALESCE(SUM(CASE WHEN l.status IN ('PENDING','APPROVED','PAID') THEN l.total_f1_volume ELSE 0 END), 0) as total_volume, " +
                    "COALESCE(SUM(CASE WHEN l.status IN ('PENDING','APPROVED','PAID') THEN l.net_rebate ELSE 0 END), 0) as total_rebate, " +
                    "COALESCE(SUM(CASE WHEN l.status = 'PENDING' THEN l.net_rebate ELSE 0 END), 0) as total_unpaid, " +
                    "COALESCE(SUM(CASE WHEN l.status = 'PAID' THEN l.net_rebate ELSE 0 END), 0) as total_paid " +
                    "FROM rebate_config c " +
                    "LEFT JOIN rebate_logs l ON l.agent_user_id = c.agent_user_id");

            List<Object> params = new ArrayList<>();
            boolean hasDateFilter = false;
            if (dateFrom != null && !dateFrom.isEmpty()) {
                sql.append(" AND l.period_start >= ?");
                params.add(dateFrom);
                hasDateFilter = true;
            }
            if (dateTo != null && !dateTo.isEmpty()) {
                sql.append(" AND l.period_end <= ?");
                params.add(dateTo);
            }
            sql.append(" GROUP BY c.agent_user_id ORDER BY total_volume DESC LIMIT ? OFFSET ?");
            params.add(limit);
            params.add((page - 1) * limit);

            ps = conn.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Integer) ps.setInt(i + 1, (Integer) p);
                else ps.setString(i + 1, p.toString());
            }
            rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("agent_user_id", rs.getInt("agent_user_id"));
                m.put("agent_nickname", rs.getString("agent_nickname"));
                m.put("rebate_percentage", pct(rs, "rebate_percentage"));
                m.put("share_percentage", pct(rs, "share_percentage"));
                m.put("period_type", rs.getString("period_type"));
                m.put("is_active", rs.getInt("is_active"));
                m.put("total_volume", rs.getLong("total_volume"));
                m.put("total_rebate", rs.getLong("total_rebate"));
                m.put("total_unpaid", rs.getLong("total_unpaid"));
                m.put("total_paid", rs.getLong("total_paid"));
                results.add(m);
            }
        } catch (Exception e) {
            logger.error("RebateService.getDashboard error", e);
        } finally {
            close(rs, ps, conn);
        }
        return results;
    }

    /**
     * Get summary statistics for a specific agent (used by Portal API).
     */
    public static Map<String, Object> getAgentSummary(int agentUserId) {
        Map<String, Object> summary = new LinkedHashMap<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getMainConn();
            ps = conn.prepareStatement(
                    "SELECT " +
                    "COALESCE(SUM(total_f1_volume), 0) as total_volume, " +
                    "COALESCE(SUM(CASE WHEN status IN ('PENDING','APPROVED','PAID') THEN net_rebate ELSE 0 END), 0) as total_earned, " +
                    "COALESCE(SUM(CASE WHEN status = 'PENDING' THEN net_rebate ELSE 0 END), 0) as total_pending, " +
                    "COALESCE(SUM(CASE WHEN status = 'PAID' THEN net_rebate ELSE 0 END), 0) as total_paid, " +
                    "COUNT(*) as total_periods " +
                    "FROM rebate_logs WHERE agent_user_id = ?");
            ps.setInt(1, agentUserId);
            rs = ps.executeQuery();
            if (rs.next()) {
                summary.put("total_volume", rs.getLong("total_volume"));
                summary.put("total_earned", rs.getLong("total_earned"));
                summary.put("total_pending", rs.getLong("total_pending"));
                summary.put("total_paid", rs.getLong("total_paid"));
                summary.put("total_periods", rs.getInt("total_periods"));
            }

            // Add config info
            Map<String, Object> config = getConfig(agentUserId);
            if (config != null) {
                summary.put("rebate_percentage", config.get("rebate_percentage"));
                summary.put("share_percentage", config.get("share_percentage"));
                summary.put("period_type", config.get("period_type"));
            }
        } catch (Exception e) {
            logger.error("RebateService.getAgentSummary error agentUserId=" + agentUserId, e);
        } finally {
            close(rs, ps, conn);
        }
        return summary;
    }

    /**
     * Get all PENDING rebate logs (for batch payout).
     */
    public static List<Map<String, Object>> getPendingLogs() {
        return queryLogs(null, "PENDING", null, null, "create_time", "asc", 1, 10000);
    }

    /**
     * Credit commission to agency wallet instead of game wallet.
     * Also logs the transaction to agency_wallet_transactions.
     */
    public static boolean creditAgencyWallet(int agentId, long amount) {
        return creditAgencyWallet(agentId, amount, "COMMISSION_DOWNLINE", null, null, null);
    }

    public static boolean creditAgencyWallet(int agentId, long amount, String type,
                                              String relatedUser, String gameAction, String note) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getMainConn();
            ps = conn.prepareStatement(
                    "INSERT INTO vinplay.agency_wallet (agent_id, balance, updated_at) VALUES (?, ?, CURRENT_TIMESTAMP) " +
                    "ON DUPLICATE KEY UPDATE balance = balance + VALUES(balance), updated_at = CURRENT_TIMESTAMP");
            ps.setInt(1, agentId);
            ps.setLong(2, amount);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                logWalletTransaction(conn, agentId, type, amount, "CREDIT", relatedUser, gameAction, note);
            }
            return ok;
        } catch (Exception e) {
            logger.error("RebateService.creditAgencyWallet error agentId=" + agentId, e);
            return false;
        } finally {
            close(ps, conn);
        }
    }

    /**
     * Get current agency wallet balance.
     */
    public static long getAgencyWalletBalance(int agentId) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getMainConn();
            ps = conn.prepareStatement("SELECT balance FROM vinplay.agency_wallet WHERE agent_id = ?");
            ps.setInt(1, agentId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getLong("balance");
            return 0;
        } catch (Exception e) {
            logger.error("RebateService.getAgencyWalletBalance error agentId=" + agentId, e);
            return 0;
        } finally {
            close(ps, conn);
        }
    }

    /**
     * Debit (subtract) from agency wallet. Used for rollback on payout failure.
     */
    public static boolean debitAgencyWallet(int agentId, long amount) {
        return debitAgencyWallet(agentId, amount, "CONVERT_TO_GAME", null);
    }

    public static boolean debitAgencyWallet(int agentId, long amount, String type, String note) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getMainConn();
            ps = conn.prepareStatement(
                    "UPDATE vinplay.agency_wallet SET balance = balance - ?, updated_at = CURRENT_TIMESTAMP " +
                    "WHERE agent_id = ? AND balance >= ?");
            ps.setLong(1, amount);
            ps.setInt(2, agentId);
            ps.setLong(3, amount);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                logWalletTransaction(conn, agentId, type, amount, "DEBIT", null, null, note);
            }
            return ok;
        } catch (Exception e) {
            logger.error("RebateService.debitAgencyWallet error agentId=" + agentId, e);
            return false;
        } finally {
            close(ps, conn);
        }
    }

    /**
     * SUN-1182: reverse rebate_logs entries created for a single bet that was
     * later cancelled / rolled-back / voided by the seamless wallet provider.
     *
     * <p>Bet-placement publishes one {@code AUTO_COMMISSION} row per agent in the
     * chain, and the {@code note} column embeds {@code source={sourceKey}}
     * where {@code sourceKey} is unique per platform-tx (e.g.
     * {@code gsc:1002:roul:abc123} for GSC, {@code awc:<platformTxId>} for AWC).
     * Cancellation reverses every non-terminal row that matches that exact
     * source.
     *
     * <p>For each match:
     * <ul>
     *   <li>If status was {@code PAID} and {@code rebate_amount &gt; 0}: debit the
     *       amount from {@code agency_wallet} (allowed to go negative — agent may
     *       have already spent the commission; ops reconciles).</li>
     *   <li>{@code UPDATE} status to {@code REVERSED} and append
     *       {@code "| REVERT: <reason>"} to the existing note for audit.</li>
     * </ul>
     *
     * <p>Idempotent: rows already in {@code REVERSED} or {@code REJECTED}
     * (terminal) are skipped, so a duplicate cancel callback is a no-op.
     *
     * <p>Scoped to {@code created_at &gt; NOW() - INTERVAL 30 DAY} so the lookup
     * uses {@code idx_rebate_agent_created} instead of full-scanning the
     * unindexed {@code note} column. Cancellations &gt;30 days late require
     * the manual c=9759 admin endpoint.
     *
     * @param sourceKey  exact bet identifier embedded in note's
     *                   {@code source=} field — must NOT include the
     *                   trailing space or any other note suffix
     * @param reason     short tag stored in note (e.g. "cancel-bet", "void-bet",
     *                   "rollback")
     * @return distinct {@code agent_user_id}s whose rows were reversed —
     *         caller invalidates {@code ResponseCacheHelper} per agent so
     *         admin agency-rolling refreshes immediately
     */
    public static java.util.Set<Integer> reverseBySourceKey(String sourceKey, String reason) {
        if (sourceKey == null || sourceKey.isEmpty()) {
            return java.util.Collections.emptySet();
        }
        // Exact-prefix match. Escape LIKE wildcards in sourceKey — AWC
        // platformTxIds occasionally contain '_'.
        String esc = sourceKey
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        String pattern = "AUTO_COMMISSION source=" + esc + " %";
        return reverseByLikePattern(pattern, reason, "source=" + sourceKey);
    }

    /**
     * SUN-1182 GSC variant: reverse rebate_logs created for a GSC bet
     * identified only by {@code wager_code} (CancelRequest.Transaction
     * lacks {@code game_code}, so the full source key
     * {@code gsc:{product}:{game}:{wager}} can't be reconstructed from
     * the cancel callback alone).
     *
     * <p>Uses a LIKE pattern matching {@code gsc:{productCode}:%:{wager} }
     * so any {@code game_code} in the middle position matches. The
     * trailing space anchors the wager_code to the end of the source
     * segment so e.g. {@code abc} won't accidentally collide with
     * {@code abcd}.
     *
     * @param productCode  GSC product code as string ("1006", "1002", …) —
     *                     pass empty/null to match across products
     * @param wagerCode    provider-issued unique wager code
     */
    public static java.util.Set<Integer> reverseGscByWagerCode(String productCode,
                                                                String wagerCode, String reason) {
        if (wagerCode == null || wagerCode.trim().isEmpty()) {
            return java.util.Collections.emptySet();
        }
        // Escape LIKE wildcards in wagerCode (provider-supplied —
        // platforms like CQ9 occasionally embed '_'). productCode is
        // numeric so doesn't need escaping when present.
        String escWager = wagerCode.trim()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        String pcSegment = (productCode != null && !productCode.trim().isEmpty())
                ? productCode.trim()
                : "%";
        String pattern = "AUTO_COMMISSION source=gsc:" + pcSegment + ":%:" + escWager + " %";
        return reverseByLikePattern(pattern, reason, "gsc-wager:" + productCode + ":" + wagerCode);
    }

    /**
     * Internal: shared implementation for {@link #reverseBySourceKey} and
     * {@link #reverseGscByWagerCode}. {@code likePattern} is fed directly
     * into a {@code LIKE ? ESCAPE '\\'} clause — caller is responsible for
     * escaping {@code %} and {@code _} in any user-supplied segments.
     *
     * <p>{@code logTag} is a short identifier shown in logs to disambiguate
     * which call-site produced the reversal; not stored in the row.
     */
    private static java.util.Set<Integer> reverseByLikePattern(String likePattern,
                                                                 String reason, String logTag) {
        java.util.Set<Integer> affectedAgents = new java.util.HashSet<>();
        if (reason == null) reason = "";
        Connection conn = null;
        try {
            conn = getMainConn();
            String findSql =
                    "SELECT id, agent_user_id, rebate_amount, status " +
                    "FROM vinplay.rebate_logs " +
                    "WHERE created_at > NOW() - INTERVAL 30 DAY " +
                    "  AND status NOT IN ('REVERSED','REJECTED') " +
                    "  AND note LIKE ? ESCAPE '\\\\'";
            java.util.List<long[]> matches = new java.util.ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(findSql)) {
                ps.setString(1, likePattern);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int agentId = rs.getInt("agent_user_id");
                        String status = rs.getString("status");
                        java.math.BigDecimal amt = rs.getBigDecimal("rebate_amount");
                        long debit = 0L;
                        if ("PAID".equals(status) && amt != null && amt.signum() > 0) {
                            debit = amt.setScale(0, java.math.RoundingMode.HALF_UP)
                                       .longValueExact();
                        }
                        matches.add(new long[]{rs.getLong("id"), agentId, debit});
                    }
                }
            }
            if (matches.isEmpty()) return affectedAgents;

            com.vinplay.dal.dao.AgencyWalletDao walletDao =
                    new com.vinplay.dal.dao.impl.AgencyWalletDaoImpl();
            for (long[] r : matches) {
                long id = r[0];
                int agentId = (int) r[1];
                long debit = r[2];
                if (debit > 0L) {
                    try {
                        walletDao.addBalance(agentId, -debit);
                        logWalletTransaction(conn, agentId, "COMMISSION_REVERSE",
                                debit, "DEBIT", null, null,
                                "Auto-reverse " + reason + " " + logTag);
                    } catch (Exception e) {
                        logger.warn("RebateService.reverseByLikePattern: wallet debit failed agentId="
                                + agentId + " amount=" + debit + " " + logTag
                                + " err=" + e.getMessage());
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE vinplay.rebate_logs SET status='REVERSED', " +
                        "note=CONCAT(COALESCE(note,''),' | REVERT: ',?) " +
                        "WHERE id = ? AND status NOT IN ('REVERSED','REJECTED')")) {
                    ps.setString(1, reason);
                    ps.setLong(2, id);
                    ps.executeUpdate();
                }
                affectedAgents.add(agentId);
            }
            logger.info("RebateService.reverseByLikePattern: reversed "
                    + matches.size() + " row(s) " + logTag
                    + " reason=" + reason + " agents=" + affectedAgents.size());
        } catch (Exception e) {
            logger.error("RebateService.reverseByLikePattern error " + logTag, e);
        } finally {
            close(conn);
        }
        return affectedAgents;
    }

    /**
     * Log a wallet transaction to agency_wallet_transactions.
     * Best-effort — failures are logged but don't block the caller.
     */
    static void logWalletTransaction(Connection conn, int agentId, String type,
                                      long amount, String direction,
                                      String relatedUser, String gameAction, String note) {
        PreparedStatement ps = null;
        try {
            // Get agent nickname
            String agentNick = "";
            try (PreparedStatement nps = conn.prepareStatement(
                    "SELECT nickname FROM vinplay_admin.useragent WHERE id = ?")) {
                nps.setInt(1, agentId);
                try (java.sql.ResultSet rs = nps.executeQuery()) {
                    if (rs.next()) agentNick = rs.getString("nickname");
                }
            }
            // Get current balance after the operation
            long balanceAfter = 0;
            try (PreparedStatement bps = conn.prepareStatement(
                    "SELECT balance FROM vinplay.agency_wallet WHERE agent_id = ?")) {
                bps.setInt(1, agentId);
                try (java.sql.ResultSet rs = bps.executeQuery()) {
                    if (rs.next()) balanceAfter = rs.getLong("balance");
                }
            }
            ps = conn.prepareStatement(
                    "INSERT INTO vinplay.agency_wallet_transactions " +
                    "(agent_id, agent_nickname, type, amount, direction, balance_after, " +
                    " related_user, game_action, note) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
            ps.setInt(1, agentId);
            ps.setString(2, agentNick);
            ps.setString(3, type);
            ps.setLong(4, amount);
            ps.setString(5, direction);
            ps.setLong(6, balanceAfter);
            ps.setString(7, relatedUser);
            ps.setString(8, gameAction);
            ps.setString(9, note);
            ps.executeUpdate();
        } catch (Exception e) {
            logger.warn("logWalletTransaction failed agentId=" + agentId + " type=" + type, e);
        } finally {
            try { if (ps != null) ps.close(); } catch (Exception ignored) {}
        }
    }

}
