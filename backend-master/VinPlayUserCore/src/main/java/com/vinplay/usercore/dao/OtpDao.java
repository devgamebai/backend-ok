/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.OtpModel
 */
package com.vinplay.usercore.dao;

import com.vinplay.doisoat.entities.DoisoatVmg;
import com.vinplay.usercore.response.LogSMSOtpResponse;
import com.vinplay.vbee.common.models.OtpModel;
import java.text.ParseException;

public interface OtpDao {
    public boolean updateOtpSMS(String var1, String var2, String var3);

    public OtpModel getOtpSMS(String var1, String var2) throws ParseException;

    public LogSMSOtpResponse getLogSMSOtp(String var1, String var2, String var3, int var4, String var5);

    public DoisoatVmg doisoatVMG(DoisoatVmg var1, String var2, String var3);
}

