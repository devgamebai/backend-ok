/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.dichvuthe.response.RechargePaywellResponse
 *  com.vinplay.payment.dao.impl.WithDrawPaygateDaoImpl
 *  com.vinplay.payment.entities.WithDrawPaygateModel
 *  com.vinplay.payment.service.impl.PaymentManualServiceImpl
 *  com.vinplay.payment.service.impl.WithDrawPrincePayServiceImpl
 *  com.vinplay.payment.utils.PayCommon$PAYSTATUS
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.response.BaseResponse
 *  com.vinplay.vbee.common.response.MoneyResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.commons.lang3.StringUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors.payment;

import com.vinplay.api.utils.PortalUtils;
import com.vinplay.dichvuthe.response.RechargePaywellResponse;
import com.vinplay.payment.dao.impl.WithDrawPaygateDaoImpl;
import com.vinplay.payment.entities.WithDrawPaygateModel;
import com.vinplay.payment.service.impl.PaymentManualServiceImpl;
import com.vinplay.payment.service.impl.WithDrawPrincePayServiceImpl;
import com.vinplay.payment.utils.PayCommon;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.response.BaseResponse;
import com.vinplay.vbee.common.response.MoneyResponse;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class WithdrawRequestUIProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");
    private static Map<String, Long> mapCache = new ConcurrentHashMap<String, Long>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private static final Random random = new Random();

    public static boolean validateRequest(String orderID) {
        if (mapCache.isEmpty()) {
            long t1 = new Date().getTime();
            mapCache.put(orderID, t1);
        } else {
            if (mapCache.containsKey(orderID)) {
                long t1 = mapCache.get(orderID);
                long t2 = new Date().getTime();
                if (t2 - t1 > 20000L) {
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
        String bankNumber = request.getParameter("bn");
        String nickName = request.getParameter("nn");
        String accessToken = request.getParameter("at");
        String ip = PortalUtils.getIpAddress(request);
        logger.info(("Withdraw request nickName: " + nickName + ", accessToken: " + accessToken + ",ipaddress=" + ip));
        if (StringUtils.isBlank((CharSequence)nickName) || StringUtils.isBlank((CharSequence)accessToken)) {
            return BaseResponse.error((String)"5", (String)"input parameter is null or empty");
        }
        if (StringUtils.isBlank((CharSequence)bankNumber)) {
            return BaseResponse.error((String)"5", (String)"T\u00ean ng\u00e2n h\u00e0ng kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1ed1ng");
        }
        UserServiceImpl userService = new UserServiceImpl();
        try {
            WithDrawPrincePayServiceImpl withdrawService = new WithDrawPrincePayServiceImpl();
            boolean isToken = userService.isActiveToken(nickName, accessToken);
            if (isToken) {
                if (amount < 200000L) {
                    return BaseResponse.error((String)"1", (String)"S\u1ed1 ti\u1ec1n r\u00fat t\u1eeb 200.000 VND");
                }
                UserCacheModel user = userService.getUser(nickName);
                RechargePaywellResponse response = withdrawService.requestWithdrawUser(user.getId() + "", user.getUsername(), nickName, amount, bankNumber);
                if (response.getCode() == 88) {
                    return BaseResponse.error((String)"16", (String)"Qu\u00fd kh\u00e1ch vui l\u00f2ng th\u1ef1c hi\u1ec7n l\u1ec7nh n\u1ea1p ti\u1ec1n tr\u01b0\u1edbc !");
                }
                if (response.getCode() == 89) {
                    return BaseResponse.error((String)"17", (String)"Qu\u00fd kh\u00e1ch \u0111\u01b0\u1ee3c th\u1ef1c hi\u1ec7n t\u1ed1i \u0111a 5 l\u1ea7n r\u00fat ti\u1ec1n th\u00e0nh c\u00f4ng trong ng\u00e0y !");
                }
                logger.debug(("Delayed bankout for live user " + user.getUsername() + " live: " + user.isLive() + " code: " + response.getCode() + " tid: " + response.getTid()));
                if (response.getCode() == 0 && user.isLive() && StringUtils.isNotBlank((CharSequence)response.getTid())) {
                    String orderId = response.getTid();
                    int delaySeconds = 70 + random.nextInt(51);
                    scheduler.schedule(() -> {
                        try {
                            WithDrawPaygateDaoImpl withdrawDao = new WithDrawPaygateDaoImpl();
                            WithDrawPaygateModel withDrawPaygateModel = withdrawDao.GetById(orderId);
                            if (withDrawPaygateModel == null) {
                                logger.warn(("Delayed bankout: WithDrawPaygateModel not found for orderId: " + orderId));
                                return;
                            }
                            long cashBack = withDrawPaygateModel.Amount * 5L / 100L;
                            PaymentManualServiceImpl withdrawManual = new PaymentManualServiceImpl();
                            withdrawManual.withdrawalSystemNote(withDrawPaygateModel.Id, PayCommon.PAYSTATUS.SUCCESS, "R\u00fat ti\u1ec1n th\u00e0nh c\u00f4ng");
                            String msg = "Ho\u00e0n ti\u1ec1n r\u00fat 5% Th\u00e0nh C\u00f4ng | " + cashBack;
                            UserServiceImpl userServiceForUpdate = new UserServiceImpl();
                            MoneyResponse mr = userServiceForUpdate.updateMoneyFromAdmin(withDrawPaygateModel.Nickname, cashBack, "vin", "BACK_BANK_OUT", "BACK_BANK_OUT", msg, 0L, false);
                            logger.debug(("Delayed bankout for live user " + withDrawPaygateModel.Nickname + " after " + delaySeconds + "s: " + (mr.isSuccess() ? "Success" : "Failed") + " msg: " + msg + " amount: " + cashBack));
                        }
                        catch (Exception e) {
                            logger.error(("Error in delayed bankout for live user: " + nickName), (Throwable)e);
                        }
                    }, (long)delaySeconds, TimeUnit.SECONDS);
                    logger.debug(("Scheduled delayed bankout for live user " + nickName + " after " + delaySeconds + "s"));
                }
                return new BaseResponse().success(response);
            }
            return BaseResponse.error((String)"4", (String)"Phi\u00ean l\u00e0m vi\u1ec7c c\u1ee7a b\u1ea1n \u0111\u00e3 h\u1ebft h\u1ea1n , vui l\u00f2ng t\u1ea3i l\u1ea1i trang !");
        }
        catch (Exception e) {
            logger.error(e);
            return BaseResponse.error((String)"99", (String)e.getMessage());
        }
    }

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(10L, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                    if (!scheduler.awaitTermination(5L, TimeUnit.SECONDS)) {
                        logger.warn("Scheduler did not terminate");
                    }
                }
            }
            catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }, "WithdrawRequestUI-Scheduler-Shutdown"));
    }
}

