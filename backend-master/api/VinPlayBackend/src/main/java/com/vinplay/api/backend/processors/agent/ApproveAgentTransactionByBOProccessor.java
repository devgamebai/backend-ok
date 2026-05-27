package com.vinplay.api.backend.processors.agent;

import com.vinplay.dal.dao.AgentDAO;
import com.vinplay.dal.dao.impl.AgentDAOImpl;
import com.vinplay.dal.entities.agent.UserAgentModel;
import com.vinplay.payment.entities.AgentTransaction;
import com.vinplay.payment.utils.Constant;
import com.vinplay.payment.utils.PayCommon.PAYSTATUS;
import com.vinplay.usercore.service.AgentTransactionsService;
import com.vinplay.usercore.service.UserService;
import com.vinplay.usercore.service.impl.AgentTransactionsServiceImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import com.vinplay.vbee.common.response.BaseResponse;
import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.rmq.RMQPublishTask;

import org.apache.commons.lang.StringUtils;
import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.Date;

public class ApproveAgentTransactionByBOProccessor implements BaseProcessor<HttpServletRequest, String> {
    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        String transId = request.getParameter("id");
        if(StringUtils.isBlank(transId))
			return BaseResponse.error(Constant.ERROR_PARAM, "TransactionId can not empty");
        
        String agentCode = request.getParameter("code");
        if(StringUtils.isBlank(agentCode))
			return BaseResponse.error(Constant.ERROR_PARAM, "Code of agent can not empty");
        
        String userApproved = request.getParameter("uap");
        if(StringUtils.isBlank(userApproved))
			return BaseResponse.error(Constant.ERROR_PARAM, "User approved can not empty");
        
        String description = request.getParameter("des");
        if(StringUtils.isBlank(description))
        	description = "Approved by: " + userApproved + " at: " + new Date();
        
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
        
        AgentTransactionsService service = new AgentTransactionsServiceImpl();
        AgentTransaction transaction = new AgentTransaction();
        transaction = service.getById(transId);
        if(transaction == null)
        	return BaseResponse.error(Constant.ERROR_PARAM, "TransactionId is invalid");
        
        if(transaction.Status != PAYSTATUS.PENDING.getId() && transaction.Status != PAYSTATUS.RECEIVED.getId())
        	return BaseResponse.error("1003", "Can not approve this transaction");
        
        if(transaction.IsDeleted == true)
        	return BaseResponse.error("1003", "Can not approve this transaction");
        
        transaction.Status = PAYSTATUS.SUCCESS.getId();
        transaction.UserApprove = agentModel.getNickname();
        try {
        	
        	String rs = service.updateStatus(transId, PAYSTATUS.SUCCESS.getId(), description, userApproved);
        	if("success".equalsIgnoreCase(rs)) {
        		transaction = service.getById(transId);
        		BaseResponseModel baseResponseModel = addMoneyAgentTopup(transaction, transaction.Money);
        		if (baseResponseModel.isSuccess()) {
        			return BaseResponse.success("0", rs, rs);
				}
        		
        		return BaseResponse.error("1003", "Can not topup money for agent. Details: " + baseResponseModel.getMessage());
        	}
        	else
        		return BaseResponse.error("1001", rs);
        }
        catch (Exception e) {
            return BaseResponse.error("-1", e.getMessage());
        }
    }
    
    public BaseResponseModel addMoneyAgentTopup(AgentTransaction transaction, long money) {
		UserService userService = new UserServiceImpl();
		return userService.updateMoneyFromAdmin(transaction.Nickname, money,
				"vin", Games.AGENT_TOPUP.getName(), Games.AGENT_TOPUP.getId() + "", transaction.toJson());
	}
}