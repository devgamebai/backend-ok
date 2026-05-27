/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.entities.TransferMoneyResponse
 *  com.vinplay.usercore.service.impl.OtpServiceImpl
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.models.UserModel
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors.money;

import com.vinplay.api.backend.report.utils.BackendUtils;
import com.vinplay.api.backend.response.TransferMoneyDaiLyResponse;
import com.vinplay.usercore.entities.TransferMoneyResponse;
import com.vinplay.usercore.service.impl.OtpServiceImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.models.UserModel;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

import java.util.ArrayList;

public class ChuyenTienDaiLyProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        int code = 1;
        TransferMoneyDaiLyResponse res = new TransferMoneyDaiLyResponse(false, String.valueOf(code));
        try {
            UserServiceImpl service = new UserServiceImpl();
            UserModel user;
            String nicknameSend = request.getParameter("nns");
            Long money = Long.parseLong(request.getParameter("mn"));
            String nicknameReceive = request.getParameter("nnr");
            String reason = request.getParameter("rs");
            String otp = request.getParameter("otp");
            String type = request.getParameter("type");
            if (!(nicknameSend == null || nicknameReceive == null || nicknameSend.isEmpty() || nicknameReceive.isEmpty() || money <= 0L || reason == null || reason.isEmpty() || (user = service.getUserByNickName(nicknameSend)) == null || user.getDaily() != 1)) {
                nicknameSend = user.getNickname();
                if (!user.isHasMobileSecurity()) {
                    TransferMoneyResponse moneyres = service.transferMoney(nicknameSend, nicknameReceive, money.longValue(), reason, true);
                    code = moneyres.getCode();
                    if (code == 0) {
                        res.setMoneyReceive(moneyres.getMoneyReceive());
                        res.setSuccess(true);
                    }
                } else if (type != null && (type.equals("1") || type.equals("0"))) {
                    if (otp.length() == 6) {
                        OtpServiceImpl otpService = new OtpServiceImpl();
                        int otpCode = otpService.checkOtp(otp, nicknameSend, type, (String)null);
                        if (otpCode == 0) {
                            TransferMoneyResponse moneyres2 = service.transferMoney(nicknameSend, nicknameReceive, money.longValue(), reason, false);
                            code = moneyres2.getCode();
                            if (code == 0) {
                                res.setMoneyReceive(moneyres2.getMoneyReceive());
                                res.setSuccess(true);
                            }
                        } else if (otpCode == 3) {
                            code = 7;
                        } else if (otpCode == 4) {
                            code = 8;
                        }
                    } else {
                        code = 7;
                    }
                }
            }
            if (code == 0) {
                ArrayList<String> listUser = new ArrayList<>();
                listUser.add(nicknameReceive);
                listUser.add(nicknameSend);
                BackendUtils.sendUpdateUserMoneyInfo(listUser);
                
                // M4. Agent -> User transfer is counted as Deposit -> requires withdrawal volume.
                UserModel receiver = service.getUserByNickName(nicknameReceive);
                if (receiver != null && receiver.getDaily() == 0) {
                    long receiveAmount = res.getMoneyReceive() > 0 ? res.getMoneyReceive() : money.longValue();
                    com.vinplay.dal.withdraw.VolumeTrackingService.addRequiredVolume(receiver.getId(), nicknameReceive, receiveAmount);
                    com.vinplay.dal.withdraw.VolumeTrackingService.updateWithdrawalStatus(receiver.getId());
                }
            }
            res.setNicknameSend(nicknameSend);
            res.setNicknameReceive(nicknameReceive);
            res.setMoneySend(money);
        }
        catch (Exception e) {
            logger.debug((Object)e);
        }
        res.setErrorCode(String.valueOf(code));
        return res.toJson();
    }
}

