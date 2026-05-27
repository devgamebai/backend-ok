package com.vinplay.api.backend.processors.agent;

import com.vinplay.dal.dao.AgentDAO;
import com.vinplay.dal.dao.impl.AgentDAOImpl;
import com.vinplay.dal.dao.CreditWalletDao;
import com.vinplay.dal.dao.impl.CreditWalletDaoImpl;
import com.vinplay.dal.entities.agent.UserAgentModel;
import com.vinplay.dal.rebate.RebateService;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PartnerSettlement4AgencyProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        JSONObject response = new JSONObject();

        try {
            String sessionAgentCode = request.getParameter("rc"); // Logged in agent
            String targetNickName = request.getParameter("nn"); // Selected partner in the left tree
            String ft = request.getParameter("ft"); // 2026-04-01
            String et = request.getParameter("et"); // 2026-04-30
            
            // Pagination
            int page = 1;
            int limit = 50;
            try { if (request.getParameter("pg") != null) page = Integer.parseInt(request.getParameter("pg")); } catch (Exception e) {}
            try { if (request.getParameter("size") != null) limit = Integer.parseInt(request.getParameter("size")); } catch (Exception e) {}
            
            String sort = request.getParameter("sort");
            if (sort == null || sort.isEmpty()) sort = "date";
            String dir = request.getParameter("dir");
            final boolean isAsc = "asc".equalsIgnoreCase(dir);

            if (sessionAgentCode == null || sessionAgentCode.isEmpty() || targetNickName == null || targetNickName.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1002");
                response.put("message", "Missing session agent code or target nickname");
                return response.toString();
            }

            AgentDAO agentDAO = new AgentDAOImpl();
            UserAgentModel targetAgent = agentDAO.DetailUserAgentByNickName(targetNickName);
            if (targetAgent == null) {
                response.put("success", false);
                response.put("errorCode", "1003");
                response.put("message", "Target agent not found");
                return response.toString();
            }

            // rebate_config.agent_user_id FK → vinplay_admin.useragent.id = targetAgent.getId()
            double casinoRate = 0;
            double slotRate = 0;
            try {
                Map<String, Object> config = RebateService.getConfig(targetAgent.getId());
                if (config != null) {
                    double defaultRate = asDouble(config.get("rebate_percentage"));
                    casinoRate = config.containsKey("casino_rate") ? asDouble(config.get("casino_rate")) : defaultRate;
                    slotRate   = config.containsKey("slot_rate")   ? asDouble(config.get("slot_rate"))   : defaultRate;
                }
            } catch (Exception rebateErr) {
                logger.warn("PartnerSettlement4AgencyProcessor: rebate config lookup failed for agentId=" + targetAgent.getId(), rebateErr);
            }

            // SUN-863: credit_balance of the target agent
            long creditBalance = 0L;
            try {
                CreditWalletDao creditWalletDao = new CreditWalletDaoImpl();
                creditBalance = creditWalletDao.getBalance(targetAgent.getId());
            } catch (Exception cwErr) {
                // Non-fatal — log and keep 0
                logger.warn("PartnerSettlement4AgencyProcessor: credit_wallet lookup failed agentId=" + targetAgent.getId(), cwErr);
            }

            // Resolve the target agent's downline player nicknames. We can't filter
            // `log_report_user.code` because that column is populated lazily by the vbee consumer
            // and is NULL for every row on staging (the Hazelcast users IMap is cold and the
            // lookup falls back to the empty string). Instead, walk the users table the same way
            // DetailMemberOfAgencyProcessor / SearchGame3rdBettingHistory4AgencyProcessor do:
            // any `users` row whose `parent_agent_id` is in the target-agent subtree OR whose
            // `referral_code` matches the target-agent code (or any legacy code recorded in
            // `agent_code_history`) is a downline player.
            List<String> downlineNicks = resolveDownlineNicknames(targetAgent);
            if (downlineNicks.isEmpty()) {
                // No downline players at all — return empty data + zero totals, not an error.
                response.put("success", true);
                response.put("errorCode", "0");
                response.put("data", new JSONArray());
                response.put("totals", zeroTotals());
                response.put("totalRecords", 0);
                response.put("page", page);
                response.put("limit", limit);
                return response.toString();
            }

            JSONArray arr = new JSONArray();
            long totalDeposit = 0, totalWithdraw = 0;
            long tBetCasino = 0, tWinCasino = 0, tRollingCasino = 0;
            long tBetSlot = 0, tWinSlot = 0, tRollingSlot = 0;
            int totalRecords = 0;

            try (Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
                String slotBet = "IFNULL(slot_pokemon,0) + IFNULL(slot_bitcoin,0) + IFNULL(slot_taydu,0) + IFNULL(slot_angrybird,0) + IFNULL(slot_thantai,0) + IFNULL(slot_thethao,0) + IFNULL(slot_chiemtinh,0) + IFNULL(slot_thanbai,0) + IFNULL(slot_bikini,0) + IFNULL(slot_galaxy,0) + IFNULL(hamcamap,0) + IFNULL(range_rover,0) + IFNULL(sexygirl,0)";
                String slotWin = "IFNULL(slot_pokemon_win,0) + IFNULL(slot_bitcoin_win,0) + IFNULL(slot_taydu_win,0) + IFNULL(slot_angrybird_win,0) + IFNULL(slot_thantai_win,0) + IFNULL(slot_thethao_win,0) + IFNULL(slot_chiemtinh_win,0) + IFNULL(slot_thanbai_win,0) + IFNULL(slot_bikini_win,0) + IFNULL(slot_galaxy_win,0) + IFNULL(hamcamap_win,0) + IFNULL(range_rover_win,0) + IFNULL(sexygirl_win,0)";
                String casinoBet = "IFNULL(wm,0) + IFNULL(ag,0) + IFNULL(ebet,0) + IFNULL(ibc,0) + IFNULL(cmd,0) + IFNULL(sbo,0) + IFNULL(fish,0) + IFNULL(taixiu,0) + IFNULL(taixiu_st,0) + IFNULL(baucua,0) + IFNULL(xocdia,0) + IFNULL(tlmn,0) + IFNULL(bacay,0) + IFNULL(minipoker,0) + IFNULL(caothap,0) + IFNULL(taixiu_sicbo,0) + IFNULL(over_under,0) + IFNULL(lode,0) + IFNULL(samtruyen,0) + IFNULL(sam,0) + IFNULL(binh,0) + IFNULL(tala,0) + IFNULL(lieng,0) + IFNULL(xito,0) + IFNULL(baicao,0) + IFNULL(poker,0) + IFNULL(xidzach,0)";
                String casinoWin = "IFNULL(wm_win,0) + IFNULL(ag_win,0) + IFNULL(ebet_win,0) + IFNULL(ibc_win,0) + IFNULL(cmd_win,0) + IFNULL(sbo_win,0) + IFNULL(fish_win,0) + IFNULL(taixiu_win,0) + IFNULL(taixiu_st_win,0) + IFNULL(baucua_win,0) + IFNULL(xocdia_win,0) + IFNULL(tlmn_win,0) + IFNULL(bacay_win,0) + IFNULL(minipoker_win,0) + IFNULL(caothap_win,0) + IFNULL(taixiu_sicbo_win,0) + IFNULL(over_under_win,0) + IFNULL(lode_win,0) + IFNULL(samtruyen_win,0) + IFNULL(sam_win,0) + IFNULL(binh_win,0) + IFNULL(tala_win,0) + IFNULL(lieng_win,0) + IFNULL(xito_win,0) + IFNULL(baicao_win,0) + IFNULL(poker_win,0) + IFNULL(xidzach_win,0)";

                // Build `nick_name IN (?, ?, ...)` placeholders for the downline list.
                StringBuilder nickPh = new StringBuilder();
                for (int i = 0; i < downlineNicks.size(); i++) {
                    if (i > 0) nickPh.append(",");
                    nickPh.append("?");
                }

                // Validate date params — only accept YYYY-MM-DD to block SQL injection
                if (ft != null && !ft.matches("\\d{4}-\\d{2}-\\d{2}")) ft = null;
                if (et != null && !et.matches("\\d{4}-\\d{2}-\\d{2}")) et = null;

                String logDateCond = "";
                if (ft != null) logDateCond += "AND time_report >= '" + ft + " 00:00:00' ";
                if (et != null) logDateCond += "AND time_report <= '" + et + " 23:59:59' ";
                String baseSql = "FROM vinplay.log_report_user WHERE nick_name IN (" + nickPh + ") " + logDateCond;
                String depositDateCond = "";
                if (ft != null) depositDateCond += "AND dt.created_at >= '" + ft + " 00:00:00' ";
                if (et != null) depositDateCond += "AND dt.created_at <= '" + et + " 23:59:59' ";
                String withdrawDateCond = "";
                if (ft != null) withdrawDateCond += "AND bw.created_at >= '" + ft + " 00:00:00' ";
                if (et != null) withdrawDateCond += "AND bw.created_at <= '" + et + " 23:59:59' ";

                // Select records
                String selectSql = "SELECT DATE_FORMAT(time_report, '%Y-%m-%d') as log_date, " +
                        "SUM(" + casinoBet + ") as bet_casino, " +
                        "SUM(" + casinoWin + ") as win_casino, " +
                        "SUM(" + slotBet + ") as bet_slot, " +
                        "SUM(" + slotWin + ") as win_slot " +
                        baseSql +
                        "GROUP BY DATE_FORMAT(time_report, '%Y-%m-%d')";

                PreparedStatement stm = conn.prepareStatement(selectSql);
                for (int i = 0; i < downlineNicks.size(); i++) {
                    stm.setString(i + 1, downlineNicks.get(i));
                }
                ResultSet rs = stm.executeQuery();
                
                Map<String, JSONObject> recordsByDate = new HashMap<>();
                while (rs.next()) {
                    JSONObject obj = new JSONObject();
                    String logDate = rs.getString("log_date");
                    obj.put("date", logDate);
                    obj.put("deposit", 0L);
                    obj.put("withdraw", 0L);
                    obj.put("net_dw", 0L);

                    long bCasino = rs.getLong("bet_casino");
                    long wCasino = rs.getLong("win_casino");
                    long bSlot = rs.getLong("bet_slot");
                    long wSlot = rs.getLong("win_slot");

                    long rCasino = (long) (bCasino * casinoRate / 100.0);
                    long rSlot = (long) (bSlot * slotRate / 100.0);

                    // Casino stats
                    obj.put("bet_c", bCasino);
                    obj.put("win_c", wCasino);
                    obj.put("profit_c", bCasino - wCasino); // Player bet - Player win (Agent Profit)
                    obj.put("rolling_c", rCasino);
                    obj.put("final_profit_c", (bCasino - wCasino) - rCasino); 

                    // Slot stats
                    obj.put("bet_s", bSlot);
                    obj.put("win_s", wSlot);
                    obj.put("profit_s", bSlot - wSlot);
                    obj.put("rolling_s", rSlot);
                    obj.put("final_profit_s", (bSlot - wSlot) - rSlot);

                    // Total stats
                    obj.put("bet_t", bCasino + bSlot);
                    obj.put("win_t", wCasino + wSlot);
                    obj.put("profit_t", (bCasino - wCasino) + (bSlot - wSlot));
                    obj.put("rolling_t", rCasino + rSlot);
                    obj.put("final_profit_t", obj.getLong("final_profit_c") + obj.getLong("final_profit_s"));

                    recordsByDate.put(logDate, obj);

                    // Accumulate totals for footer
                    tBetCasino += bCasino;
                    tWinCasino += wCasino;
                    tRollingCasino += rCasino;
                    tBetSlot += bSlot;
                    tWinSlot += wSlot;
                    tRollingSlot += rSlot;
                }
                rs.close();
                stm.close();

                // Nạp/rút must come from transaction tables, not log_report_user.
                // log_report_user.deposit/withdraw columns are not populated for bank deposits.
                String txSql = "SELECT log_date, SUM(dep) AS total_deposit, SUM(wit) AS total_withdraw FROM (" +
                        "SELECT DATE_FORMAT(dt.created_at, '%Y-%m-%d') AS log_date, dt.amount AS dep, 0 AS wit " +
                        "FROM vinplay.deposit_transactions dt " +
                        "LEFT JOIN vinplay.users u ON u.id = dt.user_id " +
                        "WHERE dt.status = 'APPROVED' " + depositDateCond +
                        "AND (u.nick_name IN (" + nickPh + ") OR u.user_name IN (" + nickPh + ") " +
                        "OR dt.nick_name IN (" + nickPh + ")) " +
                        "UNION ALL " +
                        "SELECT DATE_FORMAT(bw.created_at, '%Y-%m-%d') AS log_date, 0 AS dep, bw.amount_krw AS wit " +
                        "FROM vinplay.bank_withdrawals bw " +
                        "LEFT JOIN vinplay.users u ON u.id = bw.user_id " +
                        "WHERE bw.status IN ('APPROVED','COMPLETED') " + withdrawDateCond +
                        "AND (u.nick_name IN (" + nickPh + ") OR u.user_name IN (" + nickPh + ") " +
                        "OR bw.nick_name IN (" + nickPh + ")) " +
                        ") tx GROUP BY log_date";
                PreparedStatement txStm = conn.prepareStatement(txSql);
                int txParam = 1;
                for (String nick : downlineNicks) txStm.setString(txParam++, nick);
                for (String nick : downlineNicks) txStm.setString(txParam++, nick);
                for (String nick : downlineNicks) txStm.setString(txParam++, nick);
                for (String nick : downlineNicks) txStm.setString(txParam++, nick);
                for (String nick : downlineNicks) txStm.setString(txParam++, nick);
                for (String nick : downlineNicks) txStm.setString(txParam++, nick);
                ResultSet txRs = txStm.executeQuery();
                while (txRs.next()) {
                    String logDate = txRs.getString("log_date");
                    JSONObject obj = recordsByDate.get(logDate);
                    if (obj == null) {
                        obj = emptySettlementRow(logDate);
                        recordsByDate.put(logDate, obj);
                    }
                    long deps = txRs.getLong("total_deposit");
                    long wits = txRs.getLong("total_withdraw");
                    obj.put("deposit", deps);
                    obj.put("withdraw", wits);
                    obj.put("net_dw", deps - wits);
                    totalDeposit += deps;
                    totalWithdraw += wits;
                }
                txRs.close();
                txStm.close();

                List<JSONObject> allRecords = new ArrayList<>(recordsByDate.values());
                totalRecords = allRecords.size();

                // Sort
                final String sortKey = sort;
                allRecords.sort((a, b) -> {
                    int result = 0;
                    if ("date".equals(sortKey) || "log_date".equals(sortKey)) {
                        result = a.optString("date", "").compareTo(b.optString("date", ""));
                    } else {
                        // numeric maps like total_deposit -> deposit
                        String k = sortKey;
                        if ("total_deposit".equals(sortKey)) k = "deposit";
                        else if ("total_withdraw".equals(sortKey)) k = "withdraw";
                        result = Long.compare(a.optLong(k, 0L), b.optLong(k, 0L));
                    }
                    return isAsc ? result : -result;
                });
                
                // Paginate
                int start = Math.max(0, (page - 1) * limit);
                int end = Math.min(start + limit, allRecords.size());
                for (int i = start; i < end; i++) {
                    arr.put(allRecords.get(i));
                }
            }

            // Create Footer Total Object
            JSONObject totals = new JSONObject();
            totals.put("deposit", totalDeposit);
            totals.put("withdraw", totalWithdraw);
            totals.put("net_dw", totalDeposit - totalWithdraw);
            totals.put("bet_c", tBetCasino);
            totals.put("win_c", tWinCasino);
            totals.put("profit_c", tBetCasino - tWinCasino);
            totals.put("rolling_c", tRollingCasino);
            totals.put("final_profit_c", (tBetCasino - tWinCasino) - tRollingCasino);

            totals.put("bet_s", tBetSlot);
            totals.put("win_s", tWinSlot);
            totals.put("profit_s", tBetSlot - tWinSlot);
            totals.put("rolling_s", tRollingSlot);
            totals.put("final_profit_s", (tBetSlot - tWinSlot) - tRollingSlot);

            totals.put("bet_t", tBetCasino + tBetSlot);
            totals.put("win_t", tWinCasino + tWinSlot);
            totals.put("profit_t", (tBetCasino - tWinCasino) + (tBetSlot - tWinSlot));
            totals.put("rolling_t", tRollingCasino + tRollingSlot);
            totals.put("final_profit_t", totals.getLong("final_profit_c") + totals.getLong("final_profit_s"));

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", arr);
            response.put("totals", totals);
            response.put("totalRecords", totalRecords);
            response.put("page", page);
            response.put("limit", limit);
            // SUN-863: agent credit_balance alongside settlement data
            response.put("credit_balance", creditBalance);

        } catch (Exception e) {
            logger.error("Error in PartnerSettlement4AgencyProcessor", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }

    /**
     * Resolve the set of player nicknames that belong to `targetAgent`'s downline subtree.
     * A row in `users` is in the subtree if either:
     *   - `parent_agent_id` is in the target agent's id subtree, or
     *   - `referral_code` is in the target agent's current or historical code set.
     * Legacy codes are pulled from `vinplay_admin.agent_code_history` so historical players
     * keep flowing through the settlement report when an agent is renamed / recoded.
     */
    private static List<String> resolveDownlineNicknames(UserAgentModel targetAgent) {
        List<String> nicks = new ArrayList<>();
        if (targetAgent == null || targetAgent.getId() == null) return nicks;

        Set<Integer> subtreeIds = new HashSet<>();
        Set<String>  subtreeCodes = new HashSet<>();
        subtreeIds.add(targetAgent.getId());
        if (targetAgent.getCode() != null && !targetAgent.getCode().trim().isEmpty()) {
            subtreeCodes.add(targetAgent.getCode());
        }

        // Walk the useragent subtree + harvest legacy codes.
        try (Connection adminConn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance()
                .getConnection("mysqlpool_admin")) {
            try (PreparedStatement ps = adminConn.prepareStatement(
                    "SELECT id, code FROM vinplay_admin.useragent " +
                    "WHERE id = ? OR FIND_IN_SET(?, ancestors) > 0")) {
                ps.setInt(1, targetAgent.getId());
                ps.setString(2, String.valueOf(targetAgent.getId()));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        subtreeIds.add(rs.getInt("id"));
                        String c = rs.getString("code");
                        if (c != null && !c.trim().isEmpty()) subtreeCodes.add(c);
                    }
                }
            }

            if (!subtreeIds.isEmpty()) {
                StringBuilder idPh = new StringBuilder();
                for (int i = 0; i < subtreeIds.size(); i++) {
                    if (i > 0) idPh.append(",");
                    idPh.append("?");
                }
                try (PreparedStatement ps = adminConn.prepareStatement(
                        "SELECT old_code FROM vinplay_admin.agent_code_history " +
                        "WHERE agent_id IN (" + idPh + ")")) {
                    int idx = 1;
                    for (Integer id : subtreeIds) ps.setInt(idx++, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String oc = rs.getString("old_code");
                            if (oc != null && !oc.trim().isEmpty()) subtreeCodes.add(oc);
                        }
                    }
                } catch (Exception ignored) {
                    // agent_code_history may not exist in all environments — not fatal.
                }
            }
        } catch (Exception e) {
            logger.warn("resolveDownlineNicknames: useragent subtree resolution failed", e);
            return nicks;
        }

        if (subtreeIds.isEmpty() && subtreeCodes.isEmpty()) return nicks;

        // Fetch player nicknames from vinplay.users.
        StringBuilder idPh = new StringBuilder();
        for (int i = 0; i < subtreeIds.size(); i++) {
            if (i > 0) idPh.append(",");
            idPh.append("?");
        }
        StringBuilder codePh = new StringBuilder();
        for (int i = 0; i < subtreeCodes.size(); i++) {
            if (i > 0) codePh.append(",");
            codePh.append("?");
        }
        // Include every account in the subtree regardless of `dai_ly` — a TĐL's rolling rebate
        // is computed on *all* turnover beneath them, whether the player is a pure user or a
        // sub-agent (DL1 / DL2) who also plays. Filtering to `dai_ly=0` previously excluded
        // sub-agents' personal play from the settlement and is incorrect.
        String sql = "SELECT nick_name FROM vinplay.users WHERE nick_name IS NOT NULL AND (" +
                (subtreeIds.isEmpty()   ? "0=1" : "parent_agent_id IN (" + idPh + ")") + " OR " +
                (subtreeCodes.isEmpty() ? "0=1" : "referral_code COLLATE utf8mb4_general_ci IN (" + codePh + ")") +
                ")";

        try (Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance()
                .getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            for (Integer id : subtreeIds) ps.setInt(idx++, id);
            for (String code : subtreeCodes) ps.setString(idx++, code);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String nick = rs.getString("nick_name");
                    if (nick != null && !nick.trim().isEmpty()) nicks.add(nick);
                }
            }
        } catch (Exception e) {
            logger.warn("resolveDownlineNicknames: users lookup failed", e);
            return Collections.emptyList();
        }
        return nicks;
    }

    private static JSONObject zeroTotals() {
        JSONObject t = new JSONObject();
        String[] keys = {"deposit","withdraw","net_dw",
                "bet_c","win_c","profit_c","rolling_c","final_profit_c",
                "bet_s","win_s","profit_s","rolling_s","final_profit_s",
                "bet_t","win_t","profit_t","rolling_t","final_profit_t"};
        for (String k : keys) t.put(k, 0);
        return t;
    }

    private static JSONObject emptySettlementRow(String logDate) {
        JSONObject row = zeroTotals();
        row.put("date", logDate);
        return row;
    }

    private static double asDouble(Object v) {
        if (v == null) return 0;
        if (v instanceof Number) return ((Number) v).doubleValue();
        try { return Double.parseDouble(v.toString()); } catch (NumberFormatException e) { return 0; }
    }
}
