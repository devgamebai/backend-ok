/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.exceptions.KeyNotFoundException
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.OtpMessage
 *  com.vinplay.vbee.common.models.OtpModel
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.rmq.RMQApi
 *  com.vinplay.vbee.common.statics.TimeBasedOneTimePasswordUtil
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.usercore.service.impl;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dichvuthe.service.impl.AlertServiceImpl;
import com.vinplay.usercore.dao.impl.OtpDaoImpl;
import com.vinplay.usercore.dao.impl.UserDaoImpl;
import com.vinplay.usercore.entities.MessageMTResponse;
import com.vinplay.usercore.service.OtpService;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.exceptions.KeyNotFoundException;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.OtpMessage;
import com.vinplay.vbee.common.models.OtpModel;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.statics.TimeBasedOneTimePasswordUtil;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Date;
import java.util.concurrent.TimeoutException;
import org.apache.log4j.Logger;

public class OtpServiceImpl
implements OtpService {
    private static final Logger logger = Logger.getLogger((String)"user_core");

    @Override
    public MessageMTResponse genMessageMT(OtpMessage message, String mobile) throws Exception {
        UserServiceImpl userSer = new UserServiceImpl();
        boolean success = false;
        String messageMT = "";
        String otp = "";
        if (message.getMessageMO().equals("OZZ OTP")) {
            if (userSer.checkMobile(mobile)) {
                otp = VinPlayUtils.genOtpSMS((String)mobile, (String)message.getCommandCode());
                success = true;
                messageMT = String.format(GameCommon.MESSAGE_OTP_SUCCESS, otp);
            } else {
                messageMT = GameCommon.MESSAGE_ERROR_MOBILE;
            }
        } else if (message.getMessageMO().equals("OZZ APP")) {
            if (userSer.checkMobile(mobile)) {
                otp = VinPlayUtils.genOtpSMS((String)mobile, (String)message.getCommandCode());
                success = true;
                messageMT = String.format(GameCommon.MESSAGE_APP_SUCCESS, otp);
            } else {
                messageMT = GameCommon.MESSAGE_ERROR_MOBILE;
            }
        } else if (message.getMessageMO().equals("OZZ ODP")) {
            if (userSer.checkMobileDaiLy(mobile)) {
                OtpDaoImpl otpDao = new OtpDaoImpl();
                OtpModel otpModel = otpDao.getOtpSMS(mobile, "OZZ ODP");
                if (otpModel != null && VinPlayUtils.compareDate((Date)new Date(), (Date)otpModel.getOtpTime()) == 0) {
                    otp = otpModel.getOtp();
                } else {
                    otp = VinPlayUtils.genOtpSMS((String)mobile, (String)message.getCommandCode());
                    success = true;
                }
                messageMT = String.format(GameCommon.MESSAGE_ODP_SUCCESS, otp, VinPlayUtils.getCurrentDate());
            } else {
                messageMT = GameCommon.MESSAGE_ERROR_MOBILE;
            }
        } else {
            messageMT = GameCommon.MESSAGE_ERROR_SYNTAX;
        }
        MessageMTResponse res = new MessageMTResponse(success, otp, messageMT);
        return res;
    }

    @Override
    public boolean logOTP(OtpMessage message) throws IOException, TimeoutException, InterruptedException {
        // P1 cleanup: queue_otp retired (no consumer); SaveLogOTPProcessor deleted.
        return true;
    }

    @Override
    public boolean updateOtp(String mobile, String otp, String messageMO) throws SQLException {
        OtpDaoImpl dao = new OtpDaoImpl();
        return dao.updateOtpSMS(mobile, otp, messageMO);
    }

    @Override
    public String revertMobile(String mobile) {
        if (mobile.substring(0, 2).equals("84")) {
            return "0" + mobile.substring(2);
        }
        return mobile;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public int checkOtp(String otp, String nickname, String type, String mobile) throws SQLException, UnsupportedEncodingException, NoSuchAlgorithmException, KeyNotFoundException {
        if (GameCommon.getValueStr("OTP_DEFAULT").isEmpty()) {
            int res;
            block24 : {
                res = 3;
                if (this.checkAppOTP(nickname, otp) == 0) {
                    return 0;
                }
                try {
                    if (otp == null || type == null || otp.length() != 5 || !type.equals("0") && !type.equals("1")) break block24;
                    HazelcastInstance client = HazelcastClientFactory.getInstance();
                    IMap<String, UserModel> userMap = client.getMap("users");
                    UserModel model = null;
                    if (userMap.containsKey(nickname)) {
                        model = (UserModel)userMap.get(nickname);
                        UserCacheModel userCacheModel = (UserCacheModel)model;
                    } else {
                        UserDaoImpl userDao = new UserDaoImpl();
                        model = userDao.getUserByNickName(nickname);
                    }
                    if (model == null) break block24;
                    mobile = mobile == null ? model.getMobile() : mobile;
                    if (mobile == null || mobile.isEmpty()) break block24;
                    if (type.equals("0")) {
                        try {
                            OtpDaoImpl dao;
                            OtpModel otpModel;
                            if (userMap.containsKey(nickname)) {
                                 userMap.lock(nickname);
                            }
                            if ((otpModel = (dao = new OtpDaoImpl()).getOtpSMS(mobile, "OZZ OTP")) != null && otpModel.getOtp() != null && otpModel.getOtpTime() != null && otp.equals(otpModel.getOtp())) {
                                if (!VinPlayUtils.checkOtpTimeout((Date)otpModel.getOtpTime())) {
                                    res = 0;
                                    dao.updateOtpSMS(mobile, "", "OZZ OTP");
                                } else {
                                    res = 4;
                                }
                            }
                            break block24;
                        }
                        catch (Exception e) {
                            logger.debug(e);
                            break block24;
                        }
                        finally {
                            if (userMap.containsKey(nickname)) {
                                 userMap.unlock(nickname);
                            }
                        }
                    }
                    if (model.getMobile() == null || model.getMobile().isEmpty() || !model.isHasMobileSecurity() || !userMap.containsKey(nickname)) break block24;
                    try {
                        String otpApp;
                        userMap.lock(nickname);
                        UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                        if ((user.getOtpApp() == null || !user.getOtpApp().equals(otp)) && (otpApp = VinPlayUtils.genOtpApp((String)nickname, (String)mobile)).equals(otp)) {
                            user.setOtpApp(otp);
                            userMap.put(nickname, user);
                            res = 0;
                        }
                    }
                    catch (Exception e) {
                        logger.debug(e);
                    }
                    finally {
                         userMap.unlock(nickname);
                    }
                }
                catch (Exception e2) {
                    logger.debug(e2);
                }
            }
            return res;
        }
        if (otp.equals(GameCommon.getValueStr("OTP_DEFAULT"))) {
            return 0;
        }
        return 3;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public int checkOtpLogin(String otp, String otpType, String nickname, String mobile, boolean appSecure) throws Exception {
        if (GameCommon.getValueStr("OTP_DEFAULT").isEmpty()) {
            int res;
            block17 : {
                res = 3;
                if (this.checkAppOTP(nickname, otp) == 0) {
                    return 0;
                }
                try {
                    if (otp == null || otpType == null || otp.length() != 5 || !otpType.equals("0") && !otpType.equals("1")) break block17;
                    if (otpType.equals("0")) {
                        OtpDaoImpl dao = new OtpDaoImpl();
                        OtpModel model = dao.getOtpSMS(mobile, "OZZ OTP");
                        if (model != null && model.getOtp() != null && model.getOtpTime() != null && otp.equals(model.getOtp())) {
                            if (!VinPlayUtils.checkOtpTimeout((Date)model.getOtpTime())) {
                                res = 0;
                                dao.updateOtpSMS(mobile, "", "OZZ OTP");
                            } else {
                                res = 4;
                            }
                        }
                        break block17;
                    }
                    HazelcastInstance client = HazelcastClientFactory.getInstance();
                    IMap<String, UserModel> userMap = client.getMap("users");
                    if (userMap.containsKey(nickname)) {
                        try {
                            String otpApp;
                             userMap.lock(nickname);
                            UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                            if ((user.getOtpApp() == null || !user.getOtpApp().equals(otp)) && (otpApp = VinPlayUtils.genOtpApp((String)nickname, (String)mobile)).equals(otp)) {
                                user.setOtpApp(otp);
                                userMap.put(nickname, user);
                                res = 0;
                            }
                            break block17;
                        }
                        catch (Exception e) {
                            logger.debug(e);
                            break block17;
                        }
                        finally {
                             userMap.unlock(nickname);
                        }
                    }
                    String otpApp2 = VinPlayUtils.genOtpApp((String)nickname, (String)mobile);
                    if (otpApp2.equals(otp)) {
                        res = 0;
                    }
                }
                catch (Exception e2) {
                    logger.debug(e2);
                }
            }
            return res;
        }
        if (otp.equals(GameCommon.getValueStr("OTP_DEFAULT"))) {
            return 0;
        }
        return 3;
    }

    @Override
    public int getOdp(String nickname) throws Exception {
        int code = 1;
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, UserModel> userMap = client.getMap("users");
        UserModel model = null;
        if (userMap.containsKey(nickname)) {
            model = (UserModel)userMap.get(nickname);
            UserCacheModel userCacheModel = (UserCacheModel)model;
        } else {
            UserDaoImpl dao = new UserDaoImpl();
            model = dao.getUserByNickName(nickname);
        }
        if (model != null) {
            if (model.getDaily() != 0) {
                if (model.getMobile() != null && !model.getMobile().isEmpty() && model.isHasMobileSecurity()) {
                    OtpDaoImpl otpDao = new OtpDaoImpl();
                    OtpModel otpModel = otpDao.getOtpSMS(model.getMobile(), "OZZ ODP");
                    if (otpModel != null && VinPlayUtils.compareDate((Date)new Date(), (Date)otpModel.getOtpTime()) == 0) {
                        code = 5;
                    } else {
                        String otp = VinPlayUtils.genOtpSMS((String)model.getMobile(), (String)"OZZ ODP");
                        otpDao.updateOtpSMS(model.getMobile(), otp, "OZZ ODP");
                        AlertServiceImpl service = new AlertServiceImpl();
//                        String content = String.format(GameCommon.MESSAGE_ODP_SUCCESS, odp, VinPlayUtils.getCurrentDate());
                        service.SendSMSEsms(model.getMobile(), otp);
                        code = 0;
                    }
                } else {
                    code = 4;
                }
            } else {
                code = 3;
            }
        } else {
            code = 2;
        }
        return code;
    }

    @Override
    public int getOdp(String nickname, String type) throws Exception {
        int code = 1;
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, UserModel> userMap = client.getMap("users");
        UserModel model = null;
        if (userMap.containsKey(nickname)) {
            model = (UserModel)userMap.get(nickname);
            UserCacheModel userCacheModel = (UserCacheModel)model;
        } else {
            UserDaoImpl dao = new UserDaoImpl();
            model = dao.getUserByNickName(nickname);
        }
        if (model != null) {
            if (model.getDaily() != 0) {
                if (model.getMobile() != null && !model.getMobile().isEmpty() && model.isHasMobileSecurity()) {
                    OtpDaoImpl otpDao = new OtpDaoImpl();
                    OtpModel otpModel = otpDao.getOtpSMS(model.getMobile(), "OZZ ODP");
                    if (otpModel != null && VinPlayUtils.compareDate((Date)new Date(), (Date)otpModel.getOtpTime()) == 0) {
                        code = 5;
                    } else {
                        String otp = VinPlayUtils.genOtpSMS((String)model.getMobile(), (String)"OZZ ODP");
                        otpDao.updateOtpSMS(model.getMobile(), otp, "OZZ ODP");
                        AlertServiceImpl service = new AlertServiceImpl();
//                        String content = String.format(GameCommon.MESSAGE_ODP_SUCCESS, otp, VinPlayUtils.getCurrentDate());
                        service.SendSMSEsms(model.getMobile(), otp);
                        code = 0;
                    }
                } else {
                    code = 4;
                }
            } else {
                code = 3;
            }
        } else {
            code = 2;
        }
        return code;
    }

    @Override
    public int checkOdp(String nickname, String odp) throws Exception {
        OtpDaoImpl otpDao;
        OtpModel otpModel;
        if (this.checkAppOTP(nickname, odp) == 0) {
            return 0;
        }
        int code = 1;
        if (odp == null || odp.length() != 5) {
            code = 5;
            return code;
        }
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, UserModel> userMap = client.getMap("users");
        UserModel model = null;
        if (userMap.containsKey(nickname)) {
            model = (UserModel)userMap.get(nickname);
            UserCacheModel userCacheModel = (UserCacheModel)model;
        } else {
            UserDaoImpl dao = new UserDaoImpl();
            model = dao.getUserByNickName(nickname);
        }
        code = model != null ? (model.getDaily() != 0 ? (model.getMobile() != null && !model.getMobile().isEmpty() && model.isHasMobileSecurity() ? ((otpModel = (otpDao = new OtpDaoImpl()).getOtpSMS(model.getMobile(), "OZZ ODP")) != null && odp.equals(otpModel.getOtp()) ? (VinPlayUtils.compareDate((Date)new Date(), (Date)otpModel.getOtpTime()) == 0 ? 0 : 6) : 5) : 4) : 3) : 2;
        return code;
    }

    @Override
    public int checkOtpSmsForApp(String nickname, String otp) throws Exception {
        if (GameCommon.getValueStr("OTP_DEFAULT").isEmpty()) {
            int res = 3;
            if (this.checkAppOTP(nickname, otp) == 0) {
                return 0;
            }
            return res;
        }
        if (otp.equals(GameCommon.getValueStr("OTP_DEFAULT"))) {
            return 0;
        }
        return 3;
    }

    @Override
    public int getEsmsOTP(String nickname, String mobile, String type) throws Exception {
        int code = 1;
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, UserModel> userMap = client.getMap("users");
        UserModel model = null;
        if (userMap.containsKey(nickname)) {
            model = (UserModel)userMap.get(nickname);
            UserCacheModel userCacheModel = (UserCacheModel)model;
        } else {
            UserDaoImpl dao = new UserDaoImpl();
            model = dao.getUserByNickName(nickname);
        }
        if (model != null) {
            if (model.getMobile() != null && !model.getMobile().isEmpty() && model.isHasMobileSecurity()) {
                OtpDaoImpl otpDao = new OtpDaoImpl();
                String mobile2 = this.revertMobile(model.getMobile());
                String otp = null;
                try {
                    otp = VinPlayUtils.genOtpSMS((String)model.getMobile(), (String)"");
                }
                catch (Exception e) {
                    logger.debug(e);
                }
                otpDao.updateOtpSMS(model.getMobile(), otp, "OZZ OTP");
                AlertServiceImpl service = new AlertServiceImpl();

//                String content = String.format(GameCommon.MESSAGE_OTP_SUCCESS, otp, VinPlayUtils.getCurrentDate());
                service.SendSMSEsms(mobile2, otp);
                code = 0;
            } else {
                code = 4;
            }
        } else {
            code = 2;
        }
        return code;
    }

    @Override
    public int checkOtpEsms(String nickname, String odp) throws Exception {
        OtpDaoImpl otpDao;
        OtpModel otpModel;
        if (this.checkAppOTP(nickname, odp) == 0) {
            return 0;
        }
        int code = 1;
        if (odp == null || odp.length() != 5) {
            code = 5;
            return code;
        }
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, UserModel> userMap = client.getMap("users");
        UserModel model = null;
        if (userMap.containsKey(nickname)) {
            model = (UserModel)userMap.get(nickname);
            UserCacheModel userCacheModel = (UserCacheModel)model;
        } else {
            UserDaoImpl dao = new UserDaoImpl();
            model = dao.getUserByNickName(nickname);
        }
        code = model != null ? (model.getDaily() != 0 ? (model.getMobile() != null && !model.getMobile().isEmpty() && model.isHasMobileSecurity() ? ((otpModel = (otpDao = new OtpDaoImpl()).getOtpSMS(model.getMobile(), "OZZ ODP")) != null && odp.equals(otpModel.getOtp()) ? (VinPlayUtils.compareDate((Date)new Date(), (Date)otpModel.getOtpTime()) == 0 ? 0 : 6) : 5) : 4) : 3) : 2;
        return code;
    }

    @Override
    public int checkAppOTP(String nickname, String otp) {
        int res = 3;
        if (otp != null && otp.length() == 6) {
            try {
                String Secret = VinPlayUtils.getUserSecretKey((String)nickname);
                int INTOTP = Integer.parseInt(otp);
                if (TimeBasedOneTimePasswordUtil.validateCurrentNumber((String)Secret, (int)INTOTP, (int)0)) {
                    res = 0;
                }
            }
            catch (Exception e) {
                logger.debug(e);
            }
        }
        return res;
    }
}

