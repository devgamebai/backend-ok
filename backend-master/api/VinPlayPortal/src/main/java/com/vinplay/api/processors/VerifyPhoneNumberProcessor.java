/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.response.BaseResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.commons.lang3.StringUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.response.BaseResponse;
import java.sql.SQLException;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class VerifyPhoneNumberProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickName = request.getParameter("nn");
        String phone = request.getParameter("pn");
        String accessToken = request.getParameter("at");
        if (StringUtils.isBlank((CharSequence)nickName)) {
            return BaseResponse.error((String)"5", (String)"nickName is null or empty");
        }
        if (StringUtils.isBlank((CharSequence)accessToken)) {
            return BaseResponse.error((String)"5", (String)"accessToken is null or empty");
        }
        UserServiceImpl userService = new UserServiceImpl();
        boolean isToken = userService.isActiveToken(nickName, accessToken);
        if (!isToken) {
            return BaseResponse.error((String)"4", (String)"Phi\u00ean l\u00e0m vi\u1ec7c c\u1ee7a b\u1ea1n \u0111\u00e3 h\u1ebft h\u1ea1n , vui l\u00f2ng t\u1ea3i l\u1ea1i trang !");
        }
        try {
            if (userService.isXacThucSDT(nickName)) {
                return BaseResponse.error((String)"5", (String)"T\u00e0i kho\u1ea3n \u0111\u00e3 x\u00e1c th\u1ef1c S\u0110T");
            }
        }
        catch (Exception e2) {
            logger.error(e2);
            return BaseResponse.error((String)"5", (String)"T\u00e0i kho\u1ea3n \u0111\u00e3 x\u00e1c th\u1ef1c S\u0110T");
        }
        try {
            if (userService.isPhoneUsed(phone)) {
                return BaseResponse.error((String)"5", (String)"S\u0110T n\u00e0y \u0111\u00e3 \u0111\u01b0\u1ee3c s\u1eed d\u1ee5ng");
            }
            if (!userService.updateVerifyMobile(nickName, phone, true)) {
                return BaseResponse.error((String)"99", (String)"Update data error");
            }
            this.updateCached(nickName, phone);
            return new BaseResponse().success(phone);
        }
        catch (SQLException e1) {
            logger.error(e1);
            return "";
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void updateCached(String nickName, String mobile) {
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap userMap = client.getMap("users");
        if (userMap.containsKey(nickName)) {
            try {
                userMap.lock(nickName);
                UserCacheModel user = (UserCacheModel)userMap.get(nickName);
                user.setMobile(mobile);
                user.setVerifyMobile(true);
                userMap.put(nickName, user);
            }
            finally {
                userMap.unlock(nickName);
            }
        }
    }
}

