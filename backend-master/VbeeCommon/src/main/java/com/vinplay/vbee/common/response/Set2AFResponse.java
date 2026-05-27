/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;

public class Set2AFResponse
extends BaseResponseModel {
    private String nickname;
    private String Secret;

    public Set2AFResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getSecret() {
        return this.Secret;
    }

    public void setSecret(String Secret) {
        this.Secret = Secret;
    }
}

