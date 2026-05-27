/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response.recharge;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.RechargeBySmsResponse;
import java.util.ArrayList;
import java.util.List;

public class ExportDataRechargeBySmsResponse
extends BaseResponseModel {
    private List<RechargeBySmsResponse> transactions = new ArrayList<RechargeBySmsResponse>();

    public ExportDataRechargeBySmsResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<RechargeBySmsResponse> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<RechargeBySmsResponse> transactions) {
        this.transactions = transactions;
    }
}

