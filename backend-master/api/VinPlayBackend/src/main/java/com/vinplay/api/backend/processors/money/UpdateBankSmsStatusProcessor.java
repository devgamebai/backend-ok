package com.vinplay.api.backend.processors.money;

import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponseModel;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;

public class UpdateBankSmsStatusProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger((String) "backend");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest) param.get();
        BaseResponseModel res = new BaseResponseModel(false, String.valueOf(1001));
        try {
            String id = request.getParameter("id");
            String status = request.getParameter("status");

            if(id == null || id.isEmpty() || status == null || status.isEmpty()){
                return res.toJson();
            }

            UserServiceImpl service = new UserServiceImpl();
            boolean result = service.updateBankSmsStatus(Integer.valueOf(status), Long.valueOf(id));
            res.setSuccess(result);
        } catch (Exception e) {
            logger.debug((Object) e);
        }
        return res.toJson();
    }
}
