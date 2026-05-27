/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.response.BaseResponseModel;

public class UserResponse
extends BaseResponseModel {
    private UserModel user;

    public UserResponse(boolean success, String errorCode, UserModel user) {
        super(success, errorCode);
        this.user = user;
    }

    public UserModel getUser() {
        return this.user;
    }

    public void setUser(UserModel user) {
        this.user = user;
    }
}

