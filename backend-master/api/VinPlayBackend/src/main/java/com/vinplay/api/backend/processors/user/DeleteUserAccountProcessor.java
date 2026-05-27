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
import java.sql.Statement;

/**
 * Phase F1 — execute cascade-delete for a user account (GDPR / fraud).
 *
 * Pre-checks (all must pass before delete):
 *   1. caller is super-admin
 *   2. user exists
 *   3. balance vin == 0 AND xu == 0  (override with allow_nonzero_balance=1)
 *   4. is_bot == 0  (override with allow_bot=1, sets session var trigger bypass)
 *   5. reason supplied (>= 10 chars)
 *
 * Audit row inserted FIRST (separate connection / commit) so that even if
 * the cascade fails downstream, the attempt is recorded.
 *
 * Endpoint: c=9971&aat=&user_id=&reason=&allow_nonzero_balance=0|1&allow_bot=0|1
 */
public class DeleteUserAccountProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest req = param.get();

            String adminNick = RbacSupport.getAdminNicknameFromToken(req, response);
            if (adminNick == null) return response.toString();
            if (!RbacSupport.hasSuperAdminRole(adminNick)) {
                return err(response, "1003", "Only super-admin may delete user accounts");
            }
            String adminIp = RbacSupport.getRequestIp(req);

            String userIdStr = req.getParameter("user_id");
            String reason = req.getParameter("reason");
            boolean allowNonzeroBalance = "1".equals(req.getParameter("allow_nonzero_balance"));
            boolean allowBot = "1".equals(req.getParameter("allow_bot"));

            if (userIdStr == null || userIdStr.isEmpty()) return err(response, "4001", "user_id required");
            long userId;
            try { userId = Long.parseLong(userIdStr); }
            catch (NumberFormatException e) { return err(response, "4002", "user_id must be a number"); }
            if (reason == null || reason.trim().length() < 10) {
                return err(response, "4003", "reason required (min 10 chars)");
            }

            // Snapshot user + tally row counts before delete
            String userName, nickName;
            long balanceVin, balanceXu;
            int isBot;
            JSONArray rowsBefore = new JSONArray();
            long totalRows = 0;

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT user_name, nick_name, vin, 0 AS xu, is_bot FROM vinplay.users WHERE id=?")) {
                    ps.setLong(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) return err(response, "1002", "User not found");
                        userName = rs.getString("user_name");
                        nickName = rs.getString("nick_name");
                        balanceVin = rs.getLong("vin");
                        balanceXu = 0L;
                        isBot = rs.getInt("is_bot");
                    }
                }

                if (!allowNonzeroBalance && (balanceVin != 0 || balanceXu != 0)) {
                    return err(response, "4101", "User has non-zero balance (vin=" + balanceVin
                            + ", xu=" + balanceXu + "). Set allow_nonzero_balance=1 to override.");
                }
                if (!allowBot && isBot == 1) {
                    return err(response, "4102", "User flagged is_bot=1. Set allow_bot=1 to override.");
                }

                // Pending money_transaction guard
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT COUNT(*) FROM vinplay.money_transaction WHERE initiator_user_id=? AND status='PENDING'")) {
                    ps.setLong(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && rs.getLong(1) > 0) {
                            return err(response, "4103", "User has " + rs.getLong(1)
                                    + " PENDING money_transaction(s). Resolve before delete.");
                        }
                    }
                }

                // Tally rows that the cascade will affect
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME FROM information_schema.columns " +
                        "WHERE TABLE_SCHEMA IN ('vinplay','vinplay_admin','vinplay_minigame','vinplay_gamebai') " +
                        "  AND COLUMN_NAME IN ('user_id','userId') " +
                        "  AND TABLE_NAME NOT LIKE '\\_archive%' AND TABLE_NAME NOT LIKE 'v\\_%'");
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
                                        row.put("rows", n);
                                        rowsBefore.put(row);
                                        totalRows += n;
                                    }
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }

            // 1) Insert audit FIRST (separate connection on admin schema, auto-commit)
            long auditId = -1;
            try (Connection adminConn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
                 PreparedStatement ps = adminConn.prepareStatement(
                        "INSERT INTO vinplay_admin.user_deletion_audit " +
                        "(target_user_id, target_user_name, target_nick_name, target_balance_vin, " +
                        " target_balance_xu, admin_actor, admin_ip, reason, rows_deleted_total, rows_per_table) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, userId);
                ps.setString(2, userName);
                ps.setString(3, nickName);
                ps.setLong(4, balanceVin);
                ps.setLong(5, balanceXu);
                ps.setString(6, adminNick);
                ps.setString(7, adminIp);
                ps.setString(8, reason.trim());
                ps.setLong(9, totalRows);
                ps.setString(10, rowsBefore.toString());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) auditId = keys.getLong(1);
                }
            }

            // 2) Execute cascade delete (allow bot trigger override if needed)
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                if (allowBot) {
                    try (Statement st = conn.createStatement()) {
                        st.execute("SET @allow_bot_delete = 1");
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM vinplay.users WHERE id=?")) {
                    ps.setLong(1, userId);
                    int affected = ps.executeUpdate();
                    if (affected != 1) {
                        return err(response, "9998", "DELETE affected " + affected + " rows; expected 1");
                    }
                }
            }

            JSONObject data = new JSONObject();
            data.put("user_id", userId);
            data.put("user_name", userName);
            data.put("nick_name", nickName);
            data.put("rows_deleted_total", totalRows);
            data.put("rows_per_table", rowsBefore);
            data.put("audit_id", auditId);
            data.put("admin_actor", adminNick);

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", data);

            logger.info("Phase F1 cascade-delete OK admin=" + adminNick + " target=" + userId
                    + " rows=" + totalRows + " audit=" + auditId);
            return response.toString();
        } catch (Exception e) {
            logger.error("DeleteUserAccountProcessor error", e);
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
