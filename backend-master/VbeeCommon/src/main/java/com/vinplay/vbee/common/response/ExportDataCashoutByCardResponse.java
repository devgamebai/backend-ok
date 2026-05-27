/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.CashOutByCardResponse;
import java.util.List;

public class ExportDataCashoutByCardResponse
extends BaseResponseModel {
    private List<CashOutByCardResponse> transactions;

    public ExportDataCashoutByCardResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<CashOutByCardResponse> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<CashOutByCardResponse> transactions) {
        this.transactions = transactions;
    }
}

