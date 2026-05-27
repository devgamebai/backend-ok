/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.common.game3rd;

import com.vinplay.common.game3rd.AGGameRecordItem;
import java.util.List;

public class AgResponse {
    private long TotalTrans;
    private long TotalMoney;
    private long TotalSuccess;
    private List<AGGameRecordItem> ListTrans;

    public long getTotalTrans() {
        return this.TotalTrans;
    }

    public void setTotalTrans(long totalTrans) {
        this.TotalTrans = totalTrans;
    }

    public long getTotalMoney() {
        return this.TotalMoney;
    }

    public void setTotalMoney(long totalMoney) {
        this.TotalMoney = totalMoney;
    }

    public long getTotalSuccess() {
        return this.TotalSuccess;
    }

    public void setTotalSuccess(long totalSuccess) {
        this.TotalSuccess = totalSuccess;
    }

    public List<AGGameRecordItem> getListTrans() {
        return this.ListTrans;
    }

    public void setListTrans(List<AGGameRecordItem> listTrans) {
        this.ListTrans = listTrans;
    }

    public AgResponse(long totalTrans, long totalMoney, long totalSuccess, List<AGGameRecordItem> listTrans) {
        this.TotalTrans = totalTrans;
        this.TotalMoney = totalMoney;
        this.TotalSuccess = totalSuccess;
        this.ListTrans = listTrans;
    }

    public AgResponse() {
    }
}

