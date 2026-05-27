/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response.recharge;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.recharge.RechargeByNLResponse;
import java.util.ArrayList;
import java.util.List;

public class ExportDataRechargeByNLResponse
extends BaseResponseModel {
    private List<RechargeByNLResponse> transactions = new ArrayList<RechargeByNLResponse>();

    public ExportDataRechargeByNLResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<RechargeByNLResponse> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<RechargeByNLResponse> transactions) {
        this.transactions = transactions;
    }
}

