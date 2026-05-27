/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.entities.MessageMTResponse
 *  com.vinplay.usercore.service.impl.OtpServiceImpl
 *  com.vinplay.vbee.common.messages.OtpMessage
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  com.vinplay.vmq.service.ServiceProvider
 *  com.vinplay.vmq.service.ServiceProviderServiceLocator
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.otp;

import com.vinplay.usercore.entities.MessageMTResponse;
import com.vinplay.usercore.service.impl.OtpServiceImpl;
import com.vinplay.vbee.common.messages.OtpMessage;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import com.vinplay.vmq.service.ServiceProvider;
import com.vinplay.vmq.service.ServiceProviderServiceLocator;
import java.util.TimerTask;
import org.apache.log4j.Logger;

public class SendMessageMT
extends TimerTask {
    private static final Logger logger = Logger.getLogger(TimerTask.class);
    private OtpMessage message;

    public SendMessageMT(OtpMessage message) {
        this.message = message;
    }

    @Override
    public void run() {
        try {
            logger.debug((Object)"OTP sendMT");
            OtpServiceImpl otpService = new OtpServiceImpl();
            if (this.message.getResponseMO().equals("1")) {
                String mobile = otpService.revertMobile(this.message.getMobile());
                MessageMTResponse mtres = null;
                try {
                    mtres = otpService.genMessageMT(this.message, mobile);
                }
                catch (Exception e) {
                    logger.debug((Object)e);
                }
                int responseMT = -2;
                String messageUser = "";
                if (mtres != null) {
                    messageUser = mtres.getMessage();
                    try {
                        ServiceProviderServiceLocator locator = new ServiceProviderServiceLocator();
                        ServiceProvider provider = locator.getsendMT();
                        responseMT = provider.sendMT(this.message.getMobile(), VinPlayUtils.encodeBase64((String)messageUser), "8041", this.message.getCommandCode(), "1", this.message.getRequestId(), "1", "1", "0", "0");
                    }
                    catch (Exception e2) {
                        logger.debug((Object)e2);
                    }
                    logger.debug((Object)("OTP message sendMT: +" + messageUser));
                    if (responseMT == Integer.parseInt("1") && mtres.isSuccess()) {
                        otpService.updateOtp(mobile, mtres.getOtp(), this.message.getMessageMO());
                    }
                } else {
                    messageUser = "generate message mt error";
                }
                logger.debug((Object)("OTP message responseMT: " + responseMT));
                this.message.setMobile(mobile);
                this.message.setMessageMT(messageUser);
                this.message.setResponseMT(String.valueOf(responseMT));
            }
            otpService.logOTP(this.message);
        }
        catch (Exception e3) {
            logger.debug((Object)e3);
        }
    }
}

