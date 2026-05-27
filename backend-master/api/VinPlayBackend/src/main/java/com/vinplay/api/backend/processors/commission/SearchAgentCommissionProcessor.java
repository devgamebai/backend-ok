package com.vinplay.api.backend.processors.commission;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.vinplay.dal.dao.impl.AgentCommissionDaoImpl;
import com.vinplay.dal.entities.agent.AgentCommissionDaily;
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
import java.util.TreeMap;

public class SearchAgentCommissionProcessor implements BaseProcessor<HttpServletRequest, String> {

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
            AgentCommissionDaoImpl dao = new AgentCommissionDaoImpl();
            long total = dao.countByAgent(agentCode, fromDate, toDate);
            List<AgentCommissionDaily> records = dao.searchByAgent(agentCode, fromDate, toDate, page, limit);

            long summaryTotalCommission = 0;
            long summaryTotalBet = 0;
            List<Map<String, Object>> dataList = new ArrayList<>();

            for (AgentCommissionDaily record : records) {
                long commissionForAgent = 0;
                double earnRate = 0;
                if (record.getDistributions() != null) {
                    for (AgentCommissionDaily.AgentDistribution dist : record.getDistributions()) {
                        if (agentCode.equals(dist.getAgentCode())) {
                            commissionForAgent = dist.getCommission();
                            earnRate = dist.getEarnRate();
                            break;
                        }
                    }
                }

                Map<String, Object> row = new LinkedHashMap<>();
                row.put("date", record.getDate());
                row.put("userNickname", record.getUserNickname());
                row.put("referralCode", record.getReferralCode());
                row.put("totalBet", record.getTotalBet());
                row.put("totalBetCasino", record.getTotalBetCasino());
                row.put("totalBetSport", record.getTotalBetSport());
                row.put("totalBetGame", record.getTotalBetGame());
                row.put("earnRate", earnRate);
                row.put("commission", commissionForAgent);
                dataList.add(row);

                summaryTotalCommission += commissionForAgent;
                summaryTotalBet += record.getTotalBet();
            }

            Map<String, Object> summary = new HashMap<>();
            summary.put("totalCommission", summaryTotalCommission);
            summary.put("totalBet", summaryTotalBet);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("total", total);
            result.put("data", dataList);
            result.put("summary", summary);

            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            String json = ow.writeValueAsString(result);
            return new BaseResponse<String>().success(json);

        } catch (Exception e) {
            logger.error("SearchAgentCommission error", e);
            return BaseResponse.error("-1", e.getMessage());
        }
    }
}
