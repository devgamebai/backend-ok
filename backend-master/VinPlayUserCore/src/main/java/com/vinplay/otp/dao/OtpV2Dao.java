/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.OtpModel
 */
package com.vinplay.otp.dao;

import com.vinplay.vbee.common.models.OtpModel;
import java.util.Date;

public interface OtpV2Dao {
    public int createOtp(String var1, String var2, String var3, String var4, String var5) throws Exception;

    public boolean updateOtpCount(String var1, String var2, String var3) throws Exception;

    public boolean finishOtp(String var1) throws Exception;

    public OtpModel getOtpAfterTime(String var1, Date var2) throws Exception;

    public OtpModel getOtp(String var1, String var2) throws Exception;

    public OtpModel getOtp(String var1, String var2, Date var3) throws Exception;

    public OtpModel getOtp(String var1, String var2, Date var3, String var4) throws Exception;
}

