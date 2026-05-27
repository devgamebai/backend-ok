/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vtcpay.service;

import com.vinplay.vtcpay.request.CheckAccountRequest;
import com.vinplay.vtcpay.request.CheckTransRequest;
import com.vinplay.vtcpay.request.TopupRequest;

public interface VTCPayService {
    public String checkTrans(CheckTransRequest var1);

    public String topup(TopupRequest var1);

    public String checkAccount(CheckAccountRequest var1);
}

