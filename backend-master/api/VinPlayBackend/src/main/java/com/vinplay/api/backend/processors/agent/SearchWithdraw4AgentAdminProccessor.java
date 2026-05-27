package com.vinplay.api.backend.processors.agent;

import com.vinplay.payment.service.WithDrawManualBankService;
import com.vinplay.payment.service.impl.WithDrawManualBankServiceImpl;
import com.vinplay.payment.utils.Constant;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;
import org.apache.commons.lang.StringUtils;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class SearchWithdraw4AgentAdminProccessor implements BaseProcessor<HttpServletRequest, String> {
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
        
        int maxItem = 10;
        try {
            maxItem = Integer.parseInt(request.getParameter("mi"));
        } catch (NumberFormatException e) {
            maxItem = 10;
        }
        
        int status = -1;
        try {
        	status = Integer.parseInt(request.getParameter("st"));
        } catch (NumberFormatException e) {
        	status = -1;
        }
        
        String nickname = request.getParameter("nn");
        WithDrawManualBankService service = new WithDrawManualBankServiceImpl();
        try {
        	Map<String, Object> rs = service.FindTransaction(nickname, agentCode, status, page, maxItem, fromTime,
        			endTime, "");
        	long totalRecord = Long.parseLong(rs.get("totalRecord").toString());
        	rs.remove("totalRecord");
            return BaseResponse.success(rs, totalRecord);
        }
        catch (Exception e) {
            return BaseResponse.error("-1", e.getMessage());
        }
    }
}