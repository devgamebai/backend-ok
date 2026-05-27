/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.usercore.entities;

public class MessageMTResponse {
    private boolean success;
    private String otp;
    private String message;

    public MessageMTResponse(boolean success, String otp, String message) {
        this.success = success;
        this.otp = otp;
        this.message = message;
    }

    public boolean isSuccess() {
        return this.success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getOtp() {
        return this.otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

