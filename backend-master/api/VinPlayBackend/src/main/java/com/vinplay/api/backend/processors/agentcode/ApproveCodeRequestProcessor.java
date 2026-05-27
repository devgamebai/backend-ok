package com.vinplay.api.backend.processors.agentcode;

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

/**
 * c=9833 — Admin approves an agent code request.
 * Atomic: locks the request row, re-checks uniqueness, updates useragent.code,
 * then marks request APPROVED. Audit row written.
 *
 * Params: aat, request_id.
 */
public class ApproveCodeRequestProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();
            String idStr = request.getParameter("request_id");
            if (idStr == null || idStr.isEmpty()) return err(response, "4001", "request_id required");
            long requestId;
            try { requestId = Long.parseLong(idStr); }
            catch (NumberFormatException e) { return err(response, "4002", "request_id not numeric"); }
            String actor = resolveAdmin(request);

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
                conn.setAutoCommit(false);
                int agentId; String normalizedCode, desiredCode, agentNick, currentStatus, oldCode = null;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT agent_id, agent_nickname, desired_code, normalized_code, status " +
                        "FROM agent_code_request WHERE id=? FOR UPDATE")) {
                    ps.setLong(1, requestId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) { conn.rollback(); return err(response, "1002", "request not found"); }
                        agentId = rs.getInt("agent_id");
                        agentNick = rs.getString("agent_nickname");
                        desiredCode = rs.getString("desired_code");
                        normalizedCode = rs.getString("normalized_code");
                        currentStatus = rs.getString("status");
                    }
                }
                if (!"PENDING".equals(currentStatus)) {
                    conn.rollback();
                    return err(response, "4004", "request not in PENDING state (current: " + currentStatus + ")");
                }

                // Re-check uniqueness now (race guard)
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT id, code FROM useragent WHERE UPPER(code)=? AND id<>?")) {
                    ps.setString(1, normalizedCode);
                    ps.setInt(2, agentId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) { conn.rollback(); return err(response, "4005", "code already claimed by another agent"); }
                    }
                }

                // Get agent's current code for audit
                try (PreparedStatement ps = conn.prepareStatement("SELECT code FROM useragent WHERE id=?")) {
                    ps.setInt(1, agentId);
                    try (ResultSet rs = ps.executeQuery()) { if (rs.next()) oldCode = rs.getString(1); }
                }

                // Update useragent.code (store the original case the user typed)
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE useragent SET code=?, updatetime=NOW() WHERE id=?")) {
                    ps.setString(1, desiredCode.trim().toUpperCase());
                    ps.setInt(2, agentId);
                    ps.executeUpdate();
                }

                // Log code change history (for subtree player lookups via old codes)
                if (oldCode != null && !oldCode.isEmpty()) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO agent_code_history (agent_id, old_code, new_code) VALUES (?, ?, ?)")) {
                        ps.setInt(1, agentId);
                        ps.setString(2, oldCode);
                        ps.setString(3, desiredCode.trim().toUpperCase());
                        ps.executeUpdate();
                    }
                }

                // Also backfill parent_agent_id for users who used old code
                if (oldCode != null && !oldCode.isEmpty()) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE vinplay.users SET parent_agent_id=? WHERE referral_code=? COLLATE utf8mb4_general_ci AND (parent_agent_id IS NULL OR parent_agent_id=0)")) {
                        ps.setInt(1, agentId);
                        ps.setString(2, oldCode);
                        ps.executeUpdate();
                    } catch (Exception e) {
                        logger.warn("ApproveCodeRequest: backfill parent_agent_id failed for old code " + oldCode, e);
                    }
                }

                // Update request
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE agent_code_request SET status='APPROVED', reviewed_by=?, reviewed_at=NOW() WHERE id=?")) {
                    ps.setString(1, actor);
                    ps.setLong(2, requestId);
                    ps.executeUpdate();
                }

                conn.commit();

                // Invalidate the agents-by-code cache (routing-flagged backend
                // — Hazelcast or Redis depending on cache.routing.properties).
                try {
                    com.vinplay.vbee.common.cache.DistCache<String, Object> m =
                            com.vinplay.vbee.common.cache.CacheFactory.get("cacheAgentCode", Object.class);
                    if (oldCode != null) m.remove(oldCode.toUpperCase());
                    m.put(normalizedCode, agentId);
                } catch (Exception e) {
                    logger.warn("ApproveCodeRequestProcessor cache update failed", e);
                }

                JSONObject data = new JSONObject();
                data.put("request_id", requestId);
                data.put("agent_id", agentId);
                data.put("agent_nickname", agentNick);
                data.put("old_code", oldCode != null ? oldCode : "");
                data.put("new_code", normalizedCode);
                data.put("approved_by", actor);
                response.put("success", true);
                response.put("errorCode", "0");
                response.put("data", data);
            }
        } catch (Exception e) {
            logger.error("ApproveCodeRequestProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
            response.put("message", "Internal: " + e.getMessage());
        }
        return response.toString();
    }

    static String resolveAdmin(HttpServletRequest request) {
        try {
            String aat = request.getParameter("aat");
            if (aat == null || aat.isEmpty()) return "system";
            HazelcastInstance hz = HazelcastClientFactory.getInstance();
            IMap<String, String> tokenMap = hz.getMap("cacheToken");
            String nick = tokenMap.get(aat);
            return nick != null ? nick : "system";
        } catch (Exception e) { return "system"; }
    }

    private static String err(JSONObject r, String code, String msg) {
        r.put("success", false); r.put("errorCode", code); r.put("message", msg);
        return r.toString();
    }
}
