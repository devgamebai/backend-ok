/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response.marketing;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.marketing.MediumObj;
import java.util.ArrayList;
import java.util.List;

public class MediumSelectResponse
extends BaseResponseModel {
    private List<MediumObj> transactions = new ArrayList<MediumObj>();

    public MediumSelectResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<MediumObj> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<MediumObj> transactions) {
        this.transactions = transactions;
    }
}

