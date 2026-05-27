/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.IMap
 *  com.vinplay.payment.utils.PayUtils
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.hazelcast.HazelcastUtils
 *  com.vinplay.vbee.common.models.cache.UserActiveModel
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.models.cache.UserMoneyModel
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors;

import com.hazelcast.core.IMap;
import com.vinplay.api.processors.response.MoneyInfoResponse;
import com.vinplay.payment.utils.PayUtils;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastUtils;
import com.vinplay.vbee.common.models.cache.UserActiveModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.models.cache.UserMoneyModel;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class GetMoneyInfoProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger(GetMoneyInfoProcessor.class);

    public String execute(Param<HttpServletRequest> param) {
        int errorCode = 1;
        MoneyInfoResponse res = new MoneyInfoResponse(errorCode, "", 0L);
        HttpServletRequest request = (HttpServletRequest)param.get();
        long time = System.currentTimeMillis();
        String ip = this.getIpAddress(request);
        try {
            String command = request.getParameter("c");
            String nickname = request.getParameter("nn");
            String accessToken = request.getParameter("at");
            String merchantId = request.getParameter("mid");
            String checkSum = request.getParameter("cks");
            logger.debug((Object)("Request GetMoneyInfo id: " + time + ", command: " + command + ", nickname: " + nickname + ", accessToken: " + accessToken + ", merchantId: " + merchantId + ", checkSum: " + checkSum + ", ip: " + ip));
            if (command != null && nickname != null && accessToken != null && merchantId != null && checkSum != null) {
                if (PayUtils.checkMerchantId((String)merchantId)) {
                    String merchantKey = PayUtils.getMerchantKey((String)merchantId);
                    String value = String.valueOf(command) + nickname + accessToken + merchantId + merchantKey;
                    String ck = VinPlayUtils.getMD5Hash((String)value);
                    if (ck.equals(checkSum)) {
                        IMap userMap = HazelcastUtils.getUserMap((String)nickname);
                        IMap activeMap = HazelcastUtils.getActiveMap((String)nickname);
                        IMap moneyMap = HazelcastUtils.getMoneyMap((String)nickname);
                        if (userMap.containsKey((Object)nickname) && activeMap.containsKey((Object)nickname) && moneyMap.containsKey((Object)nickname)) {
                            UserCacheModel user = (UserCacheModel)userMap.get((Object)nickname);
                            res.setNickname(nickname);
                            if (user.getAccessToken().equals(accessToken)) {
                                if (!VinPlayUtils.sessionTimeout((long)((UserActiveModel)activeMap.get((Object)nickname)).getLastActive())) {
                                    res.setCurrentMoney(((UserMoneyModel)moneyMap.get((Object)nickname)).getVin());
                                    errorCode = 0;
                                } else {
                                    errorCode = 7;
                                }
                            } else {
                                errorCode = 6;
                            }
                        } else {
                            UserServiceImpl ser = new UserServiceImpl();
                            errorCode = ser.checkNickname(nickname) ? 7 : 5;
                        }
                    } else {
                        errorCode = 3;
                    }
                } else {
                    errorCode = 2;
                }
            } else {
                errorCode = 4;
            }
        }
        catch (Exception e) {
            logger.error((Object)e);
        }
        res.setErrorCode(errorCode);
        logger.debug((Object)("Response GetMoneyInfo id: " + time + ", res: " + res.toJson()));
        return res.toJson();
    }

    private String getIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }
}

