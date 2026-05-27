/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.dichvuthe.service.impl.AlertServiceImpl
 *  com.vinplay.usercore.dao.impl.SecurityDaoImpl
 *  com.vinplay.usercore.service.impl.OtpServiceImpl
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.usercore.utils.GameCommon
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.enums.StatusGames
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.response.BaseResponseModel
 *  com.vinplay.vbee.common.utils.StringUtils
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors.security;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dichvuthe.service.impl.AlertServiceImpl;
import com.vinplay.usercore.dao.impl.SecurityDaoImpl;
import com.vinplay.usercore.service.impl.OtpServiceImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.enums.StatusGames;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.utils.StringUtils;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class ForgetPasswordOtpProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");

    public String execute(Param<HttpServletRequest> param) {
        BaseResponseModel res;
        block18 : {
            HttpServletRequest request = (HttpServletRequest)param.get();
            res = new BaseResponseModel(false, "1001");
            String username = request.getParameter("un");
            String otp = request.getParameter("otp");
            String type = request.getParameter("type");
            if (username != null && otp != null && type != null) {
                if (!type.equals("0") && !type.equals("1")) {
                    return res.toJson();
                }
                try {
                    int statusGame = GameCommon.getValueInt((String)"STATUS_GAME");
                    if (statusGame == StatusGames.MAINTAIN.getId()) {
                        res.setErrorCode("1114");
                        logger.debug(("Response login: " + res.toJson()));
                        return res.toJson();
                    }
                    UserServiceImpl userService = new UserServiceImpl();
                    UserModel userModel = userService.getUserByUserName(username);
                    if (userModel == null) break block18;
                    if (statusGame == StatusGames.SANDBOX.getId() && !userModel.isCanLoginSandbox()) {
                        res.setErrorCode("1114");
                        logger.debug(("Response login: " + res.toJson()));
                        return res.toJson();
                    }
                    if (userModel.getMobile() == null || userModel.getMobile().isEmpty() || !userModel.isHasMobileSecurity() || userModel.getNickname() == null || userModel.getNickname().isEmpty()) break block18;
                    OtpServiceImpl otpSer = new OtpServiceImpl();
                    int code = otpSer.checkOtp(otp, userModel.getNickname(), type, (String)null);
                    if (code == 0) {
                        String password;
                        block19 : {
                            password = StringUtils.randomString((int)6);
                            SecurityDaoImpl securityDao = new SecurityDaoImpl();
                            if (securityDao.updateUserInfo(userModel.getId(), VinPlayUtils.getMD5Hash((String)password), 2)) {
                                HazelcastInstance client = HazelcastClientFactory.getInstance();
                                IMap userMap = client.getMap("users");
                                if (userMap.containsKey(userModel.getNickname())) {
                                    try {
                                        userMap.lock(userModel.getNickname());
                                        UserCacheModel user = (UserCacheModel)userMap.get(userModel.getNickname());
                                        user.setPassword(VinPlayUtils.getMD5Hash((String)password));
                                        userMap.put(userModel.getNickname(), user);
                                        res.setErrorCode("0");
                                        res.setSuccess(true);
                                    }
                                    catch (Exception e) {
                                        logger.debug(e);
                                        break block19;
                                    }
                                    try {
                                        userMap.unlock(userModel.getNickname());
                                    }
                                    catch (Exception e) {}
                                } else {
                                    res.setErrorCode("0");
                                    res.setSuccess(true);
                                }
                            }
                        }
                        if (res.getErrorCode().equals("0")) {
                            AlertServiceImpl alertSer = new AlertServiceImpl();
                            alertSer.sendSMS2One(userModel.getMobile(), "Mat khau cua ban la " + password + ". Vui long truy cap " + GameCommon.getValueStr((String)"WEB") + " de doi lai mat khau. Tong dai ho tro " + GameCommon.getValueStr((String)"HOT_LINE"), false);
                        }
                        break block18;
                    }
                    if (code == 3) {
                        res.setErrorCode("1008");
                    } else if (code == 4) {
                        res.setErrorCode("1021");
                    }
                }
                catch (Exception e2) {
                    logger.debug(e2);
                }
            }
        }
        return res.toJson();
    }
}

