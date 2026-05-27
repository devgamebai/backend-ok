/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.usercore.service.impl.GiftCodeAgentServiceImpl
 *  com.vinplay.usercore.service.impl.OtpServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.exceptions.KeyNotFoundException
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.messages.GiftCodeMessage
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.response.GiftCodeAgentResponse
 *  com.vinplay.vbee.common.response.ResultGiftCodeAgentResponse
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.usercore.service.impl.GiftCodeAgentServiceImpl;
import com.vinplay.usercore.service.impl.OtpServiceImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.exceptions.KeyNotFoundException;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messages.GiftCodeMessage;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.response.GiftCodeAgentResponse;
import com.vinplay.vbee.common.response.ResultGiftCodeAgentResponse;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import javax.servlet.http.HttpServletRequest;

public class ExportGiftCodeAgentProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        GiftCodeAgentResponse code = new GiftCodeAgentResponse();
        ResultGiftCodeAgentResponse response = new ResultGiftCodeAgentResponse(false, "1001");
        String gia = request.getParameter("gp");
        String soluong = request.getParameter("gq");
        String nguon = request.getParameter("gs");
        String dotphathanh = request.getParameter("gl");
        String moneyType = request.getParameter("mt");
        String type = request.getParameter("type");
        String nickName = request.getParameter("nn");
        String otp = request.getParameter("otp");
        if (!(gia == null || gia.equals("") || soluong == null || soluong.equals("") || nguon == null || nguon.equals("") || dotphathanh == null || dotphathanh.equals(""))) {
            UserModel userModel = null;
            try {
                userModel = (new UserServiceImpl()).getUserByNickName(nickName);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            boolean isMobileSecure = (userModel != null && userModel.isHasMobileSecurity());

            // check otp if mobile secure
            int otpCode = 0;
            if (isMobileSecure && otp.length() == 6) {
                OtpServiceImpl otpService = new OtpServiceImpl();
                try {
                    otpCode = otpService.checkOtp(otp, nickName, type, (String)null);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            } else if(isMobileSecure) {
                otpCode = 3;
            }

            // export gift code
            if (otpCode == 0) {
                GiftCodeAgentServiceImpl service = new GiftCodeAgentServiceImpl();
                GiftCodeMessage msg = new GiftCodeMessage("", gia, Integer.parseInt(soluong), nguon, 1, 1, Integer.parseInt(moneyType), dotphathanh, type, "");
                HazelcastInstance client = HazelcastClientFactory.getInstance();
                IMap userMap = client.getMap("users");
                if (userMap.containsKey((Object)nickName)) {
                    try {
                        userMap.lock((Object)nickName);
                        UserCacheModel user = (UserCacheModel)userMap.get((Object)nickName);
                        long currentMoney = user.getTotalPnl("vin");
                        code = service.exportGiftCode(msg, currentMoney, nickName);
                        if (code.ErrorCode == 0) {
                            response.setErrorCode("0");
                            response.setSuccess(true);
                            response.setTransactions(code);
                        } else if (code.ErrorCode == 1) {
                            response.setErrorCode("10002");
                        } else if (code.ErrorCode == 2) {
                            response.setErrorCode("10003");
                        }
                    }
                    catch (Exception e5) {
                        e5.printStackTrace();
                        return response.toJson();
                    }
                    try {
                        userMap.unlock((Object)nickName);
                    }
                    catch (Exception e5) {}
                }
            } else if (otpCode == 3) {
                response.setErrorCode("10004");
            } else if (otpCode == 4) {
                response.setErrorCode("10005");
            }
            return response.toJson();
        }
        return "MISSING PARAMETTER";
    }
}

