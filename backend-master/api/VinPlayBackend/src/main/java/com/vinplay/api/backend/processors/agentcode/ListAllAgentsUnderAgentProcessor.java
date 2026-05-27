package com.vinplay.api.backend.processors.agentcode;

import com.vinplay.api.backend.services.AgentHierarchyHelper;
import com.vinplay.api.backend.services.AgentHierarchyHelper.AgentInfo;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * c=9839 — Unified list: agents + players under an agent's subtree.
 *
 * Uses ancestors (FIND_IN_SET) for subtree queries, parent_agent_id for member linkage.
 *
 * Params: rc (agent nickname/code), type (all|agent|player), nn (filter), pg/size or p/l
 */
public class ListAllAgentsUnderAgentProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");
    private static final Comparator<TreeRow> TREE_CHILD_COMPARATOR = new Comparator<TreeRow>() {
        @Override
        public int compare(TreeRow a, TreeRow b) {
            if (a.isAgent != b.isAgent) {
                return a.isAgent ? -1 : 1;
            }

            int orderCmp = Integer.compare(normalizeSortOrder(a.sortOrder), normalizeSortOrder(b.sortOrder));
            if (orderCmp != 0) {
                return orderCmp;
            }

            int nickCmp = safeLower(a.nickName).compareTo(safeLower(b.nickName));
            if (nickCmp != 0) {
                return nickCmp;
            }

            return Long.compare(a.userId, b.userId);
        }
    };

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();
            String rc = request.getParameter("rc");
            String nickFilter = request.getParameter("nn");
            String typeFilter = request.getParameter("type");
            int page = 1, limit = 50;
            try { String s = request.getParameter("p"); if (s != null) page = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
            try { String s = request.getParameter("l"); if (s != null) limit = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
            try { String s = request.getParameter("pg"); if (s != null) page = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
            try { String s = request.getParameter("size"); if (s != null) limit = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
            if (page < 1) page = 1;
            if (limit < 1 || limit > 200) limit = 50;
            int offset = (page - 1) * limit;

            if (rc == null || rc.isEmpty()) return err(response, "1001", "rc required");

            // 1. Resolve agent
            AgentInfo agent = AgentHierarchyHelper.resolveAgent(rc);
            if (agent == null) return err(response, "1002", "agent not found");

            // 2. Get all agent IDs in subtree (for member lookup via parent_agent_id)
            List<Integer> subtreeIds = AgentHierarchyHelper.getSubtreeAgentIdsIncludingSelf(agent.id);
            String inIds = AgentHierarchyHelper.inPlaceholders(subtreeIds.size());

            // 3. Get agent's own user ID for self-exclusion
            long agentUserId = AgentHierarchyHelper.getAgentUserId(agent.nickname);

            // 3b. Load caller's codes (current + legacy) once for batch can_promote checks.
            Set<String> callerCodes = com.vinplay.dal.service.AgentPermissionService.loadCallerCodes(agent.id);

            // 4. Build WHERE clause
            boolean showAgents = !"player".equals(typeFilter);
            boolean showPlayers = !"agent".equals(typeFilter);

            StringBuilder where = new StringBuilder(" WHERE (u.parent_agent_id IN (" + inIds + ") OR ua.id IN (" + inIds + "))");

            // SUN-XXX: Lọc rác - Bỏ qua các đại lý bị admin xóa mềm (active = 0)
            where.append(" AND (ua.active IS NULL OR ua.active = 1)");

            // SUN-1063: hide bot accounts by default on the agent subtree UI
            // (they were polluting the SpecialAccount page). Pass include_bots=1
            // to bring them back for ops/debug queries.
            String includeBots = request.getParameter("include_bots");
            boolean wantBots = "1".equals(includeBots) || "true".equalsIgnoreCase(includeBots);
            if (!wantBots) where.append(" AND u.is_bot = 0");
            
            if (agentUserId > 0) where.append(" AND u.id != ?");
            if (!showAgents && showPlayers) {
                // Tab người chơi
                where.append(" AND u.dai_ly = 0 AND ua.id IS NULL");
            } else if (showAgents && !showPlayers) {
                // Tab đại lý
                where.append(" AND (u.dai_ly > 0 OR ua.id IS NOT NULL)");
            }
            if (nickFilter != null && !nickFilter.isEmpty()) {
                // SUN-1193: broaden search across all 4 name columns. Agents can
                // have a different `nick_name` in vinplay.users (auto-generated
                // when they registered as a player) vs `nickname` in
                // vinplay_admin.useragent (their display name as an agent).
                // The agency portal shows the latter, so QC types it into search;
                // before this fix that key only matched users.nick_name and
                // returned 0 results. SUN-1192 documents the underlying data
                // divergence — 8 affected agents on 2026-04-29.
                where.append(" AND (u.nick_name LIKE ? OR u.user_name LIKE ? OR ua.nickname LIKE ? OR ua.username LIKE ?)");
            }

            // 5. Count + Summary — single optimised aggregate query.
            // SUM(u.vin): total game wallet across ALL matched members (not just current page).
            // SUM(aw.balance): total agency wallet — JOIN via useragent on nick_name so we
            //   avoid the N+1 per-row lookup that the data query currently uses.
            // Both sums are returned in one round-trip alongside COUNT(*).
            long total = 0;
            long summaryTotalGameBalance = 0;
            long summaryTotalAgencyWallet = 0;
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                // WHERE clause references u.parent_agent_id so it hits the index on that column.
                // The agency_wallet JOIN is LEFT so players (no wallet row) contribute 0 to the sum.
                String sumSql = "SELECT COUNT(*) AS cnt, " +
                    "COALESCE(SUM(u.vin), 0) AS sum_vin, " +
                    "COALESCE(SUM(aw.balance), 0) AS sum_wallet " +
                    "FROM users u " +
                    // SUN-1192/1193: also match by username = user_name. For 8 agents
                    // discovered on 2026-04-29 the two tables have a divergent nickname
                    // (e.g. user.nick_name='SunKr_56789' but useragent.nickname='Nguyenthekr')
                    // — the old JOIN on nickname-only left those agent rows orphaned, so
                    // searching by their useragent.nickname (or username) returned nothing.
                    "LEFT JOIN vinplay_admin.useragent ua ON (ua.nickname COLLATE utf8mb4_general_ci = u.nick_name OR ua.username COLLATE utf8mb4_general_ci = u.user_name) " +
                    "LEFT JOIN vinplay.agency_wallet aw ON aw.agent_id = ua.id " +
                    where;
                PreparedStatement countStm = conn.prepareStatement(sumSql);
                int idx = 1;
                idx = AgentHierarchyHelper.setIntParams(countStm, idx, subtreeIds);
                idx = AgentHierarchyHelper.setIntParams(countStm, idx, subtreeIds);
                if (agentUserId > 0) countStm.setLong(idx++, agentUserId);
                if (nickFilter != null && !nickFilter.isEmpty()) {
                    // SUN-1193: bind once per OR-branch (4 columns)
                    String like = "%" + nickFilter + "%";
                    countStm.setString(idx++, like);
                    countStm.setString(idx++, like);
                    countStm.setString(idx++, like);
                    countStm.setString(idx, like);
                }
                ResultSet rs = countStm.executeQuery();
                if (rs.next()) {
                    total = rs.getLong("cnt");
                    summaryTotalGameBalance = rs.getLong("sum_vin");
                    summaryTotalAgencyWallet = rs.getLong("sum_wallet");
                }
                rs.close(); countStm.close();
            }

            String sortBy = request.getParameter("sort_by");
            if (sortBy == null || sortBy.isEmpty()) sortBy = request.getParameter("sort");
            String order = request.getParameter("order");
            if (order == null || order.isEmpty()) order = request.getParameter("dir");
            if (order == null || (!order.equalsIgnoreCase("ASC") && !order.equalsIgnoreCase("DESC"))) {
                order = "DESC";
            }

            boolean useTreeDefaultSort = (sortBy == null || sortBy.isEmpty()) && showAgents;

            String orderByClause;
            if (sortBy == null || sortBy.isEmpty()) {
                orderByClause = " ORDER BY u.dai_ly DESC, IFNULL(ua.level, 999) ASC, u.id DESC";
            } else {
                switch (sortBy) {
                    case "level":
                        orderByClause = " ORDER BY IFNULL(ua.level, 999) " + order + ", u.id DESC";
                        break;
                    case "total_deposit":
                    case "deposit":
                    case "t_nap":
                        orderByClause = " ORDER BY u.t_nap " + order + ", u.id DESC";
                        break;
                    case "total_withdraw":
                    case "withdraw":
                    case "t_rut":
                        orderByClause = " ORDER BY u.t_rut " + order + ", u.id DESC";
                        break;
                    case "net_profit":
                        orderByClause = " ORDER BY (u.t_nap - u.t_rut) " + order + ", u.id DESC";
                        break;
                    case "profit":
                        orderByClause = " ORDER BY u.id DESC"; // SUN-13xx: vin_total dropped, fallback to id
                        break;
                    case "create_time":
                        orderByClause = " ORDER BY u.create_time " + order + ", u.id DESC";
                        break;
                    case "last_login":
                    case "last_login_time":
                        orderByClause = " ORDER BY u.last_login " + order + ", u.id DESC";
                        break;
                    case "vin":
                    case "game_wallet_balance":
                        orderByClause = " ORDER BY u.vin " + order + ", u.id DESC";
                        break;
                    case "agency_wallet_balance":
                    case "agency_wallet":
                        orderByClause = " ORDER BY IFNULL(aw.balance, 0) " + order + ", u.id DESC";
                        break;
                    case "xu":
                        orderByClause = " ORDER BY u.id DESC"; // SUN-13xx: xu dropped, fallback to id
                        break;
                    case "status":
                        orderByClause = " ORDER BY u.status " + order + ", u.id DESC";
                        break;
                    case "id":
                        orderByClause = " ORDER BY u.id " + order;
                        break;
                    case "nick_name":
                    case "nickname":
                        orderByClause = " ORDER BY u.nick_name " + order + ", u.id DESC";
                        break;
                    case "user_name":
                    case "username":
                        orderByClause = " ORDER BY u.user_name " + order + ", u.id DESC";
                        break;
                    case "mobile":
                        orderByClause = " ORDER BY u.mobile " + order + ", u.id DESC";
                        break;
                    case "role":
                    case "dai_ly":
                        orderByClause = " ORDER BY u.dai_ly " + order + ", u.id DESC";
                        break;
                    case "parent_agent_name":
                        orderByClause = " ORDER BY pa.nickname " + order + ", u.id DESC";
                        break;
                    default:
                        orderByClause = " ORDER BY u.dai_ly DESC, IFNULL(ua.level, 999) ASC, u.id DESC";
                        break;
                }
            }

            // 6. Data query
            JSONArray arr = new JSONArray();
            List<TreeRow> treeRows = useTreeDefaultSort ? new ArrayList<TreeRow>() : null;
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                // SUN-791 follow-up: u.vin_total is selected so the `profit` column
                // can be derived directly from the cumulative game P&L counter.
                // Previously profit was calculated as (deposit - withdraw - balance),
                // which broke for agent accounts whose vin wallet also receives
                // commission credits from rebate_logs — pushing `profit` negative
                // even when the agent had actually made money from their downline.
                // SUN-13xx: xu / recharge_money / vin_total dropped → kept as 0 AS alias for response parity.
                String sql = "SELECT u.id, u.user_name, u.nick_name, u.vin, 0 AS xu, u.dai_ly, u.status, u.is_bot, " +
                    "u.referral_code, u.parent_agent_id, u.mobile, u.create_time, u.last_login, " +
                    "u.t_nap, u.t_rut, 0 AS recharge_money, 0 AS vin_total, " +
                    "ua.id AS agent_row_id, ua.nickname AS agent_nickname, ua.code AS agent_code, ua.level AS agent_level, " +
                    "ua.`order` AS agent_sort_order, " +
                    "ua.commission_rate AS agent_commission_rate, ua.last_login_time AS agent_last_login, " +
                    "pa.nickname AS parent_agent_name, pa.code AS parent_agent_code, " +
                    "aw.balance AS agency_wallet " +
                    "FROM users u " +
                    // SUN-1192/1193: also match by username = user_name. For 8 agents
                    // discovered on 2026-04-29 the two tables have a divergent nickname
                    // (e.g. user.nick_name='SunKr_56789' but useragent.nickname='Nguyenthekr')
                    // — the old JOIN on nickname-only left those agent rows orphaned, so
                    // searching by their useragent.nickname (or username) returned nothing.
                    "LEFT JOIN vinplay_admin.useragent ua ON (ua.nickname COLLATE utf8mb4_general_ci = u.nick_name OR ua.username COLLATE utf8mb4_general_ci = u.user_name) " +
                    "LEFT JOIN vinplay_admin.useragent pa ON pa.id = u.parent_agent_id " +
                    "LEFT JOIN vinplay.agency_wallet aw ON aw.agent_id = ua.id " +
                    where +
                    (useTreeDefaultSort ? "" : orderByClause + " LIMIT ? OFFSET ?");
                PreparedStatement stm = conn.prepareStatement(sql);
                int idx = 1;
                idx = AgentHierarchyHelper.setIntParams(stm, idx, subtreeIds);
                idx = AgentHierarchyHelper.setIntParams(stm, idx, subtreeIds);
                if (agentUserId > 0) stm.setLong(idx++, agentUserId);
                if (nickFilter != null && !nickFilter.isEmpty()) {
                    // SUN-1193: bind once per OR-branch (4 columns)
                    String like = "%" + nickFilter + "%";
                    stm.setString(idx++, like);
                    stm.setString(idx++, like);
                    stm.setString(idx++, like);
                    stm.setString(idx++, like);
                }
                if (!useTreeDefaultSort) {
                    stm.setInt(idx++, limit);
                    stm.setInt(idx, offset);
                }

                ResultSet rs = stm.executeQuery();
                while (rs.next()) {
                    JSONObject row = new JSONObject();
                    long userId = rs.getLong("id");
                    row.put("id", userId);
                    row.put("user_name", rs.getString("user_name"));
                    String nickName = rs.getString("nick_name");
                    row.put("nick_name", nickName);
                    row.put("vin", rs.getLong("vin"));
                    row.put("game_wallet_balance", rs.getLong("vin")); // alias for FE clarity
                    row.put("xu", 0L);
                    int daiLy = rs.getInt("dai_ly");
                    boolean hasUaId = rs.getObject("agent_row_id") != null;
                    if (daiLy == 0 && hasUaId) daiLy = 1; // Legacy accounts might have dai_ly=0 but valid useragent row
                    row.put("dai_ly", daiLy);
                    row.put("status", rs.getInt("status"));
                    // SUN-1058: expose is_bot so agent UI can filter bot accounts.
                    row.put("is_bot", rs.getInt("is_bot"));
                    row.put("referral_code", rs.getString("referral_code") != null ? rs.getString("referral_code") : "");
                    row.put("parent_agent_id", rs.getObject("parent_agent_id"));
                    row.put("parent_agent_name", rs.getString("parent_agent_name") != null ? rs.getString("parent_agent_name") : "");
                    row.put("parent_agent_code", rs.getString("parent_agent_code") != null ? rs.getString("parent_agent_code") : "");
                    row.put("mobile", rs.getString("mobile") != null ? rs.getString("mobile") : "");
                    row.put("create_time", rs.getString("create_time") != null ? rs.getString("create_time") : "");
                    row.put("last_login", rs.getString("last_login") != null ? rs.getString("last_login") : "");
                    long tNap = rs.getLong("t_nap");
                    long tRut = rs.getLong("t_rut");
                    long vin = rs.getLong("vin");
                    long rechargeMoney = 0L;
                    long vinTotal = 0L; // cumulative game P&L from user's perspective
                    // total_deposit: prefer recharge_money (tracks real card/bank deposits),
                    // fallback to t_nap. Both may be 0 on staging where money was given
                    // via direct DB update — in production recharge_money is authoritative.
                    long totalDeposit = Math.max(rechargeMoney, tNap);
                    row.put("total_deposit", totalDeposit);
                    row.put("total_withdraw", tRut);
                    row.put("net_profit", totalDeposit - tRut);
                    // SUN-791: "Lợi nhuận" column in the agency portal /partner page.
                    // Correct semantic: how much the house won from THIS user's game
                    // play, isolated from commission/deposit side effects. Negate the
                    // user's cumulative game P&L (negative vin_total = user lost to
                    // house = positive house profit). Previously `totalDeposit - tRut - vin`
                    // which double-counted commission credits as "game wins" and made
                    // agents who earned commission appear with a negative profit column.
                    row.put("profit", -vinTotal);
                    row.put("vin_total", vinTotal);
                    row.put("recharge_money", rechargeMoney);

                    // Agent-specific fields — always emit so FE gets a stable schema.
                    // Pure players (dai_ly=0, no useragent row) get defaults: level=4,
                    // agent_code="", commission_rate=0, agent_last_login="".
                    int    agLevel      = (daiLy > 0) ? daiLy : 4;
                    String agCode       = "";
                    // 2-decimal preservation for the agency FE (so 1.20 renders as "1.20",
                    // not "1.2"). org.json strips trailing zeros from every Number subtype,
                    // so we emit commission_rate as a formatted String.
                    String agCommission = "0.00";
                    String agLastLogin  = "";
                    String agNick = rs.getString("agent_nickname");
                    if (agNick != null && daiLy > 0) {
                        agLevel      = rs.getInt("agent_level");
                        agCode       = rs.getString("agent_code") != null ? rs.getString("agent_code") : "";
                        java.math.BigDecimal _cr = rs.getBigDecimal("agent_commission_rate");
                        agCommission = _cr != null
                                ? _cr.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
                                : "0.00";
                        agLastLogin  = rs.getString("agent_last_login") != null ? rs.getString("agent_last_login") : "";
                    }
                    row.put("level", agLevel);
                    // SUN-1191: role label distinguishes Master (only SpecialAccount, level=0)
                    // from regular agents (level>=1) so FE doesn't render TĐL/ĐL1/ĐL2 as "Master".
                    String roleLabel;
                    if (daiLy <= 0)        roleLabel = "player";
                    else if (agLevel == 0) roleLabel = "master";
                    else                   roleLabel = "agent";
                    row.put("role", roleLabel);
                    row.put("agent_code", agCode);
                    row.put("commission_rate", agCommission);
                    row.put("agent_last_login", agLastLogin);

                    // Agency wallet balance — only agents have one
                    if (daiLy > 0 && agNick != null) {
                        row.put("agency_wallet_balance", rs.getLong("agency_wallet")); 
                    } else {
                        row.put("agency_wallet_balance", 0);
                    }

                    // can_promote (SUN-??? agent-promote-ownership).
                    // True iff (a) row is a pure player (dai_ly=0), (b) caller is not
                    // a leaf (ĐL2 cannot promote), and (c) the caller is the row's
                    // direct inviter — referral_code in caller's code set OR
                    // parent_agent_id == caller.id. Matches the server-side check
                    // enforced in PromotePlayerToAgentProcessor.
                    boolean canPromote = false;
                    if (daiLy == 0 && agent.level < 3) {
                        String playerRefCode = rs.getString("referral_code");
                        Object paiObj = rs.getObject("parent_agent_id");
                        int playerParentAgentId = paiObj != null ? ((Number) paiObj).intValue() : 0;
                        canPromote = com.vinplay.dal.service.AgentPermissionService
                                .ownsPlayerByData(callerCodes, agent.id, playerRefCode, playerParentAgentId);
                    }
                    row.put("can_promote", canPromote);

                    if (useTreeDefaultSort) {
                        TreeRow treeRow = new TreeRow();
                        treeRow.userId = userId;
                        treeRow.agentId = rs.getInt("agent_row_id");
                        treeRow.parentAgentId = getLongOrDefault(rs.getObject("parent_agent_id"), 0L);
                        treeRow.isAgent = daiLy > 0;
                        treeRow.sortOrder = rs.getInt("agent_sort_order");
                        treeRow.nickName = nickName;
                        treeRow.data = row;
                        treeRows.add(treeRow);
                    } else {
                        arr.put(row);
                    }
                }
                rs.close(); stm.close();
            }

            if (useTreeDefaultSort) {
                List<TreeRow> orderedRows = orderTreeRows(treeRows, agent.id);
                int start = Math.min(offset, orderedRows.size());
                int end = Math.min(start + limit, orderedRows.size());
                for (int i = start; i < end; i++) {
                    arr.put(orderedRows.get(i).data);
                }
            }

            // 7. Agent self info
            JSONObject agentInfo = new JSONObject();
            agentInfo.put("agent_id", agent.id);
            agentInfo.put("nickname", agent.nickname);
            agentInfo.put("level", agent.level);
            agentInfo.put("code", agent.code);
            // SUN-1191: only level=0 (SpecialAccount) is Master.
            agentInfo.put("role", agent.level == 0 ? "master" : "agent");
            if (agent.parentId > 0) {
                AgentInfo parent = AgentHierarchyHelper.resolveAgent(String.valueOf(agent.parentId));
                if (parent != null) {
                    JSONObject parentInfo = new JSONObject();
                    parentInfo.put("agent_id", parent.id);
                    parentInfo.put("nickname", parent.nickname);
                    parentInfo.put("code", parent.code);
                    parentInfo.put("level", parent.level);
                    agentInfo.put("parent", parentInfo);
                }
            }

            // summary: aggregated over ALL matched records across all pages,
            // not just the current page — FE shows these as header stats.
            JSONObject summary = new JSONObject();
            summary.put("total_game_balance", summaryTotalGameBalance);   // tổng số dư chính (u.vin)
            summary.put("total_agency_wallet", summaryTotalAgencyWallet); // tổng số dư wallet đại lý

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", arr);
            response.put("total", total);
            response.put("page", page);
            response.put("limit", limit);
            response.put("my_level", agent.level);
            response.put("agent", agentInfo);
            response.put("summary", summary);

        } catch (Exception e) {
            logger.error("ListAllAgentsUnderAgentProcessor error", e);
            response.put("success", false); response.put("errorCode", "9999");
            response.put("message", "Internal: " + e.getMessage());
        }
        return response.toString();
    }

    private static String err(JSONObject r, String code, String msg) {
        r.put("success", false); r.put("errorCode", code); r.put("message", msg);
        return r.toString();
    }

    private static List<TreeRow> orderTreeRows(List<TreeRow> rows, int rootAgentId) {
        Map<Long, List<TreeRow>> rowsByParent = new HashMap<Long, List<TreeRow>>();
        for (TreeRow row : rows) {
            List<TreeRow> children = rowsByParent.get(row.parentAgentId);
            if (children == null) {
                children = new ArrayList<TreeRow>();
                rowsByParent.put(row.parentAgentId, children);
            }
            children.add(row);
        }

        for (List<TreeRow> children : rowsByParent.values()) {
            children.sort(TREE_CHILD_COMPARATOR);
        }

        List<TreeRow> ordered = new ArrayList<TreeRow>(rows.size());
        Set<Long> emittedUserIds = new HashSet<Long>();
        appendTreeRows(rootAgentId, rowsByParent, ordered, emittedUserIds);

        if (ordered.size() < rows.size()) {
            List<TreeRow> leftovers = new ArrayList<TreeRow>();
            for (TreeRow row : rows) {
                if (!emittedUserIds.contains(row.userId)) {
                    leftovers.add(row);
                }
            }
            leftovers.sort(TREE_CHILD_COMPARATOR);
            ordered.addAll(leftovers);
        }

        return ordered;
    }

    private static void appendTreeRows(long parentAgentId,
                                       Map<Long, List<TreeRow>> rowsByParent,
                                       List<TreeRow> ordered,
                                       Set<Long> emittedUserIds) {
        List<TreeRow> children = rowsByParent.get(parentAgentId);
        if (children == null || children.isEmpty()) {
            return;
        }

        for (TreeRow child : children) {
            if (!emittedUserIds.add(child.userId)) {
                continue;
            }
            ordered.add(child);
            if (child.isAgent && child.agentId > 0 && child.agentId != parentAgentId) {
                appendTreeRows(child.agentId, rowsByParent, ordered, emittedUserIds);
            }
        }
    }

    private static String safeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private static int normalizeSortOrder(int sortOrder) {
        return sortOrder > 0 ? sortOrder : Integer.MAX_VALUE;
    }

    private static long getLongOrDefault(Object value, long defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return defaultValue;
    }

    private static final class TreeRow {
        long userId;
        int agentId;
        long parentAgentId;
        boolean isAgent;
        int sortOrder;
        String nickName;
        JSONObject data;
    }
}
