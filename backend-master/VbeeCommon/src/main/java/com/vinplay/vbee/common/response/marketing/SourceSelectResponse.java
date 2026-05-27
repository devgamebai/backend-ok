/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response.marketing;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.marketing.SourceObj;
import java.util.ArrayList;
import java.util.List;

public class SourceSelectResponse
extends BaseResponseModel {
    private List<SourceObj> transactions = new ArrayList<SourceObj>();

    public SourceSelectResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<SourceObj> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<SourceObj> transactions) {
        this.transactions = transactions;
    }
}

