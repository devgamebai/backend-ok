/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;

public class LoginResponse
extends BaseResponseModel {
    private String sessionKey;
    private String accessToken;

    public LoginResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public LoginResponse(boolean success, String errorCode, String sessionKey, String accessToken) {
        super(success, errorCode);
        this.sessionKey = sessionKey;
        this.accessToken = accessToken;
    }

    public String getSessionKey() {
        return this.sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public String getAccessToken() {
        return this.accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}

