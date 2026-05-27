package com.vinplay.api.backend.processors.agent;

import com.vinplay.api.backend.services.AgentHierarchyHelper;
import com.vinplay.api.backend.services.AgentHierarchyHelper.AgentInfo;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class ChartDepositWithdraw4AgencyProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        JSONObject response = new JSONObject();

        try {
            String agentCode = request.getParameter("rc"); // Session agent code
            String type = request.getParameter("type"); // "day", "hour", or "month"
            if (type == null || type.isEmpty()) type = request.getParameter("segment"); // FE compat
            String ft = request.getParameter("ft"); // fromTime (yyyy-MM-dd)
            String et = request.getParameter("et"); // endTime (yyyy-MM-dd)

            if (agentCode == null || agentCode.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1002");
                response.put("message", "Missing agency code");
                return response.toString();
            }

            if (type == null) type = "day";

            // Resolve agent via hierarchy helper
            AgentInfo agent = AgentHierarchyHelper.resolveAgent(agentCode);
            if (agent == null) {
                response.put("success", false);
                response.put("errorCode", "1002");
                response.put("message", "Agency not found");
                return response.toString();
            }
            List<Integer> subtreeIds = AgentHierarchyHelper.getSubtreeAgentIdsIncludingSelf(agent.id);
            String inIds = AgentHierarchyHelper.inPlaceholders(subtreeIds.size());

            // SUN-1108 Wave 2 Tier 4: response cache. Same agent + same query
            // shape returns cached JSON within TTL.
            final String cacheKey = com.vinplay.vbee.common.cache.ResponseCacheHelper.key(
                    "ChartDepositWithdraw4Agency", agent.id, type, ft, et);
            String cachedResp = com.vinplay.vbee.common.cache.ResponseCacheHelper.get(cacheKey);
            if (cachedResp != null) {
                return cachedResp;
            }
            final boolean histOnly = com.vinplay.vbee.common.cache.ResponseCacheHelper.isHistoricalOnly(et);

            JSONArray arr = new JSONArray();
            String dateFormat;
            if ("month".equals(type)) {
                dateFormat = "%Y-%m";
            } else if ("hour".equals(type)) {
                dateFormat = "%Y-%m-%d %H:00";
            } else {
                dateFormat = "%Y-%m-%d";
            }

            try (Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                StringBuilder sql = new StringBuilder();
                sql.append("SELECT label, SUM(dep) as total_deposit, SUM(wit) as total_withdraw FROM (");
                sql.append("  SELECT DATE_FORMAT(dt.created_at, '").append(dateFormat).append("') as label, ");
                sql.append("    dt.amount as dep, 0 as wit ");
                sql.append("  FROM vinplay.deposit_transactions dt ");
                sql.append("  JOIN vinplay.users u ON u.id = dt.user_id ");
                sql.append("  WHERE u.parent_agent_id IN (").append(inIds).append(") AND dt.status = 'APPROVED' ");
                if (ft != null && !ft.isEmpty()) {
                    sql.append("  AND dt.created_at >= ? ");
                }
                if (et != null && !et.isEmpty()) {
                    sql.append("  AND dt.created_at <= ? ");
                }
                sql.append("  UNION ALL ");
                sql.append("  SELECT DATE_FORMAT(bw.created_at, '").append(dateFormat).append("') as label, ");
                sql.append("    0 as dep, bw.amount_krw as wit ");
                sql.append("  FROM vinplay.bank_withdrawals bw ");
                sql.append("  JOIN vinplay.users u ON u.id = bw.user_id ");
                sql.append("  WHERE u.parent_agent_id IN (").append(inIds).append(") AND bw.status = 'APPROVED' ");
                if (ft != null && !ft.isEmpty()) {
                    sql.append("  AND bw.created_at >= ? ");
                }
                if (et != null && !et.isEmpty()) {
                    sql.append("  AND bw.created_at <= ? ");
                }
                sql.append(") combined GROUP BY label ORDER BY label ASC");

                PreparedStatement stm = conn.prepareStatement(sql.toString());
                int idx = 1;
                // Deposit params
                idx = AgentHierarchyHelper.setIntParams(stm, idx, subtreeIds);
                if (ft != null && !ft.isEmpty()) stm.setString(idx++, ft + " 00:00:00");
                if (et != null && !et.isEmpty()) stm.setString(idx++, et + " 23:59:59");
                // Withdrawal params
                idx = AgentHierarchyHelper.setIntParams(stm, idx, subtreeIds);
                if (ft != null && !ft.isEmpty()) stm.setString(idx++, ft + " 00:00:00");
                if (et != null && !et.isEmpty()) stm.setString(idx++, et + " 23:59:59");

                ResultSet rs = stm.executeQuery();
                while (rs.next()) {
                    JSONObject obj = new JSONObject();
                    long deps = rs.getLong("total_deposit");
                    long wits = rs.getLong("total_withdraw");
                    obj.put("date", rs.getString("label"));
                    obj.put("deposit", deps);
                    obj.put("withdraw", wits);
                    obj.put("net_profit", deps - wits);
                    arr.put(obj);
                }
                rs.close();
                stm.close();
            }

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", arr);
            String json = response.toString();
            com.vinplay.vbee.common.cache.ResponseCacheHelper.put(cacheKey, json, histOnly);
            return json;

        } catch (Exception e) {
            logger.error("Error in ChartDepositWithdraw4AgencyProcessor", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}
