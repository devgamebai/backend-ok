/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.dichvuthe.response.RechargePaywellResponse
 *  com.vinplay.payment.entities.BankConfig
 *  com.vinplay.payment.entities.PaymentConfig
 *  com.vinplay.payment.service.impl.PaymentConfigServiceImpl
 *  com.vinplay.payment.service.impl.PaymentManualServiceImpl
 *  com.vinplay.payment.service.impl.RechargePayWellServiceImpl
 *  com.vinplay.payment.service.impl.RechargePrincePayServiceImpl
 *  com.vinplay.payment.utils.PayUtils
 *  com.vinplay.payment.utils.PaymentConstant$PayType
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
import com.vinplay.payment.entities.BankConfig;
import com.vinplay.payment.entities.PaymentConfig;
import com.vinplay.payment.service.impl.PaymentConfigServiceImpl;
import com.vinplay.payment.service.impl.PaymentManualServiceImpl;
import com.vinplay.payment.service.impl.RechargePayWellServiceImpl;
import com.vinplay.payment.service.impl.RechargePrincePayServiceImpl;
import com.vinplay.payment.utils.PayUtils;
import com.vinplay.payment.utils.PaymentConstant;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.response.BaseResponse;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class DepositRequestUIProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");
    private static Map<String, Long> mapCache = new ConcurrentHashMap<String, Long>();
    private static final String[] RANDOM_IP = new String[]{"127.0.0.1", "0:0:0:0:0:0:0:1"};
    private final SplittableRandom ran = new SplittableRandom();

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
        String fullName = request.getParameter("fn");
        long amount = 0L;
        try {
            amount = Long.parseLong(request.getParameter("am"));
        }
        catch (Exception e) {
            return BaseResponse.error((String)"99", (String)e.getMessage());
        }
        String bankCode = request.getParameter("bc");
        String payType = request.getParameter("pt");
        String nickName = request.getParameter("nn");
        String accessToken = request.getParameter("at");
        String providerName = request.getParameter("pn");
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
        if (!DepositRequestUIProcessor.validateRequest(nickName)) {
            return BaseResponse.error((String)"15", (String)("Trong 10s ch\u1ec9 \u0111\u01b0\u1ee3c y\u00eau c\u1ea7u n\u1ea1p ti\u1ec1n 1 l\u1ea7n , t\u00ean nh\u00e2n v\u1eadt =" + nickName));
        }
        logger.info(("Deposit request nickName: " + nickName + ", accessToken: " + accessToken + ", providerName: " + providerName + ",ipaddress=" + clientIp));
        UserServiceImpl userService = new UserServiceImpl();
        try {
            if (StringUtils.isBlank((CharSequence)nickName) || StringUtils.isBlank((CharSequence)accessToken)) {
                return BaseResponse.error((String)"5", (String)"input parameter is null or empty");
            }
            if (StringUtils.isBlank((CharSequence)payType)) {
                return BaseResponse.error((String)"5", (String)"Ph\u01b0\u01a1ng th\u1ee9c n\u1ea1p ti\u1ec1n kh\u00f4ng \u0111\u00fang");
            }
            if (StringUtils.isBlank((CharSequence)providerName)) {
                return BaseResponse.error((String)"5", (String)"Nh\u00e0 cung c\u1ea5p kh\u00f4ng \u0111\u00fang");
            }
            if (StringUtils.isBlank((CharSequence)fullName)) {
                return BaseResponse.error((String)"5", (String)"H\u1ecd t\u00ean ch\u1ee7 t\u00e0i kho\u1ea3n kh\u00f4ng \u0111\u00fang");
            }
            int payTypeInt = 0;
            try {
                payTypeInt = Integer.parseInt(payType);
            }
            catch (NumberFormatException e) {
                return BaseResponse.error((String)"99", (String)e.getMessage());
            }
            if (PaymentConstant.PayType.ONLINE.getKey() != payTypeInt && PaymentConstant.PayType.OFFLINE.getKey() != payTypeInt && PaymentConstant.PayType.MOMO_DEP.getKey() != payTypeInt && PaymentConstant.PayType.ZALO_DEP.getKey() != payTypeInt) {
                return BaseResponse.error((String)"3", (String)"Ph\u01b0\u01a1ng th\u1ee9c thanh to\u00e1n kh\u00f4ng \u0111\u00fang");
            }
            boolean isToken = userService.isActiveToken(nickName, accessToken);
            if (isToken) {
                PaymentConfigServiceImpl payConfig = new PaymentConfigServiceImpl();
                PaymentConfig config = payConfig.getConfigByKey(providerName);
                if (config == null) {
                    return BaseResponse.error((String)"7", (String)"Kh\u00f4ng h\u1ed7 tr\u1ee3 c\u1ed5ng thanh to\u00e1n n\u00e0y trong th\u1eddi \u0111i\u1ec3m hi\u1ec7n t\u1ea1i");
                }
                long minAmount = config.getConfig().getMinMoney().intValue();
                if (amount < 10000L) {
                    return BaseResponse.error((String)"1", (String)"S\u1ed1 ti\u1ec1n n\u1ea1p qu\u00e1 nh\u1ecf");
                }
                if (PaymentConstant.PayType.MOMO_DEP.getKey() != payTypeInt) {
                    if (minAmount > amount) {
                        return BaseResponse.error((String)"1", (String)("S\u1ed1 ti\u1ec1n n\u1ea1p ph\u1ea3i l\u1edbn h\u01a1n\u00a0 " + minAmount + " VN\u0110"));
                    }
                } else if (amount < 20000L) {
                    return BaseResponse.error((String)"1", (String)"S\u1ed1 ti\u1ec1n n\u1ea1p ph\u1ea3i l\u1edbn h\u01a1n\u00a0 20.000 VN\u0110");
                }
                if (amount > 300000000L) {
                    return BaseResponse.error((String)"16", (String)"S\u1ed1 ti\u1ec1n n\u1ea1p ph\u1ea3i nh\u1ecf h\u01a1n\u00a0300 tri\u1ec7u VN\u0110");
                }
                UserModel user = userService.getUserByNickName(nickName);
                String userId = user.getId() + "";
                String username = user.getUsername();
                if (user.isBanLogin() || user.isBanTransferMoney() || user.isBot()) {
                    return BaseResponse.error((String)"12", (String)"Qu\u00fd kh\u00e1ch \u0111\u00e3 b\u1ecb c\u1ea5m th\u1ef1c hi\u1ec7n ch\u1ee9c n\u0103ng n\u00e0y");
                }
                RechargePaywellResponse resultResponse = null;
                String paytypeStr = PayUtils.getPayType((int)payTypeInt, (String)providerName);
                if ("".equals(paytypeStr)) {
                    return BaseResponse.error((String)"3", (String)"H\u00ecnh th\u1ee9c thanh to\u00e1n kh\u00f4ng \u0111\u00fang");
                }
                switch (providerName) {
                    case "paywell": {
                        if (StringUtils.isBlank((CharSequence)bankCode)) {
                            return BaseResponse.error((String)"5", (String)"M\u00e3 ng\u00e2n h\u00e0ng kh\u00f4ng ch\u00ednh x\u00e1c");
                        }
                        List lstBankConfig = config.getConfig().getBanks();
                        boolean isExist = false;
                        for (Object _bc : lstBankConfig) {
                            BankConfig bankConfig = (BankConfig) _bc;
                            if (!bankConfig.getKey().equals(bankCode)) continue;
                            isExist = true;
                            break;
                        }
                        if (!isExist) {
                            return BaseResponse.error((String)"6", (String)"Ch\u01b0a h\u1ed7 tr\u1ee3 ng\u00e2n h\u00e0ng n\u00e0y");
                        }
                        RechargePayWellServiceImpl servicePaywell = new RechargePayWellServiceImpl();
                        resultResponse = servicePaywell.createTransaction(userId, username, nickName, fullName, amount, bankCode, paytypeStr);
                        break;
                    }
                    case "princepay": {
                        if ("".equals(clientIp) || clientIp.length() > 20) {
                            clientIp = RANDOM_IP[this.ran.nextInt(3) + 0];
                        }
                        RechargePrincePayServiceImpl servicePrince = new RechargePrincePayServiceImpl();
                        resultResponse = servicePrince.createTransaction(userId, username, nickName, amount, paytypeStr, fullName, bankCode, clientIp);
                        break;
                    }
                    case "clickpay": {
                        break;
                    }
                    case "manualbank": {
                        String bankAccountNum = request.getParameter("bn");
                        if (paytypeStr.equals("bank_recharge")) {
                            if (StringUtils.isBlank((CharSequence)bankAccountNum)) {
                                return BaseResponse.error((String)"5", (String)"Thi\u1ebfu s\u1ed1 t\u00e0i kho\u1ea3n ng\u00e2n h\u00e0ng");
                            }
                            if (StringUtils.isBlank((CharSequence)bankCode)) {
                                return BaseResponse.error((String)"5", (String)"Thi\u1ebfu t\u00ean ng\u00e2n h\u00e0ng ");
                            }
                        } else {
                            bankCode = paytypeStr.equals("momo_recharge") ? "momo" : "zalo";
                        }
                        String desc = request.getParameter("ds");
                        PaymentManualServiceImpl manuService = new PaymentManualServiceImpl();
                        resultResponse = manuService.deposit(nickName, fullName, Long.valueOf(amount), bankCode, bankCode, bankAccountNum, paytypeStr, desc);
                    }
                }
                if (resultResponse == null) {
                    return BaseResponse.error((String)"2", (String)"Kh\u00f4ng t\u1ea1o \u0111\u01b0\u1ee3c transaction");
                }
                logger.info(("Deposit response nickName: " + nickName + ", response : " + resultResponse.toJson()));
                if (0 == resultResponse.getCode()) {
                    return new BaseResponse().success(resultResponse.getData());
                }
                if (99 == resultResponse.getCode()) {
                    return BaseResponse.error((String)(resultResponse.getCode() + ""), (String)"C\u1ed5ng thanh to\u00e1n \u0111ang b\u1ea3o tr\u00ec , qu\u00fd kh\u00e1ch vui l\u00f2ng th\u1ef1c hi\u1ec7n l\u1ea1i trong \u00edt ph\u00fat");
                }
                if (20 == resultResponse.getCode()) {
                    return BaseResponse.error((String)(resultResponse.getCode() + ""), (String)"Qu\u00e1 nhi\u1ec1u y\u00eau c\u1ea7u g\u1eedi ti\u1ec1n , qu\u00fd kh\u00e1ch vui l\u00f2ng th\u1ef1c hi\u1ec7n l\u1ea1i trong \u00edt ph\u00fat");
                }
                return BaseResponse.error((String)(resultResponse.getCode() + ""), (String)resultResponse.getData());
            }
            return BaseResponse.error((String)"4", (String)"Phi\u00ean giao d\u1ecbch c\u1ee7a qu\u00fd kh\u00e1ch \u0111\u00e3 h\u1ebft , vui l\u00f2ng t\u1ea3i l\u1ea1i trang v\u00e0 \u0111\u0103ng nh\u1eadp");
        }
        catch (Exception e) {
            logger.error(e);
            return BaseResponse.error((String)"99", (String)e.getMessage());
        }
    }
}

