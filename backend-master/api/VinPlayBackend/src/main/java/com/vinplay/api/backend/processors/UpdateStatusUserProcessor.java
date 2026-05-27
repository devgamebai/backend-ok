/*
 * c=105 — Khoá / mở khoá tài khoản player.
 *
 * Legacy FE contract (admin-php): nn, ac (csv of bit indices), type (0|1).
 * New admin-next contract: nn, status (0|1) — implicitly bit 0 (BAN_LOGIN).
 *
 * This processor accepts BOTH shapes, NPE-safe, and writes a log_admin
 * audit row whose status reflects the actual outcome (same pattern as
 * the c=100 inline-audit fix).
 */
package com.vinplay.api.backend.processors;

import com.vinplay.usercore.service.impl.SecurityServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponseModel;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import org.apache.log4j.Logger;

public class UpdateStatusUserProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        BaseResponseModel response = new BaseResponseModel(false, "10001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickName = request.getParameter("nn");
        String action = request.getParameter("ac");
        String type = request.getParameter("type");
        // SUN-DISABLE-FIX: admin-next sends `status` instead of `ac`+`type`.
        // status=1 → ban login (ac=0, type=1); status=0 → unban login.
        // Without this the call NPE'd on action.split(",") and silently
        // failed — admin saw "success" in some toasts but the user's
        // status bit was never flipped, so they could keep playing.
        if (action == null || action.isEmpty()) {
            String status = request.getParameter("status");
            if (status != null && !status.isEmpty()) {
                action = "0"; // bit 0 = StatusUser.BAN_LOGIN
                type = status;
            }
        }
        String aat = request.getParameter("aat");
        String adminUser = com.vinplay.api.backend.auth.AdminAuthHelper.getAdminUsernameByToken(aat);

        if (nickName == null || nickName.isEmpty() || action == null || action.isEmpty()
                || type == null || (!"0".equals(type) && !"1".equals(type))) {
            response.setErrorCode("1001");
            writeAudit(adminUser, action, type, nickName, false, "missing nn/ac/type");
            return response.toJson();
        }

        SecurityServiceImpl service = new SecurityServiceImpl();
        boolean allOk = true;
        boolean anyApplied = false;
        for (String item : action.split(",")) {
            try {
                int idx = Integer.parseInt(item.trim());
                boolean ok = service.updateStatusUser(nickName, idx, type);
                if (ok) {
                    anyApplied = true;
                } else {
                    allOk = false;
                }
            } catch (NumberFormatException nfe) {
                allOk = false;
                logger.warn("UpdateStatusUserProcessor: invalid action item=" + item + " for nick=" + nickName);
            }
        }
        if (allOk && anyApplied) {
            response.setSuccess(true);
            response.setErrorCode("0");
        } else {
            response.setErrorCode("10001");
        }

        writeAudit(adminUser, action, type, nickName, response.isSuccess(), null);
        return response.toJson();
    }

    /**
     * Inline audit write — mirrors the c=100 pattern. Status comes from the
     * actual processor outcome, not the FE.
     */
    private static void writeAudit(String adminUser, String action, String type,
                                   String nickName, boolean success, String note) {
        try {
            String act = "1".equals(type) ? "Khoá tài khoản" : ("0".equals(type) ? "Mở khoá tài khoản" : "Update status user");
            String reason = note != null ? note : ("ac=" + action + " type=" + type);
            String un = adminUser != null && !adminUser.isEmpty() ? adminUser : "Admin";
            try (Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpool_admin");
                 PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO log_admin (username, action, quantity, money, account_name, money_type, reason, status) VALUES (?,?,?,?,?,?,?,?)")) {
                ps.setString(1, un);
                ps.setString(2, act);
                ps.setString(3, "0");
                ps.setString(4, "0");
                ps.setString(5, nickName != null ? nickName : "");
                ps.setString(6, "");
                ps.setString(7, reason);
                ps.setString(8, success ? "1" : "0");
                ps.executeUpdate();
            }
        } catch (Exception e) {
            logger.warn("UpdateStatusUserProcessor: log_admin audit failed nick=" + nickName + " — status flip was " + (success ? "OK" : "FAIL"), e);
        }
    }
}
