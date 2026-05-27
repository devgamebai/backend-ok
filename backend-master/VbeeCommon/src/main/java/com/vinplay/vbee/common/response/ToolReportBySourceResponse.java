/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.ReportGiftCodeResponse;
import java.util.ArrayList;
import java.util.List;

public class ToolReportBySourceResponse
extends BaseResponseModel {
    private long total;
    private List<ReportGiftCodeResponse> transactions = new ArrayList<ReportGiftCodeResponse>();

    public ToolReportBySourceResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<ReportGiftCodeResponse> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<ReportGiftCodeResponse> transactions) {
        this.transactions = transactions;
    }

    public long getTotal() {
        return this.total;
    }

    public void setTotal(long total) {
        this.total = total;
    }
}

