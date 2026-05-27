/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.dal.entities.report;

public class ReportTotalMoneyModel {
    public long moneyBot;
    public long moneyUser;
    public long moneyAgent1;
    public long moneyAgent2;
    public long moneySuperAgent;
    public long total;
    public String timeLog;

    public ReportTotalMoneyModel(long moneyBot, long moneyUser, long moneyAgent1, long moneyAgent2, long moneySuperAgent, long total, String timeLog) {
        this.moneyBot = moneyBot;
        this.moneyUser = moneyUser;
        this.moneyAgent1 = moneyAgent1;
        this.moneyAgent2 = moneyAgent2;
        this.moneySuperAgent = moneySuperAgent;
        this.total = total;
        this.timeLog = timeLog;
    }

    public ReportTotalMoneyModel() {
    }
}

