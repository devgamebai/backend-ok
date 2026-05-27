package com.vinplay.api.backend.processors.agent;

import com.google.gson.JsonObject;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.apache.log4j.Logger;

public class DetailUserAgentProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> parameter) {
        JsonObject result = new JsonObject();
        result.addProperty("success", false);
        result.addProperty("errorCode", "1001");

        HttpServletRequest request = (HttpServletRequest) parameter.get();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            String idStr = request.getParameter("id");
            if (idStr == null || idStr.isEmpty()) {
                result.addProperty("errorCode", "1002");
                return result.toString();
            }
            int id = Integer.parseInt(idStr);

            conn = ConnectionPool.getInstance().getConnection(ConnectionPool.ADMIN_POOL);
            if (conn == null) {
                logger.error("DetailUserAgentProcessor: cannot get DB connection");
                return result.toString();
            }

            String sql = "SELECT a.id, a.username, a.nickname, a.password, a.nameagent, a.address, a.phone, a.email, " +
                    "a.facebook, a.`key`, a.status, a.parentid, a.namebank, a.nameaccount, a.numberaccount, " +
                    "a.`show`, a.active, a.createtime, a.updatetime, a.`order`, a.sms, a.percent_bonus_vincard, " +
                    "a.site, a.last_login_time, a.login_times, a.level, a.code, a.ancestors, a.telegram, a.zalo, " +
                    "p.nickname as parent_nickname, p.code as parent_code, IFNULL(cw.balance, 0) as credit_balance " +
                    "FROM useragent a LEFT JOIN useragent p ON a.parentid = p.id " +
                    "LEFT JOIN vinplay.credit_wallet cw ON a.id = cw.agent_id WHERE a.id = ?";

            ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            rs = ps.executeQuery();

            if (rs.next()) {
                JsonObject agent = new JsonObject();
                agent.addProperty("id", rs.getInt("id"));
                agent.addProperty("username", rs.getString("username"));
                agent.addProperty("nickname", rs.getString("nickname"));
                agent.addProperty("nameagent", rs.getString("nameagent"));
                agent.addProperty("address", rs.getString("address"));
                agent.addProperty("phone", rs.getString("phone"));
                agent.addProperty("email", rs.getString("email"));
                agent.addProperty("facebook", rs.getString("facebook"));
                agent.addProperty("key", rs.getString("key"));
                agent.addProperty("status", rs.getInt("status"));
                agent.addProperty("parentid", rs.getInt("parentid"));
                agent.addProperty("parent_nickname", rs.getString("parent_nickname") != null ? rs.getString("parent_nickname") : "");
                agent.addProperty("parent_code", rs.getString("parent_code") != null ? rs.getString("parent_code") : "");
                agent.addProperty("namebank", rs.getString("namebank"));
                agent.addProperty("nameaccount", rs.getString("nameaccount"));
                agent.addProperty("numberaccount", rs.getString("numberaccount"));
                agent.addProperty("show", rs.getInt("show"));
                agent.addProperty("active", rs.getInt("active"));
                agent.addProperty("createtime", rs.getString("createtime"));
                agent.addProperty("updatetime", rs.getString("updatetime"));
                agent.addProperty("order", rs.getInt("order"));
                agent.addProperty("sms", rs.getString("sms"));
                agent.addProperty("percent_bonus_vincard", rs.getDouble("percent_bonus_vincard"));
                agent.addProperty("site", rs.getString("site"));
                agent.addProperty("last_login_time", rs.getString("last_login_time"));
                agent.addProperty("login_times", rs.getInt("login_times"));
                agent.addProperty("level", rs.getInt("level"));
                agent.addProperty("code", rs.getString("code"));
                agent.addProperty("ancestors", rs.getString("ancestors"));
                agent.addProperty("telegram", rs.getString("telegram"));
                agent.addProperty("zalo", rs.getString("zalo"));
                agent.addProperty("credit_balance", rs.getLong("credit_balance"));

                result.addProperty("success", true);
                result.addProperty("errorCode", "0");
                result.add("data", agent);
            } else {
                result.addProperty("errorCode", "1003"); // not found
            }

        } catch (NumberFormatException e) {
            logger.error("DetailUserAgentProcessor: invalid id", e);
            result.addProperty("errorCode", "1002");
        } catch (Exception e) {
            logger.error("DetailUserAgentProcessor error", e);
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception ignored) {}
            try { if (ps != null) ps.close(); } catch (Exception ignored) {}
            ConnectionPool.releaseConnection(conn);
        }

        return result.toString();
    }
}
