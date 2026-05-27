package com.vinplay.api.backend.processors.withdraw;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Enumeration;

/**
 * c=9641 — Update withdrawal settings (admin).
 * Accepts any setting_key=value pairs as request parameters.
 * Uses INSERT ... ON DUPLICATE KEY UPDATE for upsert.
 */
public class UpdateWithdrawalSettingsProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    // Known setting keys that can be updated
    private static final java.util.Set<String> ALLOWED_KEYS = new java.util.HashSet<String>(
            java.util.Arrays.asList(
                    "default_coefficient",
                    "min_bank_withdraw_krw",
                    "max_bank_withdraw_krw",
                    "min_crypto_withdraw_krw",
                    "max_crypto_withdraw_krw",
                    "withdraw_fee_percent",
                    "daily_withdraw_limit"
            )
    );

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String accessToken = request.getParameter("at");
            if (accessToken == null || accessToken.isEmpty()) accessToken = request.getParameter("aat");

            // Validate admin token
            if (accessToken == null || accessToken.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            HazelcastInstance instance = HazelcastClientFactory.getInstance();
            IMap<String, String> tokenMap = instance.getMap("cacheToken");
            if (!tokenMap.containsKey(accessToken)) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            // Collect setting params (skip system params like at, c)
            java.util.Map<String, String> settingsToUpdate = new java.util.LinkedHashMap<String, String>();
            Enumeration<String> paramNames = request.getParameterNames();
            while (paramNames.hasMoreElements()) {
                String key = paramNames.nextElement();
                if ("at".equals(key) || "c".equals(key)) continue;
                String value = request.getParameter(key);
                if (value != null && !value.isEmpty()) {
                    settingsToUpdate.put(key, value);
                }
            }

            if (settingsToUpdate.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "At least one setting parameter is required");
                return response.toString();
            }

            // Resolve admin nickname from token
            String adminNickname = tokenMap.get(accessToken);
            if (adminNickname == null) {
                adminNickname = "unknown";
            }

            // Upsert each setting with audit logging
            Connection conn = null;
            PreparedStatement psSelect = null;
            PreparedStatement ps = null;
            PreparedStatement psAudit = null;
            ResultSet rs = null;
            try {
                conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");

                // Read old values for audit log
                java.util.Map<String, String> oldValues = new java.util.LinkedHashMap<String, String>();
                psSelect = conn.prepareStatement("SELECT setting_key, setting_value FROM withdrawal_settings WHERE setting_key = ?");
                for (String key : settingsToUpdate.keySet()) {
                    psSelect.setString(1, key);
                    rs = psSelect.executeQuery();
                    if (rs.next()) {
                        oldValues.put(key, rs.getString("setting_value"));
                    } else {
                        oldValues.put(key, null);
                    }
                    rs.close();
                    rs = null;
                }
                psSelect.close();
                psSelect = null;

                // Upsert settings
                ps = conn.prepareStatement(
                        "INSERT INTO withdrawal_settings (setting_key, setting_value, updated_at, updated_by) " +
                        "VALUES (?, ?, NOW(), ?) ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), updated_at = NOW(), updated_by = VALUES(updated_by)");

                for (java.util.Map.Entry<String, String> entry : settingsToUpdate.entrySet()) {
                    ps.setString(1, entry.getKey());
                    ps.setString(2, entry.getValue());
                    ps.setString(3, adminNickname);
                    ps.addBatch();
                }
                ps.executeBatch();

                // Insert audit log for each changed setting
                psAudit = conn.prepareStatement(
                        "INSERT INTO settings_change_log (setting_key, old_value, new_value, changed_by, changed_at) " +
                        "VALUES (?, ?, ?, ?, NOW())");
                for (java.util.Map.Entry<String, String> entry : settingsToUpdate.entrySet()) {
                    String oldVal = oldValues.get(entry.getKey());
                    String newVal = entry.getValue();
                    // Only log if value actually changed
                    if (oldVal == null || !oldVal.equals(newVal)) {
                        psAudit.setString(1, entry.getKey());
                        psAudit.setString(2, oldVal != null ? oldVal : "");
                        psAudit.setString(3, newVal);
                        psAudit.setString(4, adminNickname);
                        psAudit.addBatch();
                    }
                }
                psAudit.executeBatch();
            } finally {
                if (rs != null) try { rs.close(); } catch (Exception ignored) {}
                if (psSelect != null) try { psSelect.close(); } catch (Exception ignored) {}
                if (ps != null) try { ps.close(); } catch (Exception ignored) {}
                if (psAudit != null) try { psAudit.close(); } catch (Exception ignored) {}
                if (conn != null) try { conn.close(); } catch (Exception ignored) {}
            }

            response.put("success", true);

        } catch (Exception e) {
            logger.error("UpdateWithdrawalSettingsProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}
