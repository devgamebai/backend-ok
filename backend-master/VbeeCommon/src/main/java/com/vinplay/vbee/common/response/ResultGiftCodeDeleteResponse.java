/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.GiftCodeDeleteResponse;

public class ResultGiftCodeDeleteResponse
extends BaseResponseModel {
    private GiftCodeDeleteResponse transactions;

    public GiftCodeDeleteResponse getTransactions() {
        return this.transactions;
    }

    public void setTransactions(GiftCodeDeleteResponse transactions) {
        this.transactions = transactions;
    }

    public ResultGiftCodeDeleteResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }
}

