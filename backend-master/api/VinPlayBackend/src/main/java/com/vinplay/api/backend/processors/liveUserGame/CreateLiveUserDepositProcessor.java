package com.vinplay.api.backend.processors.liveUserGame;

import com.vinplay.liveUser.dao.LiveUserDepositDAO;
import com.vinplay.liveUser.dao.LiveUserGameDAO;
import com.vinplay.liveUser.dao.impl.LiveUserDepositDAOImpl;
import com.vinplay.liveUser.dao.impl.LiveUserGameDAOImpl;
import com.vinplay.liveUser.entities.LiveUserDepositEntity;
import com.vinplay.liveUser.entities.LiveUserGameEntity;
import com.vinplay.payment.utils.Constant;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;
import com.vinplay.vbee.common.response.BaseResponseModel;
import org.apache.log4j.Logger;
import org.apache.logging.log4j.util.Strings;

import javax.servlet.http.HttpServletRequest;
import java.util.Calendar;
import java.util.Random;

public class CreateLiveUserDepositProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");
    LiveUserDepositDAO service = new LiveUserDepositDAOImpl();
    private LiveUserGameDAO liveUserGameDAO = new LiveUserGameDAOImpl();
    private Random rand = new Random();

    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        BaseResponseModel response = new BaseResponseModel(false, "1001");

        String actionName = request.getParameter("ac");
        if (Strings.isBlank(actionName)) {
            return BaseResponse.error(Constant.ERROR_PARAM, "actionName is null or empty");
        }

        String nickname = request.getParameter("nn");
        if (Strings.isBlank(nickname)) {
            return BaseResponse.error(Constant.ERROR_PARAM, "nickname is null or empty");
        }

        String fid = request.getParameter("fid");
        if (Strings.isBlank(fid)) {
            return BaseResponse.error(Constant.ERROR_PARAM, "fid is null or empty");
        }

        String type = request.getParameter("ty");
        if (Strings.isBlank(type)) {
            return BaseResponse.error(Constant.ERROR_PARAM, "type is null or empty");
        }

        String msg = request.getParameter("msg");
        if (Strings.isBlank(msg)) {
            return BaseResponse.error(Constant.ERROR_PARAM, "msg is null or empty");
        }

        int money = 0;
        try {
            money = Integer.valueOf(request.getParameter("mn"));
        } catch (Exception e) {

        }
        if (money < 0) {
            return BaseResponse.error(Constant.ERROR_PARAM, "money is 0 or empty");
        }

        try {
            LiveUserGameEntity userInfo = liveUserGameDAO.getByNickname(nickname);
            if (userInfo == null) {
                return BaseResponse.error(Constant.ERROR_USERTYPE, "not found user");
            }
            if (!userInfo.getActive()) {
                return BaseResponse.error(Constant.ERROR_USER_BAN, "user not enable");
            }
            LiveUserDepositEntity deposit = new LiveUserDepositEntity();
            deposit.setNick_name(nickname);
            deposit.setCash(money);
            deposit.setAction_name(actionName);
            deposit.setFid(fid);
            deposit.setType(type);
            deposit.setMsgSuccess(msg);
            deposit.setRun(false);


            int nextTime = rand.nextInt(20);
            Calendar time = Calendar.getInstance();
            time.add(Calendar.SECOND, 20 + nextTime);
            deposit.setDeposit_at(time.getTime());

            boolean ok = service.create(deposit);
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
