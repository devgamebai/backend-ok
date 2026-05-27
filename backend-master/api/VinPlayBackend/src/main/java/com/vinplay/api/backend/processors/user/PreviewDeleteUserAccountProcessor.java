package com.vinplay.api.backend.processors.user;

import com.vinplay.api.backend.processors.role.RbacSupport;
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

/**
 * Phase F1 — preview cascade-delete for a user account.
 *
 * Dry-run: lists every (schema.table) that holds rows referencing the
 * target user_id, with a row count. No mutation.
 *
 * Endpoint: c=9970&aat=&user_id=
 */
public class PreviewDeleteUserAccountProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest req = param.get();

            String adminNick = RbacSupport.getAdminNicknameFromToken(req, response);
            if (adminNick == null) return response.toString();
            if (!RbacSupport.hasSuperAdminRole(adminNick)) {
                return err(response, "1003", "Only super-admin may preview user deletion");
            }

            String userIdStr = req.getParameter("user_id");
            if (userIdStr == null || userIdStr.isEmpty()) return err(response, "4001", "user_id required");
            long userId;
            try { userId = Long.parseLong(userIdStr); }
            catch (NumberFormatException e) { return err(response, "4002", "user_id must be a number"); }

            JSONObject data = new JSONObject();
            JSONArray rowsArr = new JSONArray();
            long total = 0;

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                // Resolve the user first
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT id, user_name, nick_name, vin, 0 AS xu, is_bot FROM vinplay.users WHERE id=?")) {
                    ps.setLong(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) return err(response, "1002", "User not found");
                        data.put("user_id", rs.getLong("id"));
                        data.put("user_name", rs.getString("user_name"));
                        data.put("nick_name", rs.getString("nick_name"));
                        data.put("balance_vin", rs.getLong("vin"));
                        data.put("balance_xu", 0L);
                        data.put("is_bot", rs.getInt("is_bot") == 1);
                    }
                }

                // Enumerate every table that has user_id and count rows
                String tablesSql =
                        "SELECT TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME FROM information_schema.columns " +
                        "WHERE TABLE_SCHEMA IN ('vinplay','vinplay_admin','vinplay_minigame','vinplay_gamebai') " +
                        "  AND COLUMN_NAME IN ('user_id','userId') " +
                        "  AND TABLE_NAME NOT LIKE '\\_archive%' AND TABLE_NAME NOT LIKE 'v\\_%' " +
                        "ORDER BY TABLE_SCHEMA, TABLE_NAME";
                try (PreparedStatement ps = conn.prepareStatement(tablesSql);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String fq = rs.getString("TABLE_SCHEMA") + "." + rs.getString("TABLE_NAME");
                        String col = rs.getString("COLUMN_NAME");
                        try (PreparedStatement cps = conn.prepareStatement(
                                "SELECT COUNT(*) FROM " + fq + " WHERE " + col + "=?")) {
                            cps.setLong(1, userId);
                            try (ResultSet crs = cps.executeQuery()) {
                                if (crs.next()) {
                                    long n = crs.getLong(1);
                                    if (n > 0) {
                                        JSONObject row = new JSONObject();
                                        row.put("table", fq);
                                        row.put("column", col);
                                        row.put("rows", n);
                                        rowsArr.put(row);
                                        total += n;
                                    }
                                }
                            }
                        } catch (Exception probeErr) {
                            // table may have permission or column-rename issue; skip
                            logger.warn("preview probe skipped " + fq + "." + col + ": " + probeErr.getMessage());
                        }
                    }
                }
            }

            data.put("rows_per_table", rowsArr);
            data.put("rows_total", total);

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", data);
            return response.toString();
        } catch (Exception e) {
            logger.error("PreviewDeleteUserAccountProcessor error", e);
            return err(response, "9999", "Internal: " + e.getMessage());
        }
    }

    private static String err(JSONObject r, String code, String msg) {
        r.put("success", false);
        r.put("errorCode", code);
        r.put("message", msg);
        return r.toString();
    }
}
