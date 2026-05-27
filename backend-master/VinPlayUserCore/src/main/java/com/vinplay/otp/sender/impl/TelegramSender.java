/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.entities.OTPSenderResponse
 *  com.vinplay.usercore.utils.TelegramUtils
 *  com.vinplay.vbee.common.models.UserModel
 *  org.apache.log4j.Logger
 */
package com.vinplay.otp.sender.impl;

import com.vinplay.otp.sender.OTPSenderService;
import com.vinplay.usercore.entities.OTPSenderResponse;
import com.vinplay.usercore.utils.TelegramUtils;
import com.vinplay.vbee.common.models.UserModel;
import org.apache.log4j.Logger;

public class TelegramSender
implements OTPSenderService {
    private static final Logger logger = Logger.getLogger((String)"otp");

    @Override
    public OTPSenderService.Sender name() {
        return OTPSenderService.Sender.TELEGRAM;
    }

    @Override
    public OTPSenderResponse send(UserModel userModel, String number, String message) throws Exception {
        OTPSenderResponse res = new OTPSenderResponse(false);
        if (userModel.getTeleId() == null || userModel.getTeleId().isEmpty()) {
            res.setMessage("Not found telegram ID");
            return res;
        }
        try {
            int code = TelegramUtils.postRequest((String)userModel.getTeleId(), (String)message);
            if (code == 200) {
                res.setSuccess(true);
            } else {
                res.setMessage("Unknown Error");
            }
        }
        catch (Exception e) {
            logger.debug(e);
            res.setMessage(e.getMessage());
        }
        return res;
    }
}

