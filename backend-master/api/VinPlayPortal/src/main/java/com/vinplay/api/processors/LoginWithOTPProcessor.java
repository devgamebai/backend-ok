/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.IMap
 *  com.vinplay.usercore.service.impl.OtpServiceImpl
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.usercore.utils.GameCommon
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.enums.StatusGames
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.models.SocialModel
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.response.LoginResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors;

import com.hazelcast.core.IMap;
import com.vinplay.api.utils.PortalUtils;
import com.vinplay.api.utils.SocialUtils;
import com.vinplay.usercore.service.impl.OtpServiceImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.enums.StatusGames;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.SocialModel;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.response.LoginResponse;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class LoginWithOTPProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String username = request.getParameter("un");
        String password = request.getParameter("pw");
        String otp = request.getParameter("otp");
        String type = request.getParameter("type");
        String social = request.getParameter("s");
        String accessToken = request.getParameter("at");
        logger.debug(("Request loginWithOTP: username: " + username + ", password: " + password + ", social: " + social + ", accessToken: " + accessToken + ", otp: " + otp));
        if ((username != null && password != null || social != null && (social.equals("fb") || social.equals("gg")) && accessToken != null) && otp != null && type != null && (type.equals("1") || type.equals("0"))) {
            LoginResponse res = new LoginResponse(false, "1001");
            try {
                int statusGame = GameCommon.getValueInt((String)"STATUS_GAME");
                if (statusGame == StatusGames.MAINTAIN.getId()) {
                    res.setErrorCode("1114");
                    logger.debug(("Response login: " + res.toJson()));
                    return res.toJson();
                }
                UserServiceImpl userService = new UserServiceImpl();
                if (social != null && (social.equals("fb") || social.equals("gg"))) {
                    String cache = social.equals("fb") ? "cacheFacebook" : "cacheGoogle";
                    IMap socialMap = HazelcastClientFactory.getInstance().getMap(cache);
                    String socialId = SocialUtils.getSocialId((IMap<String, SocialModel>)socialMap, accessToken, social);
                    if (socialId == null) {
                        logger.debug(("Response login: " + res.toJson()));
                        return res.toJson();
                    }
                    if (socialId.isEmpty()) {
                        res.setErrorCode("1009");
                        logger.debug(("Response login: " + res.toJson()));
                        return res.toJson();
                    }
                    UserModel userModel = userService.getUserBySocialId(socialId, social);
                    if (userModel != null) {
                        if (statusGame == StatusGames.SANDBOX.getId() && !userModel.isCanLoginSandbox()) {
                            res.setErrorCode("1114");
                            logger.debug(("Response login: " + res.toJson()));
                            return res.toJson();
                        }
                        if (!userModel.isBanLogin()) {
                            if (userModel.getNickname() != null && !userModel.getNickname().trim().isEmpty()) {
                                if (userModel.getMobile() != null && !userModel.getMobile().isEmpty() && userModel.isHasMobileSecurity()) {
                                    OtpServiceImpl ser = new OtpServiceImpl();
                                    int code = ser.checkOtpLogin(otp, type, userModel.getNickname(), userModel.getMobile(), userModel.isHasAppSecurity());
                                    if (code == 0) {
                                        SocialUtils.socialSuccess((IMap<String, SocialModel>)socialMap, socialId, accessToken);
                                        res = PortalUtils.loginSuccess(userModel, request);
                                    } else if (code == 4) {
                                        res.setErrorCode("1021");
                                    } else {
                                        res.setErrorCode("1008");
                                    }
                                }
                            } else {
                                res.setErrorCode("2001");
                            }
                        } else {
                            res.setErrorCode("1109");
                        }
                    }
                } else {
                    UserModel userModel2 = userService.getUserByUserName(username);
                    if (userModel2 != null) {
                        if (statusGame == StatusGames.SANDBOX.getId() && !userModel2.isCanLoginSandbox()) {
                            res.setErrorCode("1114");
                            logger.debug(("Response login: " + res.toJson()));
                            return res.toJson();
                        }
                        if (!userModel2.isBanLogin()) {
                            if (userModel2.getPassword().equals(password)) {
                                if (userModel2.getNickname() != null && !userModel2.getNickname().trim().isEmpty()) {
                                    if (userModel2.getMobile() != null && !userModel2.getMobile().isEmpty() && userModel2.isHasMobileSecurity()) {
                                        OtpServiceImpl ser2 = new OtpServiceImpl();
                                        int code2 = ser2.checkOtpLogin(otp, type, userModel2.getNickname(), userModel2.getMobile(), userModel2.isHasAppSecurity());
                                        if (code2 == 0) {
                                            res = PortalUtils.loginSuccess(userModel2, request);
                                        } else if (code2 == 4) {
                                            res.setErrorCode("1021");
                                        } else {
                                            res.setErrorCode("1008");
                                        }
                                    }
                                } else {
                                    res.setErrorCode("2001");
                                }
                            } else {
                                res.setErrorCode("1007");
                            }
                        } else {
                            res.setErrorCode("1109");
                        }
                    } else {
                        res.setErrorCode("1005");
                    }
                }
            }
            catch (Exception e1) {
                logger.debug(e1);
            }
            logger.debug(("Response loginWithOTP: " + res.toJson()));
            return res.toJson();
        }
        return "MISSING PARAMETTER";
    }
}

