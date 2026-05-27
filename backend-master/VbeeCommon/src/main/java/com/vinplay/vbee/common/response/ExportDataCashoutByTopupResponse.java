/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.CashOutByTopUpResponse;
import java.util.List;

public class ExportDataCashoutByTopupResponse
extends BaseResponseModel {
    private List<CashOutByTopUpResponse> transactions;

    public ExportDataCashoutByTopupResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<CashOutByTopUpResponse> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<CashOutByTopUpResponse> transactions) {
        this.transactions = transactions;
    }
}

