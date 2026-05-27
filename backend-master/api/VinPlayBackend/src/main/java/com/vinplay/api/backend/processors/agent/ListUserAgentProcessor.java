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

public class ListUserAgentProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");
    private static final int PAGE_SIZE = 20;

    public String execute(Param<HttpServletRequest> parameter) {
        JsonObject result = new JsonObject();
        result.addProperty("success", false);
        result.addProperty("errorCode", "1001");

        HttpServletRequest request = (HttpServletRequest) parameter.get();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            String pgStr = request.getParameter("pg");
            String code = request.getParameter("code");
            String key = request.getParameter("key");
            String lv = request.getParameter("lv");

            int page = 1;
            if (pgStr != null && !pgStr.isEmpty()) {
                page = Integer.parseInt(pgStr);
            }
            if (page < 1) page = 1;

            conn = ConnectionPool.getInstance().getConnection(ConnectionPool.ADMIN_POOL);
            if (conn == null) {
                logger.error("ListUserAgentProcessor: cannot get DB connection");
                return result.toString();
            }

            // Build WHERE clause dynamically
            // SUN-924/926: filter out deleted/deactivated agents (active=0).
            // DeleteUserAgentProcessor hard-deletes the row, but admin CMS
            // also supports soft-delete (active=0). Either way, inactive
            // agents must not appear in the list.
            StringBuilder where = new StringBuilder(" WHERE a.active = 1");
            if (code != null && !code.isEmpty()) {
                where.append(" AND a.code = ?");
            }
            if (key != null && !key.isEmpty()) {
                where.append(" AND (a.username LIKE ? OR a.nickname LIKE ? OR a.nameagent LIKE ? OR a.phone LIKE ? OR p.nickname LIKE ?)");
            }
            if (lv != null && !lv.isEmpty()) {
                where.append(" AND a.level = ?");
            }

            // Count total — must include parent JOIN for p.nickname search
            String countSql = "SELECT COUNT(*) FROM useragent a LEFT JOIN useragent p ON a.parentid = p.id" + where.toString();
            ps = conn.prepareStatement(countSql);
            int paramIdx = 1;
            if (code != null && !code.isEmpty()) {
                ps.setString(paramIdx++, code);
            }
            if (key != null && !key.isEmpty()) {
                String likeKey = "%" + key + "%";
                ps.setString(paramIdx++, likeKey);
                ps.setString(paramIdx++, likeKey);
                ps.setString(paramIdx++, likeKey);
                ps.setString(paramIdx++, likeKey);
                ps.setString(paramIdx++, likeKey);
            }
            if (lv != null && !lv.isEmpty()) {
                ps.setInt(paramIdx++, Integer.parseInt(lv));
            }

            rs = ps.executeQuery();
            int totalRecords = 0;
            if (rs.next()) {
                totalRecords = rs.getInt(1);
            }
            rs.close();
            ps.close();

            int totalPages = (int) Math.ceil((double) totalRecords / PAGE_SIZE);
            if (totalPages < 1) totalPages = 1;
            if (page > totalPages) page = totalPages;

            int offset = (page - 1) * PAGE_SIZE;

            // Fetch page
            // SUN-666: percent_bonus_vincard removed from response payload
            String querySql = "SELECT a.id, a.username, a.nickname, a.nameagent, a.address, a.phone, a.email, " +
                    "a.facebook, a.`key`, a.status, a.parentid, a.namebank, a.nameaccount, a.numberaccount, " +
                    "a.`show`, a.active, a.createtime, a.updatetime, a.`order`, a.sms, " +
                    "a.site, a.last_login_time, a.login_times, a.level, a.code, a.ancestors, a.telegram, a.zalo, " +
                    "p.nickname as parent_nickname, p.code as parent_code, IFNULL(cw.balance, 0) as credit_balance " +
                    "FROM useragent a LEFT JOIN useragent p ON a.parentid = p.id " +
                    "LEFT JOIN vinplay.credit_wallet cw ON a.id = cw.agent_id" + where.toString() +
                    " ORDER BY a.id DESC LIMIT ? OFFSET ?";

            ps = conn.prepareStatement(querySql);
            paramIdx = 1;
            if (code != null && !code.isEmpty()) {
                ps.setString(paramIdx++, code);
            }
            if (key != null && !key.isEmpty()) {
                String likeKey = "%" + key + "%";
                ps.setString(paramIdx++, likeKey);
                ps.setString(paramIdx++, likeKey);
                ps.setString(paramIdx++, likeKey);
                ps.setString(paramIdx++, likeKey);
                ps.setString(paramIdx++, likeKey);
            }
            if (lv != null && !lv.isEmpty()) {
                ps.setInt(paramIdx++, Integer.parseInt(lv));
            }
            ps.setInt(paramIdx++, PAGE_SIZE);
            ps.setInt(paramIdx++, offset);

            rs = ps.executeQuery();
            JsonArray data = new JsonArray();
            while (rs.next()) {
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
                // SUN-666: percent_bonus_vincard removed from response
                agent.addProperty("site", rs.getString("site"));
                agent.addProperty("last_login_time", rs.getString("last_login_time"));
                agent.addProperty("login_times", rs.getInt("login_times"));
                agent.addProperty("level", rs.getInt("level"));
                agent.addProperty("code", rs.getString("code"));
                agent.addProperty("ancestors", rs.getString("ancestors"));
                agent.addProperty("telegram", rs.getString("telegram"));
                agent.addProperty("zalo", rs.getString("zalo"));
                agent.addProperty("credit_balance", rs.getLong("credit_balance"));
                data.add(agent);
            }

            result.addProperty("success", true);
            result.addProperty("errorCode", "0");
            result.add("data", data);
            result.addProperty("totalPages", totalPages);
            result.addProperty("currentPage", page);
            result.addProperty("totalRecords", totalRecords);

        } catch (Exception e) {
            logger.error("ListUserAgentProcessor error", e);
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception ignored) {}
            try { if (ps != null) ps.close(); } catch (Exception ignored) {}
            ConnectionPool.releaseConnection(conn);
        }

        return result.toString();
    }
}
