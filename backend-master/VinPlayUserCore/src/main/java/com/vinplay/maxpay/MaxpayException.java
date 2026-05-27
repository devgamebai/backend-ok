/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.maxpay;

public class MaxpayException
extends Exception {
    private static final long serialVersionUID = 1L;
    private String message;
    private int errorCode;

    public MaxpayException(String message, int errorCode) {
        this.message = message;
        this.errorCode = errorCode;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getErrorCode() {
        return this.errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }
}

