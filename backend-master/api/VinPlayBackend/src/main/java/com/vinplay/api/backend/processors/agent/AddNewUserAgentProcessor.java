package com.vinplay.api.backend.processors.agent;

import com.google.gson.JsonObject;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

public class AddNewUserAgentProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> parameter) {
        JsonObject result = new JsonObject();
        result.addProperty("success", false);
        result.addProperty("errorCode", "1001");

        HttpServletRequest request = (HttpServletRequest) parameter.get();
        Connection adminConn = null;
        Connection userConn = null;

        try {
            // Lấy thông tin từ request
            String nickname = getParamFallback(request, "nickname", "nn");
            String nameagent = getParamFallback(request, "nameagent", "na");
            String address = getParamFallback(request, "address", "adr");
            String phone = getParamFallback(request, "phone", "ph");
            String email = getParamFallback(request, "email", "em");
            String levelStr = getParamFallback(request, "level", "lv");
            String code = getParamFallback(request, "code", "ac");
            String pidStr = getParamFallback(request, "parentId", "pid");
            
            if (nickname == null || nickname.trim().isEmpty()) {
                result.addProperty("errorCode", "4001");
                result.addProperty("message", "nn (nickname) required");
                return result.toString();
            }
            if (levelStr == null || levelStr.isEmpty()) {
                result.addProperty("errorCode", "4001");
                result.addProperty("message", "lv (level) required");
                return result.toString();
            }

            int level = Integer.parseInt(levelStr);
            if (level < 1 || level > 3) {
                result.addProperty("errorCode", "4002");
                result.addProperty("message", "level must be 1, 2, or 3");
                return result.toString();
            }

            // ĐL1 (level=2) và ĐL2 (level=3) BẮT BUỘC có pid (parent agent)
            if (level > 1 && (pidStr == null || pidStr.trim().isEmpty())) {
                result.addProperty("errorCode", "4001");
                result.addProperty("message", "pid (parent agent ID) required for level " + level);
                return result.toString();
            }

            // Tuyến trên: nếu không truyền pid thì TĐL (level=1) tự động về Special Account (code='0')
            int parentId = -1;
            if (pidStr != null && !pidStr.trim().isEmpty()) {
                try {
                    parentId = Integer.parseInt(pidStr);
                } catch (NumberFormatException ignored) {}
            }
            // parentId sẽ được resolve sau khi có adminConn (cần query Special Account ID)

            // 1. Tìm User trong cơ sở dữ liệu Game (Tìm chính xác theo nickname)
            userConn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
            if (userConn == null) {
                return result.toString();
            }

            String realUsername = null;
            String realNickname = null;
            String playerPassword = "";

            try (PreparedStatement ps = userConn.prepareStatement("SELECT user_name, nick_name, password FROM users WHERE nick_name = ? OR user_name = ?")) {
                ps.setString(1, nickname);
                ps.setString(2, nickname); // Dự phòng trường hợp nhầm lẫn ô nhập
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        realUsername = rs.getString("user_name");
                        realNickname = rs.getString("nick_name");
                        playerPassword = rs.getString("password");
                        if (playerPassword == null) playerPassword = "";
                    }
                }
            }

            if (realUsername == null) {
                result.addProperty("errorCode", "1002");
                result.addProperty("message", "Player not found in game system: " + nickname);
                return result.toString();
            }
            if (realNickname == null || realNickname.trim().isEmpty()) {
                realNickname = realUsername;
            }

            adminConn = ConnectionPool.getInstance().getConnection(ConnectionPool.ADMIN_POOL);
            if (adminConn == null) {
                return result.toString();
            }

            // 2. Resolve parentId: nếu level=1 và chưa có parent → tự gán về Special Account (code='0')
            if (parentId == -1 && level == 1) {
                try (PreparedStatement ps = adminConn.prepareStatement("SELECT id FROM useragent WHERE code = '0' LIMIT 1")) {
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) parentId = rs.getInt(1);
                    }
                }
            }

            // 3. Build cấu trúc phả hệ Tuyến trên (Ancestors) + lấy code của parent
            String ancestors = "";
            String parentCode = "0"; // Default = SpecialAccount code
            String parentNickname = "";
            if (parentId > 0) {
                try (PreparedStatement ps = adminConn.prepareStatement("SELECT ancestors, code, nickname FROM useragent WHERE id = ?")) {
                    ps.setInt(1, parentId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            String parentAncestors = rs.getString("ancestors");
                            parentCode = rs.getString("code");
                            parentNickname = rs.getString("nickname");
                            if (parentAncestors != null && !parentAncestors.trim().isEmpty()) {
                                ancestors = parentAncestors + "," + parentId;
                            } else {
                                ancestors = String.valueOf(parentId);
                            }
                        } else {
                            result.addProperty("errorCode", "4003");
                            result.addProperty("message", "Parent agent not found");
                            return result.toString();
                        }
                    }
                }
            }

            // 4. Kiểm tra User đã là Đại lý trong hệ thống Admin chưa
            int existingAgentId = 0;
            try (PreparedStatement ps = adminConn.prepareStatement("SELECT id FROM useragent WHERE username = ?")) {
                ps.setString(1, realUsername);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) existingAgentId = rs.getInt("id");
                }
            }

            Timestamp now = new Timestamp(System.currentTimeMillis());
            int agentId = existingAgentId;

            if (existingAgentId > 0) {
                // UPDATE Đại lý hiện tại
                String updSql = "UPDATE useragent SET level=?, nickname=?, parentid=?, ancestors=?, nameagent=?, address=?, phone=?, email=?, updatetime=? ";
                if (code != null && !code.trim().isEmpty()) updSql += ", code=? ";
                updSql += "WHERE id=?";

                try (PreparedStatement ps = adminConn.prepareStatement(updSql)) {
                    int idx = 1;
                    ps.setInt(idx++, level);
                    ps.setString(idx++, realNickname);
                    ps.setInt(idx++, parentId);
                    ps.setString(idx++, ancestors);
                    ps.setString(idx++, nameagent != null ? nameagent : "");
                    ps.setString(idx++, address != null ? address : "");
                    ps.setString(idx++, phone != null ? phone : "");
                    ps.setString(idx++, email != null ? email : "");
                    ps.setTimestamp(idx++, now);
                    if (code != null && !code.trim().isEmpty()) ps.setString(idx++, code);
                    ps.setInt(idx, agentId);
                    ps.executeUpdate();
                }
                logger.info("AddNewUserAgentProcessor: updated existing agent " + realNickname);
            } else {
                // UPGRADE: Thêm mới hồ sơ Đại lý cho User thường
                if (code == null || code.trim().isEmpty()) {
                    result.addProperty("errorCode", "4001");
                    result.addProperty("message", "ac (referral code) required for new agent");
                    return result.toString();
                }
                code = code.trim().toUpperCase();
                if (code.length() > 10) code = code.substring(0, 10);

                String insSql = "INSERT INTO useragent (username, nickname, password, nameagent, address, phone, email, level, code, " +
                        "status, active, `show`, createtime, updatetime, percent_bonus_vincard, `order`, login_times, parentid, ancestors, commission_rate, deposit_rate) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 1, 1, ?, ?, 0, 0, 0, ?, ?, 0, 0)"; // deposit_rate cột đã có từ migration
                
                try (PreparedStatement ps = adminConn.prepareStatement(insSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, realUsername);
                    ps.setString(2, realNickname);
                    ps.setString(3, playerPassword);
                    ps.setString(4, nameagent != null ? nameagent : "");
                    ps.setString(5, address != null ? address : "");
                    ps.setString(6, phone != null ? phone : "");
                    ps.setString(7, email != null ? email : "");
                    ps.setInt(8, level);
                    ps.setString(9, code);
                    ps.setTimestamp(10, now);
                    ps.setTimestamp(11, now);
                    ps.setInt(12, parentId);
                    ps.setString(13, ancestors);
                    ps.executeUpdate();
                    
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) agentId = keys.getInt(1);
                    }
                }
                logger.info("AddNewUserAgentProcessor: created agent " + realNickname + " (id=" + agentId + ")");

                // Tạo cấu hình hoa hồng mốc khởi điểm — dùng userId từ game DB, không phải agentId
                try {
                    // Lấy id của user trong bảng users (game DB)
                    long gameUserId = 0;
                    try (PreparedStatement idPs = userConn.prepareStatement("SELECT id FROM users WHERE user_name = ?")) {
                        idPs.setString(1, realUsername);
                        try (ResultSet idRs = idPs.executeQuery()) {
                            if (idRs.next()) gameUserId = idRs.getLong(1);
                        }
                    }
                    if (gameUserId > 0) {
                        try (PreparedStatement rps = userConn.prepareStatement(
                                "INSERT INTO rebate_config (agent_user_id, agent_nickname, agent_level, rebate_percentage) VALUES (?, ?, ?, 0) " +
                                "ON DUPLICATE KEY UPDATE agent_level=VALUES(agent_level)")) {
                            rps.setLong(1, gameUserId);
                            rps.setString(2, realNickname);
                            rps.setInt(3, level);
                            rps.executeUpdate();
                        }
                        // SUN-716: Seed default per-game commission rates for new agents
                        AdminMakeAgentProcessor.seedDefaultGameCommissionRates(userConn, realNickname, level);
                    }
                } catch (Exception e) {
                    logger.error("AddNewUserAgentProcessor: cannot insert rebate config", e);
                }
            }

            // 4. Mở khóa quyền Đại Lý cho User trong hệ thống Game Core + sync parent info
            StringBuilder userUpdateSql = new StringBuilder("UPDATE users SET dai_ly = ?, parent_agent_id = ?, referral_code = ?");
            if (parentNickname != null && !parentNickname.isEmpty()) {
                userUpdateSql.append(", parrentUser = ?");
            }
            userUpdateSql.append(" WHERE user_name = ?");
            
            try (PreparedStatement ps = userConn.prepareStatement(userUpdateSql.toString())) {
                ps.setInt(1, level);
                ps.setInt(2, parentId);
                ps.setString(3, parentCode != null ? parentCode : "0");
                
                int usrIdx = 4;
                if (parentNickname != null && !parentNickname.isEmpty()) {
                    ps.setString(usrIdx++, parentNickname);
                }
                ps.setString(usrIdx, realUsername);
                ps.executeUpdate();
            }

            result.addProperty("success", true);
            result.addProperty("errorCode", "0");
            JsonObject data = new JsonObject();
            data.addProperty("id", agentId);
            data.addProperty("action", existingAgentId > 0 ? "updated" : "created");
            result.add("data", data);

        } catch (NumberFormatException e) {
            result.addProperty("errorCode", "4002");
            result.addProperty("message", "Invalid level format");
        } catch (Exception e) {
            logger.error("AddNewUserAgentProcessor error", e);
            result.addProperty("errorCode", "9999");
            result.addProperty("message", "Exception: " + e.getMessage());
        } finally {
            ConnectionPool.releaseConnection(adminConn);
            ConnectionPool.releaseConnection(userConn);
        }

        return result.toString();
    }

    private String getParamFallback(HttpServletRequest request, String primaryKey, String fallbackKey) {
        String val = request.getParameter(primaryKey);
        if (val != null && !val.trim().isEmpty()) {
            return val;
        }
        return request.getParameter(fallbackKey);
    }
}
