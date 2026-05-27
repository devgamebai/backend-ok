package com.vinplay.api.backend.processors.agent;

import com.vinplay.dal.dao.AgentDAO;
import com.vinplay.dal.dao.impl.AgentDAOImpl;
import com.vinplay.dal.entities.agent.UserDetailAgentModel;
import com.vinplay.payment.utils.Constant;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;
import org.json.JSONArray;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public class SearchLogUserDetailProccessor implements BaseProcessor<HttpServletRequest, String> {
    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
//        String serPath = request.getServletPath();
//        if(serPath == null || serPath.trim().isEmpty() || serPath != "/api_agent"){
//            return BaseResponse.error(Constant.ERROR_PARAM, "Not allow access this api");
//        }

        String nick_name = request.getParameter("nn");
        String fromTime = request.getParameter("ft");
        String endTime = request.getParameter("et");

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

        AgentDAO dao = new AgentDAOImpl();
        try {
            long total = dao.totalLogUserDetail(nick_name, fromTime, endTime);
            List<Object> data = dao.getLogUserDetail(nick_name, fromTime, endTime, page, maxItem);
            return BaseResponse.success(data, total);
        }
        catch (Exception e) {
            return BaseResponse.error("-1", e.getMessage());
        }
    }
}