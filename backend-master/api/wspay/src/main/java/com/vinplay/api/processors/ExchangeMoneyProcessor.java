/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.IMap
 *  com.vinplay.payment.entities.MerchantInfo
 *  com.vinplay.payment.service.PaymentService
 *  com.vinplay.payment.service.impl.PaymentServiceImpl
 *  com.vinplay.payment.utils.PayUtils
 *  com.vinplay.usercore.service.impl.MoneyInGameServiceImpl
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.usercore.utils.GameCommon
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.enums.Games
 *  com.vinplay.vbee.common.hazelcast.HazelcastUtils
 *  com.vinplay.vbee.common.messages.pay.ExchangeMessage
 *  com.vinplay.vbee.common.models.StatusUser
 *  com.vinplay.vbee.common.models.cache.UserActiveModel
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.response.MoneyResponse
 *  com.vinplay.vbee.common.statics.TransType
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors;

import com.hazelcast.core.IMap;
import com.vinplay.api.processors.response.ExchangeMoneyResponse;
import com.vinplay.payment.entities.MerchantInfo;
import com.vinplay.payment.service.PaymentService;
import com.vinplay.payment.service.impl.PaymentServiceImpl;
import com.vinplay.payment.utils.PayUtils;
import com.vinplay.usercore.service.impl.MoneyInGameServiceImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.hazelcast.HazelcastUtils;
import com.vinplay.vbee.common.messages.pay.ExchangeMessage;
import com.vinplay.vbee.common.models.StatusUser;
import com.vinplay.vbee.common.models.cache.UserActiveModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.statics.TransType;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class ExchangeMoneyProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger(ExchangeMoneyProcessor.class);
    private PaymentService pmSer = new PaymentServiceImpl();

    public String execute(Param<HttpServletRequest> param) {
        String merchantTransId;
        long time;
        ExchangeMoneyResponse res;
        int errorCode;
        block24 : {
            errorCode = 1;
            res = new ExchangeMoneyResponse("", errorCode, "", 0L, 0L, "");
            HttpServletRequest request = (HttpServletRequest)param.get();
            time = System.currentTimeMillis();
            merchantTransId = null;
            String ip = this.getIpAddress(request);
            try {
                String command = request.getParameter("c");
                String nickname = request.getParameter("nn");
                String accessToken = request.getParameter("at");
                String merchantId = request.getParameter("mid");
                merchantTransId = request.getParameter("tid");
                long money = Long.parseLong(request.getParameter("mn"));
                String moneyType = request.getParameter("mt");
                String type = request.getParameter("type");
                String checkSum = request.getParameter("cks");
                logger.debug((Object)("Request ExchangeMoney time: " + time + ", command: " + command + ", nickname: " + nickname + ", accessToken: " + accessToken + ", merchantId: " + merchantId + ", merchantTransId: " + merchantTransId + ", money: " + money + ", moneyType: " + moneyType + ", type: " + type + ", checkSum: " + checkSum + ", ip: " + ip));
                if (command != null && nickname != null && accessToken != null && merchantId != null && money > 0L && moneyType != null && type != null && (type.equals("0") || type.equals("1")) && checkSum != null && merchantTransId != null && !merchantTransId.isEmpty()) {
                    res.setMerchantTransId(merchantTransId);
                    if (PayUtils.checkMerchantId((String)merchantId)) {
                        MerchantInfo merchantInfo = PayUtils.getMerchant((String)merchantId);
                        if (moneyType.equals(merchantInfo.getMoneyType())) {
                            String value = String.valueOf(command) + nickname + accessToken + merchantId + merchantTransId + merchantInfo.getMerchantKey() + money + moneyType + type;
                            String ck = VinPlayUtils.getMD5Hash((String)value);
                            if (ck.equals(checkSum)) {
                                if (money >= merchantInfo.getMoneyMin() && money <= merchantInfo.getMoneyMax()) {
                                    IMap userMap = HazelcastUtils.getUserMap((String)nickname);
                                    IMap activeMap = HazelcastUtils.getActiveMap((String)nickname);
                                    if (userMap.containsKey((Object)nickname) && activeMap.containsKey((Object)nickname)) {
                                        try {
                                            userMap.lock((Object)nickname);
                                            UserCacheModel user = (UserCacheModel)userMap.get((Object)nickname);
                                            res.setNickname(nickname);
                                            if (user.getAccessToken().equals(accessToken)) {
                                                if (!VinPlayUtils.sessionTimeout((long)((UserActiveModel)activeMap.get((Object)nickname)).getLastActive())) {
                                                    Games game = Games.findGameByName((String)merchantId);
                                                    if (game != null && user.getDaily() == 0 && !StatusUser.checkStatus((int)user.getStatus(), (int)game.getId())) {
                                                        boolean isLimitUser = false;
                                                        long moneyUserInDay = money;
                                                        if (type.equals("0")) {
                                                            if (user.getExchangeMoneyTime() != null && VinPlayUtils.compareDate((Date)user.getExchangeMoneyTime(), (Date)new Date()) == 0) {
                                                                moneyUserInDay += user.getExchangeMoneyInDay();
                                                            }
                                                            if (moneyUserInDay > GameCommon.getValueLong((String)(String.valueOf(merchantInfo.getMerchantId()) + "CASHOUT_LIMIT_USER"))) {
                                                                isLimitUser = true;
                                                            }
                                                        }
                                                        if (!isLimitUser) {
                                                            res = this.exchangeMoney(user.getId(), nickname, money, moneyType, type, merchantId, merchantTransId, merchantInfo, ip, res);
                                                            errorCode = res.getErrorCode();
                                                            if (errorCode == 0 && type.equals("0")) {
                                                                user.setExchangeMoneyInDay(moneyUserInDay);
                                                                user.setExchangeMoneyTime(new Date());
                                                                userMap.put((Object)nickname, (Object)user);
                                                            }
                                                        } else {
                                                            errorCode = 10;
                                                        }
                                                    } else {
                                                        errorCode = 11;
                                                    }
                                                } else {
                                                    errorCode = 7;
                                                }
                                            } else {
                                                errorCode = 6;
                                            }
                                        }
                                        catch (Exception e) {
                                            logger.error((Object)("exception: " + e));
                                            break block24;
                                        }
                                        try {
                                            userMap.unlock((Object)nickname);
                                        }
                                        catch (Exception e) {}
                                        break block24;
                                    }
                                    UserServiceImpl ser = new UserServiceImpl();
                                    errorCode = ser.checkNickname(nickname) ? 7 : 5;
                                    break block24;
                                }
                                errorCode = 12;
                                break block24;
                            }
                            errorCode = 3;
                            break block24;
                        }
                        errorCode = 4;
                        break block24;
                    }
                    errorCode = 2;
                    break block24;
                }
                errorCode = 4;
            }
            catch (Exception e2) {
                logger.error((Object)e2);
            }
        }
        res.setErrorCode(errorCode);
        logger.debug((Object)("Response ExchangeMoney time: " + time + ", res: " + res.toJson()));
        logger.error((Object)("ExchangeMoney " + merchantTransId + " " + (System.currentTimeMillis() - time)));
        return res.toJson();
    }

    private ExchangeMoneyResponse exchangeMoney(int userId, String nickname, long money, String moneyType, String type, String merchantId, String merchantTransId, MerchantInfo merchantInfo, String ip, ExchangeMoneyResponse res) throws Exception {
        int errorCode = res.getErrorCode();
        if (this.pmSer.checkMerchantTransId(merchantId, merchantTransId)) {
            res.setErrorCode(9);
            return res;
        }
        if (PayUtils.checkMoneyLimit((String)nickname, (MerchantInfo)merchantInfo, (long)money, (String)type)) {
            res.setErrorCode(10);
            return res;
        }
        long moneyExchangeVin = 0L;
        long fee = 0L;
        String serviceName = "";
        String description = "";
        long exchangeMoney = 0L;
        if (type.equals("1")) {
            fee = Math.round(merchantInfo.getFeeFromVin() * (double)money);
            moneyExchangeVin = -money;
            exchangeMoney = money - fee;
            serviceName = "Chuy\u1ec3n vin sang " + moneyType;
        } else if (type.equals("0")) {
            fee = Math.round(merchantInfo.getFeeToVin() * (double)money);
            moneyExchangeVin = exchangeMoney = money - fee;
            serviceName = "Chuy\u1ec3n " + moneyType + " sang vin";
        }
        MoneyInGameServiceImpl ser = new MoneyInGameServiceImpl();
        MoneyResponse mnres = ser.updateMoneyUser(nickname, moneyExchangeVin, "vin", merchantId, serviceName, "", fee, (Long)null, TransType.NO_VIPPOINT, true);
        if (mnres.isSuccess()) {
            errorCode = 0;
            PayUtils.updateMoneyMerchant((MerchantInfo)merchantInfo, (long)money, (String)type);
        } else if (mnres.getErrorCode().equals("1002")) {
            errorCode = 8;
        }
        res.setCurrentMoney(mnres.getCurrentMoney());
        res.setExchangeMoney(exchangeMoney);
        res.setErrorCode(errorCode);
        String transNo = String.valueOf(merchantId) + System.currentTimeMillis() + userId;
        res.setTransNo(transNo);
        ExchangeMessage message = new ExchangeMessage(nickname, merchantId, merchantTransId, transNo, money, moneyType, type, exchangeMoney, fee, errorCode, ip);
        this.pmSer.logExchangeMoney(message);
        return res;
    }

    private String getIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }
}

