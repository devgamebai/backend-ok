package com.vinplay.api.backend.processors.agent;

import com.vinplay.dal.dao.AgentDAO;
import com.vinplay.dal.dao.impl.AgentDAOImpl;
import com.vinplay.dal.entities.agent.UserAgentModel;
import com.vinplay.dichvuthe.response.RechargePaywellResponse;
import com.vinplay.payment.service.RechargeManualBankService;
import com.vinplay.payment.service.impl.RechargeManualBankServiceImpl;
import com.vinplay.payment.utils.Constant;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;
import org.apache.commons.lang.StringUtils;
import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;

public class Deposit4AgentAdminProccessor implements BaseProcessor<HttpServletRequest, String> {
    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        String serPath = request.getServletPath();
        if(serPath == null || serPath.trim().isEmpty() || serPath != "/api_agent")
        	return BaseResponse.error(Constant.ERROR_PARAM, "Not allow access this api");
        
        String transId = request.getParameter("id");
        if(StringUtils.isBlank(transId))
			return BaseResponse.error(Constant.ERROR_PARAM, "TransactionId can not empty");
        
        String agentCode = request.getParameter("code");
        if(StringUtils.isBlank(agentCode))
			return BaseResponse.error(Constant.ERROR_PARAM, "Code of agent can not empty");
        
        String action = request.getParameter("ac");//Approved | Reject
        if(StringUtils.isBlank(action))
			return BaseResponse.error(Constant.ERROR_PARAM, "Action can not empty");
        
        if(!"Approved".equalsIgnoreCase(action) && !"Reject".equalsIgnoreCase(action))
        	return BaseResponse.error(Constant.ERROR_PARAM, "Action is invalid");
        
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
        
        if(agentModel.getActive() == 0)
        	return BaseResponse.error(Constant.ERROR_PARAM, "Agent is inactive");
        
        try {
        	RechargeManualBankService service = new RechargeManualBankServiceImpl();
            RechargePaywellResponse rs = new RechargePaywellResponse(1, 0L, 0, 0L, "");
            switch (action.toLowerCase()) {
				case "approved":
					rs = service.Approved(transId, agentModel.getNickname());
					break;
					
				case "reject":
					rs = service.Reject(transId, agentModel.getNickname());
					break;
			}
            
            if (rs.getCode() != 0) {
            	return BaseResponse.error(String.valueOf(rs.getCode()), rs.getData());
			}
        	
            return BaseResponse.success(String.valueOf(rs.getCode()), "success", "success");
        }
        catch (Exception e) {
            return BaseResponse.error("-1", e.getMessage());
        }
    }
}