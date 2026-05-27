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
import java.util.List;

/**
 * c=9838 — List ALL players under agent (subtree).
 * Uses ancestors (FIND_IN_SET) for subtree queries, parent_agent_id for member linkage.
 * Deprecated: use c=9839 with type=player instead.
 */
public class ListAllPlayersUnderAgentProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();
            String rc = request.getParameter("rc");
            String nickFilter = request.getParameter("nn");
            String fromTime = request.getParameter("ft");
            String toTime = request.getParameter("et");
            int page = 1, limit = 20;
            try { String s = request.getParameter("p"); if (s != null) page = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
            try { String s = request.getParameter("l"); if (s != null) limit = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
            if (page < 1) page = 1;
            if (limit < 1 || limit > 200) limit = 20;
            int offset = (page - 1) * limit;

            String sort = request.getParameter("sort");
            String dir = request.getParameter("dir");
            if (dir == null || (!dir.equalsIgnoreCase("asc") && !dir.equalsIgnoreCase("desc"))) {
                dir = "desc";
            } else {
                dir = dir.toLowerCase();
            }

            String sortCol = "u.id"; // default
            if ("vin".equals(sort)) sortCol = "u.vin";
            else if ("xu".equals(sort)) sortCol = "u.id"; // SUN-13xx: xu dropped, fallback to id
            else if ("t_nap".equals(sort) || "total_deposit".equals(sort)) sortCol = "u.t_nap";
            else if ("t_rut".equals(sort) || "total_withdraw".equals(sort)) sortCol = "u.t_rut";
            else if ("net_profit".equals(sort)) sortCol = "(u.t_nap - u.t_rut)";
            else if ("last_login".equals(sort) || "last_login_time".equals(sort)) sortCol = "u.last_login";
            else if ("status".equals(sort)) sortCol = "u.status";
            else if ("nick_name".equals(sort) || "nickname".equals(sort)) sortCol = "u.nick_name";

            if (rc == null || rc.isEmpty()) return err(response, "1001", "rc required");

            // 1. Resolve agent (supports code, nickname, username)
            AgentInfo agent = AgentHierarchyHelper.resolveAgent(rc);
            if (agent == null) return err(response, "1002", "agent not found");

            // 2. Get subtree agent IDs
            List<Integer> subtreeIds = AgentHierarchyHelper.getSubtreeAgentIdsIncludingSelf(agent.id);

            // 3. Build WHERE: players only (dai_ly = 0), under subtree via parent_agent_id.
            //    SUN-1063: hide bots by default (agent page under SpecialAccount was
            //    polluted by bot accounts). Admin can pass include_bots=1 to bring them
            //    back for ops queries.
            //    GitLab #38: canonical downline clause via AgentHierarchyHelper so A/B/C
            //    processors agree on the same definition of "downline".
            String includeBots = request.getParameter("include_bots");
            boolean wantBots = "1".equals(includeBots) || "true".equalsIgnoreCase(includeBots);
            StringBuilder where = new StringBuilder(" WHERE ");
            where.append(AgentHierarchyHelper.buildDownlineWhere(subtreeIds.size(), wantBots, "u"));
            if (nickFilter != null && !nickFilter.isEmpty()) {
                where.append(" AND u.nick_name LIKE ?");
            }
            if (fromTime != null && !fromTime.isEmpty()) {
                where.append(" AND u.create_time >= ?");
            }
            if (toTime != null && !toTime.isEmpty()) {
                where.append(" AND u.create_time <= ?");
            }

            // 4. Count
            int total = 0;
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                PreparedStatement countStm = conn.prepareStatement("SELECT COUNT(*) FROM users u" + where);
                int idx = AgentHierarchyHelper.setIntParams(countStm, 1, subtreeIds);
                if (nickFilter != null && !nickFilter.isEmpty()) countStm.setString(idx++, "%" + nickFilter + "%");
                if (fromTime != null && !fromTime.isEmpty()) countStm.setString(idx++, fromTime + " 00:00:00");
                if (toTime != null && !toTime.isEmpty()) countStm.setString(idx++, toTime + " 23:59:59");
                ResultSet rs = countStm.executeQuery();
                if (rs.next()) total = rs.getInt(1);
                rs.close(); countStm.close();
            }

            // 5. Data query
            JSONArray arr = new JSONArray();
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                // SUN-13xx: xu / recharge_money dropped → kept as 0 AS alias for response parity.
                String sql = "SELECT u.id, u.user_name, u.nick_name, u.vin, 0 AS xu, 0 AS recharge_money, u.is_bot, " +
                    "u.referral_code, u.parent_agent_id, u.mobile, u.create_time, u.last_login, u.status, " +
                    "ua.nickname AS parent_agent_name, ua.code AS parent_agent_code, ua.level AS parent_agent_level, " +
                    "u.t_nap, u.t_rut " +
                    "FROM users u " +
                    "LEFT JOIN vinplay_admin.useragent ua ON ua.id = u.parent_agent_id " +
                    where + " ORDER BY " + sortCol + " " + dir + " LIMIT ? OFFSET ?";
                PreparedStatement stm = conn.prepareStatement(sql);
                int idx = AgentHierarchyHelper.setIntParams(stm, 1, subtreeIds);
                if (nickFilter != null && !nickFilter.isEmpty()) stm.setString(idx++, "%" + nickFilter + "%");
                if (fromTime != null && !fromTime.isEmpty()) stm.setString(idx++, fromTime + " 00:00:00");
                if (toTime != null && !toTime.isEmpty()) stm.setString(idx++, toTime + " 23:59:59");
                stm.setInt(idx++, limit);
                stm.setInt(idx, offset);

                ResultSet rs = stm.executeQuery();
                while (rs.next()) {
                    JSONObject row = new JSONObject();
                    row.put("id", rs.getLong("id"));
                    row.put("user_name", rs.getString("user_name"));
                    row.put("nick_name", rs.getString("nick_name"));
                    row.put("vin", rs.getLong("vin"));
                    row.put("xu", 0L);
                    row.put("recharge_money", 0L);
                    row.put("referral_code", rs.getString("referral_code") != null ? rs.getString("referral_code") : "");
                    row.put("parent_agent_id", rs.getObject("parent_agent_id"));
                    row.put("parent_agent_name", rs.getString("parent_agent_name") != null ? rs.getString("parent_agent_name") : "");
                    row.put("parent_agent_code", rs.getString("parent_agent_code") != null ? rs.getString("parent_agent_code") : "");
                    int paLevel = rs.getInt("parent_agent_level");
                    row.put("parent_agent_level", rs.wasNull() ? 0 : paLevel);
                    row.put("mobile", rs.getString("mobile") != null ? rs.getString("mobile") : "");
                    row.put("create_time", rs.getString("create_time") != null ? rs.getString("create_time") : "");
                    row.put("last_login_time", rs.getString("last_login") != null ? rs.getString("last_login") : "");
                    row.put("status", rs.getInt("status"));
                    // SUN-1058: expose is_bot so agent UI can filter bot accounts.
                    row.put("is_bot", rs.getInt("is_bot"));
                    long tCharge = rs.getLong("t_nap");
                    long tWithdraw = rs.getLong("t_rut");
                    row.put("total_deposit", tCharge);
                    row.put("total_withdraw", tWithdraw);
                    row.put("net_profit", tCharge - tWithdraw);
                    arr.put(row);
                }
                rs.close(); stm.close();
            }

            // 6. Agent self info
            JSONObject agentInfo = new JSONObject();
            agentInfo.put("agent_id", agent.id);
            agentInfo.put("nickname", agent.nickname);
            agentInfo.put("level", agent.level);
            agentInfo.put("code", agent.code);
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

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", arr);
            response.put("total", total);
            response.put("subtree_agents", subtreeIds.size());
            response.put("agent", agentInfo);

        } catch (Exception e) {
            logger.error("ListAllPlayersUnderAgentProcessor error", e);
            response.put("success", false); response.put("errorCode", "9999");
            response.put("message", "Internal: " + e.getMessage());
        }
        return response.toString();
    }

    private static String err(JSONObject r, String code, String msg) {
        r.put("success", false); r.put("errorCode", code); r.put("message", msg);
        return r.toString();
    }
}
