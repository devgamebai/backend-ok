package com.vinplay.api.backend.processors.slot;

import com.vinplay.usercore.service.impl.CacheServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponseModel;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;

public class ResetSlotPot implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

	public String execute(Param<HttpServletRequest> param) {
		HttpServletRequest request = param.get();
		BaseResponseModel response = new BaseResponseModel(false, "1001");
		try {
			String gameName = request.getParameter("game");
			if (gameName == null || gameName.isEmpty()) {
				return "";
			}
			return reset(gameName).toJson();
		} catch (Exception e) {
			logger.error(e);
		}
		return response.toJson();
	}
	
    BaseResponseModel reset(String gameName){
        BaseResponseModel response = new BaseResponseModel(false, "1001");
        try{
            CacheServiceImpl cacheService = new CacheServiceImpl();
            String cacheKey = "reset_pot_"+gameName +"_100";
            cacheService.setValue(cacheKey, 1);
            cacheKey = "reset_pot_"+gameName +"_1000";
            cacheService.setValue(cacheKey, 1);
            cacheKey = "reset_pot_"+gameName +"_5000";
            cacheService.setValue(cacheKey, 1);
            cacheKey = "reset_pot_"+gameName +"_10000";
            cacheService.setValue(cacheKey, 1);
            response.setSuccess(true);
            response.setErrorCode("0");
            return response;
        }catch (Exception e){
            return response;
        }
    }
}
