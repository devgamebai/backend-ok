package com.vinplay.api.backend.processors.agentcode;

import com.vinplay.vbee.common.agent.AgentCodeValidator;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * c=9830 — Agent requests a vanity referral code.
 * Params: rc (agent nickname, session), desired_code.
 * Inserts a PENDING row in agent_code_request for admin review.
 */
public class RequestAgentCodeProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();
            String agentNick = request.getParameter("rc");
            String desired = request.getParameter("desired_code");

            if (agentNick == null || agentNick.isEmpty()) return err(response, "1001", "rc required");

            AgentCodeValidator.Result v = AgentCodeValidator.validate(desired);
            if (!v.ok) return err(response, v.errorCode, v.message);
            String normalized = v.normalized;

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
                // Resolve agent id
                int agentId = -1;
                try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM useragent WHERE nickname=?")) {
                    ps.setString(1, agentNick);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) agentId = rs.getInt(1);
                    }
                }
                if (agentId <= 0) return err(response, "1002", "agent not found");

                // Reserved check
                List<String> reserved = new ArrayList<>();
                try (PreparedStatement ps = conn.prepareStatement("SELECT code_pattern FROM agent_code_reserved");
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) reserved.add(rs.getString(1));
                }
                if (AgentCodeValidator.matchesReserved(normalized, reserved)) {
                    return err(response, "4004", "code is reserved");
                }

                // Uniqueness in useragent.code (case-insensitive)
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT id, nickname FROM useragent WHERE UPPER(code)=? AND nickname<>?")) {
                    ps.setString(1, normalized);
                    ps.setString(2, agentNick);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) return err(response, "4005", "code already in use by another agent");
                    }
                }

                // Check pending requests from OTHER agents for same code
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT id FROM agent_code_request WHERE normalized_code=? AND status='PENDING' AND agent_nickname<>?")) {
                    ps.setString(1, normalized);
                    ps.setString(2, agentNick);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) return err(response, "4006", "another agent has a pending request for this code");
                    }
                }

                // Check caller's own pending request
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT id, normalized_code FROM agent_code_request WHERE agent_nickname=? AND status='PENDING' LIMIT 1")) {
                    ps.setString(1, agentNick);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) return err(response, "4007",
                                "you already have a pending request (id=" + rs.getInt(1) + " code=" + rs.getString(2) + ") — cancel it first");
                    }
                }

                // Rate limit: max 3 APPROVED changes per month
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT COUNT(*) FROM agent_code_request WHERE agent_nickname=? AND status='APPROVED' " +
                        "AND reviewed_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)")) {
                    ps.setString(1, agentNick);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && rs.getInt(1) >= 3) return err(response, "4008", "max 3 code changes per month reached");
                    }
                }

                // 7-day cooloff after rejection of same code
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT id FROM agent_code_request WHERE agent_nickname=? AND normalized_code=? AND status='REJECTED' " +
                        "AND reviewed_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) LIMIT 1")) {
                    ps.setString(1, agentNick);
                    ps.setString(2, normalized);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) return err(response, "4009", "this code was rejected recently — try after 7 days");
                    }
                }

                // Auto-suggest alternatives if code is taken (for response enrichment)
                JSONArray suggestions = new JSONArray();
                boolean codeTaken = false;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT id FROM useragent WHERE UPPER(code)=? AND nickname<>?")) {
                    ps.setString(1, normalized);
                    ps.setString(2, agentNick);
                    try (ResultSet rs = ps.executeQuery()) { codeTaken = rs.next(); }
                }
                if (!codeTaken) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT id FROM agent_code_request WHERE normalized_code=? AND status='PENDING' AND agent_nickname<>?")) {
                        ps.setString(1, normalized);
                        ps.setString(2, agentNick);
                        try (ResultSet rs = ps.executeQuery()) { codeTaken = rs.next(); }
                    }
                }
                if (codeTaken) {
                    // Generate up to 5 alternatives
                    String base = normalized.replaceAll("\\d+$", ""); // strip trailing digits
                    if (base.length() < 3) base = normalized.substring(0, Math.min(normalized.length(), 6));
                    for (int s = 1; s <= 999 && suggestions.length() < 5; s++) {
                        String alt = base + s;
                        if (alt.length() > AgentCodeValidator.MAX_LEN) break;
                        boolean altFree = true;
                        try (PreparedStatement ps = conn.prepareStatement(
                                "SELECT 1 FROM useragent WHERE UPPER(code)=? UNION SELECT 1 FROM agent_code_request WHERE normalized_code=? AND status='PENDING'")) {
                            ps.setString(1, alt); ps.setString(2, alt);
                            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) altFree = false; }
                        }
                        if (altFree && !AgentCodeValidator.matchesReserved(alt, reserved)) {
                            suggestions.put(alt);
                        }
                    }
                    response.put("suggestions", suggestions);
                    return err(response, codeTaken ? "4005" : "4006",
                            "code already in use" + (suggestions.length() > 0 ? " — try: " + suggestions.join(", ") : ""));
                }

                // Insert request
                long requestId;
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO agent_code_request (agent_id, agent_nickname, desired_code, normalized_code, status) " +
                        "VALUES (?, ?, ?, ?, 'PENDING')", PreparedStatement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, agentId);
                    ps.setString(2, agentNick);
                    ps.setString(3, desired.trim());
                    ps.setString(4, normalized);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        requestId = keys.getLong(1);
                    }
                }

                JSONObject data = new JSONObject();
                data.put("request_id", requestId);
                data.put("desired_code", desired.trim());
                data.put("normalized_code", normalized);
                data.put("status", "PENDING");
                response.put("success", true);
                response.put("errorCode", "0");
                response.put("data", data);
            }
        } catch (Exception e) {
            logger.error("RequestAgentCodeProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
            response.put("message", "Internal: " + e.getMessage());
        }
        return response.toString();
    }

    private static String err(JSONObject r, String code, String msg) {
        r.put("success", false); r.put("errorCode", code); r.put("message", msg);
        return r.toString();
    }
}
