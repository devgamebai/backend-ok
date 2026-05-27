/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.dal.entities.taixiu;

public class TransactionTaiXiu {
    public long referenceId;
    public int userId;
    public String username;
    public long betValue = 0L;
    public int betSide;
    public long totalPrize = 0L;
    public long totalRefund = 0L;
    public int moneyType;
    public String timestamp;
    public String resultPhien;
    public long totalJp = 0L;

    public String before_md5;
    public String md5;

    public TransactionTaiXiu() {
    }

    public TransactionTaiXiu(long referenceId, int userId, String username, long betValue, int betSide, long totalPrize, long totalRefund, String timestamp) {
        this.referenceId = referenceId;
        this.userId = userId;
        this.username = username;
        this.betValue = betValue;
        this.betSide = betSide;
        this.totalPrize = totalPrize;
        this.totalRefund = totalRefund;
        this.timestamp = timestamp;
    }
}

