/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response.sentsms;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.sentsms.LogSentSmsObj;
import java.util.ArrayList;
import java.util.List;

public class ResultLogSentSmsResponse
extends BaseResponseModel {
    private List<LogSentSmsObj> transactions = new ArrayList<LogSentSmsObj>();

    public ResultLogSentSmsResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<LogSentSmsObj> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<LogSentSmsObj> transactions) {
        this.transactions = transactions;
    }
}

