package com.vinplay.api.backend.processors.agent;

import com.vinplay.dal.dao.AgentDAO;
import com.vinplay.dal.dao.impl.AgentDAOImpl;
import com.vinplay.dal.entities.agent.UserAgentModel;
import com.vinplay.payment.dao.AgentTransactionsDao;
import com.vinplay.payment.dao.impl.AgentTransactionsDaoImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.AgentResponse;
import com.vinplay.vbee.common.response.BaseResponse;
import org.bson.Document;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class GetTotalTransactionAgentProccessor implements BaseProcessor<HttpServletRequest, String> {
    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();

        AgentDAO agentDAO = new AgentDAOImpl();
        AgentTransactionsDao transDao = new AgentTransactionsDaoImpl();
        try {
            // Parse input
            String nickname = request.getParameter("nn");
            String fromTime = request.getParameter("ft");
            String endTime = request.getParameter("et");

            int parentId = -1;

            if (nickname != null) {
                // Get current agent
                UserAgentModel agentModel = agentDAO.DetailUserAgentByNickName(nickname);
                if (agentModel == null) {
                    return BaseResponse.error("-1", "Agent not found");
                }

                parentId = agentModel.getId();
            }

            // Get agent list
            List<AgentResponse> agentList = agentDAO.listAgentByParent(parentId);
            List<AgentResponse> agentAllList = agentDAO.listAllAgent();
            // Get transaction IN
            List<Document> transIn = transDao.getTotalTransferInTransaction(agentAllList, fromTime, endTime);
            // Get transaction OUT
            List<Document> transOut = transDao.getTotalTransferOutTransaction(agentAllList, fromTime, endTime);
            // Mapping data to agent list
            for (AgentResponse agent: agentList) {
                Document transInItem = transIn.stream().filter(item -> item.get("_id").equals(agent.nickName)).findAny().orElse(null);
                if (transInItem != null) {
                    agent.total_transfer_in = (long) transInItem.get("total_transfer");
                } else {
                    agent.total_transfer_in = 0;
                }

                Document transOutItem = transOut.stream().filter(item -> item.get("_id").equals(agent.nickName)).findAny().orElse(null);
                if (transOutItem != null) {
                    agent.total_transfer_out = (long) transOutItem.get("total_transfer");
                } else {
                    agent.total_transfer_out = 0;
                }

                agent.total_transfer = agent.total_transfer_in + agent.total_transfer_out;
            }
            // Sort data
            Collections.sort(agentList, new Comparator<AgentResponse>() {
                @Override
                public int compare(AgentResponse o1, AgentResponse o2) {
                    if (o2.total_transfer > o1.total_transfer) {
                        return 1;
                    } else if (o2.total_transfer < o1.total_transfer) {
                        return -1;
                    }
                    return 0;
                }
            });

            return BaseResponse.success(agentList, agentList.size());
        }
        catch (Exception e) {
            return BaseResponse.error("-1", e.getMessage());
        }
    }
}