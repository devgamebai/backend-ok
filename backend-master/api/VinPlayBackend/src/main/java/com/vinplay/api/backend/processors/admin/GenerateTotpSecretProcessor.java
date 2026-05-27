package com.vinplay.api.backend.processors.admin;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.statics.TimeBasedOneTimePasswordUtil;
import com.vinplay.vbee.common.utils.TotpSecretCodec;
import com.vinplay.api.backend.auth.AdminAuthHelper;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class GenerateTotpSecretProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String adminToken = request.getParameter("aat");
            
            if (adminToken == null || adminToken.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1008");
                return response.toString();
            }

            String userName = AdminAuthHelper.getAdminUsernameByToken(adminToken);
            if (userName == null || userName.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1008");
                return response.toString();
            }
            
            // Generate secret
            String secret = TimeBasedOneTimePasswordUtil.generateBase32Secret(16);
            String encryptedSecret = TotpSecretCodec.encrypt(secret);
            
            if (encryptedSecret == null) {
                response.put("success", false);
                response.put("errorCode", "1001");
                response.put("message", "Encryption failed");
                return response.toString();
            }

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
                int adminId = -1;
                int currentIs2FAEnabled = 0;
                try (PreparedStatement ps = conn.prepareStatement("SELECT ID, Is2FAEnabled FROM user WHERE UserName = ?")) {
                    ps.setString(1, userName);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            adminId = rs.getInt("ID");
                            currentIs2FAEnabled = rs.getInt("Is2FAEnabled");
                        }
                    }
                }
                
                if (adminId <= 0) {
                    response.put("success", false);
                    response.put("errorCode", "1008");
                    return response.toString();
                }

                if (currentIs2FAEnabled == 1) {
                    response.put("success", false);
                    response.put("errorCode", "1011");
                    response.put("message", "2FA is already enabled. Disable it first.");
                    return response.toString();
                }

                String sql = "UPDATE user SET GoogleAuthSecretEnc = ?, Is2FAEnabled = 0 WHERE ID = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, encryptedSecret);
                    ps.setInt(2, adminId);
                    int updated = ps.executeUpdate();
                    
                    if (updated > 0) {
                        try {
                            com.vinplay.api.backend.processors.role.RbacSupport.insertAuditLog(conn, userName, "GENERATE_2FA_SECRET", "admin_user", String.valueOf(adminId), "{}", "{\"action\":\"secret_generated\"}", com.vinplay.api.backend.processors.role.RbacSupport.getRequestIp(request));
                        } catch (Exception auditEx) {
                            logger.warn("Audit log failed for GENERATE_2FA_SECRET", auditEx);
                        }
                        
                        response.put("success", true);
                        response.put("errorCode", "0");
                        response.put("secret", secret); // return plaintext secret to FE to draw QR
                    } else {
                        response.put("success", false);
                        response.put("errorCode", "1001");
                    }
                }
            }
        } catch (Exception e) {
            logger.error("GenerateTotpSecretProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
