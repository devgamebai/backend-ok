/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.utils.GameCommon
 *  com.vinplay.vbee.common.messages.OtpMessage
 *  com.vinplay.vbee.common.utils.UserValidaton
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.otp;

import com.vinplay.api.otp.SendMessageMT;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.messages.OtpMessage;
import com.vinplay.vbee.common.utils.UserValidaton;
import java.util.Timer;
import java.util.TimerTask;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import org.apache.log4j.Logger;

@WebService(name="MOReceiver", targetNamespace="http://api.vinplay.com/")
@SOAPBinding(style=SOAPBinding.Style.RPC)
public class MOReceiver {
    private static final Logger logger = Logger.getLogger((String)"api");

    @WebMethod
    public String messageReceiver(@WebParam(name="userID") String userId, @WebParam(name="serviceID") String serviceId, @WebParam(name="commandCode") String commandCode, @WebParam(name="message") String message, @WebParam(name="requestId") String requestId) {
        logger.debug(("OTP message receiver: \n userId: " + userId + "\n serviceId: " + serviceId + "\n commandCode: " + commandCode + "\n message: " + message + "\n requestId: " + requestId));
        if (commandCode != null) {
            commandCode = commandCode.trim().toUpperCase();
        }
        if (message != null) {
            message = message.trim().toUpperCase();
        }
        if (userId != null && serviceId != null && commandCode != null && message != null && requestId != null && UserValidaton.validateMobile((String)userId) && serviceId.equals("8041") && commandCode.equals("VIN") && !requestId.isEmpty()) {
            OtpMessage messageMO = new OtpMessage(requestId, userId, commandCode, message);
            messageMO.setResponseMO("1");
            Timer timer = new Timer();
            timer.schedule((TimerTask)new SendMessageMT(messageMO), GameCommon.OTP_DELAY_SEND_MT);
            logger.debug("OTP message receiverMO: 1");
            return "1";
        }
        logger.debug("OTP message receiverMO response: -1");
        return "-1";
    }
}

