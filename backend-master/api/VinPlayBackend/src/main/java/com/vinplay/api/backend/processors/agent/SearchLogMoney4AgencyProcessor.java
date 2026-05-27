package com.vinplay.api.backend.processors.agent;

import com.vinplay.dal.service.impl.LogMoneyUserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.LogUserMoneyResponse;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class SearchLogMoney4AgencyProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        JSONObject response = new JSONObject();

        try {
            String agentCode = request.getParameter("rc"); // Session agent code OR nickname
            String usernameToFind = request.getParameter("nn"); // User nickname (Target) — optional, defaults to agent themselves

            // SUN-663: rc may be either the agent's code OR nickname.
            // Resolve to the canonical (code, nickname) pair so downstream auth works for both.
            String resolvedAgentCode = agentCode;
            String resolvedAgentNick = null;
            if (agentCode != null && !agentCode.isEmpty()) {
                try (Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
                    PreparedStatement stm = conn.prepareStatement(
                        "SELECT code, nickname FROM vinplay_admin.useragent WHERE code = ? OR nickname = ? LIMIT 1");
                    stm.setString(1, agentCode);
                    stm.setString(2, agentCode);
                    ResultSet rs = stm.executeQuery();
                    if (rs.next()) {
                        resolvedAgentCode = rs.getString("code");
                        resolvedAgentNick = rs.getString("nickname");
                    }
                    rs.close();
                    stm.close();
                }
            }
            // If FE didn't supply nn, default to the agent themselves (rolling history of self)
            if ((usernameToFind == null || usernameToFind.isEmpty()) && resolvedAgentNick != null) {
                usernameToFind = resolvedAgentNick;
            }
            if (resolvedAgentCode != null) agentCode = resolvedAgentCode;
            
            String dateFrom = request.getParameter("ft");
            String dateTo = request.getParameter("et");
            String actionName = request.getParameter("action"); // e.g. "Rolling", "Đổi điểm"
            String moneyTypeParam = request.getParameter("vn"); // "vin" or "xu"
            String moneyType = (moneyTypeParam != null && !moneyTypeParam.isEmpty()) ? moneyTypeParam : "vin";
            String descParam = request.getParameter("desc"); // Lý do
            String serviceNameParam = (descParam != null && !descParam.isEmpty()) ? "desc:" + descParam : null;
            
            int page = 1;
            try { if (request.getParameter("pg") != null) page = Integer.parseInt(request.getParameter("pg")); } catch (Exception e) {}

            if (agentCode == null || agentCode.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1002");
                response.put("message", "Missing rc (agent code or nickname)");
                return response.toString();
            }
            if (usernameToFind == null || usernameToFind.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1002");
                response.put("message", "Agent not found for rc=" + agentCode);
                return response.toString();
            }

            // Verify if usernameToFind belongs to agentCode's subtree OR IS THE AGENT HIMSELF
            boolean isAuthorized = false;

            // Step 1: Check if the agent is querying themselves
            try (Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
                PreparedStatement stm = conn.prepareStatement("SELECT 1 FROM vinplay_admin.useragent WHERE code = ? AND nickname = ? LIMIT 1");
                stm.setString(1, agentCode);
                stm.setString(2, usernameToFind);
                ResultSet rs = stm.executeQuery();
                if (rs.next()) {
                    isAuthorized = true;
                }
                rs.close();
                stm.close();
            }

            // Step 2: Check if it's a direct downline user (referral_code = agentCode)
            if (!isAuthorized) {
                try (Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                    PreparedStatement stm = conn.prepareStatement("SELECT 1 FROM users WHERE nick_name = ? AND referral_code = ? LIMIT 1");
                    stm.setString(1, usernameToFind);
                    stm.setString(2, agentCode);
                    ResultSet rs = stm.executeQuery();
                    if (rs.next()) {
                        isAuthorized = true;
                    }
                    rs.close();
                    stm.close();
                }
            }

            // Step 3: Check if the user belongs to any sub-agent in the caller's subtree
            // (TĐL should see users of ĐL1/ĐL2 under their tree)
            if (!isAuthorized) {
                try (Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
                    // First, get the agent ID for the calling agent
                    PreparedStatement stmAgent = conn.prepareStatement(
                        "SELECT id FROM vinplay_admin.useragent WHERE code = ? LIMIT 1");
                    stmAgent.setString(1, agentCode);
                    ResultSet rsAgent = stmAgent.executeQuery();
                    int callerAgentId = -1;
                    if (rsAgent.next()) {
                        callerAgentId = rsAgent.getInt("id");
                    }
                    rsAgent.close();
                    stmAgent.close();

                    if (callerAgentId > 0) {
                        // Find all sub-agent codes where ancestors contains the caller's ID
                        PreparedStatement stmSub = conn.prepareStatement(
                            "SELECT code FROM vinplay_admin.useragent WHERE FIND_IN_SET(?, ancestors) > 0");
                        stmSub.setString(1, String.valueOf(callerAgentId));
                        ResultSet rsSub = stmSub.executeQuery();
                        java.util.List<String> subAgentCodes = new java.util.ArrayList<>();
                        while (rsSub.next()) {
                            subAgentCodes.add(rsSub.getString("code"));
                        }
                        rsSub.close();
                        stmSub.close();

                        if (!subAgentCodes.isEmpty()) {
                            // Check if the user belongs to any sub-agent
                            StringBuilder placeholders = new StringBuilder();
                            for (int i = 0; i < subAgentCodes.size(); i++) {
                                if (i > 0) placeholders.append(",");
                                placeholders.append("?");
                            }
                            try (Connection connUser = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                                PreparedStatement stmUser = connUser.prepareStatement(
                                    "SELECT 1 FROM users WHERE nick_name = ? AND referral_code IN (" + placeholders + ") LIMIT 1");
                                stmUser.setString(1, usernameToFind);
                                for (int i = 0; i < subAgentCodes.size(); i++) {
                                    stmUser.setString(i + 2, subAgentCodes.get(i));
                                }
                                ResultSet rsUser = stmUser.executeQuery();
                                if (rsUser.next()) {
                                    isAuthorized = true;
                                }
                                rsUser.close();
                                stmUser.close();
                            }
                        }
                    }
                }
            }

            if (!isAuthorized) {
                response.put("success", false);
                response.put("errorCode", "1002");
                response.put("message", "Not authorized to view this user's logs");
                return response.toString();
            }

            int pageSize = 50; // Default page size matching Portal behavior
            try { if (request.getParameter("mi") != null) pageSize = Integer.parseInt(request.getParameter("mi")); } catch (Exception e) {}
            if (pageSize <= 0 || pageSize > 200) pageSize = 50;

            LogMoneyUserServiceImpl service = new LogMoneyUserServiceImpl();
            // Call the core service with proper pagination
            List<LogUserMoneyResponse> trans = service.searchLogMoneyUser(usernameToFind, null, moneyType, serviceNameParam, actionName, dateFrom, dateTo, page, 0, pageSize);
            int totalPages = service.countsearchLogMoneyUser(usernameToFind, moneyType, serviceNameParam, actionName, dateFrom, dateTo, 0);

            JSONArray arr = new JSONArray();
            if (trans != null) {
                for (LogUserMoneyResponse t : trans) {
                    JSONObject obj = new JSONObject();
                    obj.put("user_name", t.userName);
                    obj.put("nick_name", t.nickName);
                    obj.put("action_name", t.actionName);
                    obj.put("fee", t.fee);
                    obj.put("money_exchange", t.moneyExchange);
                    obj.put("current_money", t.currentMoney); // Giữ lại current_money dự phòng
                    obj.put("money_before", t.currentMoney - t.moneyExchange); // Trả thẳng luôn FE khỏi tính trừ
                    obj.put("money_after", t.currentMoney); // FE nhúng luôn biến này cho rõ ràng
                    obj.put("trans_time", t.transactionTime);
                    obj.put("description", t.description);
                    obj.put("money_type", moneyType); // Trả thêm tiền tệ ("vin" hoặc "xu") để hình map thành "Tiền chính" / "Điểm"
                    arr.put(obj);
                }
            }

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", arr);
            response.put("totalPages", totalPages);
            response.put("page", page);

        } catch (Exception e) {
            logger.error("Error in SearchLogMoney4AgencyProcessor", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}
