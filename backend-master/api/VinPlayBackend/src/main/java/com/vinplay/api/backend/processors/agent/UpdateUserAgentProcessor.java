package com.vinplay.api.backend.processors.agent;

import com.google.gson.JsonObject;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

public class UpdateUserAgentProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> parameter) {
        JsonObject result = new JsonObject();
        result.addProperty("success", false);
        result.addProperty("errorCode", "1001");

        HttpServletRequest request = (HttpServletRequest) parameter.get();
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            String idStr = request.getParameter("id");
            if (idStr == null || idStr.isEmpty()) {
                result.addProperty("errorCode", "1002");
                return result.toString();
            }
            int id = Integer.parseInt(idStr);

            String address = getParamFallback(request, "address", "adr");
            String phone = getParamFallback(request, "phone", "ph");
            String code = getParamFallback(request, "code", "ac");
            String email = getParamFallback(request, "email", "em");
            String levelStr = getParamFallback(request, "level", "lv");
            String commissionRate = getParamFallback(request, "commission_rate", "cr");
            String depositRateStr = getParamFallback(request, "deposit_rate", "dr"); // % hoa hồng theo tiền nạp


            if (commissionRate != null && !commissionRate.trim().isEmpty() && !commissionRate.trim().matches("^\\d+(\\.\\d{1,2})?$")) {
                result.addProperty("errorCode", "1002");
                return result.toString();
            }
            if (depositRateStr != null && !depositRateStr.trim().isEmpty() && !depositRateStr.trim().matches("^\\d+(\\.\\d{1,2})?$")) {
                result.addProperty("errorCode", "1002");
                result.addProperty("message", "Invalid deposit_rate format (max 2 decimal places)");
                return result.toString();
            }

            // Validate format like AddNewUserAgent
            if (code != null) {
                if (code.trim().isEmpty()) {
                    result.addProperty("errorCode", "4001");
                    result.addProperty("message", "Referral code cannot be empty");
                    return result.toString();
                }
                code = code.trim().toUpperCase();
                if (code.length() > 10) code = code.substring(0, 10);
            }
            if (email != null && !email.trim().isEmpty()) {
                if (!email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
                    result.addProperty("errorCode", "4001");
                    result.addProperty("message", "Invalid email format");
                    return result.toString();
                }
            }
            if (phone != null && !phone.trim().isEmpty()) {
                if (!phone.matches("^\\+?\\d{9,15}$")) {
                    result.addProperty("errorCode", "4001");
                    result.addProperty("message", "Invalid phone format");
                    return result.toString();
                }
            }

            conn = ConnectionPool.getInstance().getConnection(ConnectionPool.ADMIN_POOL);
            if (conn == null) {
                logger.error("UpdateUserAgentProcessor: cannot get DB connection");
                return result.toString();
            }

            // Check duplicate code before update
            if (code != null && !code.trim().isEmpty()) {
                try (PreparedStatement checkPs = conn.prepareStatement("SELECT id FROM useragent WHERE code = ? AND id != ? LIMIT 1")) {
                    checkPs.setString(1, code);
                    checkPs.setInt(2, id);
                    try (java.sql.ResultSet checkRs = checkPs.executeQuery()) {
                        if (checkRs.next()) {
                            result.addProperty("errorCode", "4005");
                            result.addProperty("message", "Referral code already exists");
                            return result.toString();
                        }
                    }
                }
            }

            // Guard: SpecialAccount (code='0') PHẢI có rate = 0 (nếu rate > 0 → hút hoa hồng toàn hệ thống)
            String agentCode = null;
            try (PreparedStatement cps = conn.prepareStatement("SELECT code FROM useragent WHERE id = ?")) {
                cps.setInt(1, id);
                try (java.sql.ResultSet crs = cps.executeQuery()) {
                    if (crs.next()) agentCode = crs.getString("code");
                }
            }
            if ("0".equals(agentCode)) {
                if ((commissionRate != null && !commissionRate.isEmpty() && Double.parseDouble(commissionRate) > 0)
                    || (depositRateStr != null && !depositRateStr.isEmpty() && Double.parseDouble(depositRateStr) > 0)) {
                    result.addProperty("errorCode", "4099");
                    result.addProperty("message", "Special Account must have zero rates — cannot set commission or deposit rate");
                    return result.toString();
                }
            }

