package com.vinplay.api.backend.processors.liveUserGame;

import com.vinplay.liveUser.service.LiveUserGameService;
import com.vinplay.liveUser.service.impl.LiveUserGameServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponseModel;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;

public class DeleteLiveUserGameProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");
    private LiveUserGameService service = new LiveUserGameServiceImpl();

    @Override
    public String execute(Param<HttpServletRequest> param) {
        BaseResponseModel response = new BaseResponseModel(false, "1001");
        HttpServletRequest request = param.get();
        String idStr = request.getParameter("id");
        String userAction = request.getParameter("ua");

        try {

            int id = Integer.parseInt(idStr);
            boolean ok =  service.delete(id, userAction);
            if (ok) {
                response.setSuccess(true);
                response.setErrorCode("0");
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e);
            response.setMessage(e.getMessage());
        }

        return response.toJson();
    }
}
