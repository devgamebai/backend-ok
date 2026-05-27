package com.vinplay.api.backend.processors.commission;

import com.vinplay.dal.dao.impl.AgentCommissionDaoImpl;
import com.vinplay.dal.dao.impl.GameCommissionRateDaoImpl;
import com.vinplay.dal.entities.agent.AgentCommissionDaily;
import com.vinplay.dal.entities.agent.UserAgentModel;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalculateAgentCommissionProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        String fromDate = request.getParameter("fd");
        String toDate = request.getParameter("td");
        String agentCode = request.getParameter("ac");

        if (fromDate == null || fromDate.trim().isEmpty() || toDate == null || toDate.trim().isEmpty()) {
            return BaseResponse.error("-1", "fromDate and toDate are required");
        }

        try {
            Map<String, UserAgentModel> codeToAgent = new HashMap<>();
            Map<Integer, UserAgentModel> idToAgent = new HashMap<>();
            CommissionCalcHelper.loadAgents(codeToAgent, idToAgent, agentCode);

            if (codeToAgent.isEmpty()) {
                return BaseResponse.success("No agents found", 0);
            }

            GameCommissionRateDaoImpl gameRateDao = new GameCommissionRateDaoImpl();
            AgentCommissionDaoImpl commissionDao = new AgentCommissionDaoImpl();
            int totalProcessed = 0;

            for (UserAgentModel agent : codeToAgent.values()) {
                String referralKey = CommissionCalcHelper.agentBusinessKey(agent);
                if (referralKey.isEmpty()) {
                    continue;
                }

                List<Map<String, Object>> users = CommissionCalcHelper.loadUsersForAgent(referralKey);
                if (users.isEmpty()) continue;

                List<String> nicknames = new ArrayList<>();
                Map<String, Double> userDefaultRateMap = new HashMap<>();
                for (Map<String, Object> u : users) {
                    String nn = (String) u.get("nick_name");
                    Double rate = (Double) u.get("commission_rate");
                    nicknames.add(nn);
                    userDefaultRateMap.put(nn, rate != null ? rate : 0.0);
                }

                Map<String, Map<Integer, Double>> userGameRates = gameRateDao.getRateMapByNickNames(nicknames);

                List<Map<String, Object>> logRecords = CommissionCalcHelper.loadLogReportUsers(nicknames, fromDate, toDate);

                for (Map<String, Object> record : logRecords) {
                    String nickName = (String) record.get("nick_name");
                    String timeReport = (String) record.get("time_report");
                    double defaultUserRate = userDefaultRateMap.getOrDefault(nickName, 0.0);
                    Map<Integer, Double> perGameRates = userGameRates.getOrDefault(nickName, new HashMap<>());

                    CommissionCalcHelper.LogRecordCommissionBreakdown br =
                            CommissionCalcHelper.computeLogRecordBreakdown(
                                    record, agent, defaultUserRate, perGameRates,
                                    codeToAgent, idToAgent, true);

                    if (br.totalBet == 0) continue;

                    List<AgentCommissionDaily.AgentDistribution> totalDistributions =
                            CommissionCalcHelper.buildTotalDistributions(
                                    agent, br.agentTotalCommission, br.totalBet, idToAgent);

                    AgentCommissionDaily daily = new AgentCommissionDaily(
                            timeReport, nickName, defaultUserRate, referralKey,
                            br.totalBet, br.totalBetCasino, br.totalBetSport, br.totalBetGame,
                            br.totalUserCommission, totalDistributions, new Date());
                    daily.setGameDetails(br.gameDetailsMap);

                    commissionDao.upsert(daily);
                    totalProcessed++;
                }
            }

            logger.info("CalculateAgentCommission completed: processed=" + totalProcessed
                    + " fromDate=" + fromDate + " toDate=" + toDate + " agentCode=" + agentCode);
            return BaseResponse.success(totalProcessed, 1);

        } catch (Exception e) {
            logger.error("CalculateAgentCommission error", e);
            return BaseResponse.error("-1", e.getMessage());
        }
    }
}
