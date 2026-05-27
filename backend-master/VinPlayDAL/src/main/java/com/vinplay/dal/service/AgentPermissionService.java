package com.vinplay.dal.service;

import com.vinplay.vbee.common.pools.ConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

/**
 * Single source of truth for agent permission checks.
 * Used by c=9839 (ListAllAgentsUnderAgent), c=9543 (DetailMemberOfAgency),
 * and c=9840 (PromotePlayerToAgent).
 */
public class AgentPermissionService {
    private static final Logger logger = LoggerFactory.getLogger("backend");

    /**
     * Determine if caller agent can promote the target player to agent.
     *
     * Rules:
     *   (a) target must be a pure player (no useragent row, dai_ly=0)
     *   (b) caller level < 3 (leaf agents cannot promote)
     *   (c) caller is the direct inviter (referral_code match OR parent_agent_id match)
     */
    public static boolean canPromote(int callerAgentId, int callerLevel, String targetNickname) {
        if (callerLevel >= 3) return false;

        try {
            // Single admin connection for both agent-existence check and code loading
            Set<String> callerCodes;
            try (Connection adminConn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
                // Check target is not already an agent
                try (PreparedStatement ps = adminConn.prepareStatement(
                        "SELECT id FROM useragent WHERE nickname = ? AND active = 1")) {
                    ps.setString(1, targetNickname);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) return false;
                    }
                }
                callerCodes = loadCallerCodesWithConn(adminConn, callerAgentId);
            }

            return ownsPlayerByCodesAndNickname(callerCodes, callerAgentId, targetNickname);
        } catch (Exception e) {
            logger.warn("canPromote check failed for agent=" + callerAgentId + " target=" + targetNickname, e);
            return false;
        }
    }

    /**
     * Check if callerAgentId is the direct inviter of the player identified by nickname.
     * Matches via referral_code (current + legacy codes) OR parent_agent_id.
     */
    public static boolean ownsPlayer(int callerAgentId, String playerNickname) {
        try {
            Set<String> callerCodes = loadCallerCodes(callerAgentId);
            return ownsPlayerByCodesAndNickname(callerCodes, callerAgentId, playerNickname);
        } catch (Exception e) {
            logger.warn("ownsPlayer check failed for agent=" + callerAgentId + " player=" + playerNickname, e);
            return false;
        }
    }

    private static boolean ownsPlayerByCodesAndNickname(Set<String> callerCodes, int callerAgentId, String playerNickname) throws Exception {
        String playerRefCode = null;
        int playerParentAgentId = 0;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT referral_code, parent_agent_id FROM users WHERE nick_name = ?")) {
            ps.setString(1, playerNickname);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    playerRefCode = rs.getString("referral_code");
                    playerParentAgentId = rs.getInt("parent_agent_id");
                }
            }
        }
        return ownsPlayerByData(callerCodes, callerAgentId, playerRefCode, playerParentAgentId);
    }

    /**
     * Load caller's current code + all legacy codes from agent_code_history.
     * Public so c=9839 can call once and reuse for batch checks.
     */
    public static Set<String> loadCallerCodes(int callerAgentId) {
        if (callerAgentId <= 0) return new HashSet<>();
        try (Connection adminConn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
            return loadCallerCodesWithConn(adminConn, callerAgentId);
        } catch (Exception e) {
            logger.warn("loadCallerCodes failed for agentId=" + callerAgentId, e);
            return new HashSet<>();
        }
    }

    static Set<String> loadCallerCodesWithConn(Connection adminConn, int callerAgentId) {
        Set<String> out = new HashSet<>();
        if (callerAgentId <= 0) return out;
        try (PreparedStatement ps = adminConn.prepareStatement(
                "SELECT code FROM useragent WHERE id = ?")) {
            ps.setInt(1, callerAgentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String c = rs.getString("code");
                    if (c != null && !c.trim().isEmpty()) out.add(c.trim().toUpperCase());
                }
            }
        } catch (Exception e) {
            // fall through
        }
        try (PreparedStatement ps = adminConn.prepareStatement(
                "SELECT old_code FROM agent_code_history WHERE agent_id = ?")) {
            ps.setInt(1, callerAgentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String c = rs.getString("old_code");
                    if (c != null && !c.trim().isEmpty()) out.add(c.trim().toUpperCase());
                }
            }
        } catch (Exception e) {
            // agent_code_history may not exist — not fatal
        }
        return out;
    }

    /**
     * Batch-friendly ownership check using pre-loaded caller codes.
     * Used by c=9839 which iterates many rows with the same caller.
     */
    public static boolean ownsPlayerByData(Set<String> callerCodes, int callerAgentId,
                                           String playerRefCode, int playerParentAgentId) {
        boolean ownsByCode = playerRefCode != null && !playerRefCode.trim().isEmpty()
                && callerCodes.contains(playerRefCode.trim().toUpperCase());
        boolean ownsByParent = playerParentAgentId > 0 && playerParentAgentId == callerAgentId;
        return ownsByCode || ownsByParent;
    }
}
