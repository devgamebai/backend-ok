package com.vinplay.api.backend.processors.admin;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class LogAdminActionProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String un = request.getParameter("un");
            String action = request.getParameter("action");
            String quantity = request.getParameter("quantity");
            String money = request.getParameter("money");
            String accountName = request.getParameter("account_name");
            String moneyType = request.getParameter("money_type");
            String reason = request.getParameter("reason");
            String status = request.getParameter("status");

            if (un == null || un.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
                // Dedupe against inline log_admin writes from canonical money paths
                // (c=100 UpdateMoneyUserProcessor, c=105 UpdateStatusUserProcessor,
                // etc., which now own their own audit per the money-ledger rule).
                // Without this guard, FE callers that also call c=USER_MONEY_LOG
                // explicitly produce a duplicate row per money action — surfaced
                // as 2 rows per add in /api/usergame/money-log.
                //
                // Match window: 10s. Same caller (username) + same target
                // (account_name) + same action label + same money + same
                // money_type within 10s = treat as the same logical event.
                String dupSql = "SELECT id FROM log_admin "
                    + "WHERE username = ? AND account_name = ? AND action = ? "
                    + "AND money = ? AND COALESCE(money_type,'') = COALESCE(?, '') "
                    + "AND timestamp >= (NOW() - INTERVAL 10 SECOND) LIMIT 1";
                boolean alreadyLogged = false;
                try (PreparedStatement dupPs = conn.prepareStatement(dupSql)) {
                    dupPs.setString(1, un);
                    dupPs.setString(2, accountName != null ? accountName : "");
                    dupPs.setString(3, action != null ? action : "");
                    dupPs.setString(4, money != null ? money : "0");
                    dupPs.setString(5, moneyType != null ? moneyType : "");
                    try (java.sql.ResultSet rs = dupPs.executeQuery()) {
                        alreadyLogged = rs.next();
                    }
                }

                if (!alreadyLogged) {
                    String sql = "INSERT INTO log_admin (username, action, quantity, money, account_name, money_type, reason, status) VALUES (?,?,?,?,?,?,?,?)";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, un);
                        ps.setString(2, action != null ? action : "");
                        ps.setString(3, quantity != null ? quantity : "0");
                        ps.setString(4, money != null ? money : "0");
                        ps.setString(5, accountName != null ? accountName : "");
                        ps.setString(6, moneyType != null ? moneyType : "");
                        ps.setString(7, reason != null ? reason : "");
                        ps.setString(8, status != null ? status : "");
                        ps.executeUpdate();
                    }
                }
            }

            response.put("success", true);
            response.put("errorCode", "0");
        } catch (Exception e) {
            logger.error("LogAdminActionProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
