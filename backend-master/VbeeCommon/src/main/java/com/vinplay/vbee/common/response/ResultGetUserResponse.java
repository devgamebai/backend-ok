/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.GetUserResponse;

public class ResultGetUserResponse
extends BaseResponseModel {
    private GetUserResponse transactions = new GetUserResponse();

    public ResultGetUserResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public GetUserResponse getTransactions() {
        return this.transactions;
    }

    public void setTransactions(GetUserResponse transactions) {
        this.transactions = transactions;
    }
}

