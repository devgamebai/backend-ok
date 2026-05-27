/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.dal.entities.taixiu;

import java.io.Serializable;

public class ResultTaiXiu
        implements Serializable {
    private static final long serialVersionUID = 1L;
    public long referenceId;
    public int result;
    public int dice1;
    public int dice2;
    public int dice3;
    public long totalTai = 0L;
    public long totalXiu = 0L;
    public int numBetTai = 0;
    public int numBetXiu = 0;
    public long totalPrize = 0L;
    public long totalRefundTai = 0L;
    public long totalRefundXiu = 0L;
    public long totalRevenue = 0L;
    public int moneyType;
    public String timestamp;
    public long totalJp = 0L;
    //
    public String md5 = "";
    public String before_md5 = "";

    public ResultTaiXiu() {
    }

    public ResultTaiXiu(long referenceId, int result, int dice1, int dice2, int dice3, int moneyType) {
        this.referenceId = referenceId;
        this.result = result;
        this.dice1 = dice1;
        this.dice2 = dice2;
        this.dice3 = dice3;
        this.moneyType = moneyType;
    }

    public ResultTaiXiu(long referenceId, int result, int dice1, int dice2, int dice3, long totalTai, long totalXiu, int numBetTai, int numBetXiu, long totalPrize, long totalRefundTai, long totalRefundXiu, long totalRevenue, int moneyType, String timestamp) {
        this.referenceId = referenceId;
        this.result = result;
        this.dice1 = dice1;
        this.dice2 = dice2;
        this.dice3 = dice3;
        this.totalTai = totalTai;
        this.totalXiu = totalXiu;
        this.numBetTai = numBetTai;
        this.numBetXiu = numBetXiu;
        this.totalPrize = totalPrize;
        this.totalRefundTai = totalRefundTai;
        this.totalRefundXiu = totalRefundXiu;
        this.totalRevenue = totalRevenue;
        this.moneyType = moneyType;
        this.timestamp = timestamp;
    }
}

