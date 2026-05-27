package com.vinplay.api.backend.processors.login;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.usercore.service.UserLoginActivityService;
import com.vinplay.usercore.service.impl.UserLoginActivityServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.response.UserLoginLogModel;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListUserLoginLogsProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        String at = request.getParameter("at");
        if (at == null || at.isEmpty()) {
            at = request.getParameter("aat");
        }
        String username = request.getParameter("username");
        String dateFrom = request.getParameter("date_from");
        String dateTo = request.getParameter("date_to");
        String _page = request.getParameter("page");
        String _limit = request.getParameter("limit");

        int page = 1;
        int limit = 20;

        Map<String, Object> response = new HashMap<>();

        try {
            if (_page != null && !_page.isEmpty()) {
                page = Integer.parseInt(_page);
            }
            if (_limit != null && !_limit.isEmpty()) {
                limit = Integer.parseInt(_limit);
                if (limit > 100) limit = 100;
            }

            UserLoginActivityService service = new UserLoginActivityServiceImpl();
            List<UserLoginLogModel> data = service.searchLoginLogs(username, dateFrom, dateTo, page, limit);
            long total = service.countLoginLogs(username, dateFrom, dateTo);

            response.put("success", true);
            response.put("data", data);
            response.put("page", page);
            response.put("limit", limit);
            response.put("total", total);
            return new ObjectMapper().writeValueAsString(response);

        } catch (Exception e) {
            logger.error("ListUserLoginLogsProcessor Exception: ", e);
            try {
                response.put("success", false);
                response.put("errorCode", "System Error");
                return new ObjectMapper().writeValueAsString(response);
            } catch (Exception jsonEx) {
                return "{\"success\":false,\"errorCode\":\"1001\"}";
            }
        }
    }
}
