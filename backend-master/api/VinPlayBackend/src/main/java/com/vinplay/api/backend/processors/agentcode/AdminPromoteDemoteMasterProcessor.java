package com.vinplay.api.backend.processors.agentcode;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * c=9841 — Admin promotes agent to Master (TĐL) or demotes Master to ĐL.
 *
 * action=promote: SET parentid=SpecialAccount(code='0') → trigger recomputes level=1, ancestors=SA_ID.
 *   Validates: agent exists, not already TĐL (level=1).
 *
 * action=demote: SET parentid=<target_master_id> → trigger recomputes level=2.
 *   Validates: agent is TĐL, target_master exists and IS TĐL,
 *   BLOCKS if agent has children (must reassign first).
 *
 * Both sync users.dai_ly.
 *
 * Params: aat, agent_id, action (promote|demote), target_master_id (for demote only).
 */
public class AdminPromoteDemoteMasterProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();
            String agentIdStr = request.getParameter("agent_id");
            String action = request.getParameter("action");
            String targetIdStr = request.getParameter("target_master_id");

            if (agentIdStr == null || agentIdStr.isEmpty()) return err(response, "4001", "agent_id required");
            if (action == null || action.isEmpty()) return err(response, "4002", "action required (promote|demote)");

            int agentId;
            try { agentId = Integer.parseInt(agentIdStr); }
            catch (NumberFormatException e) { return err(response, "4003", "agent_id not numeric"); }

            String actor = ApproveCodeRequestProcessor.resolveAdmin(request);

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
                // Get current agent info
                int currentLevel = -1;
                String nickname = null, username = null;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT level, nickname, username FROM useragent WHERE id=?")) {
                    ps.setInt(1, agentId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) return err(response, "1002", "agent not found");
                        currentLevel = rs.getInt("level");
                        nickname = rs.getString("nickname");
                        username = rs.getString("username");
                    }
                }

                if ("promote".equalsIgnoreCase(action)) {
                    if (currentLevel == 1) return err(response, "4004", "agent is already TĐL (level=1)");

                    // Resolve Special Account ID — TĐL mới phải trỏ về SA, không được orphan
                    int specialAccountId = -1;
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT id FROM useragent WHERE code = '0' LIMIT 1")) {
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) specialAccountId = rs.getInt(1);
                        }
                    }
                    if (specialAccountId <= 0) {
                        return err(response, "5001", "Special Account (code=0) not found — cannot promote");
                    }

                    // Set parentid = SpecialAccount → trigger tính level=1, ancestors='{SA_ID}'
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE useragent SET parentid=?, updatetime=NOW() WHERE id=?")) {
                        ps.setInt(1, specialAccountId);
                        ps.setInt(2, agentId);
                        ps.executeUpdate();
                    }

                    // Sync users.dai_ly, parent_agent_id, referral_code
                    syncUserRole(username, 1, specialAccountId, "0");

                    // Sync rebate_config level + seed game commission rates cho TĐL mới
                    try (Connection mainConn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                        // Update rebate_config: agent_level = 1 (TĐL)
                        try (PreparedStatement rps = mainConn.prepareStatement(
                                "UPDATE rebate_config SET agent_level = 1 WHERE agent_nickname = ?")) {
                            rps.setString(1, nickname);
                            rps.executeUpdate();
                        }
                        // SUN-716: Seed default per-game commission rates nếu chưa có
                        com.vinplay.api.backend.processors.agent.AdminMakeAgentProcessor
                                .seedDefaultGameCommissionRates(mainConn, nickname, 1);
                    } catch (Exception e) {
                        logger.warn("AdminPromoteDemoteMaster: rebate/commission sync failed for " + nickname, e);
                    }

                    // Verify trigger result
                    int newLevel = getLevel(conn, agentId);
                    JSONObject data = new JSONObject();
                    data.put("agent_id", agentId);
                    data.put("nickname", nickname);
                    data.put("old_level", currentLevel);
                    data.put("new_level", newLevel);
                    data.put("action", "promote_to_master");
                    data.put("actor", actor);
                    response.put("success", true);
                    response.put("errorCode", "0");
                    response.put("data", data);

                } else if ("demote".equalsIgnoreCase(action)) {
                    if (currentLevel != 1) return err(response, "4005", "agent is not TĐL (level=" + currentLevel + ")");
                    if (targetIdStr == null || targetIdStr.isEmpty())
                        return err(response, "4006", "target_master_id required for demote");

                    int targetId;
                    try { targetId = Integer.parseInt(targetIdStr); }
                    catch (NumberFormatException e) { return err(response, "4007", "target_master_id not numeric"); }

                    // Validate target is TĐL and get code
                    int targetLevel = -1;
                    String targetCode = null;
                    try (PreparedStatement ps = conn.prepareStatement("SELECT level, code FROM useragent WHERE id=?")) {
                        ps.setInt(1, targetId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                targetLevel = rs.getInt("level");
                                targetCode = rs.getString("code");
                            }
                        }
                    }
                    if (targetLevel != 1) return err(response, "4008", "target agent is not TĐL (level=" + targetLevel + ")");
                    if (targetId == agentId) return err(response, "4009", "cannot demote under self");

                    // Block if has children
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT COUNT(*) FROM useragent WHERE FIND_IN_SET(?, ancestors) > 0")) {
                        ps.setInt(1, agentId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next() && rs.getInt(1) > 0)
                                return err(response, "4010", "agent has " + rs.getInt(1) + " sub-agent(s) — reassign first");
                        }
                    }

                    // Set parentid=targetId → trigger makes level=2, ancestors=targetId
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE useragent SET parentid=?, updatetime=NOW() WHERE id=?")) {
                        ps.setInt(1, targetId);
                        ps.setInt(2, agentId);
                        ps.executeUpdate();
                    }

                    syncUserRole(username, 2, targetId, targetCode);

                    int newLevel = getLevel(conn, agentId);
                    JSONObject data = new JSONObject();
                    data.put("agent_id", agentId);
                    data.put("nickname", nickname);
                    data.put("old_level", currentLevel);
                    data.put("new_level", newLevel);
                    data.put("demoted_under", targetId);
                    data.put("action", "demote_from_master");
                    data.put("actor", actor);
                    response.put("success", true);
                    response.put("errorCode", "0");
                    response.put("data", data);

                } else {
                    return err(response, "4002", "action must be 'promote' or 'demote'");
                }
            }
        } catch (Exception e) {
            logger.error("AdminPromoteDemoteMasterProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
            response.put("message", "Internal: " + e.getMessage());
        }
        return response.toString();
    }

    private int getLevel(Connection conn, int agentId) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("SELECT level FROM useragent WHERE id=?")) {
            ps.setInt(1, agentId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : -1; }
        }
    }

    private void syncUserRole(String username, int level, int parentId, String referralCode) {
        if (username == null) return;
        try (Connection userConn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = userConn.prepareStatement("UPDATE users SET dai_ly=?, parent_agent_id=?, referral_code=? WHERE user_name=?")) {
            ps.setInt(1, level);
            ps.setInt(2, parentId);
            ps.setString(3, referralCode != null ? referralCode : "0");
            ps.setString(4, username);
            ps.executeUpdate();
        } catch (Exception e) {
            logger.warn("AdminPromoteDemoteMaster: user role sync failed for " + username, e);
        }
    }

    private static String err(JSONObject r, String code, String msg) {
        r.put("success", false); r.put("errorCode", code); r.put("message", msg);
        return r.toString();
    }
}
