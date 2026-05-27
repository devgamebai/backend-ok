/*
 * Decompiled with CFR 0.152.
 */
package com.payment.entities;

public class PaymentSummaryEntity {
    private String nickName;
    private long totalDeposit;
    private long totalWithdraw;
    private long depositBank;
    private long depositMomo;
    private long depositCard;
    private long profit;
    private double ratio;

    public PaymentSummaryEntity() {
        this.totalDeposit = 0L;
        this.totalWithdraw = 0L;
        this.depositBank = 0L;
        this.depositMomo = 0L;
        this.depositCard = 0L;
    }

    public PaymentSummaryEntity(String nickName, long totalDeposit, long totalWithdraw) {
        this.nickName = nickName;
        this.totalDeposit = totalDeposit;
        this.totalWithdraw = totalWithdraw;
        this.depositBank = 0L;
        this.depositMomo = 0L;
        this.depositCard = 0L;
    }

    public String getNickName() {
        return this.nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public long getTotalDeposit() {
        return this.totalDeposit;
    }

    public void setTotalDeposit(long totalDeposit) {
        this.totalDeposit = totalDeposit;
    }

    public long getTotalWithdraw() {
        return this.totalWithdraw;
    }

    public void setTotalWithdraw(long totalWithdraw) {
        this.totalWithdraw = totalWithdraw;
    }

    public long getDepositBank() {
        return this.depositBank;
    }

    public void setDepositBank(long depositBank) {
        this.depositBank = depositBank;
    }

    public long getDepositMomo() {
        return this.depositMomo;
    }

    public void setDepositMomo(long depositMomo) {
        this.depositMomo = depositMomo;
    }

    public long getDepositCard() {
        return this.depositCard;
    }

    public void setDepositCard(long depositCard) {
        this.depositCard = depositCard;
    }

    public long getProfit() {
        return this.profit;
    }

    public void setProfit(long profit) {
        this.profit = profit;
    }

    public double getRatio() {
        return this.ratio;
    }

    public void setRatio(double ratio) {
        this.ratio = ratio;
    }
}

