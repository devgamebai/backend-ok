/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.BonusFundResponse;
import java.util.ArrayList;
import java.util.List;

public class ListBonusFundResponse
extends BaseResponseModel {
    private List<BonusFundResponse> transactions = new ArrayList<BonusFundResponse>();

    public ListBonusFundResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<BonusFundResponse> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<BonusFundResponse> transactions) {
        this.transactions = transactions;
    }
}

