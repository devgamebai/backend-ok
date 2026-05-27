/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.payment.model;

import com.vinplay.payment.entities.WithDrawPaygateModel;
import java.util.List;

public class WithDrawHistory {
    private List<WithDrawPaygateModel> list;
    private long totalData;

    public List<WithDrawPaygateModel> getList() {
        return this.list;
    }

    public void setList(List<WithDrawPaygateModel> list) {
        this.list = list;
    }

    public long getTotalData() {
        return this.totalData;
    }

    public void setTotalData(long totalData) {
        this.totalData = totalData;
    }
}

