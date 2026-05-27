/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.dichvuthe.response.RechargePaywellResponse
 *  com.vinplay.payment.entities.PaymentConfig
 *  com.vinplay.payment.service.impl.PaymentConfigServiceImpl
 *  com.vinplay.payment.service.impl.RechargePayaSecServiceImpl
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.response.BaseResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.commons.lang3.StringUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors.payment;

import com.vinplay.dichvuthe.response.RechargePaywellResponse;
import com.vinplay.payment.entities.PaymentConfig;
import com.vinplay.payment.service.impl.PaymentConfigServiceImpl;
import com.vinplay.payment.service.impl.RechargePayaSecServiceImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.response.BaseResponse;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class DepositBySCRequestUIProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");
    private static Map<String, Long> mapCache = new ConcurrentHashMap<String, Long>();

    private String getIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }

    public static boolean validateRequest(String orderID) {
        if (mapCache.isEmpty()) {
            long t1 = new Date().getTime();
            mapCache.put(orderID, t1);
        } else {
            if (mapCache.containsKey(orderID)) {
                long t1 = mapCache.get(orderID);
                long t2 = new Date().getTime();
                if (t2 - t1 > 10000L) {
                    mapCache.put(orderID, t2);
                    return true;
                }
                return false;
            }
            long t1 = new Date().getTime();
            mapCache.put(orderID, t1);
        }
        return true;
    }

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        long amount = 0L;
        try {
            amount = Long.parseLong(request.getParameter("am"));
        }
        catch (Exception e) {
            return BaseResponse.error((String)"99", (String)e.getMessage());
        }
        String typeCard = request.getParameter("tc");
        String serial = request.getParameter("sn");
        String pin = request.getParameter("p");
        String accessToken = request.getParameter("at");
        String nickName = request.getParameter("nn");
        String ip = this.getIpAddress(request);
        logger.info(("ipaddress1 :" + ip));
        String clientIp = "";
        if (ip != null && !"".equals(ip)) {
            String[] arrayIp = ip.split(",");
            for (int i = 0; i < (arrayIp.length > 2 ? 2 : arrayIp.length); ++i) {
                if (arrayIp[i].length() > 40) continue;
                clientIp = arrayIp[i].trim();
                break;
            }
        }
        if (StringUtils.isBlank((CharSequence)nickName) || StringUtils.isBlank((CharSequence)accessToken)) {
            return BaseResponse.error((String)"5", (String)"input parameter is null or empty");
        }
        if (!DepositBySCRequestUIProcessor.validateRequest(nickName)) {
            return BaseResponse.error((String)"15", (String)("Trong 10s ch\u1ec9 \u0111\u01b0\u1ee3c y\u00eau c\u1ea7u n\u1ea1p ti\u1ec1n 1 l\u1ea7n , t\u00ean nh\u00e2n v\u1eadt =" + nickName));
        }
        logger.info(("Deposit SC request nickName: " + nickName + ", accessToken: " + accessToken + ", providerName: payasec.com,ipaddress=" + clientIp));
        UserServiceImpl userService = new UserServiceImpl();
        try {
            boolean isToken = userService.isActiveToken(nickName, accessToken);
            if (isToken) {
                PaymentConfigServiceImpl payConfig = new PaymentConfigServiceImpl();
                PaymentConfig config = payConfig.getConfigByKey("payasec");
                if (config == null) {
                    return BaseResponse.error((String)"7", (String)"Kh\u00f4ng h\u1ed7 tr\u1ee3 c\u1ed5ng thanh to\u00e1n n\u00e0y trong th\u1eddi \u0111i\u1ec3m hi\u1ec7n t\u1ea1i");
                }
                long minAmount = config.getConfig().getMinMoney().intValue();
                if (amount < minAmount) {
                    return BaseResponse.error((String)"1", (String)"S\u1ed1 ti\u1ec1n n\u1ea1p qu\u00e1 nh\u1ecf");
                }
                if (amount > 1000000L) {
                    return BaseResponse.error((String)"16", (String)"S\u1ed1 ti\u1ec1n n\u1ea1p ph\u1ea3i nh\u1ecf h\u01a1n\u00a01 tri\u1ec7u VN\u0110");
                }
                UserModel user = userService.getUserByNickName(nickName);
                if (user.isBanLogin() || user.isBanTransferMoney() || user.isBot()) {
                    return BaseResponse.error((String)"12", (String)"Qu\u00fd kh\u00e1ch \u0111\u00e3 b\u1ecb c\u1ea5m th\u1ef1c hi\u1ec7n ch\u1ee9c n\u0103ng n\u00e0y");
                }
                RechargePayaSecServiceImpl manuService = new RechargePayaSecServiceImpl();
                RechargePaywellResponse resultResponse = manuService.createTransaction(user.getId() + "", user.getUsername(), user.getNickname(), user.getNickname(), amount, typeCard, serial, pin);
                if (resultResponse == null) {
                    return BaseResponse.error((String)"2", (String)"Kh\u00f4ng t\u1ea1o \u0111\u01b0\u1ee3c transaction");
                }
                logger.info(("Deposit response nickName: " + nickName + ", response : " + resultResponse.toJson()));
                if (0 == resultResponse.getCode()) {
                    return new BaseResponse().success(resultResponse.getData());
                }
                if (99 == resultResponse.getCode()) {
                    return BaseResponse.error((String)(resultResponse.getCode() + ""), (String)"C\u1ed5ng thanh to\u00e1n \u0111ang b\u1ea3o tr\u00ec, qu\u00fd kh\u00e1ch vui l\u00f2ng th\u1ef1c hi\u1ec7n l\u1ea1i trong \u00edt ph\u00fat");
                }
                if (20 == resultResponse.getCode()) {
                    return BaseResponse.error((String)(resultResponse.getCode() + ""), (String)"Qu\u00e1 nhi\u1ec1u y\u00eau c\u1ea7u g\u1eedi ti\u1ec1n, qu\u00fd kh\u00e1ch vui l\u00f2ng th\u1ef1c hi\u1ec7n l\u1ea1i trong \u00edt ph\u00fat");
                }
                return BaseResponse.error((String)(resultResponse.getCode() + ""), (String)resultResponse.getData());
            }
            return BaseResponse.error((String)"4", (String)"Phi\u00ean giao d\u1ecbch c\u1ee7a qu\u00fd kh\u00e1ch \u0111\u00e3 h\u1ebft, vui l\u00f2ng t\u1ea3i l\u1ea1i trang v\u00e0 \u0111\u0103ng nh\u1eadp");
        }
        catch (Exception e) {
            logger.error(e);
            return BaseResponse.error((String)"99", (String)e.getMessage());
        }
    }
}

