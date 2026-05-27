package com.vinplay.api.backend.processors.admin;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.statics.TimeBasedOneTimePasswordUtil;
import com.vinplay.vbee.common.utils.TotpSecretCodec;
import com.vinplay.vbee.common.utils.BcryptUtils;
import com.vinplay.api.backend.auth.AdminAuthHelper;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ConfirmEnableTotpProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no lookalikes (0, O, 1, I)

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String adminToken = request.getParameter("aat");
            String otp = request.getParameter("otp");
            
            if (adminToken == null || adminToken.isEmpty() || otp == null || otp.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            String userName = AdminAuthHelper.getAdminUsernameByToken(adminToken);
            if (userName == null || userName.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1008");
                return response.toString();
            }

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
                int adminId = -1;
                try (PreparedStatement ps = conn.prepareStatement("SELECT ID FROM user WHERE UserName = ?")) {
                    ps.setString(1, userName);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) adminId = rs.getInt("ID");
                    }
                }

                if (adminId <= 0) {
                    response.put("success", false);
                    response.put("errorCode", "1008");
                    return response.toString();
                }

                String sqlGet = "SELECT GoogleAuthSecretEnc FROM user WHERE ID = ?";
                String secretEnc = null;
                try (PreparedStatement psGet = conn.prepareStatement(sqlGet)) {
                    psGet.setInt(1, adminId);
                    try (ResultSet rs = psGet.executeQuery()) {
                        if (rs.next()) {
                            secretEnc = rs.getString("GoogleAuthSecretEnc");
                        }
                    }
                }

                if (secretEnc == null || secretEnc.isEmpty()) {
                    response.put("success", false);
                    response.put("errorCode", "1001");
                    response.put("message", "2FA setup not initiated");
                    return response.toString();
                }

                String secret = TotpSecretCodec.decrypt(secretEnc);
                if (secret == null || !TimeBasedOneTimePasswordUtil.validateCurrentNumber(secret, Integer.parseInt(otp), 1)) {
                    response.put("success", false);
                    response.put("errorCode", "1009");
                    response.put("message", "Invalid OTP");
                    return response.toString();
                }

                conn.setAutoCommit(false);
                try {
                    // Update user
                    String sqlUpd = "UPDATE user SET Is2FAEnabled = 1, TwoFAEnabledAt = NOW() WHERE ID = ?";
                    try (PreparedStatement psUpd = conn.prepareStatement(sqlUpd)) {
                        psUpd.setInt(1, adminId);
                        psUpd.executeUpdate();
                    }

                    // Delete existing recovery codes if any
                    String sqlDelRec = "DELETE FROM user_recovery_codes WHERE user_id = ?";
                    try (PreparedStatement psDel = conn.prepareStatement(sqlDelRec)) {
                        psDel.setInt(1, adminId);
                        psDel.executeUpdate();
                    }

                    // Generate new recovery codes
                    List<String> plainCodes = new ArrayList<>();
                    String sqlInsRec = "INSERT INTO user_recovery_codes (user_id, code_hash, created_at) VALUES (?, ?, NOW())";
                    try (PreparedStatement psIns = conn.prepareStatement(sqlInsRec)) {
                        SecureRandom random = new SecureRandom();
                        for (int i = 0; i < 8; i++) {
                            String code = generateRecoveryCode(random);
                            plainCodes.add(code);
                            // Hash with bcrypt
                            String hash = BcryptUtils.hashPassword(code);
                            psIns.setInt(1, adminId);
                            psIns.setString(2, hash);
                            psIns.addBatch();
                        }
                        psIns.executeBatch();
                    }

                    try {
                        com.vinplay.api.backend.processors.role.RbacSupport.insertAuditLog(conn, userName, "ENABLE_2FA", "admin_user", String.valueOf(adminId), "{\"Is2FAEnabled\":0}", "{\"Is2FAEnabled\":1}", com.vinplay.api.backend.processors.role.RbacSupport.getRequestIp(request));
                    } catch (Exception auditEx) {
                        logger.warn("Audit log failed for ENABLE_2FA", auditEx);
                    }

                    conn.commit();

                    response.put("success", true);
                    response.put("errorCode", "0");
                    response.put("recoveryCodes", new JSONArray(plainCodes));
                } catch (Exception e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            }
        } catch (NumberFormatException nfe) {
            response.put("success", false);
            response.put("errorCode", "1009");
            response.put("message", "Invalid OTP format");
        } catch (Exception e) {
            logger.error("ConfirmEnableTotpProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }

    private String generateRecoveryCode(SecureRandom random) {
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        // format: XXXX-XXXX-XXXX
        sb.insert(8, '-');
        sb.insert(4, '-');
        return sb.toString();
    }
}
