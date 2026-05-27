/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.usercore.dao.impl.SecurityDaoImpl
 *  com.vinplay.usercore.service.impl.OtpServiceImpl
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.usercore.utils.GameCommon
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.enums.StatusGames
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.models.StatusUser
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.response.LoginAppResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.usercore.dao.impl.SecurityDaoImpl;
import com.vinplay.usercore.service.impl.OtpServiceImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.enums.StatusGames;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.StatusUser;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.response.LoginAppResponse;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class LoginAppOtpProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");

    public String execute(Param<HttpServletRequest> param) {
        LoginAppResponse res;
        block20 : {
            HttpServletRequest request = (HttpServletRequest)param.get();
            String username = request.getParameter("un");
            String otp = request.getParameter("otp");
            res = new LoginAppResponse(false, "1001");
            try {
                if (username == null || otp == null || username.isEmpty() || otp.isEmpty()) break block20;
                int statusGame = GameCommon.getValueInt((String)"STATUS_GAME");
                if (statusGame == StatusGames.MAINTAIN.getId()) {
                    res.setErrorCode("1114");
                    logger.debug(("Response login: " + res.toJson()));
                    return res.toJson();
                }
                UserServiceImpl userService = new UserServiceImpl();
                UserModel userModel = userService.getUserByUserName(username);
                if (userModel != null) {
                    if (statusGame == StatusGames.SANDBOX.getId() && !userModel.isCanLoginSandbox()) {
                        res.setErrorCode("1114");
                        logger.debug(("Response login: " + res.toJson()));
                        return res.toJson();
                    }
                    if (!userModel.isBanLogin()) {
                        if (userModel.getMobile() != null && !userModel.getMobile().isEmpty() && userModel.isHasMobileSecurity()) {
                            if (userModel.getNickname() != null && !userModel.getNickname().trim().isEmpty()) {
                                block22 : {
                                    OtpServiceImpl otpSer = new OtpServiceImpl();
                                    int code = otpSer.checkOtpSmsForApp(userModel.getNickname(), otp);
                                    if (code == 0) {
                                        SecurityDaoImpl dao = new SecurityDaoImpl();
                                        HazelcastInstance client = HazelcastClientFactory.getInstance();
                                        IMap userMap = client.getMap("users");
                                        if (userMap.containsKey(userModel.getNickname())) {
                                            block21 : {
                                                try {
                                                    userMap.lock(userModel.getNickname());
                                                    UserCacheModel user = (UserCacheModel)userMap.get(userModel.getNickname());
                                                    int statusNew = StatusUser.changeStatus((int)user.getStatus(), (int)6, (String)"1");
                                                    if (!dao.updateUserInfo(user.getId(), String.valueOf(statusNew), 7)) break block21;
                                                    user.setStatus(statusNew);
                                                    user.setHasAppSecurity(true);
                                                    userMap.put(userModel.getNickname(), user);
                                                    res.setSuccess(true);
                                                    res.setErrorCode("0");
                                                }
                                                catch (Exception e) {
                                                    logger.debug(e);
                                                    break block22;
                                                }
                                            }
                                            try {
                                                userMap.unlock(userModel.getNickname());
                                            }
                                            catch (Exception e) {}
                                        } else {
                                            int statusNew2 = StatusUser.changeStatus((int)userModel.getStatus(), (int)6, (String)"1");
                                            if (dao.updateUserInfo(userModel.getId(), String.valueOf(statusNew2), 7)) {
                                                res.setSuccess(true);
                                                res.setErrorCode("0");
                                            }
                                        }
                                    } else if (code == 3) {
                                        res.setErrorCode("1008");
                                    } else if (code == 4) {
                                        res.setErrorCode("1021");
                                    }
                                }
                                res.setNickname(userModel.getNickname());
                                break block20;
                            }
                            res.setErrorCode("2001");
                            break block20;
                        }
                        res.setErrorCode("1022");
                        break block20;
                    }
                    res.setErrorCode("1109");
                    break block20;
                }
                res.setErrorCode("1005");
            }
            catch (Exception e2) {
                logger.debug(e2);
            }
        }
        return res.toJson();
    }
}

