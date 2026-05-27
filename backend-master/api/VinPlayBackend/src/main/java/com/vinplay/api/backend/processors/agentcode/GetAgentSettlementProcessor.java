package com.vinplay.api.backend.processors.agentcode;

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
 * c=9844 — Kết toán đối tác (Agent Settlement Summary).
 *
 * Periodic summary per agent in caller's subtree: total volume, commission
 * earned, payouts made, player count — for a date range.
 * "lịch sử kết toán đối tác thì nó là dạng summary"
 *
 * Uses log_report_user (daily aggregated bet/win per user per game)
 * grouped by agent, with commission calculated from agent's commission_rate.
 *
 * Params: rc (agent nickname), ft/et (date range), p/l (pagination).
 */
public class GetAgentSettlementProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();
            String agentNick = request.getParameter("rc");
            String fromDate = request.getParameter("ft");
            String toDate = request.getParameter("et");

            if (agentNick == null || agentNick.isEmpty()) return err(response, "1001", "rc required");

            try (Connection adminConn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
                 Connection userConn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {

                // 1. Get agent + subtree
                int agentId = -1;
                try (PreparedStatement ps = adminConn.prepareStatement("SELECT id FROM useragent WHERE nickname=?")) {
                    ps.setString(1, agentNick);
                    try (ResultSet rs = ps.executeQuery()) { if (rs.next()) agentId = rs.getInt(1); }
                }
                if (agentId <= 0) return err(response, "1002", "agent not found");

                // All agents in subtree (including self)
                List<int[]> agents = new ArrayList<>(); // [id, level, parentid]
                List<String> agentNicknames = new ArrayList<>();
                List<String> agentCodes = new ArrayList<>();
                double[] agentRates = new double[200]; // indexed by list position

                try (PreparedStatement ps = adminConn.prepareStatement(
                        "SELECT id, nickname, level, code, commission_rate FROM useragent WHERE id=? OR FIND_IN_SET(?, ancestors) > 0 ORDER BY level, id")) {
                    ps.setInt(1, agentId);
                    ps.setInt(2, agentId);
                    try (ResultSet rs = ps.executeQuery()) {
                        int idx = 0;
                        while (rs.next()) {
                            agents.add(new int[]{rs.getInt("id"), rs.getInt("level")});
                            agentNicknames.add(rs.getString("nickname"));
                            String code = rs.getString("code");
                            agentCodes.add(code != null ? code : "");
                            if (idx < agentRates.length) agentRates[idx] = rs.getDouble("commission_rate");
                            idx++;
                        }
                    }
                }

                // 2. For each agent, find their direct players + compute volume from log_report_user
                JSONArray settlements = new JSONArray();
                long grandVolume = 0, grandCommission = 0;
                int grandPlayers = 0;

                for (int i = 0; i < agents.size(); i++) {
                    int aId = agents.get(i)[0];
                    int aLevel = agents.get(i)[1];
                    String aNick = agentNicknames.get(i);
                    String aCode = agentCodes.get(i);
                    double aRate = i < agentRates.length ? agentRates[i] : 0;

                    // Find direct players for this agent
                    List<String> playerNicks = new ArrayList<>();
                    if (!aCode.isEmpty()) {
                        try (PreparedStatement ps = userConn.prepareStatement(
                                "SELECT nick_name FROM users WHERE dai_ly=0 AND " +
                                "(referral_code COLLATE utf8mb4_general_ci=? OR parent_agent_id=?)")) {
                            ps.setString(1, aCode);
                            ps.setInt(2, aId);
                            try (ResultSet rs = ps.executeQuery()) {
                                while (rs.next()) playerNicks.add(rs.getString(1));
                            }
                        }
                    }

                    // Sum volume from log_report_user for those players
                    long volume = 0, deposit = 0, withdraw = 0;
                    if (!playerNicks.isEmpty()) {
                        StringBuilder ph = new StringBuilder();
                        for (int j = 0; j < playerNicks.size(); j++) { if (j > 0) ph.append(","); ph.append("?"); }

                        StringBuilder dateCond = new StringBuilder();
                        List<String> dateParams = new ArrayList<>();
                        if (fromDate != null && !fromDate.isEmpty()) { dateCond.append(" AND time_report >= ?"); dateParams.add(fromDate); }
                        if (toDate != null && !toDate.isEmpty()) { dateCond.append(" AND time_report <= ?"); dateParams.add(toDate); }

                        String sql = "SELECT " +
                                "COALESCE(SUM(COALESCE(taixiu,0)+COALESCE(xocdia,0)+COALESCE(minipoker,0)+" +
                                "COALESCE(slot_pokemon,0)+COALESCE(baucua,0)+COALESCE(caothap,0)+" +
                                "COALESCE(bacay,0)+COALESCE(tlmn,0)+COALESCE(slot_bitcoin,0)+" +
                                "COALESCE(slot_taydu,0)+COALESCE(slot_angrybird,0)+COALESCE(slot_thantai,0)+" +
                                "COALESCE(slot_thethao,0)),0) AS vol, " +
                                "COALESCE(SUM(COALESCE(deposit,0)),0) AS dep, " +
                                "COALESCE(SUM(COALESCE(withdraw,0)),0) AS wdr " +
                                "FROM log_report_user WHERE nick_name IN (" + ph + ")" + dateCond;
                        try (PreparedStatement ps = userConn.prepareStatement(sql)) {
                            int idx = 1;
                            for (String n : playerNicks) ps.setString(idx++, n);
                            for (String d : dateParams) ps.setString(idx++, d);
                            try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) { volume = rs.getLong("vol"); deposit = rs.getLong("dep"); withdraw = rs.getLong("wdr"); }
                            }
                        }
                    }

                    long commission = Math.round(volume * aRate / 100.0);

                    JSONObject s = new JSONObject();
                    s.put("agent_id", aId);
                    s.put("agent_nickname", aNick);
                    s.put("agent_level", aLevel);
                    s.put("agent_code", aCode);
                    s.put("commission_rate", aRate);
                    s.put("player_count", playerNicks.size());
                    s.put("total_volume", volume);
                    s.put("estimated_commission", commission);
                    s.put("total_deposit", deposit);
                    s.put("total_withdraw", withdraw);
                    settlements.put(s);

                    grandVolume += volume;
                    grandCommission += commission;
                    grandPlayers += playerNicks.size();
                }

                JSONObject overall = new JSONObject();
                overall.put("total_volume", grandVolume);
                overall.put("total_commission", grandCommission);
                overall.put("total_players", grandPlayers);
                overall.put("total_agents", agents.size());
                overall.put("from_date", fromDate != null ? fromDate : "all");
                overall.put("to_date", toDate != null ? toDate : "all");

                response.put("success", true); response.put("errorCode", "0");
                response.put("settlements", settlements);
                response.put("overall", overall);
            }
        } catch (Exception e) {
            logger.error("GetAgentSettlementProcessor error", e);
            response.put("success", false); response.put("errorCode", "9999");
            response.put("message", "Internal: " + e.getMessage());
        }
        return response.toString();
    }

    private static String err(JSONObject r, String code, String msg) {
        r.put("success", false); r.put("errorCode", code); r.put("message", msg);
        return r.toString();
    }
}
