/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.GetUserInfoResponse;
import java.util.ArrayList;
import java.util.List;

public class ResultGetUserInfoResponse
extends BaseResponseModel {
    private List<GetUserInfoResponse> transactions = new ArrayList<GetUserInfoResponse>();
    private String lstNickName;

    public String getLstNickName() {
        return this.lstNickName;
    }

    public void setLstNickName(String lstNickName) {
        this.lstNickName = lstNickName;
    }

    public ResultGetUserInfoResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<GetUserInfoResponse> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<GetUserInfoResponse> transactions) {
        this.transactions = transactions;
    }
}

