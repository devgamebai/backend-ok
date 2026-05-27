/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.messages.vtcpay.LogVTCPayTopupMessage
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.response.MoneyResponse
 *  com.vinplay.vbee.common.statics.TransType
 *  com.vinplay.vbee.common.utils.DateTimeUtils
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.vtcpay.service.impl;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dichvuthe.encode.RSA;
import com.vinplay.usercore.dao.impl.UserDaoImpl;
import com.vinplay.usercore.service.impl.MoneyInGameServiceImpl;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messages.vtcpay.LogVTCPayTopupMessage;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.statics.TransType;
import com.vinplay.vbee.common.utils.DateTimeUtils;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import com.vinplay.vtcpay.dao.impl.VTCPayDaoImpl;
import com.vinplay.vtcpay.request.CheckAccountRequest;
import com.vinplay.vtcpay.request.CheckTransRequest;
import com.vinplay.vtcpay.request.TopupRequest;
import com.vinplay.vtcpay.response.TopupResponse;
import com.vinplay.vtcpay.service.VTCPayService;
import org.apache.log4j.Logger;

public class VTCPayServiceImpl
implements VTCPayService {
    private static final Logger logger = Logger.getLogger((String)"vtc_pay");

    @Override
    public String checkAccount(CheckAccountRequest input) {
        TopupResponse response = new TopupResponse();
        try {
            int statusResponse = 0;
            String responseCode = "-99";
            String responseDes = "L\u1ed7i kh\u00f4ng x\u00e1c \u0111\u1ecbnh";
            String timeResponse = DateTimeUtils.getCurrentTime((String)"yyyyMMddHHmmss");
            String command = input.getCommand();
            String partnerId = input.getPartnerId();
            String nickName = input.getNickName();
            String sign = input.getSign();
            logger.debug(("Request checkAccountProcessor nickName: " + nickName));
            if (command != null && partnerId != null && nickName != null && sign != null) {
                String data = command + "|" + partnerId + "|" + nickName;
                if (RSA.verify(data, sign, GameCommon.getValueStr("VTCPAY_PUBLIC_KEY"))) {
                    UserDaoImpl dao = new UserDaoImpl();
                    UserModel model = dao.getUserByNickName(nickName);
                    if (model != null) {
                        statusResponse = 1;
                        responseCode = "1";
                        responseDes = "Nickname h\u1ee3p l\u1ec7";
                    } else {
                        statusResponse = -1;
                        responseCode = "-3";
                        responseDes = "Nickname kh\u00f4ng t\u1ed3n t\u1ea1i";
                    }
                } else {
                    statusResponse = -1;
                    responseCode = "-2";
                    responseDes = "Ch\u1eef k\u00fd kh\u00f4ng \u0111\u00fang";
                }
            } else {
                statusResponse = -1;
                responseCode = "-1";
                responseDes = "Param truy\u1ec1n v\u00e0o kh\u00f4ng \u0111\u00fang";
            }
            response.setStatus(statusResponse);
            response.setResponseCode(responseCode);
            response.setDescription(responseDes);
            response.setTimeResponse(timeResponse);
            logger.debug(("Response checkAccountProcessor nickName: " + nickName + ", response: " + response.toJson()));
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.error(e);
            return response.toJson();
        }
        return response.toJson();
    }

    @Override
    public String checkTrans(CheckTransRequest input) {
        TopupResponse response = new TopupResponse();
        try {
            int statusResponse = 0;
            String responseCode = "-99";
            String responseDes = "L\u1ed7i kh\u00f4ng x\u00e1c \u0111\u1ecbnh";
            String timeResponse = DateTimeUtils.getCurrentTime((String)"yyyyMMddHHmmss");
            String command = input.getCommand();
            String partnerId = input.getPartnerId();
            String requestId = input.getRequestId();
            String sign = input.getSign();
            logger.debug(("Request CheckTransProcessor TransId: " + requestId));
            if (command != null && partnerId != null && requestId != null && sign != null) {
                String data = command + "|" + partnerId + "|" + requestId;
                if (RSA.verify(data, sign, GameCommon.getValueStr("VTCPAY_PUBLIC_KEY"))) {
                    VTCPayDaoImpl dao = new VTCPayDaoImpl();
                    LogVTCPayTopupMessage message = dao.checkTrans(requestId);
                    if (message == null) {
                        statusResponse = -1;
                        responseCode = "-6";
                        responseDes = "M\u00e3 giao d\u1ecbch kh\u00f4ng t\u1ed3n t\u1ea1i";
                    } else {
                        statusResponse = message.getStatusRes();
                        responseCode = message.getResponseCode();
                        responseDes = message.getResponseDes();
                        timeResponse = message.getTimeResponse();
                    }
                } else {
                    statusResponse = -1;
                    responseCode = "-2";
                    responseDes = "Ch\u1eef k\u00fd kh\u00f4ng \u0111\u00fang";
                }
            } else {
                statusResponse = -1;
                responseCode = "-1";
                responseDes = "Param truy\u1ec1n v\u00e0o kh\u00f4ng \u0111\u00fang";
            }
            response.setStatus(statusResponse);
            response.setResponseCode(responseCode);
            response.setDescription(responseDes);
            response.setTimeResponse(timeResponse);
            logger.debug(("Response CheckTransProcessor TransId: " + requestId + ", response: " + response.toJson()));
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.error(e);
            return response.toJson();
        }
        return response.toJson();
    }

    @Override
    public String topup(TopupRequest input) {
        TopupResponse response = new TopupResponse();
        String command = input.getCommand();
        String partnerId = input.getPartnerId();
        String requestId = input.getRequestId();
        String nickName = input.getNickName();
        String price = input.getPrice();
        String timeRequest = input.getTimeRequest();
        String timeResponse = DateTimeUtils.getCurrentTime((String)"yyyyMMddHHmmss");
        String sign = input.getSign();
        int userId = 0;
        String userName = "";
        long moneyUser = 0L;
        String vinplayTransId = String.valueOf(VinPlayUtils.generateTransId());
        try {
            logger.debug(("Request TopupProcessor VinTransId: " + vinplayTransId));
            int statusResponse = 0;
            String responseCode = "-99";
            String responseDes = "L\u1ed7i kh\u00f4ng x\u00e1c \u0111\u1ecbnh";
            if (command != null && partnerId != null && requestId != null && nickName != null && price != null && timeRequest != null && sign != null) {
                String data = command + "|" + partnerId + "|" + requestId + "|" + nickName + "|" + price + "|" + timeRequest;
                if (RSA.verify(data, sign, GameCommon.getValueStr("VTCPAY_PUBLIC_KEY"))) {
                    UserDaoImpl dao = new UserDaoImpl();
                    UserModel model = dao.getUserByNickName(nickName);
                    if (model != null) {
                        HazelcastInstance client = HazelcastClientFactory.getInstance();
                        IMap<String, UserModel> userMap = client.getMap("users");
                        if (userMap.containsKey(nickName)) {
                            String[] priceList = GameCommon.getValueStr("VTCPAY_PRICE").split(",");
                            boolean isPriceValid = false;
                            for (String str : priceList) {
                                if (!str.equals(price)) continue;
                                isPriceValid = true;
                                break;
                            }
                            if (isPriceValid) {
                                response = this.updateMoneyUser(requestId, nickName, Integer.parseInt(price), response);
                                statusResponse = response.getStatus();
                                responseCode = response.getResponseCode();
                                responseDes = response.getDescription();
                                userId = ((UserCacheModel)userMap.get(nickName)).getId();
                                userName = ((UserCacheModel)userMap.get(nickName)).getUsername();
                                moneyUser = ((UserCacheModel)userMap.get(nickName)).getTotalPnl("vin");
                            } else {
                                statusResponse = -1;
                                responseCode = "-5";
                                responseDes = "M\u1ec7nh gi\u00e1 kh\u00f4ng \u0111\u00fang";
                            }
                        } else {
                            statusResponse = -1;
                            responseCode = "-4";
                            responseDes = "Nickname ch\u01b0a login v\u00e0o Vinplay";
                        }
                    } else {
                        statusResponse = -1;
                        responseCode = "-3";
                        responseDes = "Nickname kh\u00f4ng t\u1ed3n t\u1ea1i";
                    }
                } else {
                    statusResponse = -1;
                    responseCode = "-2";
                    responseDes = "Ch\u1eef k\u00fd kh\u00f4ng \u0111\u00fang";
                }
            } else {
                statusResponse = -1;
                responseCode = "-1";
                responseDes = "Param truy\u1ec1n v\u00e0o kh\u00f4ng \u0111\u00fang";
                price = "0";
            }
            response.setStatus(statusResponse);
            response.setResponseCode(responseCode);
            response.setDescription(responseDes);
            response.setTimeResponse(timeResponse);
            LogVTCPayTopupMessage message = new LogVTCPayTopupMessage(vinplayTransId, requestId, userId, userName, nickName, Integer.parseInt(price), moneyUser, statusResponse, responseCode, responseDes, timeRequest, timeResponse);
            VTCPayDaoImpl dao2 = new VTCPayDaoImpl();
            dao2.logVTCPayTopup(message);
            logger.debug(("Response TopupProcessor VinTransId: " + vinplayTransId + ", response: " + response.toJson()));
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.error(e);
            LogVTCPayTopupMessage message2 = new LogVTCPayTopupMessage(vinplayTransId, requestId, userId, userName, nickName, Integer.parseInt(price), moneyUser, -1, "-99", e.getMessage(), timeRequest, timeResponse);
            VTCPayDaoImpl dao3 = new VTCPayDaoImpl();
            dao3.logVTCPayTopup(message2);
            return response.toJson();
        }
        return response.toJson();
    }

    private synchronized TopupResponse updateMoneyUser(String requestId, String nickname, int money, TopupResponse response) {
        int status = response.getStatus();
        String resCode = response.getResponseCode();
        String resDes = response.getDescription();
        MoneyInGameServiceImpl service = new MoneyInGameServiceImpl();
        MoneyResponse moneyRes = service.updateMoneyUser(nickname, money, "vin", "TopupVTCPay", "N\u00e1\u00ba\u00a1p vin qua VTCPay", "N\u1ea1p vin qua topup VTCPay", 0L, null, TransType.NO_VIPPOINT, false);
        if (moneyRes.isSuccess()) {
            status = 1;
            resCode = requestId;
            resDes = "C\u1ed9ng ti\u1ec1n d\u1ecbch v\u1ee5 th\u00e0nh c\u00f4ng";
        }
        response.setStatus(status);
        response.setResponseCode(resCode);
        response.setDescription(resDes);
        return response;
    }
}

