/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.BaseResponseModel
 *  com.vinplay.vbee.common.response.LogMoneyUserResponse
 */
package com.vinplay.api.processors.response;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.LogMoneyUserResponse;
import java.util.ArrayList;
import java.util.List;

public class LogMoneyResponse
extends BaseResponseModel {
    private int totalPages;
    private List<LogMoneyUserResponse> transactions = new ArrayList<LogMoneyUserResponse>();

    public LogMoneyResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<LogMoneyUserResponse> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<LogMoneyUserResponse> transactions) {
        this.transactions = transactions;
    }

    public int getTotalPages() {
        return this.totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}

