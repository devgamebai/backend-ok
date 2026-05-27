package com.vinplay.api.backend.processors.admin;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.pools.ConnectionPool;
import javax.servlet.http.HttpServletRequest;
import java.sql.*;
import java.util.Date;
import org.json.JSONObject;

public class BulkLoadCacheProcessor implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> parameter) {
        JSONObject response = new JSONObject();
        try {
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            IMap userMap = client.getMap("users");
            int loaded = 0, skipped = 0;
            
            Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
            if (conn == null) {
                response.put("success", false);
                response.put("errorCode", "1030");
                return response.toString();
            }
            try {
                PreparedStatement stmt = conn.prepareStatement(
                    "SELECT id, user_name, nick_name, password, email, facebook_id, google_id, " +
                    "mobile, birthday, gender, address, vin, xu, vin_total, xu_total, safe, " +
                    "recharge_money, vip_point, status, avatar, identification, " +
                    "vip_point_save, create_time, money_vp, security_time, login_otp, is_bot " +
                    "FROM users WHERE is_bot = 0 AND vin > 0");
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    String nn = rs.getString("nick_name");
                    if (nn == null || nn.isEmpty()) continue;
                    if (userMap.containsKey(nn)) { skipped++; continue; }
                    
                    UserCacheModel u = new UserCacheModel();
                    u.setId(rs.getInt("id"));
                    u.setUsername(rs.getString("user_name"));
                    u.setNickname(nn);
                    u.setPassword(rs.getString("password"));
                    u.setEmail(rs.getString("email"));
                    u.setFacebookId(rs.getString("facebook_id"));
                    u.setGoogleId(rs.getString("google_id"));
                    u.setMobile(rs.getString("mobile"));
                    u.setBirthday(rs.getString("birthday") != null ? rs.getString("birthday") : "");
                    u.setGender(rs.getInt("gender") == 1);
                    u.setAddress(rs.getString("address"));
                    u.setVin(rs.getLong("vin"));
                    u.setXu(0L);
                    u.setVinTotal(0L);
                    u.setXuTotal(0L);
                    u.setSafe(0L);
                    u.setRechargeMoney(0L);
                    u.setVippoint(0);
                    u.setDaily(0);
                    u.setStatus(rs.getInt("status"));
                    u.setAvatar(rs.getString("avatar"));
                    u.setIdentification(rs.getString("identification"));
                    u.setVippointSave(0);
                    u.setCreateTime(rs.getTimestamp("create_time") != null ? new Date(rs.getTimestamp("create_time").getTime()) : new Date());
                    u.setMoneyVP(0);
                    u.setSecurityTime(rs.getTimestamp("security_time") != null ? new Date(rs.getTimestamp("security_time").getTime()) : null);
                    u.setLoginOtp(rs.getLong("login_otp"));
                    u.setBot(rs.getBoolean("is_bot"));
                    u.setCashout(0);
                    u.setCashoutTime(new Date());
                    u.setOnline(0);
                    userMap.put(nn, u);
                    loaded++;
                }
                rs.close(); stmt.close();
            } finally { conn.close(); }
            
            response.put("success", true);
            response.put("errorCode", "0");
            JSONObject data = new JSONObject();
            data.put("loaded", loaded);
            data.put("skipped", skipped);
            data.put("cacheSize", userMap.size());
            response.put("data", data.toString());
        } catch (Exception e) {
            response.put("success", false);
            response.put("errorCode", "1001");
            response.put("error", e.getMessage());
        }
        return response.toString();
    }
}
