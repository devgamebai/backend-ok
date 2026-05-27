package com.vinplay.api.processors.withdraw;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * c=3040: Get withdrawal status (volume tracking, commission).
 */
public class GetWithdrawalStatusProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String accessToken = request.getParameter("at");

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
            String nickname = tokenMap.get(accessToken);

            IMap<String, UserCacheModel> userMap = instance.getMap("users");
            UserCacheModel userCache = userMap.get(nickname);
            long userId = -1;
            if (userCache != null) {
                userId = userCache.getId();
            } else {
                try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                     PreparedStatement ps = conn.prepareStatement("SELECT id FROM users WHERE nick_name = ?")) {
                    ps.setString(1, nickname);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) userId = rs.getLong("id");
                    }
                }
            }
            if (userId <= 0) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            long totalRequiredVolume = 0;
            long totalActualVolume = 0;
            boolean recordExists = false;

            Connection conn = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                ps = conn.prepareStatement(
                        "SELECT total_required_volume, total_actual_volume FROM user_volume_tracking WHERE user_id = ?");
                ps.setLong(1, userId);
                rs = ps.executeQuery();
                if (rs.next()) {
                    recordExists = true;
                    totalRequiredVolume = rs.getLong("total_required_volume");
                    totalActualVolume = rs.getLong("total_actual_volume");
                }
            } finally {
                if (rs != null) try { rs.close(); } catch (Exception ignored) {}
                if (ps != null) try { ps.close(); } catch (Exception ignored) {}
                if (conn != null) try { conn.close(); } catch (Exception ignored) {}
            }

            if (!recordExists) {
                Connection conn2 = null;
                PreparedStatement ps2 = null;
                try {
                    conn2 = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                    ps2 = conn2.prepareStatement(
                            "INSERT INTO user_volume_tracking (user_id, nick_name, total_required_volume, total_actual_volume, created_at) " +
                            "VALUES (?, ?, 0, 0, NOW())");
                    ps2.setLong(1, userId);
                    ps2.setString(2, nickname);
                    ps2.executeUpdate();
                } finally {
                    if (ps2 != null) try { ps2.close(); } catch (Exception ignored) {}
                    if (conn2 != null) try { conn2.close(); } catch (Exception ignored) {}
                }
            }

            String status;
            if (totalActualVolume >= totalRequiredVolume || (totalRequiredVolume == 0 && totalActualVolume == 0)) {
                status = "ENABLED";
            } else {
                status = "DISABLED";
            }

            long remainingVolume = totalRequiredVolume - totalActualVolume;
            if (remainingVolume < 0) remainingVolume = 0;

            long commissionAvailable = 0;
            long commissionWithdrawn = 0;

            Connection conn3 = null;
            PreparedStatement ps3 = null;
            ResultSet rs3 = null;
            try {
                conn3 = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                ps3 = conn3.prepareStatement(
                        "SELECT COALESCE(SUM(fee), 0) AS total_fee, COALESCE(SUM(fee_withdrawn), 0) AS total_withdrawn " +
                        "FROM user_fee WHERE user_id = ?");
                ps3.setLong(1, userId);
                rs3 = ps3.executeQuery();
                if (rs3.next()) {
                    commissionAvailable = rs3.getLong("total_fee");
                    commissionWithdrawn = rs3.getLong("total_withdrawn");
                }
            } finally {
                if (rs3 != null) try { rs3.close(); } catch (Exception ignored) {}
                if (ps3 != null) try { ps3.close(); } catch (Exception ignored) {}
                if (conn3 != null) try { conn3.close(); } catch (Exception ignored) {}
            }

            // Get withdrawal settings
            long minBankWithdraw = 0;
            long maxBankWithdraw = Long.MAX_VALUE;

            Connection conn4 = null;
            PreparedStatement ps4 = null;
            ResultSet rs4 = null;
            try {
                conn4 = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                ps4 = conn4.prepareStatement(
                        "SELECT setting_key, setting_value FROM withdrawal_settings WHERE setting_key IN ('min_bank_withdraw_krw', 'max_bank_withdraw_krw')");
                rs4 = ps4.executeQuery();
                while (rs4.next()) {
                    String key = rs4.getString("setting_key");
                    String val = rs4.getString("setting_value");
                    if ("min_bank_withdraw_krw".equals(key) && val != null) {
                        minBankWithdraw = Long.parseLong(val);
                    } else if ("max_bank_withdraw_krw".equals(key) && val != null) {
                        maxBankWithdraw = Long.parseLong(val);
                    }
                }
            } finally {
                if (rs4 != null) try { rs4.close(); } catch (Exception ignored) {}
                if (ps4 != null) try { ps4.close(); } catch (Exception ignored) {}
                if (conn4 != null) try { conn4.close(); } catch (Exception ignored) {}
            }

            long maxWithdrawIfDisabled = commissionAvailable - commissionWithdrawn;
            if (maxWithdrawIfDisabled < 0) maxWithdrawIfDisabled = 0;

            JSONObject data = new JSONObject();
            data.put("status", status);
            data.put("total_required_volume", totalRequiredVolume);
            data.put("total_actual_volume", totalActualVolume);
            data.put("remaining_volume", remainingVolume);
            data.put("commission_available", commissionAvailable);
            data.put("commission_withdrawn", commissionWithdrawn);
            data.put("max_withdraw_if_disabled", maxWithdrawIfDisabled);
            data.put("min_bank_withdraw", minBankWithdraw);
            data.put("max_bank_withdraw", maxBankWithdraw);

            response.put("success", true);
            response.put("data", data);

        } catch (Exception e) {
            logger.error("GetWithdrawalStatusProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}
