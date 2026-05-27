package com.vinplay.api.backend.processors.commission;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.vinplay.dal.dao.impl.GameCommissionRateDaoImpl;
import com.vinplay.dal.entities.agent.UserAgentModel;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Real-time aggregate commission per direct agent (c=9536). Params: fd, td, ac (optional), pn, l.
 */
public class RealtimeAgentCommissionListProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        String fromDate = request.getParameter("fd");
        String toDate = request.getParameter("td");
        String filterCode = request.getParameter("ac");

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

        try {
            Map<String, UserAgentModel> codeToAgent = new HashMap<>();
            Map<Integer, UserAgentModel> idToAgent = new HashMap<>();
            CommissionCalcHelper.loadAllAgentsForChain(codeToAgent, idToAgent);

            List<UserAgentModel> agents = CommissionCalcHelper.loadAllActiveAgentsList();
            if (filterCode != null && !filterCode.trim().isEmpty()) {
                String f = filterCode.trim();
                agents = agents.stream()
                        .filter(a -> {
                            String bk = CommissionCalcHelper.agentBusinessKey(a);
                            if (f.equalsIgnoreCase(bk)) {
                                return true;
                            }
                            if (a.getCode() != null && f.equalsIgnoreCase(a.getCode().trim())) {
                                return true;
                            }
                            return a.getNickname() != null && f.equalsIgnoreCase(a.getNickname().trim());
                        })
                        .collect(Collectors.toList());
                if (agents.isEmpty()) {
                    return emptyJson();
                }
            }

            GameCommissionRateDaoImpl gameRateDao = new GameCommissionRateDaoImpl();
            CommissionCalcHelper.AgentUsersBatch globalBatch = CommissionCalcHelper.loadUsersForAgentsBatch(agents);
            List<String> allNicks = new ArrayList<>(globalBatch.allNicknames);
            Map<String, Map<Integer, Double>> userGameRatesAll = allNicks.isEmpty()
                    ? new HashMap<>()
                    : gameRateDao.getRateMapByNickNames(allNicks);
            Map<String, CommissionCalcHelper.UserCashflow> cashflowByNick = CommissionCalcHelper.loadAdminNapRutByNicknames(allNicks, fromDate, toDate);
            Map<String, Long> balanceByNick = CommissionCalcHelper.loadVinBalanceByNicknames(allNicks);

            List<Map<String, Object>> allRows = new ArrayList<>();
            long grandBet = 0;
            long grandComm = 0;
            long grandNap = 0;
            long grandRut = 0;
            long grandBalance = 0;

            for (UserAgentModel agent : agents) {
                String bk = CommissionCalcHelper.agentBusinessKey(agent);
                if (bk.isEmpty()) {
                    continue;
                }

                long rowBet = 0;
                long rowComm = 0;
                long rowNap = 0;
                long rowRut = 0;
                long rowBalance = 0;
                Set<String> totalUserSet = new java.util.HashSet<>();
                List<UserAgentModel> subtreeAgents = CommissionCalcHelper.collectAgentSubtree(agent, idToAgent);
                Set<String> scopedNicks = new java.util.HashSet<>();
                Map<String, UserAgentModel> ownerByNick = new HashMap<>();

                for (UserAgentModel ownerAgent : subtreeAgents) {
                    String ownerBk = CommissionCalcHelper.agentBusinessKey(ownerAgent);
                    if (ownerBk.isEmpty()) continue;
                    List<Map<String, Object>> users = globalBatch.usersByAgentCode.getOrDefault(ownerBk, new ArrayList<>());
                    for (Map<String, Object> u : users) {
                        String nn = (String) u.get("nick_name");
                        if (nn == null || nn.isEmpty()) continue;
                        if (scopedNicks.add(nn)) {
                            ownerByNick.put(nn, ownerAgent);
                        }
                    }
                }
                if (scopedNicks.isEmpty()) {
                    continue;
                }

                List<String> nicknames = new ArrayList<>(scopedNicks);
                totalUserSet.addAll(nicknames);
                List<Map<String, Object>> logRecords = CommissionCalcHelper.loadLogReportUsers(nicknames, fromDate, toDate);

                for (Map<String, Object> record : logRecords) {
                    String nickName = (String) record.get("nick_name");
                    UserAgentModel ownerAgent = ownerByNick.get(nickName);
                    if (ownerAgent == null) {
                        continue;
                    }

                    double defaultUserRate = globalBatch.userDefaultRateByNick.getOrDefault(nickName, 0.0);
                    Map<Integer, Double> perGameRates = userGameRatesAll.getOrDefault(nickName, new HashMap<>());
                    CommissionCalcHelper.LogRecordCommissionBreakdown br =
                            CommissionCalcHelper.computeLogRecordBreakdown(
                                    record, ownerAgent, defaultUserRate, perGameRates,
                                    codeToAgent, idToAgent, false);
                    if (br.totalBet == 0) continue;
                    rowBet += br.totalBet;
                    rowComm += br.agentTotalCommission.getOrDefault(bk, 0L);
                }
                int userCount = totalUserSet.size();
                for (String nn : totalUserSet) {
                    CommissionCalcHelper.UserCashflow c = cashflowByNick.getOrDefault(nn, new CommissionCalcHelper.UserCashflow());
                    rowNap += c.totalNap;
                    rowRut += c.totalRut;
                    rowBalance += balanceByNick.getOrDefault(nn, 0L);
                }

                grandBet += rowBet;
                grandComm += rowComm;
                grandNap += rowNap;
                grandRut += rowRut;
                grandBalance += rowBalance;

                double ratePct = agent.getCommission_rate() != null ? agent.getCommission_rate() : 0.0;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("code", bk);
                row.put("name", agent.getNickname());
                row.put("accountType", "agent");
                row.put("isAgent", true);
                row.put("level", agent.getLevel() != null ? agent.getLevel() : 0);
                row.put("rate", ratePct);
                row.put("totalBet", rowBet);
                row.put("commission", rowComm);
                row.put("userCount", userCount);
                row.put("totalNap", rowNap);
                row.put("totalRut", rowRut);
                row.put("totalBalance", rowBalance);
                allRows.add(row);
            }

            int totalAgents = allRows.size();
            int fromIdx = (page - 1) * limit;
            int toIdx = Math.min(fromIdx + limit, totalAgents);
            List<Map<String, Object>> pageRows = fromIdx >= totalAgents
                    ? new ArrayList<>()
                    : allRows.subList(fromIdx, toIdx);

            Map<String, Object> summary = new HashMap<>();
            summary.put("totalBet", grandBet);
            summary.put("totalCommission", grandComm);
            summary.put("totalNap", grandNap);
            summary.put("totalRut", grandRut);
            summary.put("totalBalance", grandBalance);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("total", totalAgents);
            result.put("agents", pageRows);
            result.put("summary", summary);

            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            String json = ow.writeValueAsString(result);
            return new BaseResponse<String>().success(json);

        } catch (Exception e) {
            logger.error("RealtimeAgentCommissionList error", e);
            return BaseResponse.error("-1", e.getMessage());
        }
    }

    private String emptyJson() throws Exception {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalBet", 0L);
        summary.put("totalCommission", 0L);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", 0);
        result.put("agents", new ArrayList<>());
        result.put("summary", summary);
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        return new BaseResponse<String>().success(ow.writeValueAsString(result));
    }
}
