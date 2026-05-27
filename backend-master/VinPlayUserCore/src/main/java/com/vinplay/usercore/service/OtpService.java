/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.exceptions.KeyNotFoundException
 *  com.vinplay.vbee.common.messages.OtpMessage
 */
package com.vinplay.usercore.service;

import com.vinplay.usercore.entities.MessageMTResponse;
import com.vinplay.vbee.common.exceptions.KeyNotFoundException;
import com.vinplay.vbee.common.messages.OtpMessage;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.concurrent.TimeoutException;

public interface OtpService {
    public MessageMTResponse genMessageMT(OtpMessage var1, String var2) throws Exception;

    public boolean logOTP(OtpMessage var1) throws IOException, TimeoutException, InterruptedException;

    public boolean updateOtp(String var1, String var2, String var3) throws SQLException;

    public int checkOtp(String var1, String var2, String var3, String var4) throws SQLException, UnsupportedEncodingException, NoSuchAlgorithmException, KeyNotFoundException;

    public int checkOtpLogin(String var1, String var2, String var3, String var4, boolean var5) throws Exception;

    public String revertMobile(String var1);

    public int getOdp(String var1) throws Exception;

    public int getOdp(String var1, String var2) throws Exception;

    public int checkOdp(String var1, String var2) throws Exception;

    public int checkOtpSmsForApp(String var1, String var2) throws Exception;

    public int checkOtpEsms(String var1, String var2) throws Exception;

    public int getEsmsOTP(String var1, String var2, String var3) throws Exception;

    public int checkAppOTP(String var1, String var2);
}

