/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.usercore.dao.impl.SecurityDaoImpl
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.usercore.utils.GameCommon
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.enums.StatusGames
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.models.StatusUser
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.response.Set2AFResponse
 *  com.vinplay.vbee.common.statics.TimeBasedOneTimePasswordUtil
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.usercore.dao.impl.SecurityDaoImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.enums.StatusGames;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.StatusUser;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.response.Set2AFResponse;
import com.vinplay.vbee.common.statics.TimeBasedOneTimePasswordUtil;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class Set2AFProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String username = request.getParameter("un");
        String secret = request.getParameter("secret");
        String otp = request.getParameter("otp");
        Set2AFResponse res = new Set2AFResponse(false, "1001");
        try {
            if (username == null) return res.toJson();
            if (username.isEmpty()) return res.toJson();
            int statusGame = GameCommon.getValueInt((String)"STATUS_GAME");
            if (statusGame == StatusGames.MAINTAIN.getId()) {
                res.setErrorCode("1114");
                logger.debug(("Response login: " + res.toJson()));
                return res.toJson();
            }
            UserServiceImpl userService = new UserServiceImpl();
            UserModel userModel = userService.getUserByUserName(username);
            if (userModel == null) {
                res.setErrorCode("1005");
                return res.toJson();
            }
            if (statusGame == StatusGames.SANDBOX.getId() && !userModel.isCanLoginSandbox()) {
                res.setErrorCode("1114");
                logger.debug(("Response login: " + res.toJson()));
                return res.toJson();
            }
            String nick_name = userModel.getNickname();
            if (nick_name != null && !nick_name.trim().isEmpty()) {
                if (secret == null || secret.isEmpty()) {
                    res.setSuccess(true);
                    res.setNickname(nick_name);
                    res.setErrorCode("0");
                    res.setSecret(TimeBasedOneTimePasswordUtil.generateBase32Secret());
                    return res.toJson();
                }
                if (secret != null && !secret.isEmpty() && otp != null && !otp.isEmpty()) {
                    int INTOTP = Integer.parseInt(otp);
                    if (!TimeBasedOneTimePasswordUtil.validateCurrentNumber((String)secret, (int)INTOTP, (int)0)) {
                        res.setErrorCode("3004");
                        return res.toJson();
                    }
                    HazelcastInstance client = HazelcastClientFactory.getInstance();
                    IMap userMap = client.getMap("users");
                    if (!userMap.containsKey(nick_name)) {
                        res.setErrorCode("3002");
                        return res.toJson();
                    }
                    try {
                        userMap.lock(nick_name);
                        UserCacheModel user = (UserCacheModel)userMap.get(nick_name);
                        int statusNew = StatusUser.changeStatus((int)user.getStatus(), (int)4, (String)"1");
                        statusNew = StatusUser.changeStatus((int)statusNew, (int)6, (String)"1");
                        SecurityDaoImpl dao = new SecurityDaoImpl();
                        if (dao.updateUserInfo(user.getId(), String.valueOf(statusNew), 13)) {
                            user.setStatus(statusNew);
                            user.setHasMobileSecurity(true);
                            user.setHasAppSecurity(true);
                            user.setSecurityTime(new Date());
                            userMap.put(nick_name, user);
                            if (VinPlayUtils.setUserSecretKey((String)nick_name, (String)secret)) {
                                res.setSuccess(true);
                                res.setNickname(nick_name);
                                res.setErrorCode("0");
                                res.setSecret(secret);
                                String string = res.toJson();
                                return string;
                            }
                            res.setErrorCode("3003");
                            return res.toJson();
                        }
                        res.setErrorCode("3000");
                        return res.toJson();
                    }
                    catch (Exception e) {
                        res.setErrorCode(e.getMessage());
                        logger.debug(("activeMobile error: " + e.getMessage()));
                        return res.toJson();
                    }
                    finally {
                        userMap.unlock(nick_name);
                    }
                }
                res.setErrorCode("1008");
                return res.toJson();
            }
            res.setErrorCode("2001");
            return res.toJson();
        }
        catch (Exception e2) {
            logger.debug(e2);
        }
        return res.toJson();
    }
}

