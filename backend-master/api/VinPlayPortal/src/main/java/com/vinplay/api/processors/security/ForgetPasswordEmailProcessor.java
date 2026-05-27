/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.dichvuthe.service.impl.AlertServiceImpl
 *  com.vinplay.usercore.dao.impl.SecurityDaoImpl
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
 *  com.vinplay.vbee.common.utils.UserValidaton
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors.security;

import com.google.gson.JsonObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dichvuthe.service.impl.AlertServiceImpl;
import com.vinplay.usercore.dao.impl.SecurityDaoImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.enums.StatusGames;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.utils.DateTimeUtils;
import com.vinplay.vbee.common.utils.BcryptUtils;
import com.vinplay.vbee.common.utils.StringUtils;
import com.vinplay.vbee.common.utils.UserValidaton;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class ForgetPasswordEmailProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");

    public String execute(Param<HttpServletRequest> param) {
        BaseResponseModel res;
        block15 : {
            HttpServletRequest request = (HttpServletRequest)param.get();
            res = new BaseResponseModel(false, "1001");
            String username = request.getParameter("un");
            String email = request.getParameter("email");
            if (username != null && email != null) {
                try {
                    int statusGame = GameCommon.getValueInt((String)"STATUS_GAME");
                    if (statusGame == StatusGames.MAINTAIN.getId()) {
                        res.setErrorCode("1114");
                        logger.debug(("Response login: " + res.toJson()));
                        return res.toJson();
                    }
                    if (UserValidaton.validateEmail((String)email)) {
                        UserServiceImpl userService = new UserServiceImpl();
                        UserModel userModel = userService.getUserByUserName(username);
                        if (userModel == null) break block15;
                        if (statusGame == StatusGames.SANDBOX.getId() && !userModel.isCanLoginSandbox()) {
                            res.setErrorCode("1114");
                            logger.debug(("Response login: " + res.toJson()));
                            return res.toJson();
                        }
                        if (userModel.getEmail() == null || userModel.getEmail().isEmpty() || !userModel.isHasEmailSecurity() || userModel.getNickname() == null || userModel.getNickname().isEmpty()) break block15;
                        if (email.equals(userModel.getEmail())) {
                            String password;
                            block16 : {
                                password = StringUtils.randomString((int)12);
                                String hashedPassword = BcryptUtils.hashPassword(password);
                                SecurityDaoImpl securityDao = new SecurityDaoImpl();
                                if (securityDao.updateUserInfo(userModel.getId(), hashedPassword, 2)) {
                                    HazelcastInstance client = HazelcastClientFactory.getInstance();
                                    IMap userMap = client.getMap("users");
                                    if (userMap.containsKey(userModel.getNickname())) {
                                        try {
                                            userMap.lock(userModel.getNickname());
                                            UserCacheModel user = (UserCacheModel)userMap.get(userModel.getNickname());
                                            user.setPassword(hashedPassword);
                                            userMap.put(userModel.getNickname(), user);
                                            res.setErrorCode("0");
                                            res.setSuccess(true);
                                        }
                                        catch (Exception e) {
                                            logger.debug(e);
                                            break block16;
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
                                ArrayList<String> receives = new ArrayList<String>();
                                receives.add(email);

                                String subject = String.format("[CGame] Reset Password - %s (UTC)", DateTimeUtils.getFormatTime("yyyy-MM-dd HH:mm:ss", new Date()));
                                JsonObject params = new JsonObject();
                                params.addProperty("Password", password);
                                params.addProperty("Logo", String.format("%s/res/common/mail/logo.png?v=1", GameCommon.getValueStr((String)"WEB")));
                                params.addProperty("WEB", GameCommon.getValueStr((String)"WEB"));
                                alertSer.sendEmail(subject, "reset-password", params.toString(), receives);
                            }
                            break block15;
                        }
                        res.setErrorCode("1028");
                        break block15;
                    }
                    res.setErrorCode("103");
                }
                catch (Exception e2) {
                    logger.debug(e2);
                }
            }
        }
        return res.toJson();
    }
}

