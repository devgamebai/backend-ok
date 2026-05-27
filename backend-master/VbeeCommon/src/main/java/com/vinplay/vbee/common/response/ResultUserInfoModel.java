/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.UserInfoModel;
import java.util.ArrayList;
import java.util.List;

public class ResultUserInfoModel
extends BaseResponseModel {
    private List<UserInfoModel> transactions = new ArrayList<UserInfoModel>();
    private String lstPhone;

    public String getLstPhone() {
        return this.lstPhone;
    }

    public void setLstPhone(String lstPhone) {
        this.lstPhone = lstPhone;
    }

    public ResultUserInfoModel(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<UserInfoModel> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<UserInfoModel> transactions) {
        this.transactions = transactions;
    }
}

