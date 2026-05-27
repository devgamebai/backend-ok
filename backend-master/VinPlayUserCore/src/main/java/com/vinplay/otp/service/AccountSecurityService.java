/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.entities.OTPResponse
 *  com.vinplay.usercore.entities.OTPSenderResponse
 *  com.vinplay.vbee.common.messages.OtpMessage
 */
package com.vinplay.otp.service;

import com.vinplay.usercore.entities.OTPResponse;
import com.vinplay.usercore.entities.OTPSenderResponse;
import com.vinplay.vbee.common.messages.OtpMessage;

public interface AccountSecurityService {
    public OTPSenderResponse sendMessageOTP(OtpMessage var1) throws Exception;

    public OTPResponse checkOTPByMobile(String var1, String var2, boolean var3) throws Exception;

    public OTPResponse checkOTPAndFinish(String var1, String var2, boolean var3) throws Exception;
}

