package com.vinplay.api.backend.processors.slot;

import com.vinplay.usercore.service.impl.CacheServiceImpl;
import com.vinplay.usercore.service.impl.OtpServiceImpl;
import com.vinplay.usercore.service.impl.SecurityServiceImpl;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponseModel;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;

public class SetSlotJackpot implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        BaseResponseModel response = new BaseResponseModel(false, "1001");
        try {
            String nickname = request.getParameter("nickname");
            String action = request.getParameter("act");
            String gameName = request.getParameter("game");
            if(nickname == null || nickname.isEmpty() || action == null || action.isEmpty() || gameName == null || gameName.isEmpty()){
                return "";
            }

            if(action.equals("add")){
                return addUserToJackpotGame(nickname, gameName).toJson();
            }
            if(action.equals("remove")){
                return RemoveUserToJackpotGame(gameName).toJson();
            }



        }
        catch (Exception e) {
            logger.debug((Object)e);
        }
        return response.toJson();
    }
    BaseResponseModel addUserToJackpotGame(String nickname, String gameName){
        BaseResponseModel response = new BaseResponseModel(false, "1001");
        String cacheKey = "user_force_jackpot_"+gameName;
        try{
            CacheServiceImpl cacheService = new CacheServiceImpl();
            String userForce = "";
            try{
                userForce = cacheService.getValueStr(cacheKey);
            }catch (Exception e){

            }
            if(!userForce.isEmpty()){
                response.setErrorCode("1002");
                response.setSuccess(false);
                return response;
            }
            cacheService.setValue(cacheKey, nickname);
            response.setSuccess(true);
            response.setErrorCode("0");
            return response;
        }catch (Exception e){

            return response;
        }
    }
    BaseResponseModel RemoveUserToJackpotGame(String gameName){
        BaseResponseModel response = new BaseResponseModel(false, "1001");
        String cacheKey = "user_force_jackpot_"+gameName;
        try{
            CacheServiceImpl cacheService = new CacheServiceImpl();

            cacheService.removeKey(cacheKey);
            response.setSuccess(true);
            response.setErrorCode("0");
            return response;
        }catch (Exception e){

            return response;
        }
    }
}
