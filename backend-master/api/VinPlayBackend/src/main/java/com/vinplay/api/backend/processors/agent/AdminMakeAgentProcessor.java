package com.vinplay.api.backend.processors.agent;

import com.vinplay.api.backend.services.CommissionRateValidator;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * c=9850 — Admin: make any player an agent at any level (unified endpoint).
 *
 * Combines create agent + promote + set level into one call.
 * If player is already an agent, updates their level/parent/rate.
 * If player has no game account, creates one.
 *
 * Params:
 *   nn   = player nickname (required)
 *   lv   = level: 1=TĐL, 2=ĐL1, 3=ĐL2 (required)
 *   ac   = referral code (required for new agents)
 *   cr   = commission_rate (optional, default from parent or 0)
 *   pid  = parent agent ID (required for lv=2,3; ignored for lv=1)
 *   dr   = deposit_rate (optional)
 */
public class AdminMakeAgentProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        response.put("success", false);

        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String nickname = request.getParameter("nn");
            String levelStr = request.getParameter("lv");
            String code = request.getParameter("ac");
            String crStr = request.getParameter("cr");
            String pidStr = request.getParameter("pid");
            String drStr = request.getParameter("dr");

            if (nickname == null || nickname.isEmpty()) {
                return err(response, "4001", "nn (nickname) required");
            }
            if (levelStr == null || levelStr.isEmpty()) {
                return err(response, "4001", "lv (level: 1=TĐL, 2=ĐL1, 3=ĐL2) required");
            }

            int level = Integer.parseInt(levelStr);
            if (level < 1 || level > 3) {
                return err(response, "4002", "level must be 1 (TĐL), 2 (ĐL1), or 3 (ĐL2)");
            }

            int parentId = -1;
            if (level > 1) {
                // ĐL1, ĐL2 bắt buộc phải có pid
                if (pidStr == null || pidStr.isEmpty()) {
                    return err(response, "4001", "pid (parent agent ID) required for level " + level);
                }
                parentId = Integer.parseInt(pidStr);
            } else if (pidStr != null && !pidStr.trim().isEmpty()) {
                // TĐL: cho phép truyền pid tùy chọn (nếu không có sẽ tự gán Special Account)
                try { parentId = Integer.parseInt(pidStr.trim()); } catch (NumberFormatException ignored) {}
            }
            // Nếu parentId vẫn là -1 và level=1 → sẽ resolve Special Account sau khi có DB connection

            double commissionRate = 0;
            if (crStr != null && !crStr.isEmpty()) {
                commissionRate = Double.parseDouble(crStr);
            }

            double depositRate = 0;
            if (drStr != null && !drStr.isEmpty()) {
                depositRate = Double.parseDouble(drStr);
            }

            try (Connection adminConn = ConnectionPool.getInstance().getConnection(ConnectionPool.ADMIN_POOL)) {
                // Resolve parentId cho TĐL (level=1): auto-parent về Special Account nếu chưa có
                if (parentId == -1 && level == 1) {
                    try (PreparedStatement ps = adminConn.prepareStatement(
                            "SELECT id FROM useragent WHERE code = '0' LIMIT 1")) {
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) parentId = rs.getInt(1);
                        }
                    }
                }

                // Validate commission rate (sau khi parentId đã được resolve)
                String rateError = CommissionRateValidator.validate(adminConn, 0, commissionRate, parentId);
                if (rateError != null) {
                    return err(response, "4005", rateError);
                }

                // Build ancestors string + lấy code của parent để sync vào users.referral_code
                String ancestors = "";
                String parentCode = "0"; // Default = SpecialAccount code
                String parentNickname = "";
                if (parentId > 0) {
                    try (PreparedStatement ps = adminConn.prepareStatement(
                            "SELECT ancestors, code, nickname FROM useragent WHERE id = ?")) {
                        ps.setInt(1, parentId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                String parentAncestors = rs.getString("ancestors");
                                parentCode = rs.getString("code");
                                parentNickname = rs.getString("nickname");
                                if (parentAncestors != null && !parentAncestors.isEmpty()) {
                                    ancestors = parentAncestors + "," + parentId;
                                } else {
                                    ancestors = String.valueOf(parentId);
                                }
                            } else {
                                return err(response, "4003", "Parent agent not found: " + parentId);
                            }
                        }
                    }
                }

                // Find user's game account
                String username = null;
                String realNickname = null;
                long userId = 0;
                try (Connection userConn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                    try (PreparedStatement ps = userConn.prepareStatement(
                            "SELECT id, user_name, nick_name FROM users WHERE nick_name = ? OR user_name = ?")) {
                        ps.setString(1, nickname);
                        ps.setString(2, nickname);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                userId = rs.getLong("id");
                                username = rs.getString("user_name");
                                realNickname = rs.getString("nick_name");
                            }
                        }
                    }
                }

                if (username == null) {
                    return err(response, "1002", "Player not found: " + nickname);
                }
                if (realNickname == null || realNickname.trim().isEmpty()) {
                    realNickname = username;
                }

                // Check if already an agent
                int existingAgentId = 0;
                try (PreparedStatement ps = adminConn.prepareStatement(
                        "SELECT id FROM useragent WHERE username = ? OR nickname = ?")) {
                    ps.setString(1, username);
                    ps.setString(2, nickname);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) existingAgentId = rs.getInt("id");
                    }
                }

                Timestamp now = new Timestamp(System.currentTimeMillis());
                int agentId;
                int currentLevel = -1;

                if (existingAgentId > 0) {
                    // Update existing agent
                    
                    // -------------------------------------------------------------
                    // Hierarchy & Level Protection Logic
                    // -------------------------------------------------------------
                    try (PreparedStatement lvlPs = adminConn.prepareStatement("SELECT level FROM useragent WHERE id = ?")) {
                        lvlPs.setInt(1, existingAgentId);
                        try (ResultSet lvlRs = lvlPs.executeQuery()) {
                            if (lvlRs.next()) currentLevel = lvlRs.getInt("level");
                        }
                    }
                    
                    if (currentLevel > 0 && level != currentLevel) {
                        // 1. Block Demotion (hạ cấp) 
                        if (level > currentLevel) {
                            return err(response, "4011", "Demotion (Hạ cấp) is not allowed. Agent is currently level " + currentLevel);
                        }
                        // PM confirmed: Promotion allowed. Cascade down hierarchy happens later.
                    }

                    StringBuilder sql = new StringBuilder("UPDATE useragent SET level=?, nickname=?, parentid=?, ancestors=?, commission_rate=?, percent_bonus_vincard=?, updatetime=?");
                    if (code != null && !code.isEmpty()) sql.append(", code=?");
                    if (depositRate > 0) sql.append(", deposit_rate=?");
                    sql.append(" WHERE id=?");

                    PreparedStatement ps = adminConn.prepareStatement(sql.toString());
                    int idx = 1;
                    ps.setInt(idx++, level);
                    ps.setString(idx++, realNickname);
                    ps.setInt(idx++, parentId);
                    ps.setString(idx++, ancestors);
                    ps.setDouble(idx++, commissionRate);
                    ps.setDouble(idx++, commissionRate);
                    ps.setTimestamp(idx++, now);
                    if (code != null && !code.isEmpty()) ps.setString(idx++, code);
                    if (depositRate > 0) ps.setDouble(idx++, depositRate);
                    ps.setInt(idx, existingAgentId);
                    ps.executeUpdate();
                    ps.close();
                    agentId = existingAgentId;
                    logger.info("AdminMakeAgent: updated agent " + nickname + " id=" + agentId + " level=" + level);
                } else {
                    // Create new agent
                    if (level != 1) {
                        return err(response, "4012", "Admin can only create TĐL (level=1) directly from game users. To create sub-agents (ĐL1, ĐL2), their parent agent must promote them.");
                    }
                    if (code == null || code.isEmpty()) {
                        return err(response, "4001", "ac (referral code) required for new agent");
                    }
                    code = code.trim().toUpperCase();
                    if (code.length() > 10) code = code.substring(0, 10);

                    // Get player's game password to sync to useragent
                    String playerPassword = "";
                    try (Connection userConn2 = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                         PreparedStatement pwPs = userConn2.prepareStatement("SELECT password FROM users WHERE user_name = ?")) {
                        pwPs.setString(1, username);
                        try (ResultSet pwRs = pwPs.executeQuery()) {
                            if (pwRs.next()) playerPassword = pwRs.getString("password") != null ? pwRs.getString("password") : "";
                        }
                    }

                    String sql = "INSERT INTO useragent (username, nickname, password, level, code, commission_rate, percent_bonus_vincard, " +
                            "deposit_rate, parentid, ancestors, active, status, `show`, createtime, updatetime) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 1, 1, ?, ?)";
                    PreparedStatement ps = adminConn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
                    ps.setString(1, username);
                    ps.setString(2, realNickname);
                    ps.setString(3, playerPassword);  // sync game password
                    ps.setInt(4, level);
                    ps.setString(5, code);
                    ps.setDouble(6, commissionRate);
                    ps.setDouble(7, commissionRate);
                    ps.setDouble(8, depositRate);
                    ps.setInt(9, parentId);
                    ps.setString(10, ancestors);
                    ps.setTimestamp(11, now);
                    ps.setTimestamp(12, now);
                    ps.executeUpdate();

                    ResultSet keys = ps.getGeneratedKeys();
                    agentId = keys.next() ? keys.getInt(1) : 0;
                    keys.close();
                    ps.close();
                    logger.info("AdminMakeAgent: created agent " + realNickname + " id=" + agentId + " level=" + level);
                }

                // Update user record: dai_ly, parent_agent_id, referral_code, parrentUser
                try (Connection userConn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
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
                        ps.setString(usrIdx, username);
                        ps.executeUpdate();
                    }
                }

                // Sync rebate_config — use agentId (useragent.id) to satisfy FK fk_rc_agent
                try (Connection mainConn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                    try (PreparedStatement ps = mainConn.prepareStatement(
                            "INSERT INTO rebate_config (agent_user_id, agent_nickname, agent_level, rebate_percentage) " +
                            "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE rebate_percentage=VALUES(rebate_percentage), agent_level=VALUES(agent_level)")) {
                        ps.setInt(1, agentId);   // useragent.id — FK references useragent(id)
                        ps.setString(2, realNickname);
                        ps.setInt(3, level);
                        ps.setDouble(4, commissionRate);
                        ps.executeUpdate();
                    }
                    // SUN-716: Seed default per-game commission rates for new agents
                    seedDefaultGameCommissionRates(mainConn, realNickname, level);
                }

                // If this was an upgrade of an existing agent, cascade level changes down the tree
                if (existingAgentId > 0 && currentLevel > 0 && level < currentLevel) {
                    cascadeAgentLevelUpdate(adminConn, existingAgentId);
                }

                JSONObject data = new JSONObject();
                data.put("agent_id", agentId);
                data.put("nickname", realNickname);
                data.put("level", level);
                data.put("level_name", level == 1 ? "TĐL" : level == 2 ? "ĐL1" : "ĐL2");
                data.put("code", code != null ? code : "");
                data.put("commission_rate", commissionRate);
                data.put("parent_id", parentId);
                data.put("ancestors", ancestors);
                data.put("action", existingAgentId > 0 ? "updated" : "created");

                response.put("success", true);
                response.put("errorCode", "0");
                response.put("data", data);
            }
        } catch (NumberFormatException e) {
            return err(response, "4002", "Invalid number format: " + e.getMessage());
        } catch (Exception e) {
            logger.error("AdminMakeAgentProcessor error", e);
            response.put("errorCode", "9999");
            response.put("message", "Internal: " + e.getMessage());
        }
        return response.toString();
    }

    /**
     * SUN-716 originally seeded a hardcoded list of 22 game_keys at rate=0
     * for every new agent. SUN-1255 Phase 1 replaces that with
     * policy-driven propagation: copy every game_key the immediate
     * parent has, with rate = max(floor, parent_rate - step). Step+floor
     * come from {@code commission_rate_policy} for the master TĐL.
     *
     * <p>Keeping the same signature so the three existing call sites
     * (this class, AddNewUserAgentProcessor, AddNewUserAgentChildProcessor)
     * don't change. {@code level} is no longer consulted — depth is
     * implicit via the parent chain.
     */
    public static void seedDefaultGameCommissionRates(java.sql.Connection conn, String agentNickname, int level) {
        try {
            int agentId;
            try (java.sql.PreparedStatement ps = conn.prepareStatement(
                    "SELECT id FROM vinplay_admin.useragent WHERE nickname = ? LIMIT 1")) {
                ps.setString(1, agentNickname);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        Logger.getLogger("backend").warn(
                                "seedDefaultGameCommissionRates: no useragent for " + agentNickname);
                        return;
                    }
                    agentId = rs.getInt(1);
                }
            }
            int rows = com.vinplay.dal.service.CommissionPropagator.seedRatesForNewAgent(conn, agentId);
            Logger.getLogger("backend").info(
                    "seedDefaultGameCommissionRates: propagator seeded " + rows
                  + " rates for " + agentNickname + " (id=" + agentId + ")");
        } catch (Exception e) {
            Logger.getLogger("backend").warn(
                    "seedDefaultGameCommissionRates failed for " + agentNickname, e);
        }
    }

    private static String err(JSONObject r, String code, String msg) {
        r.put("success", false);
        r.put("errorCode", code);
        r.put("message", msg);
        return r.toString();
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
                
                cascadeAgentLevelUpdate(adminConn, childId);
            }
        } catch (Exception e) {
            logger.error("Error cascading agent level update", e);
        }
    }
}
