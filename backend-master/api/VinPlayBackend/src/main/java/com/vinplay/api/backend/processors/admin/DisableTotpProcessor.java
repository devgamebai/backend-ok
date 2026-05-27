package com.vinplay.api.backend.processors.admin;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.statics.TimeBasedOneTimePasswordUtil;
import com.vinplay.vbee.common.utils.TotpSecretCodec;
import com.vinplay.vbee.common.utils.BcryptUtils;
import com.vinplay.api.backend.auth.AdminAuthHelper;
import com.vinplay.api.backend.processors.role.RbacSupport;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DisableTotpProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String adminToken = request.getParameter("aat");
            String targetUsername = request.getParameter("targetUsername");
            String otp = request.getParameter("otp");
            
            if (adminToken == null || adminToken.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1008");
                return response.toString();
            }

            String currentUsername = AdminAuthHelper.getAdminUsernameByToken(adminToken);
            
            if (currentUsername == null || currentUsername.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1008");
                return response.toString();
            }

            String target = (targetUsername != null && !targetUsername.isEmpty()) ? targetUsername : currentUsername;
            boolean isSelf = currentUsername.equalsIgnoreCase(target);
            
            // RBAC Check
            if (!isSelf) {
                if (!RbacSupport.hasSuperAdminRole(currentUsername) && 
                    !RbacSupport.hasPermission(currentUsername, "system.user.manage_2fa")) {
                    response.put("success", false);
                    response.put("errorCode", "1001");
                    response.put("message", "Permission denied: Requires system.user.manage_2fa");
                    return response.toString();
                }
            }

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
                int targetAdminId = -1;
                String secretEnc = null;
                String sqlGetTarget = "SELECT ID, GoogleAuthSecretEnc FROM user WHERE UserName = ?";
                try (PreparedStatement psGet = conn.prepareStatement(sqlGetTarget)) {
                    psGet.setString(1, target);
                    try (ResultSet rs = psGet.executeQuery()) {
                        if (rs.next()) {
                            targetAdminId = rs.getInt("ID");
                            secretEnc = rs.getString("GoogleAuthSecretEnc");
                        }
                    }
                }

                if (targetAdminId <= 0) {
                    response.put("success", false);
                    response.put("errorCode", "1005");
                    response.put("message", "Target user not found");
                    return response.toString();
                }

                // If self, must provide OTP to confirm
                if (isSelf) {
                    if (otp == null || otp.isEmpty()) {
                        response.put("success", false);
                        response.put("errorCode", "1008");
                        response.put("message", "OTP required to disable 2FA");
                        return response.toString();
                    }

                    boolean isValidOtp = false;
                    if (otp.length() == 6 && secretEnc != null) {
                        String secret = TotpSecretCodec.decrypt(secretEnc);
                        if (secret != null) {
                            try {
                                if (TimeBasedOneTimePasswordUtil.validateCurrentNumber(secret, Integer.parseInt(otp), 1)) {
                                    isValidOtp = true;
                                }
                            } catch (NumberFormatException nfe) {}
                        }
                    } else {
                        // Check recovery code
                        String sqlRec = "SELECT id, code_hash FROM user_recovery_codes WHERE user_id = ? AND used_at IS NULL";
                        try (PreparedStatement psRec = conn.prepareStatement(sqlRec)) {
                            psRec.setInt(1, targetAdminId);
                            try (ResultSet rsRec = psRec.executeQuery()) {
                                while (rsRec.next()) {
                                    String codeHash = rsRec.getString("code_hash");
                                    if (BcryptUtils.checkPassword(otp, codeHash)) {
                                        isValidOtp = true;
                                        break; // We are disabling anyway, no need to mark as used
                                    }
                                }
                            }
                        }
                    }

                    if (!isValidOtp) {
                        response.put("success", false);
                        response.put("errorCode", "1009");
                        response.put("message", "Invalid OTP or Recovery Code");
                        return response.toString();
                    }
                }

                conn.setAutoCommit(false);
                try {
                    // Disable 2FA
                    String sqlUpd = "UPDATE user SET Is2FAEnabled = 0, GoogleAuthSecretEnc = NULL, TwoFAResetCount = TwoFAResetCount + 1, Last2FAWindow = NULL WHERE ID = ?";
                    try (PreparedStatement psUpd = conn.prepareStatement(sqlUpd)) {
                        psUpd.setInt(1, targetAdminId);
                        psUpd.executeUpdate();
                    }

                    // Delete recovery codes
                    String sqlDelRec = "DELETE FROM user_recovery_codes WHERE user_id = ?";
                    try (PreparedStatement psDel = conn.prepareStatement(sqlDelRec)) {
                        psDel.setInt(1, targetAdminId);
                        psDel.executeUpdate();
                    }

                    // Audit Log
                    logger.info("Admin " + currentUsername + " disabled 2FA for " + target);
                    try {
                        RbacSupport.insertAuditLog(conn, currentUsername, "DISABLE_2FA", "admin_user", String.valueOf(targetAdminId), "{\"Is2FAEnabled\":1}", "{\"Is2FAEnabled\":0}", RbacSupport.getRequestIp(request));
                    } catch (Exception auditEx) {
                        logger.warn("Audit log failed for DISABLE_2FA", auditEx);
                    }

                    conn.commit();

                    response.put("success", true);
                    response.put("errorCode", "0");
                } catch (Exception e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            }
        } catch (Exception e) {
            logger.error("DisableTotpProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
