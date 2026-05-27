/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.api.backend.response;

import com.vinplay.api.backend.response.UserInfoNormalModel;
import com.vinplay.vbee.common.response.BaseResponseModel;

public class UserNormalInfoResponse
extends BaseResponseModel {
    private UserInfoNormalModel user;

    public UserNormalInfoResponse(boolean success, String errorCode, UserInfoNormalModel user) {
        super(success, errorCode);
        this.user = user;
    }

    public UserInfoNormalModel getUser() {
        return this.user;
    }

    public void setUser(UserInfoNormalModel user) {
        this.user = user;
    }
}

