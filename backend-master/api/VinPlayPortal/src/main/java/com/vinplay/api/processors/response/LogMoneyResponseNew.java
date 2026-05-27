/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.BaseResponseModel
 *  com.vinplay.vbee.common.response.LogUserMoneyResponse
 */
package com.vinplay.api.processors.response;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.LogUserMoneyResponse;
import java.util.ArrayList;
import java.util.List;

public class LogMoneyResponseNew
extends BaseResponseModel {
    private int totalPages;
    private List<LogUserMoneyResponse> transactions = new ArrayList<LogUserMoneyResponse>();

    public LogMoneyResponseNew(boolean success, String errorCode) {
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

