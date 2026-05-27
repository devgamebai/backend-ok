package com.vinplay.api.backend.processors.agent;

import com.vinplay.payment.utils.Constant;
import com.vinplay.usercore.service.AgentTransactionsService;
import com.vinplay.usercore.service.impl.AgentTransactionsServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;
import org.apache.commons.lang.StringUtils;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class SearchAgentTransactionProccessor implements BaseProcessor<HttpServletRequest, String> {
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
    	
        String fromTime = request.getParameter("ft");
        String endTime = request.getParameter("et");
        int page = 1;
        try {
            page = Integer.parseInt(request.getParameter("pg"));
        } catch (NumberFormatException e) {
            page = 1;
        }
        
        int status = -1;
        try {
        	status = Integer.parseInt(request.getParameter("st"));
        } catch (NumberFormatException e) {
        	status = -1;
        }
        
        AgentTransactionsService service = new AgentTransactionsServiceImpl();
        try {
        	Map<String, Object> rs = service.searchWithAgentCode(agentCode, status, fromTime, endTime, page);
        	long totalRecord = Long.parseLong(rs.get("totalRecord").toString());
        	rs.remove("totalRecord");
            return BaseResponse.success(rs, totalRecord);
        }
        catch (Exception e) {
            return BaseResponse.error("-1", e.getMessage());
        }
    }
}