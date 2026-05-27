package com.vinplay.api.backend.processors.agent;

import com.vinplay.dal.dao.AgentDAO;
import com.vinplay.dal.dao.impl.AgentDAOImpl;
import com.vinplay.dal.entities.agent.UserAgentModel;
import com.vinplay.payment.service.RechargeManualBankService;
import com.vinplay.payment.service.impl.RechargeManualBankServiceImpl;
import com.vinplay.payment.utils.Constant;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;

public class UpdateDepositUserToAgentProccessor implements BaseProcessor<HttpServletRequest, String> {
    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        String serPath = request.getServletPath();
        if(serPath == null || serPath.trim().isEmpty() || serPath != "/api_agent"){
            return BaseResponse.error(Constant.ERROR_PARAM, "Not allow access this api");
        }

        String txid = request.getParameter("tx");
        String agentNickname = request.getParameter("an");
        int status = Integer.parseInt(request.getParameter("st"));
        String content = request.getParameter("mm");

        if(StringUtils.isBlank(txid))
            return BaseResponse.error(Constant.ERROR_PARAM, "TransId can not empty");

        if(StringUtils.isBlank(agentNickname))
			return BaseResponse.error(Constant.ERROR_PARAM, "Agent nickname can not empty");

        UserAgentModel agentModel = new UserAgentModel();
        AgentDAO agentDao = new AgentDAOImpl();
        try {
            agentModel = agentDao.DetailUserAgentByNickName(agentNickname);
        } catch (SQLException e1) {
            e1.printStackTrace();
            agentModel = null;
        }

        if(agentModel == null)
            return BaseResponse.error(Constant.ERROR_PARAM, "Agent account is not exist");

        RechargeManualBankService service = new RechargeManualBankServiceImpl();
        try {
        	Object rs = service.UpdateTransactionDetail(agentModel.getNickname(), txid, content, status);

            return BaseResponse.success(rs, 1);
        }
        catch (Exception e) {
            return BaseResponse.error("-1", e.getMessage());
        }
    }
}