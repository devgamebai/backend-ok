/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.usercore.utils.GameCommon
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.enums.StatusGames
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.response.BaseResponseModel
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors.security;

import com.vinplay.api.utils.PortalUtils;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.enums.StatusGames;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.response.BaseResponseModel;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class ForgetPasswordProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        BaseResponseModel res = new BaseResponseModel(false, "1001");
        String username = request.getParameter("un");
        String captcha = request.getParameter("cp");
        String captchaId = request.getParameter("cid");
        if (username != null && captcha != null && captchaId != null) {
            try {
                int statusGame = GameCommon.getValueInt((String)"STATUS_GAME");
                if (statusGame == StatusGames.MAINTAIN.getId()) {
                    res.setErrorCode("1114");
                    logger.debug(("Response login: " + res.toJson()));
                    return res.toJson();
                }
                if (PortalUtils.checkCaptcha(captcha, captchaId)) {
                    UserServiceImpl userService = new UserServiceImpl();
                    UserModel userModel = userService.getUserByUserName(username);
                    if (userModel != null) {
                        if (statusGame == StatusGames.SANDBOX.getId() && !userModel.isCanLoginSandbox()) {
                            res.setErrorCode("1114");
                            logger.debug(("Response login: " + res.toJson()));
                            return res.toJson();
                        }
                        if (userModel.getNickname() != null && !userModel.getNickname().isEmpty()) {
                            if (userModel.getMobile() != null && !userModel.getMobile().isEmpty() && userModel.isHasMobileSecurity() && !userModel.isHasEmailSecurity()) {
                                res.setErrorCode("1023");
                            } else if (!userModel.isHasMobileSecurity() && userModel.getEmail() != null && !userModel.getEmail().isEmpty() && userModel.isHasEmailSecurity()) {
                                res.setErrorCode("1026");
                            } else if (userModel.getMobile() != null && !userModel.getMobile().isEmpty() && userModel.isHasMobileSecurity() && userModel.getEmail() != null && !userModel.getEmail().isEmpty() && userModel.isHasEmailSecurity()) {
                                res.setErrorCode("1027");
                            } else {
                                res.setErrorCode("1022");
                            }
                        } else {
                            res.setErrorCode("2001");
                        }
                    } else {
                        res.setErrorCode("1005");
                    }
                } else {
                    res.setErrorCode("115");
                }
            }
            catch (Exception e) {
                logger.debug(e);
            }
        }
        return res.toJson();
    }
}