            // Validate commission_rate constraints (child ≤ parent) — skip for SpecialAccount (always 0)
            if (commissionRate != null && !commissionRate.isEmpty() && !"0".equals(agentCode)) {
                double newRate = Double.parseDouble(commissionRate);
                // Fetch parent ID for this agent
                int parentId = -1;
                try (PreparedStatement pps = conn.prepareStatement("SELECT parentid FROM useragent WHERE id = ?")) {
                    pps.setInt(1, id);
                    try (java.sql.ResultSet prs = pps.executeQuery()) {
                        if (prs.next()) parentId = prs.getInt("parentid");
                    }
                }
                String rateError = com.vinplay.api.backend.services.CommissionRateValidator.validate(conn, id, newRate, parentId);
                if (rateError != null) {
                    result.addProperty("errorCode", "4005");
                    result.addProperty("message", rateError);
                    return result.toString();
                }
            }

            // -------------------------------------------------------------
            // Hierarchy & Level Protection Logic
            // -------------------------------------------------------------
            int currentLevel = -1;
            try (PreparedStatement lvlPs = conn.prepareStatement("SELECT level FROM useragent WHERE id = ?")) {
                lvlPs.setInt(1, id);
                try (java.sql.ResultSet lvlRs = lvlPs.executeQuery()) {
                    if (lvlRs.next()) currentLevel = lvlRs.getInt("level");
                }
            }

            // Enforce hierarchy automatically if updated to Level 1 (TĐL) (excludes SpecialAccount itself)
            int tdlForceParentId = -1;
            String tdlForceParentNickname = "";
            if (levelStr != null && "1".equals(levelStr) && !"0".equals(agentCode)) {
                try (PreparedStatement sps = conn.prepareStatement("SELECT id, nickname FROM useragent WHERE code = '0' LIMIT 1")) {
                    try (java.sql.ResultSet srs = sps.executeQuery()) {
                        if (srs.next()) {
                            tdlForceParentId = srs.getInt("id");
                            tdlForceParentNickname = srs.getString("nickname");
                        }
                    }
                }
            }

            if (levelStr != null && !levelStr.isEmpty() && currentLevel > 0) {
                int newLevel = Integer.parseInt(levelStr);
                if (newLevel != currentLevel) {
                    // 1. Block Demotion (hạ cấp) in this API
                    if (newLevel > currentLevel) {
                        result.addProperty("errorCode", "4011");
                        result.addProperty("message", "Demotion (Hạ cấp) is not allowed.");
                        return result.toString();
                    }
                    
                    // 2. Block Promotion to intermediate levels (e.g., DL2 -> DL1) because this API lacks parentid assignment
                    if (newLevel < currentLevel && newLevel != 1) {
                        result.addProperty("errorCode", "4013");
                        result.addProperty("message", "Cannot promote to intermediate level (" + newLevel + ") without reassigning parent. Please use Create/Update Agent API to explicitly set Parent ID.");
                        return result.toString();
                    }
                    
                    // PM confirmed: Promotion to TDL (Level 1) is allowed. Cascade update will be triggered down the hierarchy.
                }
            }

            // Build dynamic UPDATE
            StringBuilder sql = new StringBuilder("UPDATE useragent SET updatetime = ?");
            int paramCount = 1;

            if (address != null) { sql.append(", address = ?"); paramCount++; }
            if (phone != null) { sql.append(", phone = ?"); paramCount++; }
            if (code != null) { sql.append(", code = ?"); paramCount++; }
            if (email != null) { sql.append(", email = ?"); paramCount++; }
            if (levelStr != null && !levelStr.isEmpty()) { 
                sql.append(", level = ?"); paramCount++; 
                if (tdlForceParentId > 0) { sql.append(", parentid = ?, ancestors = ?"); paramCount += 2; }
            }
            if (commissionRate != null && !commissionRate.isEmpty()) { sql.append(", commission_rate = ?, percent_bonus_vincard = ?"); paramCount += 2; }
            if (depositRateStr != null && !depositRateStr.isEmpty()) { sql.append(", deposit_rate = ?"); paramCount++; }

