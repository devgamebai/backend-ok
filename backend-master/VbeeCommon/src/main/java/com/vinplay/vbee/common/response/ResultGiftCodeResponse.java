/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.GiftCodeUpdateResponse;

public class ResultGiftCodeResponse
extends BaseResponseModel {
    private GiftCodeUpdateResponse transactions = new GiftCodeUpdateResponse(false, "1001");

    public GiftCodeUpdateResponse getTransactions() {
        return this.transactions;
    }

    public void setTransactions(GiftCodeUpdateResponse transactions) {
        this.transactions = transactions;
    }

    public ResultGiftCodeResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }
}

