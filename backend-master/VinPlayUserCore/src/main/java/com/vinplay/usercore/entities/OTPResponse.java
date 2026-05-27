/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.usercore.entities;

public class OTPResponse {
    private boolean success;
    private String message;
    private String sender;
    private String number;

    public OTPResponse(boolean success, String otp, String message) {
        this.success = success;
        this.message = message;
    }

    public OTPResponse(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return this.success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSender() {
        return this.sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getNumber() {
        return this.number;
    }

    public void setNumber(String number) {
        this.number = number;
    }
}

