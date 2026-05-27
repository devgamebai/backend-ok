/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.GiftCodeAgentResponse;

public class ResultGiftCodeAgentResponse
extends BaseResponseModel {
    private GiftCodeAgentResponse transactions;

    public ResultGiftCodeAgentResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public GiftCodeAgentResponse getTransactions() {
        return this.transactions;
    }

    public void setTransactions(GiftCodeAgentResponse transactions) {
        this.transactions = transactions;
    }
}

