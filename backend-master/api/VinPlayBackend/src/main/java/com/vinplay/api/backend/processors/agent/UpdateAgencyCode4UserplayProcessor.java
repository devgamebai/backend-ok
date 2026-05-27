package com.vinplay.api.backend.processors.agent;

import com.vinplay.dal.dao.AgentDAO;
import com.vinplay.dal.dao.impl.AgentDAOImpl;
import com.vinplay.payment.utils.Constant;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;

import javax.servlet.http.HttpServletRequest;

public class UpdateAgencyCode4UserplayProcessor implements BaseProcessor<HttpServletRequest, String> {
    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
//        String serPath = request.getServletPath();
//        if(serPath == null || serPath.trim().isEmpty() || serPath != "/api_agent"){
//            return BaseResponse.error(Constant.ERROR_PARAM, "Not allow access this api");
//        }


        String nickname = request.getParameter("nn");
        String agencyCode = request.getParameter("rc");
        if(nickname == null || nickname.trim().isEmpty())
        	return BaseResponse.error(Constant.ERROR_PARAM, "Nickname not empty");
        
        if(agencyCode == null || agencyCode.trim().isEmpty())
        	return BaseResponse.error(Constant.ERROR_PARAM, "Agency code not empty");
        
        AgentDAO dao = new AgentDAOImpl();
        try {
            String result = dao.updateAgencyCode4Userplay(nickname, agencyCode);
            if(result == "success")
                return BaseResponse.success("", "Update thành công", null);
            
            return BaseResponse.error("-1", result);    
        }
        catch (Exception e) {
            return BaseResponse.error("-1", e.getMessage());
        }
    }
}