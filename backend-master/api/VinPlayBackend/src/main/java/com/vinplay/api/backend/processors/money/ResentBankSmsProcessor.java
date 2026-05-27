package com.vinplay.api.backend.processors.money;

import com.vinplay.api.backend.report.utils.BackendUtils;
import com.vinplay.usercore.entities.TransferMoneyResponse;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.response.BaseResponseModel;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;

public class ResentBankSmsProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger((String) "backend");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest) param.get();
        BaseResponseModel res = new BaseResponseModel(false, String.valueOf(1001));
        try {
            String id = request.getParameter("id");
            String nickname = request.getParameter("nickname");
            String amount = request.getParameter("amount");
            String ann = "bankingttbv";

            if(id == null || id.isEmpty() || nickname == null || nickname.isEmpty()){
                return res.toJson();
            }

            UserServiceImpl service = new UserServiceImpl();
            UserModel user = service.getUserByNickName(ann);
            if(user == null || user.getDaily() != 1){
                return res.toJson();
            }

            int count = service.countBankSmsLst(id, null, null, null, null, -1, "", "");
            if (count == 0) {
                return res.toJson();
            }

            long money = Long.valueOf(amount);
            int code = 1;
            if (nickname != null && !nickname.isEmpty() && money > 0) {
                ann = user.getNickname();
                TransferMoneyResponse moneyres2 = service.transferMoney(ann, nickname, money, "Resent Bank SMS", false);
                code = moneyres2.getCode();
                if (code == 0) {
                    res.setSuccess(true);
                    res.setErrorCode("0");
                }
                if (code == 0) {
                    ArrayList<String> listUser = new ArrayList<>();
                    listUser.add(nickname);
                    listUser.add(ann);
                    BackendUtils.sendUpdateUserMoneyInfo(listUser);
                }
            }
            service.updateBankSmsStatus(nickname, res.isSuccess() ? 3 : 0, Long.valueOf(id));
            res.setErrorCode(String.valueOf(code));
        } catch (Exception e) {
            logger.debug((Object) e);
        }
        return res.toJson();
    }
}
