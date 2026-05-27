/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.messages.vtcpay.LogVTCPayTopupMessage
 */
package com.vinplay.vtcpay.dao;

import com.vinplay.vbee.common.messages.vtcpay.LogVTCPayTopupMessage;

public interface VTCPayDao {
    public boolean logVTCPayTopup(LogVTCPayTopupMessage var1);

    public LogVTCPayTopupMessage checkTrans(String var1);
}

