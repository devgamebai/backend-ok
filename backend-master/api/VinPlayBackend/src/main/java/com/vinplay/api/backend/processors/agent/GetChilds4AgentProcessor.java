package com.vinplay.api.backend.processors.agent;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.apache.log4j.Logger;

public class GetChilds4AgentProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> parameter) {
        JsonObject result = new JsonObject();
        result.addProperty("success", false);
        result.addProperty("errorCode", "1001");

        HttpServletRequest request = (HttpServletRequest) parameter.get();
        String parentIdStr = request.getParameter("id");
        if (parentIdStr == null || parentIdStr.isEmpty()) {
            result.addProperty("errorCode", "1002");
            return result.toString();
        }

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            int parentId = Integer.parseInt(parentIdStr);
            conn = ConnectionPool.getInstance().getConnection(ConnectionPool.ADMIN_POOL);
            if (conn == null) {
                logger.error("GetChilds4AgentProcessor: cannot get DB connection");
                return result.toString();
            }

            String sql = "SELECT a.id, a.nickname, a.code, a.level, a.ancestors, a.commission_rate, a.percent_bonus_vincard, IFNULL(cw.balance, 0) as credit_balance " +
                         "FROM useragent a LEFT JOIN vinplay.credit_wallet cw ON a.id = cw.agent_id " +
                         "WHERE FIND_IN_SET(?, a.ancestors) > 0 AND a.id != ? ORDER BY a.level, a.nickname";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, parentId);
            ps.setInt(2, parentId);
            rs = ps.executeQuery();

            JsonArray data = new JsonArray();
            while (rs.next()) {
                JsonObject item = new JsonObject();
                item.addProperty("id", rs.getInt("id"));
                item.addProperty("nickname", rs.getString("nickname"));
                item.addProperty("code", rs.getString("code"));
                item.addProperty("level", rs.getInt("level"));
                item.addProperty("ancestors", rs.getString("ancestors"));
                // SUN-1098: 2-decimal string preserves trailing zeros for FE.
                item.addProperty("commission_rate", com.vinplay.dal.utils.PctFormatter.formatRs(rs, "commission_rate"));
                item.addProperty("percent_bonus_vincard", rs.getDouble("percent_bonus_vincard"));
                item.addProperty("credit_balance", rs.getLong("credit_balance"));
                data.add(item);
            }

            result.addProperty("success", true);
            result.addProperty("errorCode", "0");
            result.add("data", data);

        } catch (NumberFormatException e) {
            result.addProperty("errorCode", "1002");
        } catch (Exception e) {
            logger.error("GetChilds4AgentProcessor error", e);
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception ignored) {}
            try { if (ps != null) ps.close(); } catch (Exception ignored) {}
            ConnectionPool.releaseConnection(conn);
        }

        return result.toString();
    }
}
