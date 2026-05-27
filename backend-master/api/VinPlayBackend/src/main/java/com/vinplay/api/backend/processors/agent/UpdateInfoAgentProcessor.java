package com.vinplay.api.backend.processors.agent;

import com.vinplay.dal.dao.AgentDAO;
import com.vinplay.dal.dao.impl.AgentDAOImpl;
import com.vinplay.dal.entities.agent.UserAgentModel;
import com.vinplay.payment.utils.Constant;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

public class UpdateInfoAgentProcessor implements BaseProcessor<HttpServletRequest, String> {
    @Override
    public String execute(Param<HttpServletRequest> param) {

    	HttpServletRequest request = param.get();
        String serPath = request.getServletPath();
        if(serPath == null || serPath.trim().isEmpty() || serPath != "/api_agent"){
            return BaseResponse.error(Constant.ERROR_PARAM, "Not allow access this api");
        }

        String nickname = request.getParameter("nn");
        String nameagent = request.getParameter("na");
        String address = request.getParameter("adr");
        String phone = request.getParameter("ph");
        String email = request.getParameter("em");
        String facebook = request.getParameter("fa");
        if(StringUtils.isBlank(nickname))
        	return BaseResponse.error(Constant.ERROR_PARAM, "Nickname can not empty");
        
        AgentDAO dao = new AgentDAOImpl();
        UserAgentModel userAgentModel = new  UserAgentModel();
		try {
			userAgentModel = dao.DetailUserAgentByNickName(nickname);
		} catch (SQLException e1) {
			userAgentModel = null;
			e1.printStackTrace();
		}
        if(userAgentModel == null)
        	return BaseResponse.error(Constant.ERROR_PARAM, "Can not found agent");
        
        if(StringUtils.isBlank(userAgentModel.getKey()))
        	userAgentModel.setKey("A" + userAgentModel.getLevel());
        
        if(StringUtils.isBlank(userAgentModel.getStatus()))
        	userAgentModel.setStatus("A");
        
        if(userAgentModel.getNamebank() == null)
        	userAgentModel.setNamebank("");
        
        if(userAgentModel.getNameaccount() == null)
        	userAgentModel.setNameaccount("");
        
        if(userAgentModel.getNumberaccount() == null)
        	userAgentModel.setNumberaccount("");
        
        userAgentModel.setNameagent(nameagent);
        userAgentModel.setAddress(address);
        userAgentModel.setPhone(phone);
        userAgentModel.setEmail(email);
        userAgentModel.setFacebook(facebook);
        userAgentModel.setUpdatetime(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault())
	    	      .toInstant()));
        try {
            Boolean check = dao.UpdateUserAgent(userAgentModel);
            if(check){
                return BaseResponse.success("", "success", null);
            }else {
                return BaseResponse.error("-1", "Update information failure");
            }
        }
        catch (Exception e) {
            return BaseResponse.error("-1", e.getMessage());
        }
    }
}