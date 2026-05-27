/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.dal.model;

import com.vinplay.dal.entities.log.LogMoneyUserNapTieuVinModel;
import java.util.List;

public class LogMoneyUserVin4Report {
    private long totalData;
    private long totalBet;
    private long totalFee;
    private long totalSoVongcuoc;
    private long totalMoneyExchange;
    private List<LogMoneyUserNapTieuVinModel> list;

    public List<LogMoneyUserNapTieuVinModel> getList() {
        return this.list;
    }

    public void setList(List<LogMoneyUserNapTieuVinModel> list) {
        this.list = list;
    }

    public long getTotalData() {
        return this.totalData;
    }

    public void setTotalData(long totalData) {
        this.totalData = totalData;
    }

    public long getTotalBet() {
        return this.totalBet;
    }

    public void setTotalBet(long totalBet) {
        this.totalBet = totalBet;
    }

    public long getTotalFee() {
        return this.totalFee;
    }

    public void setTotalFee(long totalFee) {
        this.totalFee = totalFee;
    }

    public long getTotalSoVongcuoc() {
        return this.totalSoVongcuoc;
    }

    public void setTotalSoVongcuoc(long totalSoVongcuoc) {
        this.totalSoVongcuoc = totalSoVongcuoc;
    }

    public long getTotalMoneyExchange() {
        return this.totalMoneyExchange;
    }

    public void setTotalMoneyExchange(long totalMoneyExchange) {
        this.totalMoneyExchange = totalMoneyExchange;
    }
}

