/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response.marketing;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.marketing.CampainObj;
import java.util.ArrayList;
import java.util.List;

public class CampainSelectResponse
extends BaseResponseModel {
    private List<CampainObj> transactions = new ArrayList<CampainObj>();

    public CampainSelectResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<CampainObj> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<CampainObj> transactions) {
        this.transactions = transactions;
    }
}

