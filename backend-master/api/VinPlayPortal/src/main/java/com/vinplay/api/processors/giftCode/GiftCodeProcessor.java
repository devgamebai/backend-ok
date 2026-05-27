/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.vinplay.giftcode.GiftCodeModel
 *  com.vinplay.giftcode.GiftCodeUtil
 *  com.vinplay.usercore.service.UserService
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.enums.Games
 *  com.vinplay.vbee.common.statics.TransType
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors.giftCode;

import com.google.gson.Gson;
import com.vinplay.api.processors.giftCode.GiftCodeDescription;
import com.vinplay.api.processors.giftCode.GiftCodeResponse;
import com.vinplay.giftcode.GiftCodeModel;
import com.vinplay.giftcode.GiftCodeUtil;
import com.vinplay.usercore.service.UserService;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.statics.TransType;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class GiftCodeProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private Logger logger = Logger.getLogger((String)"api");

    public String execute(Param<HttpServletRequest> param) {
        GiftCodeResponse response = new GiftCodeResponse(false, "100");
        Gson gson = new Gson();
        HttpServletRequest request = (HttpServletRequest)param.get();
        String username = request.getParameter("un");
        String giftCode = request.getParameter("giftcode");
        String ip = this.getIpAddress(request);
        UserServiceImpl userService = new UserServiceImpl();
        GiftCodeModel giftCodeModel = GiftCodeUtil.getGiftCode((String)giftCode);
        this.logger.debug(("GiftCodeProcessor " + request.getQueryString()));
        if (giftCodeModel != null) {
            int errorCode = GiftCodeUtil.isUsedGiftCode((GiftCodeModel)giftCodeModel, (String)username, (String)ip, (UserService)userService);
            if (String.valueOf(errorCode).equals("99")) {
                response.setErrorCode(errorCode + "");
                response.setMessage("Qu\u00fd kh\u00e1ch \u0111\u00e3 \u0111\u01b0\u1ee3c nh\u1eadn giftcode \u0111\u1ee3t n\u00e0y r\u1ed3i");
                return response.toJson();
            }
            if (String.valueOf(errorCode).equals("18")) {
                response.setErrorCode(errorCode + "");
                response.setMessage("Qu\u00fd kh\u00e1ch vui l\u00f2ng nh\u1eadn giftcode \u0111\u1ee3t sau !");
                return response.toJson();
            }
            if (String.valueOf(errorCode).equals("20")) {
                response.setErrorCode(errorCode + "");
                response.setMessage("Qu\u00fd kh\u00e1ch vui l\u00f2ng th\u00eam t\u00e0i kho\u1ea3n ng\u00e2n h\u00e0ng \u0111\u1ec3 nh\u1eadn giftcode ");
                return response.toJson();
            }
            if (String.valueOf(errorCode).equals("19")) {
                response.setErrorCode(errorCode + "");
                response.setMessage("Qu\u00fd kh\u00e1ch vui l\u00f2ng x\u00e1c th\u1ef1c S\u0110T \u0111\u1ec3 nh\u1eadn giftcode");
                return response.toJson();
            }
            response.setErrorCode(errorCode + "");
            if (errorCode == 0) {
                response = new GiftCodeResponse(true, "0");
                userService.updateMoney(username, (long)giftCodeModel.money, "vin", Games.GIFT_CODE.getName(), Games.GIFT_CODE.getId() + "", gson.toJson(new GiftCodeDescription(giftCodeModel.giftcode)), 0L, null, TransType.NO_VIPPOINT);
                response.currentMoney = userService.getCurrentMoneyUserCache(username, "vin");
            }
        }
        return response.toJson();
    }

    private String getIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }
}

