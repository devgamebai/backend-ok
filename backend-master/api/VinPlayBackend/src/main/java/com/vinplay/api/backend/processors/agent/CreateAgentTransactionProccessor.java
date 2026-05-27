package com.vinplay.api.backend.processors.agent;

import com.vinplay.dal.dao.AgentDAO;
import com.vinplay.dal.dao.impl.AgentDAOImpl;
import com.vinplay.dal.entities.agent.UserAgentModel;
import com.vinplay.payment.entities.AgentTransaction;
import com.vinplay.payment.utils.Constant;
import com.vinplay.payment.utils.PayCommon.PAYSTATUS;
import com.vinplay.usercore.service.AgentTransactionsService;
import com.vinplay.usercore.service.impl.AgentTransactionsServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;
import org.apache.commons.lang.StringUtils;
import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;

public class CreateAgentTransactionProccessor implements BaseProcessor<HttpServletRequest, String> {
    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        String serPath = request.getServletPath();
        if(serPath == null || serPath.trim().isEmpty() || serPath != "/api_agent"){
            return BaseResponse.error(Constant.ERROR_PARAM, "Not allow access this api");
        }

        String agentCode = request.getParameter("code");
        if(StringUtils.isBlank(agentCode))
			return BaseResponse.error(Constant.ERROR_PARAM, "Code of agent can not empty");
        
        String fromBankNumber = request.getParameter("fbn");
        if(StringUtils.isBlank(fromBankNumber))
			return BaseResponse.error(Constant.ERROR_PARAM, "Bank number deposit can not empty");
        
        String toBankNumber = request.getParameter("tbn");
        if(StringUtils.isBlank(toBankNumber))
			return BaseResponse.error(Constant.ERROR_PARAM, "Bank number target can not empty");
        
        long money = 0;
        try {
        	money = Long.parseLong(request.getParameter("m"));
        } catch (NumberFormatException e) {
        	money = 0;
        }
        
        if(money == 0)
        	return BaseResponse.error(Constant.ERROR_PARAM, "Money is invalid");
        
        AgentDAO agentDao = new AgentDAOImpl();
        UserAgentModel agentModel;
		try {
			agentModel = agentDao.DetailUserAgentByCode(agentCode);
		} catch (SQLException e1) {
			e1.printStackTrace();
			agentModel = null;
		}
        if(agentModel == null)
        	return BaseResponse.error(Constant.ERROR_PARAM, "Agent code is invalid");
        
        String content = request.getParameter("ct");
        if(StringUtils.isBlank(content))
        	content = "Topup: " + money + ". Banking from: " + fromBankNumber + " to " + toBankNumber;
        
        AgentTransactionsService service = new AgentTransactionsServiceImpl();
        AgentTransaction transaction = new AgentTransaction();
        transaction.AgentCode = agentCode;
        transaction.AgentId = agentModel.getId().toString();
        transaction.Bonus = 0;
        transaction.Description = "";
        transaction.Fee = 0;
        transaction.FromBankNumber = fromBankNumber;
        transaction.Id = "";
        transaction.IsDeleted = false;
        transaction.Money = money;
        transaction.Nickname = agentModel.getNickname();
        transaction.Point = money;
        transaction.Status = PAYSTATUS.PENDING.getId();
        transaction.ToBankNumber = toBankNumber;
        transaction.UserApprove = "";
        transaction.Username = agentModel.getUsername();
        transaction.Content = content;
        try {
        	String rs = service.create(transaction);
        	if("success".equalsIgnoreCase(rs))
        		return BaseResponse.success("0", rs, rs);
        	else
        		return BaseResponse.error("1001", rs);
        }
        catch (Exception e) {
            return BaseResponse.error("-1", e.getMessage());
        }
    }
}