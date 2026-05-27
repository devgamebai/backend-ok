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

public class DeleteAgentBankProcessor implements BaseProcessor<HttpServletRequest, String> {
	@Override
	public String execute(Param<HttpServletRequest> param) {
		HttpServletRequest request = param.get();
        String serPath = request.getServletPath();
        if(serPath == null || serPath.trim().isEmpty() || serPath != "/api_agent"){
            return BaseResponse.error(Constant.ERROR_PARAM, "Not allow access this api");
        }
        
        String idStr = request.getParameter("id");
        String code = request.getParameter("code");
		if(StringUtils.isBlank(code))
			return BaseResponse.error(Constant.ERROR_PARAM, "Code of agent can not empty");
		
		Long id = 0l;
		try {
			id = Long.parseLong(idStr);
		}catch (Exception e) {
			return BaseResponse.error(Constant.ERROR_PARAM, "Id is invalid");
		}
		
		AgentBankService service = new AgentBankServiceImpl();
		AgentBank agentBank = new AgentBank();
		agentBank = service.getById(id);
		if(agentBank == null)
			return BaseResponse.error(Constant.ERROR_PARAM, "Id is invalid");
		
		if(!agentBank.getAgent_code().equals(code))
			return BaseResponse.error(Constant.ERROR_PARAM, "Code of agent is invalid");
		
		String rs = service.Delete(id);
		return "success".equals(rs) ? BaseResponse.success("0", rs, rs) : BaseResponse.error("1001", rs);
	}
}
