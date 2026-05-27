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
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * c=9840 — Promote a player to agent (one level below caller).
 *
 * Rules:
 *   TĐL (level=1) → promotes player to ĐL1 (level=2)
 *   ĐL1 (level=2) → promotes player to ĐL2 (level=3)
 *   ĐL2 (level=3) → CANNOT promote (leaf)
 *
 * Ownership rule (SUN-??? agent-promote-ownership): only the *direct*
 * inviting agent can promote a player, not any ancestor. A caller is the
 * inviter iff one of:
 *   - users.referral_code matches the caller's current code OR any legacy
 *     code in vinplay_admin.agent_code_history.old_code for that caller
 *   - users.parent_agent_id equals the caller's useragent.id (fallback
 *     for players that signed up without a referral code and were linked
 *     to an agent later)
 *
 * Being anywhere else in the caller's subtree (e.g. master TĐL above the
 * actual inviter) is NOT sufficient — that was the pre-SUN-??? behavior
 * and allowed the master to steal sub-agents' promotions.
 *
 * Creates a useragent row (trigger auto-computes level+ancestors from parentid).
 * Syncs users.dai_ly to match the new agent level.
 * Auto-generates a 6-digit referral code if none provided.
 *
 * Params: rc (caller nickname), user_id (player to promote),
 *         cr (commission_rate, optional, default 0),
 *         code (referral code, optional, auto-gen if empty).
 */
