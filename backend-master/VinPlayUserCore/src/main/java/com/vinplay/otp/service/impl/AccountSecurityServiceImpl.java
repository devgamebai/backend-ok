/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.IMap
 *  org.apache.log4j.Logger
 */
package com.vinplay.otp.service.impl;

import com.hazelcast.core.IMap;
import com.vinplay.otp.dao.OtpV2Dao;
import com.vinplay.otp.dao.impl.OtpV2DaoImpl;
import com.vinplay.otp.sender.MangerSender;
import com.vinplay.otp.sender.OTPSenderService;
import com.vinplay.otp.service.AccountSecurityService;
import com.vinplay.usercore.dao.impl.UserDaoImpl;
import com.vinplay.usercore.entities.OTPResponse;
import com.vinplay.usercore.entities.OTPSenderResponse;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messages.OtpMessage;
import com.vinplay.vbee.common.models.OtpModel;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.utils.StringUtils;
import java.util.Calendar;
import org.apache.log4j.Logger;

public class AccountSecurityServiceImpl
implements AccountSecurityService {
    private static final Logger logger = Logger.getLogger((String)"otp");
    private OtpV2Dao otpDao = new OtpV2DaoImpl();

    private String getOtp() {
        String otp = StringUtils.randomStringNumber(6);
        return otp;
    }

    @Override
    public OTPSenderResponse sendMessageOTP(OtpMessage message) throws Exception {
        OTPSenderResponse res = new OTPSenderResponse(false);
        IMap userMap = HazelcastClientFactory.getInstance().getMap("users");
        UserModel userModel = null;
        if (userMap.containsKey(message.getNickname())) {
            userModel = (UserModel)userMap.get(message.getNickname());
        } else {
            UserDaoImpl userDao = new UserDaoImpl();
            userModel = userDao.getUserByNickName(message.getNickname());
        }
        if (userModel == null) {
            res.setMessage("Not found user");
            return res;
        }
        String mobile = userModel.getMobile();
        if (message.getMobile() != null) {
            mobile = message.getMobile();
        } else if (!message.getMobile().isEmpty() && mobile.contains(message.getMobile())) {
            res.setMessage("Mobile not match in user data!");
            return res;
        }
        if (mobile == null) {
            res.setMessage("OTP not enable!");
            return res;
        }
        OTPSenderService sender = MangerSender.getName(message.getSender());
        if (sender == null) {
            res.setMessage("Not found sender");
            return res;
        }
        String otp = this.getOtp();
        String msg = "OGK: Your OTP code " + otp;
        try {
            int result = this.otpDao.createOtp(mobile, otp, message.getType(), sender.name().toString(), message.getNickname());
            if (result == 1) {
                res = sender.send(userModel, mobile, msg);
                if (!res.isSuccess()) {
                    res.setMessage(res.getMessage());
                    res.setSuccess(false);
                    this.otpDao.finishOtp(mobile);
                } else {
                    res.setOtp(otp);
                    res.setMessage("Send OTP success!");
                    res.setSuccess(true);
                }
                return res;
            }
            if (result == 2) {
                res.setMessage("Otp has sent!");
                res.setSuccess(true);
                return res;
            }
            logger.error("Save otp to database have error!");
            res.setMessage("Server have error!");
            return res;
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.debug(e);
            res.setMessage(e.getMessage());
            return res;
        }
    }

    @Override
    public OTPResponse checkOTPByMobile(String otp, String mobile, boolean odp) throws Exception {
        OTPResponse res = new OTPResponse(false);
        OtpModel otpModel = null;
        if (odp) {
            Calendar aCalendar = Calendar.getInstance();
            aCalendar.add(11, 0);
            otpModel = this.otpDao.getOtp(mobile, otp, aCalendar.getTime());
        } else {
            otpModel = this.otpDao.getOtp(mobile, otp);
        }
        if (otpModel != null) {
            if (otpModel.getOtp().contains(otp)) {
                res.setSuccess(true);
                res.setNumber(otpModel.getMobile());
                this.otpDao.updateOtpCount(otpModel.getMobile(), otpModel.getOtp(), otpModel.getType());
            } else {
                res.setMessage("OTP not match");
            }
        } else {
            res.setMessage("Not found OTP");
        }
        return res;
    }

    @Override
    public OTPResponse checkOTPAndFinish(String otp, String mobile, boolean odp) throws Exception {
        OTPResponse res = this.checkOTPByMobile(otp, mobile, odp);
        if (res.isSuccess()) {
            this.otpDao.finishOtp(mobile);
        }
        return res;
    }
}

