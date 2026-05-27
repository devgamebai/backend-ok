package com.vinplay.api.backend.processors.agent;

import com.vinplay.payment.utils.Constant;
import com.vinplay.usercore.service.AgentBankService;
import com.vinplay.usercore.service.impl.AgentBankServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;
import org.apache.commons.lang.StringUtils;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class SearchAgentBankProccessor implements BaseProcessor<HttpServletRequest, String> {
    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        String serPath = request.getServletPath();
        if(serPath == null || serPath.trim().isEmpty() || serPath != "/api_agent"){
            return BaseResponse.error(Constant.ERROR_PARAM, "Not allow access this api");
        }

        String keyword = request.getParameter("key");
        String agentCode = request.getParameter("code");
        if(StringUtils.isBlank(agentCode))
			return BaseResponse.error(Constant.ERROR_PARAM, "Code of agent can not empty");

        int page, maxItem;
        try {
            page = Integer.parseInt(request.getParameter("pg"));
        } catch (NumberFormatException e) {
            page = 1;
        }

        try {
            maxItem = Integer.parseInt(request.getParameter("mi"));
        } catch (NumberFormatException e) {
            maxItem = 10;
        }

        AgentBankService service = new AgentBankServiceImpl();
        try {
        	Map<String, Object> rs = service.search(keyword, agentCode, page, maxItem);
            return BaseResponse.success(rs.get("agentBanks"), Long.parseLong(rs.get("total").toString()));
        }
        catch (Exception e) {
            return BaseResponse.error("-1", e.getMessage());
        }
    }
}