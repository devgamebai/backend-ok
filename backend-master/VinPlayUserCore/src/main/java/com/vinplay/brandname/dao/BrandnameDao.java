/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.messages.BrandnameDLVRMessage
 */
package com.vinplay.brandname.dao;

import com.vinplay.doisoat.entities.DoisoatBrandname;
import com.vinplay.usercore.response.LogBrandnameResponse;
import com.vinplay.vbee.common.messages.BrandnameDLVRMessage;

public interface BrandnameDao {
    public int getLastRequestId();

    public boolean updateMessageDLVR(BrandnameDLVRMessage var1);

    public LogBrandnameResponse getLogBrandname(String var1, String var2, String var3, String var4, String var5, int var6, String var7);

    public DoisoatBrandname doisoatBrandname(DoisoatBrandname var1, String var2, String var3);
}

