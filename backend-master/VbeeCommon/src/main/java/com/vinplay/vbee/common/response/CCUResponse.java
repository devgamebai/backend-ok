/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.models.LogCCUModel;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.List;

public class CCUResponse
extends BaseResponseModel {
    private List<LogCCUModel> transactions = new ArrayList<LogCCUModel>();

    public CCUResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<LogCCUModel> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<LogCCUModel> transactions) {
        this.transactions = transactions;
    }
}

