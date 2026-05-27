package com.vinplay.api.backend.processors.agentcode;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.api.backend.services.AgentHierarchyHelper;
import com.vinplay.api.backend.services.AgentHierarchyHelper.AgentInfo;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * c=9845 — List members under agent with online/offline status.
 *
 * Online = user has an active token in Hazelcast cacheToken map.
 * Offline = no token found.
 *
 * Params: rc (agent nickname), status (online|offline|all, default all),
 *         nn (filter), p/l (pagination).
 */
public class ListOnlineOfflineMembersProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();
            String agentNick = request.getParameter("rc");
            String statusFilter = request.getParameter("status"); // online|offline|all
            String nickFilter = request.getParameter("nn");
            int page = 1, limit = 20;
            try { if (request.getParameter("p") != null) page = Integer.parseInt(request.getParameter("p")); } catch (NumberFormatException ignored) {}
            try { if (request.getParameter("l") != null) limit = Integer.parseInt(request.getParameter("l")); } catch (NumberFormatException ignored) {}
            if (page < 1) page = 1;
            if (limit < 1 || limit > 200) limit = 20;

            if (agentNick == null || agentNick.isEmpty()) return err(response, "1001", "rc required");

            // GitLab #38: canonical downline resolution via AgentHierarchyHelper.
            // Previously this processor used a manual subtree walk + dual-anchor
            // (referral_code IN codes OR parent_agent_id IN ids) + no is_bot
            // filter, which produced a different count than ListAllPlayersUnderAgent
            // (A) and ReportGeneral4Agency (C). All three now share the same
            // definition: parent_agent_id IN (subtree) AND dai_ly=0 AND is_bot=0
            // (bots hidden by default; pass include_bots=1 for ops queries).
            AgentInfo agent = AgentHierarchyHelper.resolveAgent(agentNick);
            if (agent == null) return err(response, "1002", "agent not found");
            List<Integer> subtreeIds = AgentHierarchyHelper.getSubtreeAgentIdsIncludingSelf(agent.id);
            if (subtreeIds.isEmpty()) {
                response.put("success", true); response.put("errorCode", "0");
                response.put("data", new JSONArray()); response.put("total", 0);
                response.put("online_count", 0); response.put("offline_count", 0);
                return response.toString();
            }

            String includeBots = request.getParameter("include_bots");
            boolean wantBots = "1".equals(includeBots) || "true".equalsIgnoreCase(includeBots);

            {
                String sql = "SELECT id, nick_name, user_name, vin, last_login, status FROM users WHERE "
                        + AgentHierarchyHelper.buildDownlineWhere(subtreeIds.size(), wantBots);
                if (nickFilter != null && !nickFilter.isEmpty()) sql += " AND nick_name LIKE ?";
                sql += " ORDER BY id DESC";

                List<JSONObject> allPlayers = new ArrayList<>();
                HazelcastInstance hz = HazelcastClientFactory.getInstance();
                IMap<String, String> tokenMap = hz != null ? hz.getMap("cacheToken") : null;

                try (Connection userConn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                     PreparedStatement ps = userConn.prepareStatement(sql)) {
                    int idx = AgentHierarchyHelper.setIntParams(ps, 1, subtreeIds);
                    if (nickFilter != null && !nickFilter.isEmpty()) ps.setString(idx, "%" + nickFilter + "%");
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String nick = rs.getString("nick_name");
                            boolean online = false;
                            if (tokenMap != null && nick != null) {
                                // Check if any token maps to this nickname
                                try { online = tokenMap.containsValue(nick); } catch (Exception ignored) {}
                            }

                            // Apply status filter
                            if ("online".equalsIgnoreCase(statusFilter) && !online) continue;
                            if ("offline".equalsIgnoreCase(statusFilter) && online) continue;

                            JSONObject p = new JSONObject();
                            p.put("id", rs.getLong("id"));
                            p.put("nick_name", nick);
                            p.put("user_name", rs.getString("user_name"));
                            p.put("vin", rs.getLong("vin"));
                            p.put("last_login", rs.getString("last_login") != null ? rs.getString("last_login") : "");
                            p.put("status", rs.getInt("status"));
                            p.put("online", online);
                            allPlayers.add(p);
                        }
                    }
                }

                // Count online/offline
                int onlineCount = 0, offlineCount = 0;
                for (JSONObject p : allPlayers) {
                    if (p.getBoolean("online")) onlineCount++; else offlineCount++;
                }

                // Paginate
                int total = allPlayers.size();
                int totalPages = Math.max(1, (int) Math.ceil((double) total / limit));
                int from = (page - 1) * limit, to = Math.min(from + limit, total);
                JSONArray data = new JSONArray();
                for (int i = from; i < to; i++) data.put(allPlayers.get(i));

                response.put("success", true); response.put("errorCode", "0");
                response.put("data", data); response.put("total", total);
                response.put("page", page); response.put("totalPages", totalPages);
                response.put("online_count", onlineCount);
                response.put("offline_count", offlineCount);
            }
        } catch (Exception e) {
            logger.error("ListOnlineOfflineMembersProcessor error", e);
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
