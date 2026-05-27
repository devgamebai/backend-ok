/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.models.UserLive;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.List;

public class ResultUserLiveResponse
extends BaseResponseModel {
    private List<UserLive> listUser = new ArrayList<UserLive>();

    public ResultUserLiveResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<UserLive> getListUser() {
        return this.listUser;
    }

    public void setListUser(List<UserLive> listUser) {
        this.listUser = listUser;
    }
}

