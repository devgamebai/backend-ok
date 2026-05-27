/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response.recharge;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.RechargeByCardReponse;
import java.util.ArrayList;
import java.util.List;

public class ExportDataRechargeByCardResponse
extends BaseResponseModel {
    private List<RechargeByCardReponse> transactions = new ArrayList<RechargeByCardReponse>();

    public ExportDataRechargeByCardResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<RechargeByCardReponse> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<RechargeByCardReponse> transactions) {
        this.transactions = transactions;
    }
}

