/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.models.UserSunReal;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.List;

public class ResultUserSunRealResponse
extends BaseResponseModel {
    private List<UserSunReal> listUser = new ArrayList<UserSunReal>();

    public ResultUserSunRealResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<UserSunReal> getListUser() {
        return this.listUser;
    }

    public void setListUser(List<UserSunReal> listUser) {
        this.listUser = listUser;
    }
}

