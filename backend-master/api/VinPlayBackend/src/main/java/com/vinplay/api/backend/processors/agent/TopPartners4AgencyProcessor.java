package com.vinplay.api.backend.processors.agent;

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

public class TopPartners4AgencyProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        HttpServletRequest request = param.get();
        try {
            String agentCode = request.getParameter("rc");
            String fromDate = request.getParameter("ft");
            String endDate = request.getParameter("et");

            if (agentCode == null || agentCode.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1002");
                response.put("message", "Missing agency code");
                return response.toString();
            }

            AgentInfo agent = AgentHierarchyHelper.resolveAgent(agentCode);
            if (agent == null) {
                response.put("success", false);
                response.put("errorCode", "1002");
                response.put("message", "Agency not found");
                return response.toString();
            }

            if (fromDate == null) fromDate = "2020-01-01";
            if (endDate == null) endDate = "2030-01-01";

            java.util.List<JSONObject> f1List = new java.util.ArrayList<>();
            String sqlF1 = "SELECT id, nickname FROM vinplay_admin.useragent WHERE parentid = ?";
            try (Connection connAdmin = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
                 PreparedStatement stmF1 = connAdmin.prepareStatement(sqlF1)) {
                stmF1.setInt(1, agent.id);
                try (ResultSet rs = stmF1.executeQuery()) {
                    while (rs.next()) {
                        JSONObject obj = new JSONObject();
                        obj.put("id", rs.getInt("id"));
                        obj.put("name", rs.getString("nickname"));
                        obj.put("revenue", 0L);
                        f1List.add(obj);
                    }
                }
            }

            if (!f1List.isEmpty()) {
                for (JSONObject f1 : f1List) {
                    int f1Id = f1.getInt("id");
                    java.util.List<Integer> f1Subtree = AgentHierarchyHelper.getSubtreeAgentIdsIncludingSelf(f1Id);
                    String inIds = AgentHierarchyHelper.inPlaceholders(f1Subtree.size());

                    StringBuilder sqlBet = new StringBuilder();
                    sqlBet.append("SELECT IFNULL(SUM(IFNULL(wm,0) + IFNULL(ag,0) + IFNULL(ibc,0) + IFNULL(cmd,0) + IFNULL(sbo,0) + IFNULL(ebet,0) + IFNULL(fish,0) + IFNULL(slot_pokemon,0) + IFNULL(slot_bitcoin,0) + IFNULL(slot_taydu,0) + IFNULL(slot_angrybird,0) + IFNULL(slot_thantai,0) + IFNULL(slot_thanbai,0) + IFNULL(slot_thethao,0) + IFNULL(slot_chiemtinh,0) + IFNULL(slot_bikini,0) + IFNULL(slot_galaxy,0) + IFNULL(minipoker,0) + IFNULL(caothap,0) + IFNULL(baucua,0) + IFNULL(xocdia,0) + IFNULL(taixiu,0) + IFNULL(taixiu_st,0) + IFNULL(tlmn,0) + IFNULL(bacay,0)), 0) AS total_bet ");
                    sqlBet.append("FROM log_report_user WHERE nick_name IN (");
                    sqlBet.append("  SELECT u1.nick_name FROM users u1 WHERE u1.parent_agent_id IN (").append(inIds).append(")");
                    sqlBet.append("  UNION ");
                    sqlBet.append("  SELECT ua.nickname COLLATE utf8mb4_general_ci FROM vinplay_admin.useragent ua WHERE ua.id IN (").append(inIds).append(")");
                    sqlBet.append(") AND time_report >= ? AND time_report <= ?");

                    try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                         PreparedStatement stm = conn.prepareStatement(sqlBet.toString())) {
                        int idx = 1;
                        idx = AgentHierarchyHelper.setIntParams(stm, idx, f1Subtree);
                        idx = AgentHierarchyHelper.setIntParams(stm, idx, f1Subtree);
                        stm.setString(idx++, fromDate + " 00:00:00");
                        stm.setString(idx++, endDate + " 23:59:59");
                        try (ResultSet rs = stm.executeQuery()) {
                            if (rs.next()) {
                                f1.put("revenue", rs.getLong("total_bet"));
                            }
                        }
                    } catch (Exception e) { logger.warn("Error calculating revenue for F1 " + f1Id, e); }
                }

                java.util.Collections.sort(f1List, new java.util.Comparator<JSONObject>() {
                    @Override
                    public int compare(JSONObject o1, JSONObject o2) {
                        return Long.compare(o2.getLong("revenue"), o1.getLong("revenue"));
                    }
                });
            }

            JSONArray arr = new JSONArray();
            int limit = Math.min(5, f1List.size());
            for (int i = 0; i < limit; i++) {
                JSONObject item = f1List.get(i);
                item.put("rank", i + 1);
                item.remove("id");
                arr.put(item);
            }

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("top_partners", arr);

        } catch (Exception e) {
            logger.error("TopPartners4AgencyProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}
