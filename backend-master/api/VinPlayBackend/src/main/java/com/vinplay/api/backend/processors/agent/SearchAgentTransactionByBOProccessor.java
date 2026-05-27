package com.vinplay.api.backend.processors.agent;

import com.vinplay.usercore.service.AgentTransactionsService;
import com.vinplay.usercore.service.impl.AgentTransactionsServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class SearchAgentTransactionByBOProccessor implements BaseProcessor<HttpServletRequest, String> {
    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        String keyword = request.getParameter("key");
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
        	Map<String, Object> rs = service.search(keyword, status, fromTime, endTime, page);
            return BaseResponse.success(rs.get("transactions"), Long.parseLong(rs.get("totalRecord").toString()));
        }
        catch (Exception e) {
            return BaseResponse.error("-1", e.getMessage());
        }
    }
}