package com.vinplay.api.backend.processors.agent;

import com.vinplay.dal.dao.AgentDAO;
import com.vinplay.dal.dao.UserDao;
import com.vinplay.dal.dao.impl.AgentDAOImpl;
import com.vinplay.dal.dao.impl.UserDaoImpl;
import com.vinplay.dal.entities.agent.UserAgentModel;
import com.vinplay.payment.service.RechargeManualBankService;
import com.vinplay.payment.service.impl.RechargeManualBankServiceImpl;
import com.vinplay.payment.utils.Constant;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.response.BaseResponse;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.Map;

public class SearchDepositUserToAgentProccessor implements BaseProcessor<HttpServletRequest, String> {
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

        UserAgentModel agentModel = new UserAgentModel();
        AgentDAO agentDao = new AgentDAOImpl();
        try {
            agentModel = agentDao.DetailUserAgentByCode(agentCode);
        } catch (SQLException e1) {
            e1.printStackTrace();
            agentModel = null;
        }

        if(agentModel == null)
            return BaseResponse.error(Constant.ERROR_PARAM, "Agent account is not exist");

        String nickName = request.getParameter("nn");

        UserModel userModel = new UserModel();
        UserDao userDao = new UserDaoImpl();
        try {
            userModel = userDao.getUserByNickName(nickName);
        } catch (SQLException e1) {
            e1.printStackTrace();
            userModel = null;
        }

        if(!StringUtils.isBlank(nickName) && userModel == null)
            return BaseResponse.error(Constant.ERROR_PARAM, "User account is not exist");

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

        RechargeManualBankService service = new RechargeManualBankServiceImpl();
        try {
        	Map<String, Object> rs = service.FindTransactionUserToAgent(agentModel.getNickname(), userModel != null ? userModel.getNickname() : "", status, page, maxItem, fromTime,
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