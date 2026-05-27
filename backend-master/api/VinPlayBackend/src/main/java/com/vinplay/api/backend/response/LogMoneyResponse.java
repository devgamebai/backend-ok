/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.BaseResponseModel
 *  com.vinplay.vbee.common.response.LogUserMoneyResponse
 */
package com.vinplay.api.backend.response;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.LogUserMoneyResponse;
import java.util.ArrayList;
import java.util.List;

public class LogMoneyResponse
extends BaseResponseModel {
    private int totalPages;
    private List<LogUserMoneyResponse> transactions = new ArrayList<LogUserMoneyResponse>();

    public LogMoneyResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<LogUserMoneyResponse> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<LogUserMoneyResponse> transactions) {
        this.transactions = transactions;
    }

    public int getTotalPages() {
        return this.totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}

