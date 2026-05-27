package com.vinplay.api.backend.processors.commission;

import com.vinplay.dal.dao.impl.GameCommissionRateDaoImpl;
import com.vinplay.dal.entities.agent.AgentCommissionDaily;
import com.vinplay.dal.entities.agent.UserAgentModel;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Real-time per-user commission rows for one agent (c=9537). Params: ac (required), fd, td, pn, l.
 */
public class RealtimeAgentCommissionDetailProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        String agentCode = request.getParameter("ac");
        String fromDate = request.getParameter("fd");
        String toDate = request.getParameter("td");

        if (agentCode == null || agentCode.trim().isEmpty()) {
            return BaseResponse.error("-1", "agentCode is required");
        }
        if (fromDate == null || fromDate.trim().isEmpty() || toDate == null || toDate.trim().isEmpty()) {
            return BaseResponse.error("-1", "fromDate and toDate are required");
        }

        int page = 1;
        int limit = 20;
        try {
            page = Integer.parseInt(request.getParameter("pn"));
        } catch (NumberFormatException ignored) {
        }
        try {
            limit = Integer.parseInt(request.getParameter("l"));
        } catch (NumberFormatException ignored) {
        }
        if (page < 1) page = 1;
        if (limit < 1) limit = 20;

        String ac = agentCode.trim();

        try {
            Map<String, UserAgentModel> codeToAgent = new HashMap<>();
            Map<Integer, UserAgentModel> idToAgent = new HashMap<>();
            CommissionCalcHelper.loadAllAgentsForChain(codeToAgent, idToAgent);

            UserAgentModel directAgent = CommissionCalcHelper.resolveAgent(codeToAgent, ac);
            if (directAgent == null) {
                return BaseResponse.error("-1", "Agent not found: " + ac);
            }
            String bk = CommissionCalcHelper.agentBusinessKey(directAgent);
            List<UserAgentModel> subtreeAgents = CommissionCalcHelper.collectAgentSubtree(directAgent, idToAgent);
            if (subtreeAgents.isEmpty()) {
                return buildSuccess(0, new ArrayList<>(), 0L, 0L, 0L, 0L, 0L);
            }

            CommissionCalcHelper.AgentUsersBatch batch = CommissionCalcHelper.loadUsersForAgentsBatch(subtreeAgents);
            if (batch.allNicknames.isEmpty()) {
                return buildSuccess(0, new ArrayList<>(), 0L, 0L, 0L, 0L, 0L);
            }
            List<String> allNicknames = new ArrayList<>(batch.allNicknames);

            GameCommissionRateDaoImpl gameRateDao = new GameCommissionRateDaoImpl();
            Map<String, Map<Integer, Double>> userGameRates = gameRateDao.getRateMapByNickNames(allNicknames);
            List<Map<String, Object>> logRecords = CommissionCalcHelper.loadLogReportUsers(allNicknames, fromDate, toDate);
            Map<String, CommissionCalcHelper.UserCashflow> cashflowByNick =
                    CommissionCalcHelper.loadAdminNapRutByNicknames(allNicknames, fromDate, toDate);
            Map<String, Long> balanceByNick = CommissionCalcHelper.loadVinBalanceByNicknames(allNicknames);

            List<Map<String, Object>> allRows = new ArrayList<>();
            for (Map<String, Object> record : logRecords) {
                Map<String, Object> row = buildRow(record, bk, batch, userGameRates, codeToAgent, idToAgent, cashflowByNick, balanceByNick, false);
                if (row != null) {
                    allRows.add(row);
                }
            }

            Set<String> nicksWithLog = new HashSet<>();
            for (Map<String, Object> rec : logRecords) {
                String n = (String) rec.get("nick_name");
                if (n != null) {
                    nicksWithLog.add(n.trim());
                }
            }
            for (String nick : batch.allNicknames) {
                if (nick == null) {
                    continue;
                }
                String nn = nick.trim();
                if (nicksWithLog.contains(nn)) {
                    continue;
                }
                Map<String, Object> zeroRec = CommissionCalcHelper.buildZeroLogReportRecord(nn, fromDate);
                Map<String, Object> row = buildRow(zeroRec, bk, batch, userGameRates, codeToAgent, idToAgent, cashflowByNick, balanceByNick, true);
                if (row != null) {
                    allRows.add(row);
                }
            }

            allRows.sort(Comparator
                    .comparing((Map<String, Object> m) -> (String) m.get("date"), Comparator.reverseOrder())
                    .thenComparing(m -> (String) m.get("nick")));

            int total = allRows.size();
            int fromIdx = (page - 1) * limit;
            int toIdx = Math.min(fromIdx + limit, total);

            long summaryBet = 0;
            long summaryComm = 0;
            long summaryNap = 0;
            long summaryRut = 0;
            long summaryBalance = 0;
            List<Map<String, Object>> pageRows = new ArrayList<>();
            if (fromIdx < total) {
                for (int i = fromIdx; i < toIdx; i++) {
                    Map<String, Object> r = allRows.get(i);
                    pageRows.add(r);
                    summaryBet += ((Number) r.get("totalBet")).longValue();
                    summaryComm += ((Number) r.get("agentComm")).longValue();
                    summaryNap += ((Number) r.get("totalNap")).longValue();
                    summaryRut += ((Number) r.get("totalRut")).longValue();
                    summaryBalance += ((Number) r.get("balance")).longValue();
                }
            }

            return buildSuccess(total, pageRows, summaryBet, summaryComm, summaryNap, summaryRut, summaryBalance);

        } catch (Exception e) {
            logger.error("RealtimeAgentCommissionDetail error", e);
            return BaseResponse.error("-1", e.getMessage());
        }
    }

    private Map<String, Object> buildRow(
            Map<String, Object> record,
            String rootBk,
            CommissionCalcHelper.AgentUsersBatch batch,
            Map<String, Map<Integer, Double>> userGameRates,
            Map<String, UserAgentModel> codeToAgent,
            Map<Integer, UserAgentModel> idToAgent,
            Map<String, CommissionCalcHelper.UserCashflow> cashflowByNick,
            Map<String, Long> balanceByNick,
            boolean noLogInPeriod) {
        String nickRaw = (String) record.get("nick_name");
        if (nickRaw == null) {
            return null;
        }
        String nickName = nickRaw.trim();
        logger.debug("buildRow " + nickName);
        UserAgentModel ownerAgent = batch.ownerByNick.get(nickName);
        if (ownerAgent == null) {
            return null;
        }

        logger.debug("buildRow 1 " + nickName);
        String timeReport = (String) record.get("time_report");
        double defaultUserRate = batch.userDefaultRateByNick.getOrDefault(nickName, 0.0);
        Map<Integer, Double> perGameRates = userGameRates.getOrDefault(nickName, new HashMap<>());

        CommissionCalcHelper.LogRecordCommissionBreakdown br =
                CommissionCalcHelper.computeLogRecordBreakdown(
                        record, ownerAgent, defaultUserRate, perGameRates,
                        codeToAgent, idToAgent, false);

        List<AgentCommissionDaily.AgentDistribution> totalDists =
                CommissionCalcHelper.buildTotalDistributions(
                        ownerAgent, br.agentTotalCommission, br.totalBet, idToAgent);
        logger.debug("buildRow 2 " + nickName);
        double earnRate = 0;
        long commissionForAgent = 0;
        for (AgentCommissionDaily.AgentDistribution d : totalDists) {
            if (rootBk.equals(d.getAgentCode())) {
                earnRate = d.getEarnRate();
                commissionForAgent = d.getCommission();
                break;
            }
        }

        CommissionCalcHelper.UserCashflow cash = cashflowByNick.getOrDefault(nickName, new CommissionCalcHelper.UserCashflow());
        UserAgentModel rowAsAgent = CommissionCalcHelper.resolveAgentByPlayerNick(codeToAgent, nickName);
        boolean isAgentAccount = rowAsAgent != null;
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("date", timeReport);
        row.put("nick", nickName);
        row.put("accountType", isAgentAccount ? "agent" : "user");
        row.put("isAgent", isAgentAccount);
        if (isAgentAccount) {
            String c = rowAsAgent.getCode();
            if (c != null && !c.trim().isEmpty()) {
                row.put("agentCode", c.trim());
            }
        }
        row.put("totalBet", br.totalBet);
        row.put("casino", br.totalBetCasino);
        row.put("sport", br.totalBetSport);
        row.put("game", br.totalBetGame);
        row.put("userRate", defaultUserRate);
        row.put("userComm", br.totalUserCommission);
        row.put("agentEarnRate", earnRate);
        row.put("agentComm", commissionForAgent);
        row.put("totalNap", cash.totalNap);
        row.put("totalRut", cash.totalRut);
        row.put("balance", balanceByNick.getOrDefault(nickName, 0L));
        if (noLogInPeriod) {
            row.put("noLogInPeriod", true);
        }
        logger.debug("buildRow 3 " + nickName);
        return row;
    }

    private String buildSuccess(int total, List<Map<String, Object>> data, long summaryBet, long summaryComm, long summaryNap, long summaryRut, long summaryBalance) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalBet", summaryBet);
        summary.put("totalCommission", summaryComm);
        summary.put("totalNap", summaryNap);
        summary.put("totalRut", summaryRut);
        summary.put("totalBalance", summaryBalance);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("data", data);
        result.put("summary", summary);

        return BaseResponse.success(result, (long) total);
    }
}
