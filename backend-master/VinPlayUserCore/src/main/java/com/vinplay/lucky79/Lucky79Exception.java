/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.lucky79;

public class Lucky79Exception
extends Exception {
    private static final long serialVersionUID = 1L;
    private String message;
    private int errorCode;

    public Lucky79Exception(String message, int errorCode) {
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

