package com.vinplay.api.backend.services;

import com.vinplay.api.backend.auth.AdminAuthHelper;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared helper for agent hierarchy queries.
 * Uses ancestors column with FIND_IN_SET for subtree queries
 * and parent_agent_id for player→agent linkage.
 */
public class AgentHierarchyHelper {

    private static final Logger logger = Logger.getLogger("backend");

    /**
     * Resolved agent info.
     */
    public static class AgentInfo {
        public int id;
        public String nickname;
        public String code;
        public int level;
        public int parentId;
        public String ancestors;  // cột 'ancestors' trong DB (comma-separated parent IDs)
        public double commissionRate;
    }

    /**
     * Returns true for the platform root Master account only.
     *
     * SUN-1297: reports that can expand to whole-site data must share one
     * definition of "site master"; otherwise the same account can see
     * all-site data in one processor and subtree data in another.
     */
    public static boolean isSiteMaster(AgentInfo agent) {
        return agent != null
                && "SpecialAccount".equals(agent.nickname)
                && agent.level == 0
                && "0".equals(agent.code);
    }

    /**
     * Shared guard for whole-site Master queries.
     */
    public static boolean isValidAdminToken(String aat) {
        return AdminAuthHelper.verifyToken(aat);
    }

    /**
     * Resolve agent by code, nickname, or username.
     * Returns null if not found.
     */
    public static AgentInfo resolveAgent(String rc) {
        if (rc == null || rc.trim().isEmpty()) return null;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT id, nickname, code, level, parentid, IFNULL(ancestors,'') AS ancestors, IFNULL(commission_rate,0) AS commission_rate " +
                 "FROM useragent WHERE code = ? OR nickname = ? OR username = ? LIMIT 1")) {
            ps.setString(1, rc.trim()); ps.setString(2, rc.trim()); ps.setString(3, rc.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    AgentInfo info = new AgentInfo();
                    info.id = rs.getInt("id");
                    info.nickname = rs.getString("nickname");
                    info.code = rs.getString("code") != null ? rs.getString("code") : "";
                    info.level = rs.getInt("level");
                    info.parentId = rs.getInt("parentid");
                    info.ancestors = rs.getString("ancestors");
                    info.commissionRate = rs.getDouble("commission_rate");
                    return info;
                }
            }
        } catch (Exception e) {
            logger.error("resolveAgent error rc=" + rc, e);
        }
        return null;
    }

    /**
     * Get ALL agent IDs in the subtree (excluding the agent itself).
     * Uses FIND_IN_SET(agentId, ancestors) for subtree lookup.
     */
    public static List<Integer> getSubtreeAgentIds(int agentId) {
        List<Integer> ids = new ArrayList<>();
        // SUN-765: ancestors stores comma-separated parent IDs ("17,6,2").
        // Direct child: parentid = agentId. Deeper descendant: agentId appears
        // anywhere in the ancestors list. FIND_IN_SET handles all positions
        // (start / middle / end) without LIKE glob gymnastics.
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT id FROM useragent WHERE FIND_IN_SET(?, IFNULL(ancestors,'')) > 0 AND active = 1")) {
            ps.setString(1, String.valueOf(agentId));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt("id"));
            }
        } catch (Exception e) {
            logger.error("getSubtreeAgentIds error agentId=" + agentId, e);
        }
        return ids;
    }

    /**
     * Get ALL agent IDs in subtree INCLUDING the agent itself.
     */
    public static List<Integer> getSubtreeAgentIdsIncludingSelf(int agentId) {
        List<Integer> ids = new ArrayList<>();
        ids.add(agentId);
        ids.addAll(getSubtreeAgentIds(agentId));
        return ids;
    }

    /**
     * Get user IDs of the agent's own users row (for self-exclusion).
     */
    public static long getAgentUserId(String agentNickname) {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT id FROM users WHERE nick_name = ? LIMIT 1")) {
            ps.setString(1, agentNickname);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong("id");
            }
        } catch (Exception e) {
            logger.error("getAgentUserId error nick=" + agentNickname, e);
        }
        return -1;
    }

    /**
     * Build SQL IN clause placeholders: "?,?,?"
     */
    public static String inPlaceholders(int count) {
        if (count <= 0) return "NULL";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(",");
            sb.append("?");
        }
        return sb.toString();
    }

    /**
     * Set integer list params starting at idx, returns next idx.
     */
    public static int setIntParams(PreparedStatement ps, int startIdx, List<Integer> values) throws java.sql.SQLException {
        for (int v : values) ps.setInt(startIdx++, v);
        return startIdx;
    }

    /**
     * Canonical downline WHERE fragment (GitLab #38).
     * Produces: "parent_agent_id IN (?,?,?,...) AND dai_ly = 0 [AND is_bot = 0]"
     *
     * Single anchor on `parent_agent_id`. Historically one processor also
     * OR-joined on `referral_code IN (codes)` to catch players that weren't
     * parent-linked — that anchor is now redundant since `a1b61c1c`
     * (fix/agency-tree-sync-parent) back-fills `parent_agent_id` on agent
     * creation and promotion. Keeping the dual-anchor produced different
     * counts across the three processors (#38) with no real coverage win.
     *
     * Caller binds the IN params with {@link #setIntParams}.
     * @param subtreeSize  count of agent IDs already gathered via
     *                     {@link #getSubtreeAgentIdsIncludingSelf}
     * @param includeBots  true keeps is_bot=1 rows; false (default for
     *                     player-facing views) hides them. Admin/ops
     *                     queries that need the full ground truth pass
     *                     true via an explicit `include_bots=1` query
     *                     param on the processor.
     */
    public static String buildDownlineWhere(int subtreeSize, boolean includeBots) {
        return buildDownlineWhere(subtreeSize, includeBots, null);
    }

    /**
     * Aliased variant. Pass e.g. "u" when the query uses {@code FROM users u}
     * — emits "u.parent_agent_id IN (...) AND u.dai_ly = 0 [AND u.is_bot = 0]".
     * Null / empty alias omits the prefix.
     */
    public static String buildDownlineWhere(int subtreeSize, boolean includeBots, String alias) {
        String p = (alias == null || alias.isEmpty()) ? "" : (alias + ".");
        StringBuilder sb = new StringBuilder();
        sb.append(p).append("parent_agent_id IN (").append(inPlaceholders(subtreeSize)).append(") AND ")
          .append(p).append("dai_ly = 0");
        if (!includeBots) sb.append(" AND ").append(p).append("is_bot = 0");
        return sb.toString();
    }

    /**
     * Count players in an agent's downline using the canonical definition.
     * Convenience wrapper — opens its own vinplay connection from
     * {@code mysqlpoolname}. For queries that already have a connection
     * open, use {@link #buildDownlineWhere} and bind params yourself.
     */
    public static int countDownline(List<Integer> subtreeAgentIds, boolean includeBots) {
        if (subtreeAgentIds == null || subtreeAgentIds.isEmpty()) return 0;
        String sql = "SELECT COUNT(id) FROM users WHERE " + buildDownlineWhere(subtreeAgentIds.size(), includeBots);
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setIntParams(ps, 1, subtreeAgentIds);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) {
            logger.error("countDownline error", e);
        }
        return 0;
    }
}
