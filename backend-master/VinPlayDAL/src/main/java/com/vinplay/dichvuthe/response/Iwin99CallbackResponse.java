/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 */
package com.vinplay.dichvuthe.response;

import com.google.gson.Gson;

public class Iwin99CallbackResponse {
    private int errorCode;
    private String errorDescription;

    public Iwin99CallbackResponse(int errorCode, String errorDescription) {
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
    }

    public int getErrorCode() {
        return this.errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorDescription() {
        return this.errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}

