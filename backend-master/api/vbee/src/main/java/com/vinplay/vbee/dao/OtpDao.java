/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.messages.BrandnameDLVRMessage
 *  com.vinplay.vbee.common.messages.BrandnameMessage
 *  com.vinplay.vbee.common.messages.OtpMessage
 */
package com.vinplay.vbee.dao;

import com.vinplay.vbee.common.messages.BrandnameDLVRMessage;
import com.vinplay.vbee.common.messages.BrandnameMessage;
import com.vinplay.vbee.common.messages.OtpMessage;

public interface OtpDao {
    public boolean saveLogOtp(OtpMessage var1);

    public boolean saveLogBrandname(BrandnameMessage var1);

    public boolean saveLogBrandnameDLVR(BrandnameDLVRMessage var1);
}

