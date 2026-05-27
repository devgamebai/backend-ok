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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * c=9842 — Admin per-branch revenue view.
 *
 * Returns volume/commission breakdown per Master Agent (TĐL) tree,
 * rolled up through all levels (F1→F4). Admin sees every branch.
 *
 * For each TĐL branch: total betting volume, total deposit, total withdraw,
 * player count, sub-agent count at each level.
 *
 * Uses: useragent (tree structure) + vinplay.log_report_user (daily bet/win)
 *       + users (player count per branch via parent_agent_id/referral_code).
 *
 * Params: aat, ft (from date, optional), et (to date, optional).
 */
public class AdminBranchRevenueProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();
            String fromDate = request.getParameter("ft");
            String toDate = request.getParameter("et");

            try (Connection adminConn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
                 Connection userConn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {

                // 1. Get all TĐL (Master Agents, level=1)
                List<int[]> masters = new ArrayList<>(); // [id]
                Map<Integer, JSONObject> masterMap = new LinkedHashMap<>();
                try (PreparedStatement ps = adminConn.prepareStatement(
                        "SELECT id, nickname, code, commission_rate FROM useragent WHERE level=1 ORDER BY id")) {
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            int id = rs.getInt("id");
                            JSONObject branch = new JSONObject();
                            branch.put("master_id", id);
                            branch.put("master_nickname", rs.getString("nickname"));
                            branch.put("master_code", rs.getString("code") != null ? rs.getString("code") : "");
                            // SUN-1098: 2-decimal string preserves trailing zeros.
                            branch.put("commission_rate", com.vinplay.dal.utils.PctFormatter.formatRs(rs, "commission_rate"));
                            masterMap.put(id, branch);
                        }
                    }
                }

                // 2. For each TĐL, count sub-agents + collect all agent nicknames in subtree
                for (Map.Entry<Integer, JSONObject> entry : masterMap.entrySet()) {
                    int masterId = entry.getKey();
                    JSONObject branch = entry.getValue();

                    // Sub-agents in tree
                    List<String> subtreeNicknames = new ArrayList<>();
                    subtreeNicknames.add(branch.getString("master_nickname"));
                    int dl1Count = 0, dl2Count = 0;
                    try (PreparedStatement ps = adminConn.prepareStatement(
                            "SELECT nickname, level FROM useragent WHERE FIND_IN_SET(?, ancestors) > 0")) {
                        ps.setInt(1, masterId);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                subtreeNicknames.add(rs.getString("nickname"));
                                int lv = rs.getInt("level");
                                if (lv == 2) dl1Count++;
                                else if (lv == 3) dl2Count++;
                            }
                        }
                    }
                    branch.put("dl1_count", dl1Count);
                    branch.put("dl2_count", dl2Count);
                    branch.put("total_agents", 1 + dl1Count + dl2Count);

                    // 3. Collect all codes in subtree (current + historical)
                    List<String> codes = new ArrayList<>();
                    StringBuilder idPh = new StringBuilder("?");
                    List<Integer> subtreeIds = new ArrayList<>();
                    subtreeIds.add(masterId);
                    try (PreparedStatement ps = adminConn.prepareStatement(
                            "SELECT id, code FROM useragent WHERE id=? OR FIND_IN_SET(?, ancestors) > 0")) {
                        ps.setInt(1, masterId);
                        ps.setInt(2, masterId);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                if (rs.getInt("id") != masterId) subtreeIds.add(rs.getInt("id"));
                                String c = rs.getString("code");
                                if (c != null && !c.isEmpty()) codes.add(c);
                            }
                        }
                    }

                    // 4. Count players in branch
                    int playerCount = 0;
                    if (!codes.isEmpty()) {
                        StringBuilder codePh = new StringBuilder();
                        for (int i = 0; i < codes.size(); i++) {
                            if (i > 0) codePh.append(",");
                            codePh.append("?");
                        }
                        try (PreparedStatement ps = userConn.prepareStatement(
                                "SELECT COUNT(*) FROM users WHERE dai_ly=0 AND referral_code COLLATE utf8mb4_general_ci IN (" + codePh + ")")) {
                            for (int i = 0; i < codes.size(); i++) ps.setString(i + 1, codes.get(i));
                            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) playerCount = rs.getInt(1); }
                        }
                    }
                    branch.put("player_count", playerCount);

                    // 5. Revenue: sum from log_report_user for all nicknames in subtree + their players
                    // log_report_user has columns per game: taixiu, taixiu_win, slot_pokemon, slot_pokemon_win, etc.
                    // Sum all bet columns for volume, all _win columns for payout
                    long totalVolume = 0, totalPayout = 0, totalDeposit = 0, totalWithdraw = 0;
                    if (!subtreeNicknames.isEmpty()) {
                        // Get player nicknames under this branch
                        List<String> allNicks = new ArrayList<>(subtreeNicknames);
                        if (!codes.isEmpty()) {
                            StringBuilder codePh = new StringBuilder();
                            for (int i = 0; i < codes.size(); i++) {
                                if (i > 0) codePh.append(",");
                                codePh.append("?");
                            }
                            try (PreparedStatement ps = userConn.prepareStatement(
                                    "SELECT nick_name FROM users WHERE dai_ly=0 AND referral_code COLLATE utf8mb4_general_ci IN (" + codePh + ")")) {
                                for (int i = 0; i < codes.size(); i++) ps.setString(i + 1, codes.get(i));
                                try (ResultSet rs = ps.executeQuery()) {
                                    while (rs.next()) allNicks.add(rs.getString(1));
                                }
                            }
                        }

                        if (!allNicks.isEmpty()) {
                            StringBuilder nickPh = new StringBuilder();
                            for (int i = 0; i < allNicks.size(); i++) {
                                if (i > 0) nickPh.append(",");
                                nickPh.append("?");
                            }

                            StringBuilder dateCond = new StringBuilder();
                            List<String> dateParams = new ArrayList<>();
                            if (fromDate != null && !fromDate.isEmpty()) {
                                dateCond.append(" AND time_report >= ?");
                                dateParams.add(fromDate);
                            }
                            if (toDate != null && !toDate.isEmpty()) {
                                dateCond.append(" AND time_report <= ?");
                                dateParams.add(toDate);
                            }

                            String revSql = "SELECT " +
                                    "COALESCE(SUM(COALESCE(taixiu,0)+COALESCE(xocdia,0)+COALESCE(minipoker,0)+" +
                                    "COALESCE(slot_pokemon,0)+COALESCE(baucua,0)+COALESCE(caothap,0)+" +
                                    "COALESCE(bacay,0)+COALESCE(tlmn,0)+COALESCE(slot_bitcoin,0)+" +
                                    "COALESCE(slot_taydu,0)+COALESCE(slot_angrybird,0)+COALESCE(slot_thantai,0)+" +
                                    "COALESCE(slot_thethao,0)),0) AS total_volume, " +
                                    "COALESCE(SUM(COALESCE(taixiu_win,0)+COALESCE(xocdia_win,0)+COALESCE(minipoker_win,0)+" +
                                    "COALESCE(slot_pokemon_win,0)+COALESCE(baucua_win,0)+COALESCE(caothap_win,0)+" +
                                    "COALESCE(bacay_win,0)+COALESCE(tlmn_win,0)+COALESCE(slot_bitcoin_win,0)+" +
                                    "COALESCE(slot_taydu_win,0)+COALESCE(slot_angrybird_win,0)+COALESCE(slot_thantai_win,0)+" +
                                    "COALESCE(slot_thethao_win,0)),0) AS total_payout, " +
                                    "COALESCE(SUM(COALESCE(deposit,0)),0) AS total_deposit, " +
                                    "COALESCE(SUM(COALESCE(withdraw,0)),0) AS total_withdraw " +
                                    "FROM log_report_user WHERE nick_name IN (" + nickPh + ")" + dateCond;
                            try (PreparedStatement ps = userConn.prepareStatement(revSql)) {
                                int idx = 1;
                                for (String n : allNicks) ps.setString(idx++, n);
                                for (String d : dateParams) ps.setString(idx++, d);
                                try (ResultSet rs = ps.executeQuery()) {
                                    if (rs.next()) {
                                        totalVolume = rs.getLong("total_volume");
                                        totalPayout = rs.getLong("total_payout");
                                        totalDeposit = rs.getLong("total_deposit");
                                        totalWithdraw = rs.getLong("total_withdraw");
                                    }
                                }
                            }
                        }
                    }
                    branch.put("total_volume", totalVolume);
                    branch.put("total_payout", totalPayout);
                    branch.put("house_net", totalVolume - totalPayout);
                    branch.put("total_deposit", totalDeposit);
                    branch.put("total_withdraw", totalWithdraw);
                }

                JSONArray branches = new JSONArray();
                long grandVolume = 0, grandPayout = 0, grandDeposit = 0, grandWithdraw = 0;
                int grandPlayers = 0, grandAgents = 0;
                for (JSONObject b : masterMap.values()) {
                    branches.put(b);
                    grandVolume += b.getLong("total_volume");
                    grandPayout += b.getLong("total_payout");
                    grandDeposit += b.getLong("total_deposit");
                    grandWithdraw += b.getLong("total_withdraw");
                    grandPlayers += b.getInt("player_count");
                    grandAgents += b.getInt("total_agents");
                }

                JSONObject overall = new JSONObject();
                overall.put("total_volume", grandVolume);
                overall.put("total_payout", grandPayout);
                overall.put("house_net", grandVolume - grandPayout);
                overall.put("total_deposit", grandDeposit);
                overall.put("total_withdraw", grandWithdraw);
                overall.put("total_players", grandPlayers);
                overall.put("total_agents", grandAgents);
                overall.put("branch_count", masterMap.size());

                response.put("success", true);
                response.put("errorCode", "0");
                response.put("branches", branches);
                response.put("overall", overall);
            }
        } catch (Exception e) {
            logger.error("AdminBranchRevenueProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
            response.put("message", "Internal: " + e.getMessage());
        }
        return response.toString();
    }
}
