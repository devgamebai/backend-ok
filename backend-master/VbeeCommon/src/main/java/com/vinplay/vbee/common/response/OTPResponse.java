/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;

public class OTPResponse
extends BaseResponseModel {
    private String otp;

    public OTPResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public String getOtp() {
        return this.otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }
}

