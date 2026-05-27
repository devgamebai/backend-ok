package com.vinplay.api.backend.processors.agent;

import com.vinplay.api.backend.response.ReportLogUserResponse;
import com.vinplay.dal.dao.AgentDAO;
import com.vinplay.dal.dao.impl.AgentDAOImpl;
import com.vinplay.payment.utils.Constant;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExportUsersProcessor implements BaseProcessor<HttpServletRequest, String> {
    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        ReportLogUserResponse res = new ReportLogUserResponse(false, "1001");
//        String serPath = request.getServletPath();
//        if(serPath == null || serPath.trim().isEmpty() || serPath != "/api_agent"){
//            return BaseResponse.error(Constant.ERROR_PARAM, "Not allow access this api");
//        }

        String refcode = request.getParameter("rc");
        if(refcode == null || refcode.trim().isEmpty()){
            return BaseResponse.error(Constant.ERROR_PARAM, "Code of agency not empty");
        }

        int page = 1, maxItem = 10;
        try {
            page = Integer.parseInt(request.getParameter("pg"));
        } catch (NumberFormatException e) {
            page = -1;
        }
        
        try {
            maxItem = Integer.parseInt(request.getParameter("mi"));
        } catch (NumberFormatException e) {
            maxItem = -1;
        }
        
        List<Map<String, Object>> data = new ArrayList<>();
        try {
            AgentDAO dao = new AgentDAOImpl();
            data = dao.getUsers4Agent(refcode, page, maxItem);
            if (data == null || data.size() == 0)
                return res.toJson();

            res.total = Integer.parseInt(data.get(data.size() - 1).get("total").toString());
            data.remove(data.size() - 1);
            res.setData(data);
            res.setErrorCode("0");
            res.setSuccess(true);
            return res.toJson();
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"success\":false,\"errorCode\":\"1001\"}";
        }
    }
}

