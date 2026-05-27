/*
 * Decompiled with CFR 0.152.
 */
package com.payment.response;

public class BankInfo {
    private String qrUrl;
    private String qr;
    private String bankType;
    private String note;
    private int amount;
    private String bankAccount;
    private String bankNo;

    public String getQrUrl() {
        return this.qrUrl;
    }

    public void setQrUrl(String qrUrl) {
        this.qrUrl = qrUrl;
    }

    public String getQr() {
        return this.qr;
    }

    public void setQr(String qr) {
        this.qr = qr;
    }

    public String getBankType() {
        return this.bankType;
    }

    public void setBankType(String bankType) {
        this.bankType = bankType;
    }

    public String getNote() {
        return this.note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public int getAmount() {
        return this.amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getBankAccount() {
        return this.bankAccount;
    }

    public void setBankAccount(String bankAccount) {
        this.bankAccount = bankAccount;
    }

    public String getBankNo() {
        return this.bankNo;
    }

    public void setBankNo(String bankNo) {
        this.bankNo = bankNo;
    }
}