            sql.append(" WHERE id = ?");

            ps = conn.prepareStatement(sql.toString());
            int idx = 1;
            ps.setTimestamp(idx++, new Timestamp(System.currentTimeMillis()));

            if (address != null) ps.setString(idx++, address);
            if (phone != null) ps.setString(idx++, phone);
            if (code != null) ps.setString(idx++, code);
            if (email != null) ps.setString(idx++, email);
            if (levelStr != null && !levelStr.isEmpty()) { 
                ps.setInt(idx++, Integer.parseInt(levelStr)); 
                if (tdlForceParentId > 0) {
                    ps.setInt(idx++, tdlForceParentId);
                    ps.setString(idx++, String.valueOf(tdlForceParentId));
                }
            }
            if (commissionRate != null && !commissionRate.isEmpty()) { ps.setDouble(idx++, Double.parseDouble(commissionRate)); ps.setDouble(idx++, Double.parseDouble(commissionRate)); }
            if (depositRateStr != null && !depositRateStr.isEmpty()) ps.setDouble(idx++, Double.parseDouble(depositRateStr));

            ps.setInt(idx, id);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                result.addProperty("success", true);
                result.addProperty("errorCode", "0");

                // Step 3: Sync properties to game DB (users, rebate_config)
                if ((commissionRate != null && !commissionRate.isEmpty()) || (levelStr != null && !levelStr.isEmpty())) {
                    try {
                        String agentNick = "";
                        String agentUsername = "";
                        int agentLevel = 1;
                        try (PreparedStatement nps = conn.prepareStatement("SELECT nickname, username, level FROM useragent WHERE id = ?")) {
                            nps.setInt(1, id);
                            try (java.sql.ResultSet nrs = nps.executeQuery()) {
                                if (nrs.next()) {
                                    agentNick = nrs.getString("nickname");
                                    agentUsername = nrs.getString("username");
                                    agentLevel = nrs.getInt("level");
                                }
                            }
                        }
                        
                        try (java.sql.Connection mainConn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                            long gameUserId = 0;
                            try (PreparedStatement idPs = mainConn.prepareStatement("SELECT id FROM users WHERE user_name = ?")) {
                                idPs.setString(1, agentUsername);
                                try (java.sql.ResultSet idRs = idPs.executeQuery()) {
                                    if (idRs.next()) gameUserId = idRs.getLong("id");
                                }
                            }
                            
                            if (gameUserId > 0) {
                                // Sync user agent level in game DB
                                if (levelStr != null && !levelStr.isEmpty()) {
                                    if (agentLevel == 1 && tdlForceParentId > 0) {
                                        try (PreparedStatement syncUsers = mainConn.prepareStatement("UPDATE users SET dai_ly = ?, parent_agent_id = ?, referral_code = '0', parrentUser = ? WHERE id = ?")) {
                                            syncUsers.setInt(1, agentLevel);
                                            syncUsers.setInt(2, tdlForceParentId);
                                            syncUsers.setString(3, tdlForceParentNickname);
                                            syncUsers.setLong(4, gameUserId);
                                            syncUsers.executeUpdate();
                                        }
                                    } else {
                                        try (PreparedStatement syncUsers = mainConn.prepareStatement("UPDATE users SET dai_ly = ? WHERE id = ?")) {
                                            syncUsers.setInt(1, agentLevel);
                                            syncUsers.setLong(2, gameUserId);
                                            syncUsers.executeUpdate();
                                        }
                                    }
                                }
                                
                                // Sync rebate_config
                                if (commissionRate != null && !commissionRate.isEmpty()) {
                                    try (PreparedStatement syncPs = mainConn.prepareStatement(
                                            "INSERT INTO rebate_config (agent_user_id, agent_nickname, agent_level, rebate_percentage) " +
                                            "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE rebate_percentage = VALUES(rebate_percentage), agent_level = VALUES(agent_level)")) {
                                        syncPs.setLong(1, gameUserId);
                                        syncPs.setString(2, agentNick);
                                        syncPs.setInt(3, agentLevel);
                                        syncPs.setDouble(4, Double.parseDouble(commissionRate));
                                        syncPs.executeUpdate();
                                    }
                                } else if (levelStr != null && !levelStr.isEmpty()) {
                                    try (PreparedStatement syncPs = mainConn.prepareStatement(
                                            "UPDATE rebate_config SET agent_level = ? WHERE agent_user_id = ?")) {
                                        syncPs.setInt(1, agentLevel);
                                        syncPs.setLong(2, gameUserId);
                                        syncPs.executeUpdate();
                                    }
                                }
                            }
                        }
                    } catch (Exception syncEx) {
                        logger.warn("UpdateUserAgentProcessor: DB sync failed for id=" + id, syncEx);
                    }
                    
                    // CASCADE level promotion downstream for any sub-agents.
                    // This touches children to fire BEFORE UPDATE trigger (fixing ancestors+level)
                    // and then syncs their users.dai_ly and rebate_config.
                    if (levelStr != null && !levelStr.isEmpty()) {
                        int parsedLevel = Integer.parseInt(levelStr);
                        if (parsedLevel < currentLevel) {
                            cascadeAgentLevelUpdate(conn, id);
                        }
                    }
                }

            } else {
                result.addProperty("errorCode", "1003"); // agent not found
            }

        } catch (NumberFormatException e) {
            logger.error("UpdateUserAgentProcessor: invalid number param", e);
            result.addProperty("errorCode", "1002");
        } catch (Exception e) {
            logger.error("UpdateUserAgentProcessor error", e);
        } finally {
            try { if (ps != null) ps.close(); } catch (Exception ignored) {}
            ConnectionPool.releaseConnection(conn);
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

    private void cascadeAgentLevelUpdate(Connection adminConn, int parentAgentId) {
        try {
            List<Integer> childIds = new ArrayList<>();
            try (PreparedStatement ps = adminConn.prepareStatement("SELECT id FROM useragent WHERE parentid = ?")) {
                ps.setInt(1, parentAgentId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) childIds.add(rs.getInt("id"));
                }
            }
            for (int childId : childIds) {
                // Touch useragent to fire BEFORE UPDATE trigger (recalculates Level and Ancestors relative to parent)
                try (PreparedStatement uPs = adminConn.prepareStatement("UPDATE useragent SET updatetime=NOW() WHERE id = ?")) {
                    uPs.setInt(1, childId);
                    uPs.executeUpdate();
                }
                
                int newLevel = -1;
                String childNick = "";
                try (PreparedStatement ps = adminConn.prepareStatement("SELECT level, nickname FROM useragent WHERE id = ?")) {
                    ps.setInt(1, childId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            newLevel = rs.getInt("level");
                            childNick = rs.getString("nickname");
                        }
                    }
                }
                
                if (newLevel > 0 && childNick != null && !childNick.isEmpty()) {
                    try (Connection userConn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                        try (PreparedStatement psUser = userConn.prepareStatement("UPDATE users SET dai_ly = ? WHERE nick_name = ?")) {
                            psUser.setInt(1, newLevel);
                            psUser.setString(2, childNick);
                            psUser.executeUpdate();
                        }
                        try (PreparedStatement psRebate = userConn.prepareStatement("UPDATE rebate_config SET agent_level = ? WHERE agent_nickname = ?")) {
                            psRebate.setInt(1, newLevel);
                            psRebate.setString(2, childNick);
                            psRebate.executeUpdate();
                        }
                    }
                }
                
                // Recurse for deeper sub-agents (safe because max depth is 3)
                cascadeAgentLevelUpdate(adminConn, childId);
            }
        } catch (Exception e) {
            logger.error("Error cascading agent level update", e);
        }
    }
}
