package com.vinplay.api.backend.processors.liveUserGame;

import com.vinplay.liveUser.entities.LiveUserGameEntity;
import com.vinplay.liveUser.service.LiveUserGameService;
import com.vinplay.liveUser.service.impl.LiveUserGameServiceImpl;
import com.vinplay.payment.utils.Constant;
import com.vinplay.usercore.service.UserService;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.response.BaseResponse;
import com.vinplay.vbee.common.response.BaseResponseModel;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;

public class UpdateLiveUserGameProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");
    private LiveUserGameService service = new LiveUserGameServiceImpl();
    private UserService userService = new UserServiceImpl();

    @Override
    public String execute(Param<HttpServletRequest> param) {

        BaseResponseModel response = new BaseResponseModel(false, "1001");

        if (!"POST".equalsIgnoreCase(param.get().getMethod())) {
            response.setMessage( "METHOD_NOT_ENABLE");
            return response.toJson();
        }
        String jsonString = "";
        try {
            jsonString = IOUtils.toString(param.get().getReader());
            JSONObject obj = new JSONObject(jsonString);

            String nickName = obj.getString("nick_name");
            if (Strings.isBlank(nickName)) {
                return BaseResponse.error(Constant.ERROR_PARAM, "nick_name is null or empty");
            }

            String active = obj.getString("active");
            if (Strings.isBlank(active)) {
                return BaseResponse.error(Constant.ERROR_PARAM, "active is null or empty");
            }

            String expired = obj.getString("expired");
            if (Strings.isBlank(active)) {
                return BaseResponse.error(Constant.ERROR_PARAM, "expired is null or empty");
            }

            String action_by =  obj.getString("action_by");

            UserModel userInfo = userService.getUserByNickName(nickName);
            if (userInfo == null) {
                return BaseResponse.error(Constant.ERROR_PARAM, "not found user");
            }

            LiveUserGameEntity userLive = new LiveUserGameEntity();
            userLive.setNick_name(nickName);
            if (active.trim().toLowerCase().contains("true")) {
                userLive.setActive(true);
            }

            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            userLive.setExpired_at(format.parse(expired));


            boolean ok =  service.update(userLive, action_by);
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
