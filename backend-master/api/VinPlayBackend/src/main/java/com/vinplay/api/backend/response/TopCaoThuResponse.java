/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.TopCaoThu
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.api.backend.response;

import com.vinplay.vbee.common.models.TopCaoThu;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.List;

public class TopCaoThuResponse
extends BaseResponseModel {
    private String date;
    private List<TopCaoThu> userList;

    public TopCaoThuResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public String getDate() {
        return this.date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public List<TopCaoThu> getUserList() {
        return this.userList;
    }

    public void setUserList(List<TopCaoThu> userList) {
        this.userList = userList;
    }
}

