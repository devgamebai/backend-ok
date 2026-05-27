package com.vinplay.api.backend.processors.liveUserGame;

import com.vinplay.liveUser.entities.LiveUserGameEntity;
import com.vinplay.liveUser.service.LiveUserGameService;
import com.vinplay.liveUser.service.impl.LiveUserGameServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponseModel;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;

public class GetLiveUserGameProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");
    private LiveUserGameService service = new LiveUserGameServiceImpl();

    @Override
    public String execute(Param<HttpServletRequest> param) {
        BaseResponseModel response = new BaseResponseModel(false, "1001");
        HttpServletRequest request = param.get();
        String idRaw = request.getParameter("id");

        int id = 0;
        try {
            id =Integer.parseInt(idRaw);
        } catch (Exception e) {

        }
        if (id < 0) {
            return response.toJson();
        }

        try {
            LiveUserGameEntity data = service.get(id);
            response.setData(data);
            response.setSuccess(true);
            response.setErrorCode("0");
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.debug(e);
        }
        return response.toJson();
    }
}
