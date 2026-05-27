/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.otp.service.AccountSecurityService
 *  com.vinplay.otp.service.impl.AccountSecurityServiceImpl
 *  com.vinplay.usercore.dao.impl.UserDaoImpl
 *  com.vinplay.usercore.entities.OTPResponse
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.response.BaseResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.commons.lang3.StringUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors.otp;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.api.processors.common.AuthProcessor;
import com.vinplay.otp.service.AccountSecurityService;
import com.vinplay.otp.service.impl.AccountSecurityServiceImpl;
import com.vinplay.usercore.dao.impl.UserDaoImpl;
import com.vinplay.usercore.entities.OTPResponse;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.response.BaseResponse;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class VerifyRegisterOtpProcessor
extends AuthProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");
    AccountSecurityService service = new AccountSecurityServiceImpl();

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String otp = request.getParameter("otp");
        String mobile = request.getParameter("m");
        if (StringUtils.isBlank((CharSequence)mobile)) {
            return BaseResponse.error((String)"5", (String)"mobile is null or empty");
        }
        UserModel userModel = this.getUser(param);
        if (userModel == null) {
            return notAuth;
        }
        try {
            if (this.userService.checkMobile(mobile)) {
                return BaseResponse.error((String)"5", (String)"S\u1ed1 \u0111i\u1ec7n tho\u1ea1i \u0111\u00e3 \u0111\u01b0\u1ee3c s\u1eed d\u1ee5ng!");
            }
            OTPResponse result = this.service.checkOTPAndFinish(otp, mobile, false);
            if (!result.isSuccess()) {
                return BaseResponse.error((String)"99", (String)result.getMessage());
            }
            UserDaoImpl userDao = new UserDaoImpl();
            boolean checked = userDao.verifyMobile(userModel.getNickname(), mobile, true);
            if (!checked) {
                return BaseResponse.error((String)"99", (String)"C\u00f3 l\u1ed7i x\u1ea3y ra xin hay th\u1eed l\u1ea1i!");
            }
            this.updateCachedSms(userModel.getNickname(), mobile);
            return BaseResponse.success((String)"0", (String)"\u0110\u0103ng k\u00ed s\u1ed1 \u0111i\u1ec7n tho\u1ea1i th\u00e0nh c\u00f4ng!", new HashMap());
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.debug(e);
            return BaseResponse.error((String)"99", (String)"Server Error!");
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void updateCachedSms(String nickName, String mobile) {
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap userMap = client.getMap("users");
        if (userMap.containsKey(nickName)) {
            try {
                userMap.lock(nickName);
                UserCacheModel user = (UserCacheModel)userMap.get(nickName);
                user.setMobile(mobile);
                user.setOtpSender("SMS");
                user.setVerifyMobile(true);
                userMap.put(nickName, user);
            }
            finally {
                userMap.unlock(nickName);
            }
        }
    }
}

