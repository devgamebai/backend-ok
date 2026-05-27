package com.vinplay.api.backend.processors.rebate;

import com.vinplay.api.backend.services.DifferentialCommissionCalculator;
import com.vinplay.api.backend.services.DifferentialCommissionCalculator.AgentRate;
import com.vinplay.api.backend.services.DifferentialCommissionCalculator.Distribution;
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
 * c=9758 — Calculate differential commission for a user's volume (spec §5.3).
 *
 * Admin-triggered. Given a user nickname + volume, walks the agent chain
 * via referral_code → useragent.code → ancestors, applies differential
 * commission formula, and creates PENDING rebate_logs per agent.
 *
 * Admin then approves via c=9753 TriggerRebatePayoutProcessor (existing).
 *
 * Params:
 *   nn      = user nickname (whose volume we're calculating commission for)
 *   volume  = bet volume (long, VIN)
 *   ps      = period_start (YYYY-MM-DD, optional, defaults to today)
 *   pe      = period_end (YYYY-MM-DD, optional, defaults to today)
 *   pt      = period_type (DAILY/WEEKLY/MONTHLY, default DAILY)
 *   dry     = 1 to preview without inserting logs (optional)
 *
 * Response: { success, distributions: [{agentId, ownPct, childPct, diffPct, amount, logId}], totalPaid, volume }
 */
public class CalcDifferentialCommissionProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        response.put("success", false);
        response.put("errorCode", "1001");

        try {
            HttpServletRequest request = param.get();

            String userNick = request.getParameter("nn");
            String volumeStr = request.getParameter("volume");
            String periodStart = request.getParameter("ps");
            String periodEnd = request.getParameter("pe");
            String periodType = request.getParameter("pt");
            String gameKey = request.getParameter("game"); // optional: per-game rate
            boolean dryRun = "1".equals(request.getParameter("dry"));

            if (userNick == null || userNick.isEmpty() || volumeStr == null || volumeStr.isEmpty()) {
                response.put("errorCode", "4001");
                response.put("message", "nn and volume required");
                return response.toString();
            }

            long volume = Long.parseLong(volumeStr);
            if (volume <= 0) {
                response.put("errorCode", "4002");
                response.put("message", "volume must be positive");
                return response.toString();
            }
            if (periodType == null || periodType.isEmpty()) periodType = "DAILY";
            if (periodStart == null || periodStart.isEmpty()) periodStart = todayDate();
            if (periodEnd == null || periodEnd.isEmpty()) periodEnd = todayDate();

            // 1. Get user's referral_code + check if user is also an agent
            String refCode = null;
            int selfAgentId = -1;
            double selfAgentRate = 0;
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                // Get referral code
                try (PreparedStatement ps = conn.prepareStatement("SELECT referral_code FROM vinplay.users WHERE nick_name = ?")) {
                    ps.setString(1, userNick);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) refCode = rs.getString("referral_code");
                    }
                }
                // Check if bettor is also an agent (self-rebate)
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT id, nickname, commission_rate FROM vinplay_admin.useragent WHERE nickname = ? OR username = ?")) {
                    ps.setString(1, userNick);
                    ps.setString(2, userNick);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            selfAgentId = rs.getInt("id");
                            selfAgentRate = rs.getDouble("commission_rate");
                            // Use per-game rate if game specified
                            if (gameKey != null && !gameKey.isEmpty()) {
                                String selfNick = rs.getString("nickname");
                                selfAgentRate = getPerGameRate(conn, selfNick, gameKey, selfAgentRate);
                            }
                        }
                    }
                }
            }
            if (refCode == null || refCode.isEmpty()) {
                // User has no referral agent — but if they ARE an agent, they still get self-rebate
                if (selfAgentId <= 0) {
                    response.put("errorCode", "1002");
                    response.put("message", "User has no referral agent");
                    return response.toString();
                }
            }

            // 2. Build agent chain (direct → top), using per-game rates if game specified
            List<AgentRate> chain = (refCode != null && !refCode.isEmpty())
                    ? buildAgentChain(refCode, gameKey) : new ArrayList<>();

            // 2b. Self-rebate: if bettor is an agent, insert at position 0
            // This means the agent earns their own rate on their own bets
            if (selfAgentId > 0) {
                // Only add if not already in chain (avoid double-counting)
                boolean alreadyInChain = false;
                for (AgentRate ar : chain) {
                    if (ar.agentId == selfAgentId) { alreadyInChain = true; break; }
                }
                if (!alreadyInChain) {
                    chain.add(0, new AgentRate(selfAgentId, selfAgentRate));
                }
            }

            if (chain.isEmpty()) {
                response.put("errorCode", "1003");
                response.put("message", "Agent chain empty — no commission applicable");
                return response.toString();
            }

            // 3. Calculate differential commission distribution
            List<Distribution> distributions = DifferentialCommissionCalculator.calculate(chain, volume);
            long totalPaid = DifferentialCommissionCalculator.totalAmount(distributions);

            // 4. Insert PENDING rebate_logs (unless dry run)
            // SUN-658: Flag self-rebate vs downline
            JSONArray distJson = new JSONArray();
            if (!dryRun) {
                for (Distribution d : distributions) {
                    String rebateType = (selfAgentId > 0 && d.agentId == selfAgentId) ? "SELF" : "DOWNLINE";
                    long logId = insertPendingLog(d, userNick, volume, periodStart, periodEnd, periodType, rebateType);
                    JSONObject e = new JSONObject();
                    e.put("agentId", d.agentId);
                    e.put("ownPct", d.ownPct);
                    e.put("childPct", d.childPct);
                    e.put("diffPct", d.differentialPct);
                    e.put("amount", d.amount);
                    e.put("logId", logId);
                    e.put("rebateType", rebateType);
                    distJson.put(e);
                }
            } else {
                for (Distribution d : distributions) {
                    String rebateType = (selfAgentId > 0 && d.agentId == selfAgentId) ? "SELF" : "DOWNLINE";
                    JSONObject e = new JSONObject();
                    e.put("agentId", d.agentId);
                    e.put("ownPct", d.ownPct);
                    e.put("childPct", d.childPct);
                    e.put("diffPct", d.differentialPct);
                    e.put("amount", d.amount);
                    e.put("rebateType", rebateType);
                    distJson.put(e);
                }
            }

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("userNickname", userNick);
            response.put("volume", volume);
            response.put("totalPaid", totalPaid);
            response.put("chainDepth", chain.size());
            response.put("distributions", distJson);
            response.put("dryRun", dryRun);
            response.put("periodStart", periodStart);
            response.put("periodEnd", periodEnd);
            response.put("periodType", periodType);

            logger.info("DifferentialCommission: user=" + userNick + " volume=" + volume
                    + " chainDepth=" + chain.size() + " totalPaid=" + totalPaid
                    + " dryRun=" + dryRun);

        } catch (Exception e) {
            logger.error("CalcDifferentialCommissionProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
            response.put("message", "Internal: " + e.getMessage());
        }
        return response.toString();
    }

    /**
     * Walk referral_code → direct agent → ancestors.
     * Returns chain ordered [direct, parent, ..., top].
     */
    /**
     * Look up per-game rate for an agent. Returns game rate if set, otherwise global rate.
     */
    private double getPerGameRate(Connection conn, String agentNick, String gameKey, double globalRate) {
        if (gameKey == null || gameKey.isEmpty()) return globalRate;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT rate FROM vinplay.game_commission_rate WHERE agent_nickname = ? AND game_key = ?")) {
            ps.setString(1, agentNick);
            ps.setString(2, gameKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("rate");
            }
        } catch (Exception e) {
            logger.warn("getPerGameRate error nick=" + agentNick + " game=" + gameKey, e);
        }
        return globalRate;
    }

    private List<AgentRate> buildAgentChain(String refCode, String gameKey) throws Exception {
        List<AgentRate> chain = new ArrayList<>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
            // Direct agent by code
            int directId = -1;
            String ancestorsStr = null;
            double directRate = 0;
            String directNick = null;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, nickname, ancestors, commission_rate FROM vinplay_admin.useragent WHERE code = ?")) {
                ps.setString(1, refCode);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        directId = rs.getInt("id");
                        directNick = rs.getString("nickname");
                        ancestorsStr = rs.getString("ancestors");
                        directRate = rs.getDouble("commission_rate");
                    }
                }
            }
            if (directId == -1) return chain;
            // Use per-game rate if specified
            if (gameKey != null && !gameKey.isEmpty() && directNick != null) {
                directRate = getPerGameRate(conn, directNick, gameKey, directRate);
            }
            chain.add(new AgentRate(directId, directRate));

            // Ancestors — ordered by level ASC so closest parent comes first, top comes last
            if (ancestorsStr != null && !ancestorsStr.trim().isEmpty()) {
                // Sanitize: ancestors is comma-separated int IDs (e.g. "1,2,5")
                if (!ancestorsStr.matches("^[0-9,]+$")) {
                    logger.warn("Invalid ancestors format: " + ancestorsStr);
                    return chain;
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT id, nickname, commission_rate FROM vinplay_admin.useragent WHERE id IN (" + ancestorsStr + ") ORDER BY level DESC")) {
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            double rate = rs.getDouble("commission_rate");
                            if (gameKey != null && !gameKey.isEmpty()) {
                                rate = getPerGameRate(conn, rs.getString("nickname"), gameKey, rate);
                            }
                            chain.add(new AgentRate(rs.getInt("id"), rate));
                        }
                    }
                }
            }
        }
        return chain;
    }

    private long insertPendingLog(Distribution d, String userNick, long volume,
                                  String periodStart, String periodEnd, String periodType, String rebateType) throws Exception {
        // Resolve agent nickname
        String agentNick = "";
        try (Connection nickConn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
            try (PreparedStatement nps = nickConn.prepareStatement("SELECT nickname FROM vinplay_admin.useragent WHERE id=?")) {
                nps.setInt(1, d.agentId);
                try (ResultSet nrs = nps.executeQuery()) {
                    if (nrs.next()) agentNick = nrs.getString("nickname");
                }
            }
        }
        String sql = "INSERT INTO vinplay.rebate_logs " +
                "(agent_user_id, agent_nickname, agent_level, period_start, period_end, period_type, " +
                "total_f1_volume, rebate_percentage, share_percentage, own_percentage, child_percentage, differential_pct, " +
                "rebate_amount, share_amount, net_rebate, status, rebate_type, note, created_at) " +
                "VALUES (?, ?, 0, ?, ?, ?, ?, ?, 0, ?, ?, ?, ?, 0, ?, 'PENDING', ?, ?, NOW())";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, d.agentId);
            ps.setString(2, agentNick);
            ps.setString(3, periodStart + " 00:00:00");
            ps.setString(4, periodEnd + " 23:59:59");
            ps.setString(5, periodType);
            ps.setLong(6, volume);
            ps.setDouble(7, d.ownPct);
            ps.setDouble(8, d.ownPct);
            ps.setDouble(9, d.childPct);
            ps.setDouble(10, d.differentialPct);
            ps.setLong(11, d.amount);
            ps.setLong(12, d.amount);
            ps.setString(13, rebateType);
            ps.setString(14, "Differential from user=" + userNick);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        }
        return -1;
    }

    private String todayDate() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
    }
}