public class PromotePlayerToAgentProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();
            String callerNick = request.getParameter("rc");
            String userIdStr = request.getParameter("user_id");
            String commRateStr = request.getParameter("cr");
            String desiredCode = request.getParameter("code");

            if (callerNick == null || callerNick.isEmpty()) return err(response, "1001", "rc required");
            if (userIdStr == null || userIdStr.isEmpty()) return err(response, "4001", "user_id required");

            long userId;
            try { userId = Long.parseLong(userIdStr); }
            catch (NumberFormatException e) { return err(response, "4002", "user_id not numeric"); }

            double commRate = 0;
            if (commRateStr != null && !commRateStr.isEmpty()) {
                try { commRate = Double.parseDouble(commRateStr); }
                catch (NumberFormatException e) { return err(response, "4003", "cr not numeric"); }
            }

            try (Connection adminConn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
                // 1. Resolve caller's agent info
                int callerId = -1, callerLevel = -1;
                double callerCommRate = 0;
                try (PreparedStatement ps = adminConn.prepareStatement(
                        "SELECT id, level, commission_rate FROM useragent WHERE nickname=?")) {
                    ps.setString(1, callerNick);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            callerId = rs.getInt("id");
                            callerLevel = rs.getInt("level");
                            callerCommRate = rs.getDouble("commission_rate");
                        }
                    }
                }
                if (callerId <= 0) return err(response, "1002", "caller agent not found");

                // 2. Level check: only TĐL (1) and ĐL1 (2) can promote
                if (callerLevel >= 3) {
                    return err(response, "4004", "ĐL2 cannot promote — leaf agent");
                }
                int targetLevel = callerLevel + 1;

                // 3. Commission rate validation
                if (commRate > callerCommRate) {
                    return err(response, "4005",
                            "commission rate " + commRate + " exceeds caller's rate " + callerCommRate);
                }

                // 4. Lookup the player
                String playerNick = null, playerUsername = null;
                int playerDaiLy = -1;
                try (Connection userConn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                    try (PreparedStatement ps = userConn.prepareStatement(
                            "SELECT user_name, nick_name, dai_ly, parent_agent_id, referral_code FROM users WHERE id=?")) {
                        ps.setLong(1, userId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) return err(response, "1003", "player not found");
                            playerUsername = rs.getString("user_name");
                            playerNick = rs.getString("nick_name");
                            playerDaiLy = rs.getInt("dai_ly");
                        }
                    }
                }
                if (playerDaiLy > 0) {
                    return err(response, "4006", "user is already an agent (dai_ly=" + playerDaiLy + ")");
                }
                if (playerNick == null || playerNick.isEmpty()) {
                    return err(response, "4007", "user has no nickname — cannot promote");
                }

                // 5. Ownership check (SUN-??? agent-promote-ownership).
                //    Only the *direct inviter* can promote — match via referral_code
                //    (+ legacy codes) OR fallback parent_agent_id == callerId.
                Set<String> callerCodes = com.vinplay.dal.service.AgentPermissionService.loadCallerCodes(callerId);
                String playerRefCode = null;
                int    playerParentAgentId = 0;
                try (Connection userConn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                     PreparedStatement ps = userConn.prepareStatement(
                             "SELECT referral_code, parent_agent_id FROM users WHERE id=?")) {
                    ps.setLong(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            playerRefCode = rs.getString("referral_code");
                            playerParentAgentId = rs.getInt("parent_agent_id");
                        }
                    }
                }
                boolean ownsByCode = playerRefCode != null && !playerRefCode.trim().isEmpty()
                        && callerCodes.contains(playerRefCode.trim().toUpperCase());
                boolean ownsByParent = playerParentAgentId > 0 && playerParentAgentId == callerId;
                if (!ownsByCode && !ownsByParent) {
                    return err(response, "4011",
                            "You are not the direct inviter for this player — only the agent whose referral code was used at signup can promote them");
                }

                // 6. Check username not already in useragent
                try (PreparedStatement ps = adminConn.prepareStatement(
                        "SELECT id FROM useragent WHERE username=? OR nickname=?")) {
                    ps.setString(1, playerUsername);
                    ps.setString(2, playerNick);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) return err(response, "4009", "user already has an agent record (id=" + rs.getInt(1) + ")");
                    }
                }

                // 7. Generate referral code if not provided
                String code = desiredCode;
                if (code == null || code.trim().isEmpty()) {
                    code = generateCode(adminConn);
                } else {
                    code = code.trim().toUpperCase();
                    // Check uniqueness
                    try (PreparedStatement ps = adminConn.prepareStatement(
                            "SELECT id FROM useragent WHERE UPPER(code)=?")) {
                        ps.setString(1, code);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) return err(response, "4010", "code already in use");
                        }
                    }
                }

                // 8. Get player's game password to sync to useragent
                String playerPassword = "";
                try (Connection userConn2 = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                     PreparedStatement pwPs = userConn2.prepareStatement("SELECT password FROM users WHERE id = ?")) {
                    pwPs.setLong(1, userId);
                    try (ResultSet pwRs = pwPs.executeQuery()) {
                        if (pwRs.next()) playerPassword = pwRs.getString("password") != null ? pwRs.getString("password") : "";
                    }
                }

                // 9. INSERT into useragent (trigger computes level + ancestors from parentid)
                Timestamp now = new Timestamp(System.currentTimeMillis());
                int newAgentId;
                try (PreparedStatement ps = adminConn.prepareStatement(
                        "INSERT INTO useragent (username, nickname, password, nameagent, code, " +
                        "status, active, `show`, createtime, updatetime, commission_rate, `order`, " +
                        "login_times, parentid, site) " +
                        "VALUES (?, ?, ?, ?, ?, '1', 1, 1, ?, ?, ?, 0, 0, ?, 'XX')",
                        PreparedStatement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, playerUsername);    // username
                    ps.setString(2, playerNick);       // nickname
                    ps.setString(3, playerPassword);   // password (synced from game)
                    ps.setString(4, playerNick);       // nameagent
                    ps.setString(5, code);             // code
                    ps.setTimestamp(6, now);            // createtime
                    ps.setTimestamp(7, now);            // updatetime
                    ps.setDouble(8, commRate);         // commission_rate
                    ps.setInt(9, callerId);            // parentid → trigger sets level+ancestors
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        newAgentId = keys.getInt(1);
                    }
                }

                // 9. Sync users.dai_ly + parent_agent_id
                try (Connection userConn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                    try (PreparedStatement ps = userConn.prepareStatement(
                            "UPDATE users SET dai_ly=?, parent_agent_id=? WHERE id=?")) {
                        ps.setInt(1, targetLevel);
                        ps.setInt(2, callerId);
                        ps.setLong(3, userId);
                        ps.executeUpdate();
                    }
                }

                // 10. Insert rebate_config
                try (PreparedStatement ps = adminConn.prepareStatement(
                        "INSERT INTO rebate_config (agent_user_id, agent_nickname, agent_level, rebate_percentage) " +
                        "VALUES (?, ?, ?, ?)")) {
                    ps.setInt(1, newAgentId);
                    ps.setString(2, playerNick);
                    ps.setInt(3, targetLevel);
                    ps.setDouble(4, commRate);
                    ps.executeUpdate();
                } catch (Exception e) {
                    logger.warn("PromotePlayerToAgent: rebate_config insert failed", e);
                }

                // 11. SUN-716: Seed default per-game commission rates
                try (Connection mainConn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                    com.vinplay.api.backend.processors.agent.AdminMakeAgentProcessor
                            .seedDefaultGameCommissionRates(mainConn, playerNick, targetLevel);
                } catch (Exception e) {
                    logger.warn("PromotePlayerToAgent: seedDefaultGameCommissionRates failed", e);
                }

                JSONObject data = new JSONObject();
                data.put("agent_id", newAgentId);
                data.put("user_id", userId);
                data.put("nickname", playerNick);
                data.put("level", targetLevel);
                data.put("code", code);
                data.put("commission_rate", commRate);
                data.put("parent_agent_id", callerId);
                data.put("promoted_by", callerNick);
                response.put("success", true);
                response.put("errorCode", "0");
                response.put("data", data);
            }
        } catch (Exception e) {
            logger.error("PromotePlayerToAgentProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
            response.put("message", "Internal: " + e.getMessage());
        }
        return response.toString();
    }

    /** Generate a unique 6-digit numeric code. */
    private String generateCode(Connection conn) throws Exception {
        Random rng = new Random();
        for (int attempt = 0; attempt < 100; attempt++) {
            String code = String.valueOf(100000 + rng.nextInt(900000));
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id FROM useragent WHERE code=?")) {
                ps.setString(1, code);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return code;
                }
            }
        }
        return String.valueOf(System.currentTimeMillis() % 1000000);
    }

    /**
     * Return the set of UPPER-CASE codes that identify the caller:
     *   - their current useragent.code
     *   - every agent_code_history.old_code row for this agent_id
     * Used by the promote-ownership check to tolerate agents who have
     * been renamed (code changed) without re-migrating old player signups.
     */
    /** @deprecated Use {@link com.vinplay.dal.service.AgentPermissionService#loadCallerCodes(int)} */
    static Set<String> loadCallerCodes(Connection adminConn, int callerId) {
        return com.vinplay.dal.service.AgentPermissionService.loadCallerCodes(callerId);
    }

    private static String err(JSONObject r, String code, String msg) {
        r.put("success", false); r.put("errorCode", code); r.put("message", msg);
        return r.toString();
    }
}
