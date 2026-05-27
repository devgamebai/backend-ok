/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;

public class SendMailResponse
extends BaseResponseModel {
    private String nickName;

    public SendMailResponse(boolean success, String errorCode, String nickname) {
        super(success, errorCode);
        this.nickName = nickname;
    }

    public String getNickName() {
        return this.nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }
}

