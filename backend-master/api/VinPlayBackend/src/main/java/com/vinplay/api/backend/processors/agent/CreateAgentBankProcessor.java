package com.vinplay.api.backend.processors.agent;

import com.vinplay.payment.utils.Constant;
import com.vinplay.usercore.entities.AgentBank;
import com.vinplay.usercore.service.AgentBankService;
import com.vinplay.usercore.service.impl.AgentBankServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;
import org.apache.commons.lang.StringUtils;
import javax.servlet.http.HttpServletRequest;

public class CreateAgentBankProcessor implements BaseProcessor<HttpServletRequest, String> {
	@Override
	public String execute(Param<HttpServletRequest> param) {
		HttpServletRequest request = param.get();
        String serPath = request.getServletPath();
        if(serPath == null || serPath.trim().isEmpty() || serPath != "/api_agent"){
            return BaseResponse.error(Constant.ERROR_PARAM, "Not allow access this api");
        }

        String code = request.getParameter("code");
        String bankAccount = request.getParameter("ba");
		String bankBranch = request.getParameter("br");
		String bankCode = request.getParameter("bc");
		String bankNumber = request.getParameter("bn");
		if(StringUtils.isBlank(bankAccount))
			return BaseResponse.error(Constant.ERROR_PARAM, "Bank account can not empty");
		
		if(StringUtils.isBlank(bankBranch))
			return BaseResponse.error(Constant.ERROR_PARAM, "Nickname can not empty");
		
		if(StringUtils.isBlank(bankCode))
			return BaseResponse.error(Constant.ERROR_PARAM, "Bank code can not empty");
		
		if(StringUtils.isBlank(bankNumber))
			return BaseResponse.error(Constant.ERROR_PARAM, "Bank number can not empty");
		
		if(StringUtils.isBlank(code))
			return BaseResponse.error(Constant.ERROR_PARAM, "Code of agent can not empty");
		
		AgentBankService service = new AgentBankServiceImpl();
		AgentBank agentBank = new AgentBank();
		agentBank.setAgent_code(code);
		agentBank.setBank_acount(bankAccount);
		agentBank.setBank_branch(bankBranch);
		agentBank.setBank_code(bankCode);
		agentBank.setBank_number(bankNumber);
		String rs = service.create(agentBank);
		return "success".equals(rs) ? BaseResponse.success("0", rs, rs) : BaseResponse.error("1001", rs);
	}
}
