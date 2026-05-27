/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.BotServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.response.MoneyResponse
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors.ad;

import com.vinplay.dal.service.impl.BotServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.response.MoneyResponse;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import javax.servlet.http.HttpServletRequest;

public class BotProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickname = request.getParameter("nn");
        long money = Long.parseLong(request.getParameter("mn"));
        BotServiceImpl botService = new BotServiceImpl();
        try {
            botService.login(nickname);
            MoneyResponse res = botService.addMoney(nickname, -money, "vin", "Chuy\u1ec3n vin cho \u0111\u1ea1i l\u00fd");
            if (res.isSuccess()) {
                return "B\u00e1n th\u00e0nh c\u00f4ng " + money + " VIN c\u1ee7a " + nickname;
            }
            if (res.getErrorCode().equals("1002")) {
                return "Kh\u00f4ng \u0111\u1ee7 ti\u1ec1n";
            }
            return "Kh\u00f4ng h\u1ee3p l\u1ec7";
        }
        catch (UnsupportedEncodingException | NoSuchAlgorithmException | SQLException e) {
            e.printStackTrace();
            return "th\u1ea5t b\u1ea1i";
        }
    }
}

