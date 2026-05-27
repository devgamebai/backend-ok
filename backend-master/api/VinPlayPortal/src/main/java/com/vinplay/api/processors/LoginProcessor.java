/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.IMap
 *  com.vinplay.usercore.service.impl.MarketingServiceImpl
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.usercore.utils.GameCommon
 *  com.vinplay.usercore.utils.UserMakertingUtil
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.enums.StatusGames
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.messages.UserMarketingMessage
 *  com.vinplay.vbee.common.models.SocialModel
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.response.LoginResponse
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors;

import com.hazelcast.core.IMap;
import com.vinplay.api.utils.PortalUtils;
import com.vinplay.api.utils.SocialUtils;
import com.vinplay.usercore.service.impl.MarketingServiceImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.usercore.utils.UserMakertingUtil;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.enums.StatusGames;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messages.UserMarketingMessage;
import com.vinplay.vbee.common.models.SocialModel;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.response.LoginResponse;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class LoginProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String username = request.getParameter("un");
        String password = request.getParameter("pw");
        String social = request.getParameter("s");
        String accessToken = request.getParameter("at");
        request.getHeader("user-agent");
        logger.debug(("Request login: username: " + username + ", password: " + password + ", social: " + social + ", accessToken: " + accessToken));
        if (username != null && password != null || social != null && (social.equals("fb") || social.equals("gg")) && accessToken != null) {
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
                    if (userModel == null) {
                        if (statusGame == StatusGames.SANDBOX.getId()) {
                            res.setErrorCode("1114");
                            return res.toJson();
                        }
                        if (userService.insertUserBySocial(socialId, social)) {
                            socialMap.put(socialId, new SocialModel(accessToken, socialId, new Date()));
                            String campaign = request.getParameter("utm_campaign");
                            String medium = request.getParameter("utm_medium");
                            String source = request.getParameter("utm_source");
                            if (campaign != null && medium != null && source != null) {
                                MarketingServiceImpl mktService = new MarketingServiceImpl();
                                UserMarketingMessage message = new UserMarketingMessage(username, "", 0, VinPlayUtils.getCurrentDateMarketing(), campaign, medium, source);
                                mktService.saveUserMarketing(message);
                                UserMakertingUtil.newRegisterUser((String)campaign, (String)medium, (String)source);
                            }
                            res.setErrorCode("2001");
                        }
                    } else {
                        if (statusGame == StatusGames.SANDBOX.getId() && !userModel.isCanLoginSandbox()) {
                            res.setErrorCode("1114");
                            logger.debug(("Response login: " + res.toJson()));
                            return res.toJson();
                        }
                        if (!userModel.isBanLogin()) {
                            if (userModel.getNickname() != null && !userModel.getNickname().trim().isEmpty()) {
                                if (userModel.isHasLoginSecurity() && userModel.getLoginOtp() >= 0L && userModel.getLoginOtp() <= userModel.getVinTotal()) {
                                    res.setErrorCode("1012");
                                } else {
                                    SocialUtils.socialSuccess((IMap<String, SocialModel>)socialMap, socialId, accessToken);
                                    res = PortalUtils.loginSuccess(userModel, request);
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
                                    if (userModel2.isHasLoginSecurity() && userModel2.getLoginOtp() >= 0L && userModel2.getLoginOtp() <= userModel2.getVinTotal()) {
                                        res.setErrorCode("1012");
                                    } else {
                                        res = PortalUtils.loginSuccess(userModel2, request);
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
            logger.debug(("Response login: " + res.toJson()));
            return res.toJson();
        }
        return "MISSING PARAMETTER";
    }
}

